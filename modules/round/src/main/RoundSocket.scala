package lila.round

import akka.actor.{ ActorSystem, ActorSelection, Scheduler, Cancellable }
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi._
import actorApi.round._
import chess.format.Uci
import chess.{ Color, White, Black, Speed, Centis, MoveMetrics }
import lila.chat.Chat
import lila.common.{ Bus, IpAddress }
import lila.game.Game.{ PlayerId, FullId }
import lila.game.{ Game, Event }
import lila.hub.actorApi.map.{ Tell, TellIfExists, Exists }
import lila.hub.actorApi.round.{ Berserk, RematchYes, RematchNo, Abort, Resign, TourStanding }
import lila.hub.actorApi.socket.remote.TellSriIn
import lila.hub.actorApi.tv.TvSelect
import lila.hub.actorApi.DeployPost
import lila.hub.DuctConcMap
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, SocketVersion, GetVersion, makeMessage }
import lila.user.User

final class RoundSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    roundDependencies: RoundDuct.Dependencies,
    proxyDependencies: GameProxy.Dependencies,
    deployPersistence: DeployPersistence,
    scheduleExpiration: ScheduleExpiration,
    tournamentActor: lila.hub.actors.TournamentApi,
    messenger: Messenger,
    goneWeightsFor: Game => Fu[(Float, Float)]
)(implicit system: ActorSystem) {

  import RoundSocket._

  def getGame(gameId: Game.ID): Fu[Option[Game]] = rounds.getOrMake(gameId).getGame addEffect { g =>
    if (!g.isDefined) finishRound(Game.Id(gameId))
  }

  def gameIfPresent(gameId: Game.ID): Fu[Option[Game]] = rounds.getIfPresent(gameId).??(_.getGame)

  def updateIfPresent(game: Game): Fu[Game] =
    rounds.getIfPresent(game.id).fold(fuccess(game))(_.getGame.map(_ | game))

  val rounds = new DuctConcMap[RoundDuct](
    mkDuct = id => {
      val duct = new RoundDuct(
        dependencies = roundDependencies,
        gameId = id,
        socketSend = send
      )(new GameProxy(id, proxyDependencies))
      terminationDelay schedule Game.Id(id)
      duct.getGame foreach {
        _ foreach { game =>
          scheduleExpiration(game)
          goneWeightsFor(game) map { RoundDuct.SetGameInfo(game, _) } foreach duct.!
        }
      }
      duct
    },
    initialCapacity = 32768
  )

  def tellRound(gameId: Game.Id, msg: Any): Unit = rounds.tell(gameId.value, msg)

  private lazy val roundHandler: Handler = {
    case Protocol.In.PlayerDo(id, tpe, o) => tpe match {
      case "moretime" => tellRound(id.gameId, Moretime(id.playerId))
      case "rematch-yes" => tellRound(id.gameId, RematchYes(id.playerId.value))
      case "rematch-no" => tellRound(id.gameId, RematchNo(id.playerId.value))
      case "takeback-yes" => tellRound(id.gameId, TakebackYes(id.playerId))
      case "takeback-no" => tellRound(id.gameId, TakebackNo(id.playerId))
      case "draw-yes" => tellRound(id.gameId, DrawYes(id.playerId))
      case "draw-no" => tellRound(id.gameId, DrawNo(id.playerId))
      case "draw-claim" => tellRound(id.gameId, DrawClaim(id.playerId))
      case "resign" => tellRound(id.gameId, Resign(id.playerId.value))
      case "resign-force" => tellRound(id.gameId, ResignForce(id.playerId))
      case "draw-force" => tellRound(id.gameId, DrawForce(id.playerId))
      case "abort" => tellRound(id.gameId, Abort(id.playerId.value))
      case "outoftime" => tellRound(id.gameId, QuietFlag) // mobile app BC
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
    case Protocol.In.Flag(gameId, color, fromPlayerId) => tellRound(gameId, ClientFlag(color, fromPlayerId))
    case c: Protocol.In.PlayerChatSay => tellRound(c.gameId, c)
    case Protocol.In.WatcherChatSay(gameId, userId, msg) => messenger.watcher(Chat.Id(gameId.value), userId, msg)
    case RP.In.ChatTimeout(roomId, modId, suspect, reason) =>
      messenger.timeout(Chat.Id(s"$roomId/w"), modId, suspect, reason)
    case Protocol.In.PlayerMove(fullId, uci, blur, lag) =>
      tellRound(fullId.gameId, HumanPlay(fullId.playerId, uci, blur, lag, none))
    case Protocol.In.Berserk(gameId, userId) => tournamentActor ! Berserk(gameId.value, userId)
    case Protocol.In.PlayerOnlines(onlines) => onlines foreach {
      case (gameId, Some(on)) =>
        tellRound(gameId, on)
        terminationDelay cancel gameId
      case (gameId, _) =>
        if (rounds exists gameId.value) terminationDelay schedule gameId
    }
    case Protocol.In.Bye(fullId) => tellRound(fullId.gameId, ByePlayer(fullId.playerId))
    case RP.In.TellRoomSri(gameId, P.In.TellSri(sri, user, tpe, o)) => tpe match {
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
    case hold: Protocol.In.HoldAlert => tellRound(hold.fullId.gameId, hold)
    case r: Protocol.In.SelfReport => Bus.publish(r, "selfReport")
    case userTv: Protocol.In.UserTv => tellRound(userTv.gameId, userTv)
    case P.In.TellSri(sri, userId, tpe, msg) => // eval cache
      Bus.publish(TellSriIn(sri.value, userId, msg), s"remoteSocketIn:$tpe")
    case RP.In.SetVersions(versions) => versions foreach {
      case (roomId, version) => rounds.tell(roomId, SetVersion(version))
    }
    case P.In.WsBoot =>
      logger.warn("Remote socket boot")
      // schedule termination for all game ducts
      // until players actually reconnect
      rounds foreachKey { id => terminationDelay schedule Game.Id(id) }
      rounds.tellAll(DeployPost)
  }

  private def finishRound(gameId: Game.Id): Unit =
    rounds.terminate(gameId.value, _ ! RoundDuct.Stop)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("r-out").apply _

  remoteSocketApi.subscribe("r-in", Protocol.In.reader)(
    roundHandler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)

  Bus.subscribeFun("tvSelect", "roundSocket", "tourStanding") {
    case TvSelect(gameId, speed, json) => send(Protocol.Out.tvSelect(gameId, speed, json))
    case Tell(gameId, BotConnected(color, v)) => send(Protocol.Out.botConnected(gameId, color, v))
    case Tell(gameId, msg) => rounds.tell(gameId, msg)
    case TellIfExists(gameId, msg) => rounds.tellIfPresent(gameId, msg)
    case Exists(gameId, promise) => promise success rounds.exists(gameId)
    case TourStanding(tourId, json) => send(Protocol.Out.tourStanding(tourId, json))
  }

  system.scheduler.scheduleWithFixedDelay(25 seconds, 5 seconds) {
    () => rounds.tellAll(RoundDuct.Tick)
  }

  private val terminationDelay = new TerminationDelay(system.scheduler, 1 minute, finishRound)
}

object RoundSocket {

  val ragequitTimeout = 10.seconds
  val disconnectTimeout = 120.seconds

  def gameDisconnectTimeout(speed: Option[Speed]): FiniteDuration =
    disconnectTimeout * speed.fold(1) {
      case Speed.Classical => 3
      case Speed.Rapid => 2
      case _ => 1
    }

  object Protocol {

    object In {

      case class PlayerOnlines(onlines: Iterable[(Game.Id, Option[RoomCrowd])]) extends P.In
      case class PlayerDo(fullId: FullId, tpe: String, msg: JsObject) extends P.In
      case class PlayerMove(fullId: FullId, uci: Uci, blur: Boolean, lag: MoveMetrics) extends P.In
      case class PlayerChatSay(gameId: Game.Id, userIdOrColor: Either[User.ID, Color], msg: String) extends P.In
      case class WatcherChatSay(gameId: Game.Id, userId: User.ID, msg: String) extends P.In
      case class Bye(fullId: FullId) extends P.In
      case class HoldAlert(fullId: FullId, ip: IpAddress, mean: Int, sd: Int) extends P.In
      case class Flag(gameId: Game.Id, color: Color, fromPlayerId: Option[PlayerId]) extends P.In
      case class Berserk(gameId: Game.Id, userId: User.ID) extends P.In
      case class SelfReport(fullId: FullId, ip: IpAddress, userId: Option[User.ID], name: String) extends P.In
      case class UserTv(gameId: Game.Id, userId: User.ID) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "r/ons" => PlayerOnlines {
          P.In.commas(raw.args) map {
            _ splitAt Game.gameIdSize match {
              case (gameId, cs) => (
                Game.Id(gameId),
                if (cs.isEmpty) None else Some(RoomCrowd(cs(0) == '+', cs(1) == '+'))
              )
            }
          }
        }.some
        case "r/do" => raw.get(2) {
          case Array(fullId, payload) => for {
            obj <- Json.parse(payload).asOpt[JsObject]
            tpe <- obj str "t"
          } yield PlayerDo(FullId(fullId), tpe, obj)
        }
        case "r/move" => raw.get(5) {
          case Array(fullId, uciS, blurS, lagS, mtS) => Uci(uciS) map { uci =>
            PlayerMove(FullId(fullId), uci, P.In.boolean(blurS), MoveMetrics(centis(lagS), centis(mtS)))
          }
        }
        case "chat/say" => raw.get(3) {
          case Array(roomId, author, msg) =>
            PlayerChatSay(Game.Id(roomId), readColor(author).toRight(author), msg).some
        }
        case "chat/say/w" => raw.get(3) {
          case Array(roomId, userId, msg) => WatcherChatSay(Game.Id(roomId), userId, msg).some
        }
        case "r/berserk" => raw.get(2) {
          case Array(gameId, userId) => Berserk(Game.Id(gameId), userId).some
        }
        case "r/bye" => Bye(Game.FullId(raw.args)).some
        case "r/hold" => raw.get(4) {
          case Array(fullId, ip, meanS, sdS) => for {
            mean <- meanS.toIntOption
            sd <- sdS.toIntOption
          } yield HoldAlert(FullId(fullId), IpAddress(ip), mean, sd)
        }
        case "r/report" => raw.get(4) {
          case Array(fullId, ip, user, name) => SelfReport(FullId(fullId), IpAddress(ip), P.In.optional(user), name).some
        }
        case "r/flag" => raw.get(3) {
          case Array(gameId, color, playerId) => readColor(color) map {
            Flag(Game.Id(gameId), _, P.In.optional(playerId) map PlayerId.apply)
          }
        }
        case "r/tv/user" => raw.get(2) {
          case Array(gameId, userId) => UserTv(Game.Id(gameId), userId).some
        }
        case _ => RP.In.reader(raw)
      }

      private def centis(s: String): Option[Centis] =
        if (s == "-") none
        else s.toIntOption map Centis.apply

      private def readColor(s: String) =
        if (s == "w") Some(White)
        else if (s == "b") Some(Black)
        else None
    }

    object Out {

      def resyncPlayer(fullId: FullId) = s"r/resync/player $fullId"
      def gone(fullId: FullId, gone: Boolean) = s"r/gone $fullId ${P.Out.boolean(gone)}"

      def tellVersion(roomId: RoomId, version: SocketVersion, e: Event) = {
        val flags = new StringBuilder(2)
        if (e.watcher) flags += 's'
        else if (e.owner) flags += 'p'
        else e.only.map(_.fold('w', 'b')).orElse {
          e.moveBy.map(_.fold('W', 'B'))
        } foreach flags.+=
        if (e.troll) flags += 't'
        if (flags.isEmpty) flags += '-'
        s"r/ver $roomId $version $flags ${e.typ} ${e.data}"
      }

      def userTvNewGame(gameId: Game.Id, userId: User.ID) =
        s"r/tv/user $gameId $userId"

      def tvSelect(gameId: Game.ID, speed: chess.Speed, data: JsObject) =
        s"tv/select $gameId ${speed.id} ${Json stringify data}"

      def botConnected(gameId: Game.ID, color: Color, v: Boolean) =
        s"r/bot/online $gameId ${P.Out.color(color)} ${P.Out.boolean(v)}"

      def tourStanding(tourId: String, data: JsValue) =
        s"r/tour/standing $tourId ${Json stringify data}"
    }
  }

  private final class TerminationDelay(
      scheduler: Scheduler,
      duration: FiniteDuration,
      terminate: Game.Id => Unit
  ) {
    import java.util.concurrent.ConcurrentHashMap
    import java.util.function.BiFunction

    private[this] val terminations = new ConcurrentHashMap[String, Cancellable](32768)

    def schedule(gameId: Game.Id): Unit = terminations.compute(
      gameId.value,
      new BiFunction[String, Cancellable, Cancellable] {
        def apply(id: String, canc: Cancellable) = {
          Option(canc).foreach(_.cancel)
          scheduler.scheduleOnce(duration) {
            terminations remove id
            terminate(Game.Id(id))
          }
        }
      }
    )

    def cancel(gameId: Game.Id): Unit =
      Option(terminations remove gameId.value).foreach(_.cancel)
  }
}
