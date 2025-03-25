package lila.round

import akka.actor.{ Cancellable, CoordinatedShutdown, Scheduler }
import chess.format.Uci
import chess.{ Black, Centis, Color, MoveMetrics, Speed, White }
import play.api.libs.json.*
import reactivemongo.api.Cursor
import scalalib.actor.AsyncActorConcMap

import lila.chat.BusChan
import lila.common.Json.given
import lila.common.{ Bus, Lilakka }
import lila.core.game.TvSelect
import lila.core.misc.map.{ Exists, Tell, TellAll, TellIfExists, TellMany }
import lila.core.net.IpAddress
import lila.core.round.*
import lila.core.socket.remote.TellSriIn
import lila.core.socket.{ protocol as P, * }
import lila.core.user.FlairGet
import lila.room.RoomSocket.{ Protocol as RP, * }

final class RoundSocket(
    socketKit: ParallelSocketKit,
    putUserLag: userLag.Put,
    roundDependencies: RoundAsyncActor.Dependencies,
    proxyDependencies: GameProxy.Dependencies,
    scheduleExpiration: ScheduleExpiration,
    messenger: Messenger,
    goneWeightsFor: Game => Fu[(Float, Float)],
    mobileSocket: RoundMobile,
    shutdown: CoordinatedShutdown
)(using Executor, FlairGet)(using scheduler: Scheduler):

  import RoundSocket.*

  private var stopping = false

  Lilakka.shutdown(shutdown, _.PhaseServiceUnbind, "Stop round socket"): () =>
    stopping = true
    rounds
      .tellAllWithAck(RoundAsyncActor.LilaStop.apply)
      .map: nb =>
        Lilakka.shutdownLogger.info(s"$nb round asyncActors have stopped")

  def getGame(gameId: GameId): Fu[Option[Game]] =
    rounds
      .getOrMake(gameId)
      .getGame
      .addEffect: g =>
        if g.isEmpty then finishRound(gameId)

  def getGames(gameIds: List[GameId]): Fu[List[(GameId, Option[Game])]] =
    gameIds.parallel: id =>
      rounds.getOrMake(id).getGame.dmap { id -> _ }

  def getMany(gameIds: List[GameId]): Fu[List[GameAndSocketStatus]] =
    gameIds
      .sequentially: id =>
        gameAndStatusIfPresent(id).orElse:
          roundDependencies.gameRepo
            .game(id)
            .map2: g =>
              GameAndSocketStatus(g, SocketStatus.default)
      .map(_.flatten)

  def gameIfPresent(gameId: GameId): Fu[Option[Game]] = rounds.getIfPresent(gameId).so(_.getGame)

  // get the proxied version of the game
  def upgradeIfPresent(game: Game): Fu[Game] =
    rounds.getIfPresent(game.id).fold(fuccess(game))(_.getGame.dmap(_ | game))

  // update the proxied game
  def updateIfPresent(gameId: GameId)(f: Game => Game): Funit =
    rounds.getIfPresent(gameId).so(_.updateGame(f))

  def gameAndStatusIfPresent(gameId: GameId): Fu[Option[GameAndSocketStatus]] =
    rounds.askIfPresent[GameAndSocketStatus](gameId)(GetGameAndSocketStatus.apply)

  def flushIfPresent(gameId: GameId): Funit =
    rounds.getIfPresent(gameId).so(_.flushGame())

  val rounds = AsyncActorConcMap[GameId, RoundAsyncActor](
    mkAsyncActor = id =>
      makeRoundActor(id, SocketVersion(0), roundDependencies.gameRepo.game(id).recoverDefault(none[Game])),
    initialCapacity = 65_536
  )

  private def makeRoundActor(id: GameId, version: SocketVersion, gameFu: Fu[Option[Game]]) =
    given proxy: GameProxy = GameProxy(id, proxyDependencies, gameFu)
    val roundActor = RoundAsyncActor(
      dependencies = roundDependencies,
      gameId = id,
      socketSend = sendForGameId(id),
      putUserLag,
      version = version
    )
    terminationDelay.schedule(id)
    gameFu.dforeach:
      _.foreach: game =>
        scheduleExpiration.exec(game)
        goneWeightsFor(game).dforeach: w =>
          roundActor ! RoundAsyncActor.SetGameInfo(game, w)
    roundActor

  private val roundHandler: SocketHandler =
    case Protocol.In.PlayerMove(fullId, uci, blur, lag) if !stopping =>
      rounds.tell(fullId.gameId, HumanPlay(fullId.playerId, uci, blur, lag, none))
    case Protocol.In.PlayerDo(fullId, tpe) if !stopping =>
      def forward(f: GamePlayerId => Any) = rounds.tell(fullId.gameId, f(fullId.playerId))
      tpe match
        case "moretime"      => forward(Moretime(_))
        case "rematch-yes"   => forward(Rematch(_, true))
        case "rematch-no"    => forward(Rematch(_, false))
        case "takeback-yes"  => forward(Takeback(_, true))
        case "takeback-no"   => forward(Takeback(_, false))
        case "draw-yes"      => forward(Draw(_, true))
        case "draw-no"       => forward(Draw(_, false))
        case "draw-claim"    => forward(DrawClaim(_))
        case "resign"        => forward(Resign(_))
        case "resign-force"  => forward(ResignForce(_))
        case "blindfold-yes" => forward(Blindfold(_, true))
        case "blindfold-no"  => forward(Blindfold(_, false))
        case "draw-force"    => forward(DrawForce(_))
        case "abort"         => forward(Abort(_))
        case "outoftime"     => forward(_ => QuietFlag) // mobile app BC
        case t               => logger.warn(s"Unhandled round socket message: $t")
    case Protocol.In.Flag(gameId, color, fromPlayerId) => rounds.tell(gameId, ClientFlag(color, fromPlayerId))
    case Protocol.In.PlayerChatSay(id, Right(color), msg) =>
      gameIfPresent(id).foreach:
        _.foreach:
          messenger.owner(_, color, msg)
    case Protocol.In.PlayerChatSay(id, Left(userId), msg) =>
      messenger.owner(id, userId, msg)
    case Protocol.In.WatcherChatSay(id, userId, msg) =>
      messenger.watcher(id, userId, msg)
    case RP.In.ChatTimeout(roomId, modId, suspect, reason, text) =>
      messenger.timeout(ChatId(s"$roomId/w"), suspect, reason, text)(using modId)
    case Protocol.In.Berserk(gameId, userId) => Bus.publish(Berserk(gameId, userId), "berserk")
    case Protocol.In.PlayerOnlines(onlines) =>
      onlines.foreach:
        case (gameId, Some(on)) =>
          rounds.tell(gameId, on)
          terminationDelay.cancel(gameId)
        case (gameId, _) =>
          if rounds.exists(gameId) then terminationDelay.schedule(gameId)
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
        data <- mobileSocket.online(game.game, anyId, game.socket)
      yield sendForGameId(anyId.gameId).exec(Protocol.Out.respond(reqId, data))

    case Protocol.In.WsLatency(millis) => MoveLatMonitor.wsLatency.set(millis)
    case P.In.WsBoot =>
      logger.warn("Remote socket boot")
      // schedule termination for all game asyncActors
      // until players actually reconnect
      rounds.foreachKey(terminationDelay.schedule)
      rounds.tellAll(RoundAsyncActor.WsBoot)

  private def finishRound(gameId: GameId): Unit =
    rounds.terminate(gameId, _ ! RoundAsyncActor.Stop)

  private val send: ParallelSocketSend = socketKit.send("r-out", 16)

  private val sendForGameId: GameId => SocketSend = gameId =>
    SocketSend(msg => send.sticky(gameId.value, msg))

  socketKit
    .subscribe("r-in", Protocol.In.reader.orElse(RP.In.reader), 16)(
      roundHandler.orElse(socketKit.baseHandler)
    )
    .andDo(send(P.Out.boot))

  Bus.subscribeFun(
    "tvSelect",
    "roundSocket",
    "tourStanding",
    "startGame",
    "finishGame",
    "roundUnplayed"
  ):
    case TvSelect(gameId, speed, _, json) =>
      sendForGameId(gameId).exec(Protocol.Out.tvSelect(gameId, speed, json))
    case Tell(id, e @ BotConnected(color, v)) =>
      val gameId = GameId(id)
      rounds.tell(gameId, e)
      sendForGameId(gameId).exec(Protocol.Out.botConnected(gameId, color, v))
    case Tell(gameId, msg)          => rounds.tell(GameId(gameId), msg)
    case TellIfExists(gameId, msg)  => rounds.tellIfPresent(GameId(gameId), msg)
    case TellMany(gameIds, msg)     => rounds.tellIds(gameIds.asInstanceOf[Seq[GameId]], msg)
    case TellAll(msg)               => rounds.tellAll(msg)
    case Exists(gameId, promise)    => promise.success(rounds.exists(GameId(gameId)))
    case TourStanding(tourId, json) => send(Protocol.Out.tourStanding(tourId, json))
    case lila.core.game.StartGame(game) if game.hasClock =>
      game.userIds.some
        .filter(_.nonEmpty)
        .foreach: usersPlaying =>
          sendForGameId(game.id).exec(Protocol.Out.startGame(usersPlaying))
    case lila.core.game.FinishGame(game, _) if game.hasClock =>
      game.userIds.some
        .filter(_.nonEmpty)
        .foreach: usersPlaying =>
          sendForGameId(game.id).exec(Protocol.Out.finishGame(game.id, game.winnerColor, usersPlaying))
    case lila.core.round.DeleteUnplayed(gameId) => finishRound(gameId)

  Bus.subscribeFun(BusChan.round.chan, BusChan.global.chan):
    case lila.core.chat.ChatLine(id, l, json) =>
      val line = lila.chat.RoundLine(l, json, id.value.endsWith("/w"))
      rounds.tellIfPresent(GameId.take(id.value), line)
    case lila.core.chat.OnTimeout(id, userId) if id.value.endsWith("/w") =>
      send:
        RP.Out.tellRoom(GameId.take(id.value).into(RoomId), makeMessage("chat_timeout", userId))
    case lila.core.chat.OnReinstate(id, userId) if id.value.endsWith("/w") =>
      send:
        RP.Out.tellRoom(GameId.take(id.value).into(RoomId), makeMessage("chat_reinstate", userId))

  scheduler.scheduleWithFixedDelay(25.seconds, tickInterval): () =>
    rounds.tellAll(RoundAsyncActor.Tick)

  scheduler.scheduleWithFixedDelay(60.seconds, 60.seconds): () =>
    lila.mon.round.asyncActorCount.update(rounds.size)

  private val terminationDelay = TerminationDelay(scheduler, 1.minute, finishRound)

  // on startup we get all ongoing game IDs and versions from lila-ws
  // load them into round actors with batched DB queries
  private def preloadRoundsWithVersions(rooms: Iterable[(String, SocketVersion)]) =
    val bootLog = lila.log("boot")

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
          .byIdsCursor(GameId.from(ids))
          .foldWhile(Set.empty[GameId])(
            (ids, game) =>
              Cursor.Cont[Set[GameId]]:
                gamePromises.get(game.id).foreach(_.success(game.some))
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
          if missingIds.nonEmpty then
            bootLog.warn:
              s"RoundSocket ${missingIds.size} round games could not be loaded: ${missingIds.take(20).mkString(" ")}"
            missingIds.foreach: id =>
              gamePromises.get(id).foreach(_.success(none))
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
    if !pov.game.hasClock
    then 30.days
    else
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

      val reader: P.In.Reader =
        case P.RawMsg("r/ons", raw) =>
          PlayerOnlines:
            P.In
              .commas(raw.args)
              .map:
                _.splitAt(GameId.size) match
                  case (gameId, cs) =>
                    (
                      GameId(gameId),
                      cs.nonEmpty.option(RoomCrowd(cs(0) == '+', cs(1) == '+'))
                    )
          .some
        case P.RawMsg("r/do", raw) =>
          raw.get(2) { case Array(fullId, payload) =>
            for
              obj <- Json.parse(payload).asOpt[JsObject]
              tpe <- obj.str("t")
            yield PlayerDo(GameFullId(fullId), tpe)
          }
        case P.RawMsg("r/move", raw) =>
          raw.get(6) { case Array(fullId, uciS, blurS, lagS, mtS, fraS) =>
            Uci(uciS).map: uci =>
              PlayerMove(
                GameFullId(fullId),
                uci,
                P.In.boolean(blurS),
                MoveMetrics(centis(lagS), centis(mtS), centis(fraS))
              )
          }
        case P.RawMsg("chat/say", raw) =>
          raw.get(3) { case Array(roomId, author, msg) =>
            PlayerChatSay(GameId(roomId), readColor(author).toRight(UserId(author)), msg).some
          }
        case P.RawMsg("chat/say/w", raw) =>
          raw.get(3) { case Array(roomId, userId, msg) =>
            WatcherChatSay(GameId(roomId), UserId(userId), msg).some
          }
        case P.RawMsg("r/berserk", raw) =>
          raw.get(2) { case Array(gameId, userId) =>
            Berserk(GameId(gameId), UserId(userId)).some
          }
        case P.RawMsg("r/bye", raw) => Bye(GameFullId(raw.args)).some
        case P.RawMsg("r/hold", raw) =>
          raw.get(4) { case Array(fullId, ip, meanS, sdS) =>
            for
              mean <- meanS.toIntOption
              sd   <- sdS.toIntOption
              ip   <- IpAddress.from(ip)
            yield HoldAlert(GameFullId(fullId), ip, mean, sd)
          }
        case P.RawMsg("r/report", raw) =>
          raw.get(4) { case Array(fullId, ip, user, name) =>
            IpAddress.from(ip).map { ip =>
              SelfReport(GameFullId(fullId), ip, UserId.from(P.In.optional(user)), name)
            }
          }
        case P.RawMsg("r/flag", raw) =>
          raw.get(3) { case Array(gameId, color, playerId) =>
            readColor(color).map:
              Flag(GameId(gameId), _, P.In.optional(playerId).map { GamePlayerId(_) })
          }
        case P.RawMsg("r/get", raw) =>
          raw.get(2) { case Array(reqId, anyId) =>
            reqId.toIntOption.map:
              GetGame(_, GameAnyId(anyId))
          }
        case P.RawMsg("r/latency", raw) => raw.args.toIntOption.map(WsLatency.apply)

      private def centis(s: String): Option[Centis] =
        if s == "-" then none
        else Centis.from(s.toIntOption)

      private def readColor(s: String) =
        if s == "w" then Some(White)
        else if s == "b" then Some(Black)
        else None

    object Out:

      def resyncPlayer(fullId: GameFullId)        = s"r/resync/player $fullId"
      def gone(fullId: GameFullId, gone: Boolean) = s"r/gone $fullId ${P.Out.boolean(gone)}"
      def goneIn(fullId: GameFullId, millis: Long) =
        val seconds = Math.ceil(millis / 1000d / tickSeconds).toInt * tickSeconds
        s"r/goneIn $fullId $seconds"

      def tellVersion(roomId: RoomId, version: SocketVersion, e: lila.core.game.Event) =
        val flags = StringBuilder(2)
        if e.watcher then flags += 's'
        else if e.owner then flags += 'p'
        else
          e.only
            .map(_.fold('w', 'b'))
            .orElse:
              e.moveBy.map(_.fold('W', 'B'))
            .foreach(flags.+=)
        if e.troll then flags += 't'
        if flags.isEmpty then flags += '-'
        s"r/ver $roomId $version $flags ${e.typ} ${e.data}"

      def tvSelect(gameId: GameId, speed: chess.Speed, data: JsObject) =
        s"tv/select $gameId ${speed.id} ${Json.stringify(data)}"

      def botConnected(gameId: GameId, color: Color, v: Boolean) =
        s"r/bot/online $gameId ${P.Out.color(color)} ${P.Out.boolean(v)}"

      def tourStanding(tourId: TourId, data: JsValue) =
        s"r/tour/standing $tourId ${Json.stringify(data)}"

      def startGame(users: List[UserId]) = s"r/start ${P.Out.commas(users)}"
      def finishGame(gameId: GameId, winner: Option[Color], users: List[UserId]) =
        s"r/finish $gameId ${P.Out.color(winner)} ${P.Out.commas(users)}"

      def respond(reqId: Int, data: JsObject) = s"req/response $reqId ${Json.stringify(data)}"

      def versioningReady = "r/versioning-ready"

  final private class TerminationDelay(
      scheduler: Scheduler,
      duration: FiniteDuration,
      terminate: GameId => Unit
  )(using Executor):

    private val terminations = scalalib.ConcurrentMap[GameId, Cancellable](65536)

    def schedule(gameId: GameId): Unit =
      terminations.compute(gameId): canc =>
        canc.foreach(_.cancel())
        scheduler
          .scheduleOnce(duration):
            terminations.remove(gameId)
            terminate(gameId)
          .some

    def cancel(gameId: GameId): Unit =
      terminations.remove(gameId).foreach(_.cancel())
