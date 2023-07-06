package lila.round

import actorApi.*, round.*
import alleycats.Zero
import chess.{ Black, Centis, Color, White }
import play.api.libs.json.*
import scala.util.chaining.*

import lila.game.{ Event, Game, GameRepo, Player as GamePlayer, Pov, Progress }
import lila.hub.actorApi.round.{
  Abort,
  BotPlay,
  FishnetPlay,
  FishnetStart,
  IsOnGame,
  RematchNo,
  RematchYes,
  Resign
}
import lila.hub.AsyncActor
import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.socket.{ Socket, SocketVersion, SocketSend, GetVersion, UserLagCache }

final private[round] class RoundAsyncActor(
    dependencies: RoundAsyncActor.Dependencies,
    gameId: GameId,
    socketSend: SocketSend,
    private var version: SocketVersion
)(using ec: Executor, proxy: GameProxy)
    extends AsyncActor:

  import RoundSocket.Protocol
  import RoundAsyncActor.*
  import dependencies.*

  private var takebackSituation: Option[TakebackSituation] = None

  private var mightBeSimul = true // until proven otherwise

  final private class Player(color: Color):

    private var offlineSince: Option[Long] = nowMillis.some
    // whether the player closed the window intentionally
    private var bye: Boolean        = false
    private var botConnections: Int = 0

    def botConnected = botConnections > 0

    var userId     = none[UserId]
    var goneWeight = 1f

    def isOnline = offlineSince.isEmpty || botConnected

    def setOnline(on: Boolean): Unit =
      isLongGone foreach { _ so notifyGone(color, gone = false) }
      offlineSince = if on then None else offlineSince orElse nowMillis.some
      bye = bye && !on
    def setBye(): Unit =
      bye = true

    private def isHostingSimul: Fu[Boolean] = mightBeSimul so userId so isSimulHost.apply

    private def timeoutMillis: Long = {
      val base = {
        if bye then RoundSocket.ragequitTimeout
        else
          proxy.withGameOptionSync { g =>
            RoundSocket.povDisconnectTimeout(g pov color)
          } | RoundSocket.disconnectTimeout
      }.toMillis * goneWeight
      base atLeast RoundSocket.ragequitTimeout.toMillis.toFloat
    }.toLong

    def isLongGone: Fu[Boolean] = {
      !botConnected && offlineSince.exists(_ < (nowMillis - timeoutMillis))
    } so !isHostingSimul

    def showMillisToGone: Fu[Option[Long]] =
      if botConnected then fuccess(none)
      else
        val now = nowMillis
        offlineSince
          .filter: since =>
            bye || (now - since) > 5000
          .so: since =>
            isHostingSimul.map:
              !_ option (timeoutMillis + since - now)

    def setBotConnected(v: Boolean) =
      botConnections = Math.max(0, botConnections + (if v then 1 else -1))
  end Player

  private val whitePlayer = Player(White)
  private val blackPlayer = Player(Black)

  export proxy.{ game as getGame, update as updateGame }

  val process: AsyncActor.ReceiveAsync =

    case SetGameInfo(game, (whiteGoneWeight, blackGoneWeight)) =>
      fuccess:
        whitePlayer.userId = game.player(White).userId
        blackPlayer.userId = game.player(Black).userId
        mightBeSimul = game.isSimul
        whitePlayer.goneWeight = whiteGoneWeight
        blackPlayer.goneWeight = blackGoneWeight
        if game.playableByAi then player.requestFishnet(game, this)

    // socket stuff

    case ByePlayer(playerId) =>
      proxy.withPov(playerId):
        _.so: pov =>
          fuccess(getPlayer(pov.color).setBye())

    case GetVersion(promise) =>
      fuccess:
        promise success version
    case SetVersion(v) =>
      fuccess:
        version = v

    case RoomCrowd(white, black) =>
      fuccess:
        whitePlayer setOnline white
        blackPlayer setOnline black

    case IsOnGame(color, promise) =>
      fuccess:
        promise success getPlayer(color).isOnline

    case GetSocketStatus(promise) =>
      getSocketStatus tap promise.completeWith

    case GetGameAndSocketStatus(promise) =>
      getSocketStatus
        .zip(getGame)
        .map: (socket, game) =>
          GameAndSocketStatus(game err s"Game $gameId not found", socket)
        .tap(promise.completeWith)

    case HasUserId(userId, promise) =>
      fuccess:
        promise.success:
          (userId.is(whitePlayer.userId) && whitePlayer.isOnline) ||
            (userId.is(blackPlayer.userId) && blackPlayer.isOnline)

    case lila.chat.RoundLine(line, watcher) =>
      fuccess:
        publish(List(line match
          case l: lila.chat.UserLine   => Event.UserMessage(l, watcher)
          case l: lila.chat.PlayerLine => Event.PlayerMessage(l)
        ))

    case Protocol.In.HoldAlert(fullId, ip, mean, sd) =>
      handle(fullId.playerId): pov =>
        gameRepo hasHoldAlert pov flatMap {
          if _ then funit
          else
            lila
              .log("cheat")
              .info(
                s"hold alert $ip https://lichess.org/${pov.gameId}/${pov.color.name}#${pov.game.ply} ${pov.player.userId | "anon"} mean: $mean SD: $sd"
              )
            lila.mon.cheat.holdAlert.increment()
            gameRepo.setHoldAlert(pov, GamePlayer.HoldAlert(ply = pov.game.ply, mean = mean, sd = sd)).void
        } inject Nil

    case a: lila.analyse.actorApi.AnalysisProgress =>
      fuccess:
        socketSend:
          RP.Out.tellRoom(
            roomId,
            Socket.makeMessage(
              "analysisProgress",
              Json.obj(
                "analysis" -> lila.analyse.JsonView.bothPlayers(a.game.startedAtPly, a.analysis),
                "tree" -> lila.tree.Node.minimalNodeJsonWriter.writes:
                  TreeBuilder(a.game, a.analysis.some, a.initialFen, JsonView.WithFlags())
              )
            )
          )

    // round stuff

    case p: HumanPlay =>
      handle(p.playerId): pov =>
        if pov.player.isAi then fufail(s"player $pov can't play AI")
        else if pov.game.outoftime(withGrace = true) then finisher.outOfTime(pov.game)
        else
          recordLag(pov)
          player.human(p, this)(pov)
      .chronometer.lap.addEffects(
        err =>
          p.promise.foreach(_ failure err)
          socketSend(Protocol.Out.resyncPlayer(GameFullId(gameId, p.playerId)))
        ,
        lap =>
          p.promise.foreach(_ success {})
          lila.mon.round.move.time.record(lap.nanos)
          MoveLatMonitor recordMicros lap.micros
      )

    case p: BotPlay =>
      val res = proxy.withPov(p.playerId) {
        _.so: pov =>
          if pov.game.outoftime(withGrace = true) then finisher.outOfTime(pov.game)
          else player.bot(p.uci, this)(pov)
      } dmap publish
      p.promise.foreach(_ completeWith res)
      res

    case FishnetPlay(uci, hash) =>
      handle: game =>
        player.fishnet(game, hash, uci)
      .mon(_.round.move.time)

    case Abort(playerId) =>
      handle(playerId): pov =>
        pov.game.abortableByUser so finisher.abort(pov)

    case Resign(playerId) =>
      handle(playerId): pov =>
        pov.game.resignable so finisher.other(pov.game, _.Resign, Some(!pov.color))

    case ResignAi =>
      handleAi: pov =>
        pov.game.resignable so finisher.other(pov.game, _.Resign, Some(!pov.color))

    case GoBerserk(color, promise) =>
      handle(color): pov =>
        val berserked = pov.game.goBerserk(color)
        berserked.so { progress =>
          proxy.save(progress) >> gameRepo.goBerserk(pov) inject progress.events
        } >>- promise.success(berserked.isDefined)

    case ResignForce(playerId) =>
      handle(playerId): pov =>
        pov.mightClaimWin.so:
          getPlayer(!pov.color).isLongGone flatMap {
            if _ then
              finisher.rageQuit(
                pov.game,
                Some(pov.color) ifFalse pov.game.situation.opponentHasInsufficientMaterial
              )
            else fuccess(List(Event.Reload))
          }

    case DrawForce(playerId) =>
      handle(playerId): pov =>
        (pov.game.forceDrawable && !pov.game.hasAi && pov.game.hasClock && !pov.isMyTurn).so:
          getPlayer(!pov.color).isLongGone flatMap {
            if _ then finisher.rageQuit(pov.game, None)
            else fuccess(List(Event.Reload))
          }

    case AbortForce =>
      handle: game =>
        game.playable so finisher.abortForce(game)

    // checks if any player can safely (grace) be flagged
    case QuietFlag =>
      handle: game =>
        game.outoftime(withGrace = true) so finisher.outOfTime(game)

    // flags a specific player, possibly without grace if self
    case ClientFlag(color, from) =>
      handle: game =>
        (game.turnColor == color).so:
          val toSelf = from has game.player(color).id
          game.outoftime(withGrace = !toSelf) so finisher.outOfTime(game)

    // exceptionally we don't publish events
    // if the game is abandoned, then nobody is around to see it
    case Abandon =>
      proxy.withGame: game =>
        game.abandoned.so:
          if game.abortable then finisher.other(game, _.Aborted, None)
          else finisher.other(game, _.Resign, Some(!game.player.color))

    case DrawYes(playerId)   => handle(playerId)(drawer.yes)
    case DrawNo(playerId)    => handle(playerId)(drawer.no)
    case DrawClaim(playerId) => handle(playerId)(drawer.claim)
    case Cheat(color) =>
      handle: game =>
        (game.playable && !game.imported).so:
          finisher.other(game, _.Cheat, Some(!color))
    case TooManyPlies => handle(drawer force _)

    case Threefold =>
      proxy.withGame: game =>
        drawer autoThreefold game map {
          _.foreach: pov =>
            this ! DrawClaim(pov.player.id)
        }

    case RematchYes(playerId) => handle(playerId)(rematcher.yes)
    case RematchNo(playerId)  => handle(playerId)(rematcher.no)

    case TakebackYes(playerId) =>
      handle(playerId): pov =>
        takebacker.yes(~takebackSituation)(pov) map { (events, situation) =>
          takebackSituation = situation.some
          events
        }
    case TakebackNo(playerId) =>
      handle(playerId): pov =>
        takebacker.no(~takebackSituation)(pov) map { (events, situation) =>
          takebackSituation = situation.some
          events
        }

    case Moretime(playerId, duration) =>
      handle(playerId): pov =>
        moretimer(pov, duration) flatMapz { progress =>
          proxy save progress inject progress.events
        }

    case ForecastPlay(lastMove) =>
      handle: game =>
        forecastApi.nextMove(game, lastMove) map { mOpt =>
          mOpt.foreach: move =>
            this ! HumanPlay(game.player.id, move, blur = false)
          Nil
        }

    case LilaStop(promise) =>
      proxy
        .withGame: g =>
          g.playable.so:
            proxy saveAndFlush:
              g.clock.fold(Progress(g)): clock =>
                g.withClock:
                  clock
                    .giveTime(g.turnColor, Centis(2000))
                    .giveTime(!g.turnColor, Centis(1000))
        .tap(promise.completeWith)

    case WsBoot =>
      handle: game =>
        game.playable.so:
          messenger.volatile(game, "Lichess has been updated! Sorry for the inconvenience.")
          val progress = moretimer.give(game, Color.all, 20 seconds)
          proxy save progress inject progress.events

    case BotConnected(color, v) =>
      fuccess:
        getPlayer(color) setBotConnected v

    case NoStart =>
      handle: game =>
        game.timeBeforeExpiration.exists(_.centis == 0) so {
          if game.isSwiss then
            game.startClock.so: g =>
              proxy save g inject List(Event.Reload)
          else finisher.noStart(game)
        }

    case StartClock =>
      handle: game =>
        game.startClock.so: g =>
          proxy save g inject List(Event.Reload)

    case FishnetStart =>
      proxy.withGame: g =>
        g.playableByAi so player.requestFishnet(g, this)

    case Tick =>
      proxy.withGameOptionSync { g =>
        (g.forceResignable && g.bothPlayersHaveMoved) so fuccess:
          Color.all.foreach: c =>
            if !getPlayer(c).isOnline && getPlayer(!c).isOnline then
              getPlayer(c).showMillisToGone foreach {
                _.so: millis =>
                  if millis <= 0 then notifyGone(c, gone = true)
                  else g.clock.exists(_.remainingTime(c).millis > millis + 3000) so notifyGoneIn(c, millis)
              }
      } | funit

    case Stop => proxy.terminate() >>- socketSend(RP.Out.stop(roomId))

  private def getPlayer(color: Color): Player = color.fold(whitePlayer, blackPlayer)

  private def getSocketStatus: Future[SocketStatus] =
    whitePlayer.isLongGone zip blackPlayer.isLongGone map { (whiteIsGone, blackIsGone) =>
      SocketStatus(
        version = version,
        whiteOnGame = whitePlayer.isOnline,
        whiteIsGone = whiteIsGone,
        blackOnGame = blackPlayer.isOnline,
        blackIsGone = blackIsGone
      )
    }

  private def recordLag(pov: Pov): Unit =
    if (pov.game.playedTurns.value & 30) == 10 then
      // Triggers every 32 moves starting on ply 10.
      // i.e. 10, 11, 42, 43, 74, 75, ...
      for
        user  <- pov.player.userId
        clock <- pov.game.clock
        lag   <- clock.lag(pov.color).lagMean
      do UserLagCache.put(user, lag)

  private def notifyGone(color: Color, gone: Boolean): Funit =
    proxy.withPov(color): pov =>
      fuccess:
        socketSend(Protocol.Out.gone(pov.fullId, gone))
        publishBoardBotGone(pov, gone option 0L)

  private def notifyGoneIn(color: Color, millis: Long): Funit =
    proxy.withPov(color): pov =>
      fuccess:
        socketSend(Protocol.Out.goneIn(pov.fullId, millis))
        publishBoardBotGone(pov, millis.some)

  private def publishBoardBotGone(pov: Pov, millis: Option[Long]) =
    if lila.game.Game.isBoardOrBotCompatible(pov.game) then
      lila.common.Bus.publish(
        lila.game.actorApi.BoardGone(pov, millis.map(m => (m.atLeast(0) / 1000).toInt)),
        lila.game.actorApi.BoardGone makeChan gameId
      )

  private def handle(op: Game => Fu[Events]): Funit =
    proxy.withGame: g =>
      handleAndPublish(op(g))

  private def handle(playerId: GamePlayerId)(op: Pov => Fu[Events]): Funit =
    proxy.withPov(playerId):
      _.so: pov =>
        handleAndPublish(op(pov))

  private def handle(color: Color)(op: Pov => Fu[Events]): Funit =
    proxy.withPov(color): pov =>
      handleAndPublish(op(pov))

  private def handleAndPublish(events: Fu[Events]): Funit =
    events dmap publish recover errorHandler("handle")

  private def handleAi(op: Pov => Fu[Events]): Funit =
    proxy.withGame:
      _.aiPov.so: p =>
        handleAndPublish(op(p))

  private def publish[A](events: Events): Unit =
    if events.nonEmpty then
      events.foreach: e =>
        version = version.incVersion
        socketSend:
          Protocol.Out.tellVersion(roomId, version, e)
      if events.exists:
          case e: Event.Move => e.threefold
          case _             => false
      then this ! Threefold

  private def errorHandler(name: String): PartialFunction[Throwable, Unit] =
    case e: FishnetError =>
      logger.info(s"Round fishnet error $name: ${e.getMessage}")
      lila.mon.round.error.fishnet.increment().unit
    case e: BenignError =>
      logger.info(s"Round client error $name: ${e.getMessage}")
      lila.mon.round.error.client.increment().unit
    case e: Exception =>
      logger.warn(s"$name: ${e.getMessage}")
      lila.mon.round.error.other.increment().unit

  def roomId = gameId into RoomId

object RoundAsyncActor:

  case class HasUserId(userId: UserId, promise: Promise[Boolean])
  case class SetGameInfo(game: lila.game.Game, goneWeights: (Float, Float))
  case object Tick
  case object Stop
  case object WsBoot
  case class LilaStop(promise: Promise[Unit])

  private[round] case class TakebackSituation(nbDeclined: Int, lastDeclined: Option[Instant]):

    def decline = TakebackSituation(nbDeclined + 1, nowInstant.some)

    def delaySeconds = (math.pow(nbDeclined min 10, 2) * 10).toInt

    def offerable = lastDeclined.fold(true) { _ isBefore nowInstant.minusSeconds(delaySeconds) }

    def reset = takebackSituationZero.zero

  private[round] given takebackSituationZero: Zero[TakebackSituation] =
    Zero(TakebackSituation(0, none))

  private[round] class Dependencies(
      val gameRepo: GameRepo,
      val messenger: Messenger,
      val takebacker: Takebacker,
      val moretimer: Moretimer,
      val finisher: Finisher,
      val rematcher: Rematcher,
      val player: Player,
      val drawer: Drawer,
      val forecastApi: ForecastApi,
      val isSimulHost: IsSimulHost,
      val jsonView: JsonView
  )
