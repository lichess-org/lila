package lila.round

import actorApi.*
import actorApi.round.*
import akka.actor.{ Cancellable, CoordinatedShutdown, Scheduler }
import chess.format.Uci
import chess.{ Black, Centis, Color, MoveMetrics, Speed, White }
import play.api.libs.json.*

import lila.chat.BusChan
import lila.common.{ Bus, IpAddress, Lilakka }
import lila.common.Json.given
import lila.game.{ Event, Game, Pov }
import lila.hub.actorApi.map.{ Exists, Tell, TellAll, TellIfExists, TellMany }
import lila.hub.actorApi.round.{ Abort, Berserk, RematchNo, RematchYes, Resign, TourStanding }
import lila.hub.actorApi.socket.remote.{ TellSriIn, TellSriOut }
import lila.hub.actorApi.tv.TvSelect
import lila.hub.AsyncActorConcMap
import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.socket.RemoteSocket.{ Protocol as P, * }
import lila.socket.{ Socket, SocketVersion, SocketSend }
import reactivemongo.api.Cursor

final class RoundSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    roundDependencies: RoundAsyncActor.Dependencies,
    proxyDependencies: GameProxy.Dependencies,
    scheduleExpiration: ScheduleExpiration,
    messenger: Messenger,
    goneWeightsFor: Game => Fu[(Float, Float)],
    mobileSocket: RoundMobileSocket,
    shutdown: CoordinatedShutdown
)(using ec: Executor, scheduler: Scheduler):

  import RoundSocket.*

  private var stopping = false

  Lilakka.shutdown(shutdown, _.PhaseServiceUnbind, "Stop round socket"): () =>
    stopping = true
    rounds.tellAllWithAck(RoundAsyncActor.LilaStop.apply) map { nb =>
      Lilakka.shutdownLogger.info(s"$nb round asyncActors have stopped")
    }

  def getGame(gameId: GameId): Fu[Option[Game]] =
    rounds.getOrMake(gameId).getGame addEffect { g =>
      if g.isEmpty then finishRound(gameId)
    }
  def getGames(gameIds: List[GameId]): Fu[List[(GameId, Option[Game])]] =
    gameIds
      .map: id =>
        rounds.getOrMake(id).getGame dmap { id -> _ }
      .parallel

  def gameIfPresent(gameId: GameId): Fu[Option[Game]] = rounds.getIfPresent(gameId).so(_.getGame)

  // get the proxied version of the game
  def upgradeIfPresent(game: Game): Fu[Game] =
    rounds.getIfPresent(game.id).fold(fuccess(game))(_.getGame.dmap(_ | game))

  // update the proxied game
  def updateIfPresent(gameId: GameId)(f: Game => Game): Funit =
    rounds.getIfPresent(gameId).so(_ updateGame f)

  val rounds = AsyncActorConcMap[GameId, RoundAsyncActor](
    mkAsyncActor =
      id => makeRoundActor(id, SocketVersion(0), roundDependencies.gameRepo game id recoverDefault none),
    initialCapacity = 65536
  )

  private def makeRoundActor(id: GameId, version: SocketVersion, gameFu: Fu[Option[Game]]) =
    val proxy = GameProxy(id, proxyDependencies, gameFu)
    val roundActor = RoundAsyncActor(
      dependencies = roundDependencies,
      gameId = id,
      socketSend = sendForGameId(id),
      version = version
    )(using ec, proxy)
    terminationDelay schedule id
    gameFu.dforeach:
      _.foreach: game =>
        scheduleExpiration(game)
        goneWeightsFor(game).dforeach: w =>
          roundActor ! RoundAsyncActor.SetGameInfo(game, w)
    roundActor

  private val roundHandler: Handler =
    case Protocol.In.PlayerMove(fullId, uci, blur, lag) if !stopping =>
      rounds.tell(fullId.gameId, HumanPlay(fullId.playerId, uci, blur, lag, none))
    case Protocol.In.PlayerDo(fullId, tpe) if !stopping =>
      def forward(f: GamePlayerId => Any) = rounds.tell(fullId.gameId, f(fullId.playerId))
      tpe match
        case "moretime"     => forward(Moretime(_))
        case "rematch-yes"  => forward(RematchYes(_))
        case "rematch-no"   => forward(RematchNo(_))
        case "takeback-yes" => forward(TakebackYes(_))
        case "takeback-no"  => forward(TakebackNo(_))
        case "draw-yes"     => forward(DrawYes(_))
        case "draw-no"      => forward(DrawNo(_))
        case "draw-claim"   => forward(DrawClaim(_))
        case "resign"       => forward(Resign(_))
        case "resign-force" => forward(ResignForce(_))
        case "draw-force"   => forward(DrawForce(_))
        case "abort"        => forward(Abort(_))
        case "outoftime"    => forward(_ => QuietFlag) // mobile app BC
        case t              => logger.warn(s"Unhandled round socket message: $t")
    case Protocol.In.Flag(gameId, color, fromPlayerId) => rounds.tell(gameId, ClientFlag(color, fromPlayerId))
    case Protocol.In.PlayerChatSay(id, Right(color), msg) =>
      gameIfPresent(id).foreach:
        _.foreach:
          messenger.owner(_, color, msg).unit
    case Protocol.In.PlayerChatSay(id, Left(userId), msg) =>
      messenger.owner(id, userId, msg).unit
    case Protocol.In.WatcherChatSay(id, userId, msg) =>
      messenger.watcher(id, userId, msg).unit
    case RP.In.ChatTimeout(roomId, modId, suspect, reason, text) =>
      messenger.timeout(ChatId(s"$roomId/w"), suspect, reason, text)(using modId).unit
    case Protocol.In.Berserk(gameId, userId) => Bus.publish(Berserk(gameId, userId), "berserk")
    case Protocol.In.PlayerOnlines(onlines) =>
      onlines.foreach:
        case (gameId, Some(on)) =>
          rounds.tell(gameId, on)
          terminationDelay cancel gameId
        case (gameId, _) =>
          if rounds exists gameId then terminationDelay schedule gameId
    case Protocol.In.Bye(fullId) => rounds.tell(fullId.gameId, ByePlayer(fullId.playerId))
    case RP.In.TellRoomSri(_, P.In.TellSri(_, _, tpe, _)) =>
      logger.warn(s"Unhandled round socket message: $tpe")
    case hold: Protocol.In.HoldAlert => rounds.tell(hold.fullId.gameId, hold)
    case r: Protocol.In.SelfReport   => Bus.publish(r, "selfReport")
    case P.In.TellSri(sri, userId, tpe, msg) => // eval cache
      Bus.publish(TellSriIn(sri.value, userId, msg), s"remoteSocketIn:$tpe")
    case RP.In.SetVersions(versions) =>
      preloadRoundsWithVersions(versions)
      send(Protocol.Out.versioningReady)
    case P.In.Ping(id) => send(P.Out.pong(id))
    case Protocol.In.GetGame(reqId, anyId) =>
      for
        game <- rounds.ask[GameAndSocketStatus](anyId.gameId)(GetGameAndSocketStatus.apply)
        data <- mobileSocket.json(game.game, game.socket, anyId)
      yield sendForGameId(anyId.gameId)(Protocol.Out.respond(reqId, data))

    case Protocol.In.WsLatency(millis) => MoveLatMonitor.wsLatency.set(millis)
    case P.In.WsBoot =>
      logger.warn("Remote socket boot")
      // schedule termination for all game asyncActors
      // until players actually reconnect
      rounds foreachKey terminationDelay.schedule
      rounds.tellAll(RoundAsyncActor.WsBoot)

  private def finishRound(gameId: GameId): Unit =
    rounds.terminate(gameId, _ ! RoundAsyncActor.Stop)

  private val send: Sender = remoteSocketApi.makeSender("r-out", parallelism = 16)

  private val sendForGameId: GameId => SocketSend = gameId =>
    SocketSend(msg => send.sticky(gameId.value, msg))

  remoteSocketApi.subscribeRoundRobin("r-in", Protocol.In.reader, parallelism = 16)(
    roundHandler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)

  Bus.subscribeFun("tvSelect", "roundSocket", "tourStanding", "startGame", "finishGame"):
    case TvSelect(gameId, speed, json) =>
      sendForGameId(gameId)(Protocol.Out.tvSelect(gameId, speed, json))
    case Tell(id, e @ BotConnected(color, v)) =>
      val gameId = GameId(id)
      rounds.tell(gameId, e)
      sendForGameId(gameId)(Protocol.Out.botConnected(gameId, color, v))
    case Tell(gameId, msg)          => rounds.tell(GameId(gameId), msg)
    case TellIfExists(gameId, msg)  => rounds.tellIfPresent(GameId(gameId), msg)
    case TellMany(gameIds, msg)     => rounds.tellIds(gameIds.asInstanceOf[Seq[GameId]], msg)
    case TellAll(msg)               => rounds.tellAll(msg)
    case Exists(gameId, promise)    => promise success rounds.exists(GameId(gameId))
    case TourStanding(tourId, json) => send(Protocol.Out.tourStanding(tourId, json))
    case lila.game.actorApi.StartGame(game) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty) foreach { usersPlaying =>
        sendForGameId(game.id)(Protocol.Out.startGame(usersPlaying))
      }
    case lila.game.actorApi.FinishGame(game, _, _) if game.hasClock =>
      game.userIds.some.filter(_.nonEmpty) foreach { usersPlaying =>
        sendForGameId(game.id)(Protocol.Out.finishGame(game.id, game.winnerColor, usersPlaying))
      }

  Bus.subscribeFun(BusChan.Round.chan, BusChan.Global.chan):
    case lila.chat.ChatLine(id, l) =>
      val line = lila.chat.RoundLine(l, id.value endsWith "/w")
      rounds.tellIfPresent(GameId.take(id.value), line)
    case lila.chat.OnTimeout(id, userId) =>
      send:
        RP.Out.tellRoom(GameId take id.value into RoomId, Socket.makeMessage("chat_timeout", userId))
    case lila.chat.OnReinstate(id, userId) =>
      send:
        RP.Out.tellRoom(GameId take id.value into RoomId, Socket.makeMessage("chat_reinstate", userId))

  scheduler.scheduleWithFixedDelay(25 seconds, tickInterval): () =>
    rounds.tellAll(RoundAsyncActor.Tick)

  scheduler.scheduleWithFixedDelay(60 seconds, 60 seconds): () =>
    lila.mon.round.asyncActorCount.update(rounds.size).unit

  private val terminationDelay = TerminationDelay(scheduler, 1 minute, finishRound)

  // on startup we get all ongoing game IDs and versions from lila-ws
  // load them into round actors with batched DB queries
  private def preloadRoundsWithVersions(rooms: Iterable[(String, SocketVersion)]) =
    val bootLog = lila log "boot"

    // load all actors synchronously, giving them game futures from promises we'll fulfill later
    val gamePromises: Map[GameId, Promise[Option[Game]]] = rooms.view.map { (id, version) =>
      val promise = Promise[Option[Game]]()
      val gameId  = GameId(id)
      rounds.loadOrTell(
        gameId,
        load = () => makeRoundActor(gameId, version, promise.future),
        tell = _ ! SetVersion(version)
      )
      gameId -> promise
    }.toMap

    // fulfill the promises with batched DB requests
    rooms
      .map(_._1)
      .grouped(1024)
      .map: ids =>
        roundDependencies.gameRepo
          .byIdsCursor(GameId from ids)
          .foldWhile(Set.empty[GameId])(
            (ids, game) =>
              Cursor.Cont[Set[GameId]]:
                gamePromises.get(game.id).foreach(_ success game.some)
                ids + game.id
            ,
            Cursor.ContOnError { (_, err) => bootLog.error("Can't load round game", err) }
          )
          .recover { case e: Exception =>
            bootLog.error(s"RoundSocket Can't load ${ids.size} round games", e)
            Set.empty
          }
          .chronometer
          .log(bootLog)(loadedIds => s"RoundSocket Loaded ${loadedIds.size}/${ids.size} round games")
          .result
      .parallel
      .map(_.flatten.toSet)
      .andThen:
        case scala.util.Success(loadedIds) =>
          val missingIds = gamePromises.keySet -- loadedIds
          if (missingIds.nonEmpty)
            bootLog.warn:
              s"RoundSocket ${missingIds.size} round games could not be loaded: ${missingIds.take(20) mkString " "}"
            missingIds.foreach: id =>
              gamePromises.get(id).foreach(_ success none)
        case scala.util.Failure(err) =>
          bootLog.error(s"RoundSocket Can't load ${gamePromises.size} round games", err)
      .chronometer
      .log(bootLog)(ids => s"RoundSocket Done loading ${ids.size}/${gamePromises.size} round games")

object RoundSocket:

  val tickSeconds       = 5
  val tickInterval      = tickSeconds.seconds
  val ragequitTimeout   = 10.seconds
  val disconnectTimeout = 40.seconds

  def povDisconnectTimeout(pov: Pov): FiniteDuration =
    disconnectTimeout * {
      pov.game.speed match
        case Speed.Classical => 3
        case Speed.Rapid     => 2
        case _               => 1
    } / {
      import chess.variant.*
      (pov.game.chess.board.materialImbalance, pov.game.variant) match
        case (_, Antichess | Crazyhouse | Horde)                                   => 1
        case (i, _) if (pov.color.white && i <= -4) || (pov.color.black && i >= 4) => 3
        case _                                                                     => 1
    } / {
      if pov.player.hasUser then 1 else 2
    }

  object Protocol:

    object In:

      case class PlayerOnlines(onlines: Iterable[(GameId, Option[RoomCrowd])])                    extends P.In
      case class PlayerDo(fullId: GameFullId, tpe: String)                                        extends P.In
      case class PlayerMove(fullId: GameFullId, uci: Uci, blur: Boolean, lag: MoveMetrics)        extends P.In
      case class PlayerChatSay(gameId: GameId, userIdOrColor: Either[UserId, Color], msg: String) extends P.In
      case class WatcherChatSay(gameId: GameId, userId: UserId, msg: String)                      extends P.In
      case class Bye(fullId: GameFullId)                                                          extends P.In
      case class HoldAlert(fullId: GameFullId, ip: IpAddress, mean: Int, sd: Int)                 extends P.In
      case class Flag(gameId: GameId, color: Color, fromPlayerId: Option[GamePlayerId])           extends P.In
      case class Berserk(gameId: GameId, userId: UserId)                                          extends P.In
      case class SelfReport(fullId: GameFullId, ip: IpAddress, userId: Option[UserId], name: String)
          extends P.In
      case class WsLatency(millis: Int)             extends P.In
      case class GetGame(reqId: Int, id: GameAnyId) extends P.In

      val reader: P.In.Reader = raw =>
        raw.path match
          case "r/ons" =>
            PlayerOnlines:
              P.In.commas(raw.args) map {
                _ splitAt GameId.size match
                  case (gameId, cs) =>
                    (
                      GameId(gameId),
                      cs.nonEmpty option RoomCrowd(cs(0) == '+', cs(1) == '+')
                    )
              }
            .some
          case "r/do" =>
            raw.get(2) { case Array(fullId, payload) =>
              for
                obj <- Json.parse(payload).asOpt[JsObject]
                tpe <- obj str "t"
              yield PlayerDo(GameFullId(fullId), tpe)
            }
          case "r/move" =>
            raw.get(6) { case Array(fullId, uciS, blurS, lagS, mtS, fraS) =>
              Uci(uciS).map: uci =>
                PlayerMove(
                  GameFullId(fullId),
                  uci,
                  P.In.boolean(blurS),
                  MoveMetrics(centis(lagS), centis(mtS), centis(fraS))
                )
            }
          case "chat/say" =>
            raw.get(3) { case Array(roomId, author, msg) =>
              PlayerChatSay(GameId(roomId), readColor(author).toRight(UserId(author)), msg).some
            }
          case "chat/say/w" =>
            raw.get(3) { case Array(roomId, userId, msg) =>
              WatcherChatSay(GameId(roomId), UserId(userId), msg).some
            }
          case "r/berserk" =>
            raw.get(2) { case Array(gameId, userId) =>
              Berserk(GameId(gameId), UserId(userId)).some
            }
          case "r/bye" => Bye(GameFullId(raw.args)).some
          case "r/hold" =>
            raw.get(4) { case Array(fullId, ip, meanS, sdS) =>
              for
                mean <- meanS.toIntOption
                sd   <- sdS.toIntOption
                ip   <- IpAddress.from(ip)
              yield HoldAlert(GameFullId(fullId), ip, mean, sd)
            }
          case "r/report" =>
            raw.get(4) { case Array(fullId, ip, user, name) =>
              IpAddress.from(ip).map { ip =>
                SelfReport(GameFullId(fullId), ip, UserId from P.In.optional(user), name)
              }
            }
          case "r/flag" =>
            raw.get(3) { case Array(gameId, color, playerId) =>
              readColor(color).map:
                Flag(GameId(gameId), _, P.In.optional(playerId) map { GamePlayerId(_) })
            }
          case "r/get" =>
            raw.get(2) { case Array(reqId, anyId) =>
              reqId.toIntOption.map:
                GetGame(_, GameAnyId(anyId))
            }
          case "r/latency" => raw.args.toIntOption map WsLatency.apply
          case _           => RP.In.reader(raw)

      private def centis(s: String): Option[Centis] =
        if (s == "-") none
        else Centis from s.toIntOption

      private def readColor(s: String) =
        if (s == "w") Some(White)
        else if (s == "b") Some(Black)
        else None

    object Out:

      def resyncPlayer(fullId: GameFullId)        = s"r/resync/player $fullId"
      def gone(fullId: GameFullId, gone: Boolean) = s"r/gone $fullId ${P.Out.boolean(gone)}"
      def goneIn(fullId: GameFullId, millis: Long) =
        val seconds = Math.ceil(millis / 1000d / tickSeconds).toInt * tickSeconds
        s"r/goneIn $fullId $seconds"

      def tellVersion(roomId: RoomId, version: SocketVersion, e: Event) =
        val flags = StringBuilder(2)
        if (e.watcher) flags += 's'
        else if (e.owner) flags += 'p'
        else
          e.only.map(_.fold('w', 'b')).orElse {
            e.moveBy.map(_.fold('W', 'B'))
          } foreach flags.+=
        if (e.troll) flags += 't'
        if (flags.isEmpty) flags += '-'
        s"r/ver $roomId $version $flags ${e.typ} ${e.data}"

      def tvSelect(gameId: GameId, speed: chess.Speed, data: JsObject) =
        s"tv/select $gameId ${speed.id} ${Json stringify data}"

      def botConnected(gameId: GameId, color: Color, v: Boolean) =
        s"r/bot/online $gameId ${P.Out.color(color)} ${P.Out.boolean(v)}"

      def tourStanding(tourId: TourId, data: JsValue) =
        s"r/tour/standing $tourId ${Json stringify data}"

      def startGame(users: List[UserId]) = s"r/start ${P.Out.commas(users)}"
      def finishGame(gameId: GameId, winner: Option[Color], users: List[UserId]) =
        s"r/finish $gameId ${P.Out.color(winner)} ${P.Out.commas(users)}"

      def respond(reqId: Int, data: JsObject) = s"req/response $reqId ${Json stringify data}"

      def versioningReady = "r/versioning-ready"

  final private class TerminationDelay(
      scheduler: Scheduler,
      duration: FiniteDuration,
      terminate: GameId => Unit
  )(using Executor):
    import java.util.concurrent.ConcurrentHashMap

    private[this] val terminations = ConcurrentHashMap[GameId, Cancellable](65536)

    def schedule(gameId: GameId): Unit =
      terminations
        .compute(
          gameId,
          (id, canc) =>
            Option(canc).foreach(_.cancel())
            scheduler.scheduleOnce(duration):
              terminations remove id
              terminate(id)
        )
        .unit

    def cancel(gameId: GameId): Unit =
      Option(terminations remove gameId).foreach(_.cancel())
