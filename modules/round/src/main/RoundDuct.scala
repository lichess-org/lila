package lila.round

import actorApi._, round._
import chess.{ Black, Centis, Color, White }
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise
import scala.util.chaining._

import lila.game.Game.{ FullId, PlayerId }
import lila.game.{ Game, GameRepo, Pov, Event, Progress, Player => GamePlayer }
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
import lila.hub.Duct
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.Socket.{ makeMessage, GetVersion, SocketVersion }
import lila.socket.UserLagCache
import lila.user.User

final private[round] class RoundDuct(
    dependencies: RoundDuct.Dependencies,
    gameId: Game.ID,
    socketSend: String => Unit
)(implicit
    ec: scala.concurrent.ExecutionContext,
    proxy: GameProxy
) extends Duct {

  import RoundSocket.Protocol
  import RoundDuct._
  import dependencies._

  private var takebackSituation: Option[TakebackSituation] = None

  private var version = SocketVersion(0)

  private var mightBeSimul = true // until proven otherwise

  final private class Player(color: Color) {

    private var offlineSince: Option[Long] = nowMillis.some
    // wether the player closed the window intentionally
    private var bye: Boolean = false
    // connected as a bot
    private var botConnected: Boolean = false

    var userId     = none[User.ID]
    var goneWeight = 1f

    def isOnline = offlineSince.isEmpty || botConnected

    def setOnline(on: Boolean): Unit = {
      isLongGone foreach { _ ?? notifyGone(color, gone = false) }
      offlineSince = if (on) None else offlineSince orElse nowMillis.some
      bye = bye && !on
    }
    def setBye(): Unit = {
      bye = true
    }

    private def isHostingSimul: Fu[Boolean] = mightBeSimul ?? userId ?? isSimulHost

    private def timeoutMillis: Long = {
      val base = {
        if (bye) RoundSocket.ragequitTimeout
        else
          proxy.withGameOptionSync { g =>
            RoundSocket.povDisconnectTimeout(g pov color)
          } | RoundSocket.disconnectTimeout
      }.toMillis * goneWeight
      base atLeast RoundSocket.ragequitTimeout.toMillis.toFloat
    }.toLong

    def isLongGone: Fu[Boolean] = {
      !botConnected && offlineSince.exists(_ < (nowMillis - timeoutMillis))
    } ?? !isHostingSimul

    def showMillisToGone: Fu[Option[Long]] =
      if (botConnected) fuccess(none)
      else {
        val now = nowMillis
        offlineSince.filter { since =>
          bye || (now - since) > 5000
        } ?? { since =>
          isHostingSimul map {
            !_ option (timeoutMillis + since - now)
          }
        }
      }

    def setBotConnected(v: Boolean) =
      botConnected = v

    def isBotConnected = botConnected
  }

  private val whitePlayer = new Player(White)
  private val blackPlayer = new Player(Black)

  def getGame: Fu[Option[Game]]          = proxy.game
  def updateGame(f: Game => Game): Funit = proxy update f

  val process: Duct.ReceiveAsync = {

    case SetGameInfo(game, (whiteGoneWeight, blackGoneWeight)) =>
      fuccess {
        whitePlayer.userId = game.player(White).userId
        blackPlayer.userId = game.player(Black).userId
        mightBeSimul = game.isSimul
        whitePlayer.goneWeight = whiteGoneWeight
        blackPlayer.goneWeight = blackGoneWeight
        if (game.playableByAi) player.requestFishnet(game, this)
      }

    // socket stuff

    case ByePlayer(playerId) =>
      proxy.withPov(playerId) {
        _ ?? { pov =>
          fuccess(getPlayer(pov.color).setBye())
        }
      }

    case GetVersion(promise) =>
      fuccess {
        promise success version
      }
    case SetVersion(v) =>
      fuccess {
        version = v
      }

    case RoomCrowd(white, black) =>
      fuccess {
        whitePlayer setOnline white
        blackPlayer setOnline black
      }

    case IsOnGame(color, promise) =>
      fuccess {
        promise success getPlayer(color).isOnline
      }

    case GetSocketStatus(promise) =>
      whitePlayer.isLongGone zip blackPlayer.isLongGone map { case (whiteIsGone, blackIsGone) =>
        promise success SocketStatus(
          version = version,
          whiteOnGame = whitePlayer.isOnline,
          whiteIsGone = whiteIsGone,
          blackOnGame = blackPlayer.isOnline,
          blackIsGone = blackIsGone
        )
      }

    case HasUserId(userId, promise) =>
      fuccess {
        promise success {
          (whitePlayer.userId.has(userId) && whitePlayer.isOnline) ||
          (blackPlayer.userId.has(userId) && blackPlayer.isOnline)
        }
      }

    case lila.chat.actorApi.RoundLine(line, watcher) =>
      fuccess {
        publish(List(line match {
          case l: lila.chat.UserLine   => Event.UserMessage(l, watcher)
          case l: lila.chat.PlayerLine => Event.PlayerMessage(l)
        }))
      }

    case Protocol.In.HoldAlert(fullId, ip, mean, sd) =>
      handle(fullId.playerId) { pov =>
        gameRepo hasHoldAlert pov flatMap {
          case true => funit
          case false =>
            lila
              .log("cheat")
              .info(
                s"hold alert $ip https://lichess.org/${pov.gameId}/${pov.color.name}#${pov.game.turns} ${pov.player.userId | "anon"} mean: $mean SD: $sd"
              )
            lila.mon.cheat.holdAlert.increment()
            gameRepo.setHoldAlert(pov, GamePlayer.HoldAlert(ply = pov.game.turns, mean = mean, sd = sd)).void
        } inject Nil
      }

    case a: lila.analyse.actorApi.AnalysisProgress =>
      fuccess {
        socketSend(
          RP.Out.tellRoom(
            roomId,
            makeMessage(
              "analysisProgress",
              Json.obj(
                "analysis" -> lila.analyse.JsonView.bothPlayers(a.game, a.analysis),
                "tree" -> lila.tree.Node.minimalNodeJsonWriter.writes {
                  TreeBuilder(
                    id = a.analysis.id,
                    pgnMoves = a.game.pgnMoves,
                    variant = a.variant,
                    analysis = a.analysis.some,
                    initialFen = a.initialFen,
                    withFlags = JsonView.WithFlags(),
                    clocks = none
                  )
                }
              )
            )
          )
        )
      }

    // round stuff

    case p: HumanPlay =>
      handle(p.playerId) { pov =>
        if (pov.player.isAi) fufail(s"player $pov can't play AI")
        else if (pov.game.outoftime(withGrace = true)) finisher.outOfTime(pov.game)
        else {
          recordLag(pov)
          player.human(p, this)(pov)
        }
      }.chronometer.lap.addEffects(
        err => {
          p.promise.foreach(_ failure err)
          socketSend(Protocol.Out.resyncPlayer(Game.Id(gameId) full p.playerId))
        },
        lap => {
          p.promise.foreach(_ success {})
          lila.mon.round.move.time.record(lap.nanos)
          MoveLatMonitor record lap.micros
        }
      )

    case p: BotPlay =>
      val res = proxy.withPov(PlayerId(p.playerId)) {
        _ ?? { pov =>
          if (pov.game.outoftime(withGrace = true)) finisher.outOfTime(pov.game)
          else player.bot(p.uci, this)(pov)
        }
      } dmap publish
      p.promise.foreach(_ completeWith res)
      res

    case FishnetPlay(uci, ply) =>
      handle { game =>
        player.fishnet(game, ply, uci)
      }.mon(_.round.move.time)

    case Abort(playerId) =>
      handle(PlayerId(playerId)) { pov =>
        pov.game.abortable ?? finisher.abort(pov)
      }

    case Resign(playerId) =>
      handle(PlayerId(playerId)) { pov =>
        pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
      }

    case ResignAi =>
      handleAi { pov =>
        pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
      }

    case GoBerserk(color, promise) =>
      handle(color) { pov =>
        val berserked = pov.game.goBerserk(color)
        berserked.?? { progress =>
          proxy.save(progress) >> gameRepo.goBerserk(pov) inject progress.events
        } >>- promise.success(berserked.isDefined)
      }

    case ResignForce(playerId) =>
      handle(playerId) { pov =>
        (pov.game.resignable && !pov.game.hasAi && pov.game.hasClock && !pov.isMyTurn) ?? {
          getPlayer(!pov.color).isLongGone flatMap {
            case true =>
              finisher.rageQuit(
                pov.game,
                Some(pov.color) ifFalse pov.game.situation.opponentHasInsufficientMaterial
              )
            case _ => fuccess(List(Event.Reload))
          }
        }
      }

    case DrawForce(playerId) =>
      handle(playerId) { pov =>
        (pov.game.drawable && !pov.game.hasAi && pov.game.hasClock && pov.game.bothPlayersHaveMoved) ?? {
          getPlayer(!pov.color).isLongGone flatMap {
            case true => finisher.rageQuit(pov.game, None)
            case _    => fuccess(List(Event.Reload))
          }
        }
      }

    // checks if any player can safely (grace) be flagged
    case QuietFlag =>
      handle { game =>
        game.outoftime(withGrace = true) ?? finisher.outOfTime(game)
      }

    // flags a specific player, possibly without grace if self
    case ClientFlag(color, from) =>
      handle { game =>
        (game.turnColor == color) ?? {
          val toSelf = from has PlayerId(game.player(color).id)
          game.outoftime(withGrace = !toSelf) ?? finisher.outOfTime(game)
        }
      }

    // exceptionally we don't publish events
    // if the game is abandoned, then nobody is around to see it
    case Abandon =>
      proxy withGame { game =>
        game.abandoned ?? {
          if (game.abortable) finisher.other(game, _.Aborted, None)
          else finisher.other(game, _.Resign, Some(!game.player.color))
        }
      }

    case DrawYes(playerId)   => handle(playerId)(drawer.yes)
    case DrawNo(playerId)    => handle(playerId)(drawer.no)
    case DrawClaim(playerId) => handle(playerId)(drawer.claim)
    case Cheat(color) =>
      handle { game =>
        (game.playable && !game.imported) ?? {
          finisher.other(game, _.Cheat, Some(!color))
        }
      }
    case TooManyPlies => handle(drawer force _)

    case Threefold =>
      proxy withGame { game =>
        drawer autoThreefold game map {
          _ foreach { pov =>
            this ! DrawClaim(PlayerId(pov.player.id))
          }
        }
      }

    case RematchYes(playerId) => handle(PlayerId(playerId))(rematcher.yes)
    case RematchNo(playerId)  => handle(PlayerId(playerId))(rematcher.no)

    case TakebackYes(playerId) =>
      handle(playerId) { pov =>
        takebacker.yes(~takebackSituation)(pov) map { case (events, situation) =>
          takebackSituation = situation.some
          events
        }
      }
    case TakebackNo(playerId) =>
      handle(playerId) { pov =>
        takebacker.no(~takebackSituation)(pov) map { case (events, situation) =>
          takebackSituation = situation.some
          events
        }
      }

    case Moretime(playerId, duration) =>
      handle(playerId) { pov =>
        moretimer(pov, duration) flatMap {
          _ ?? { progress =>
            proxy save progress inject progress.events
          }
        }
      }

    case ForecastPlay(lastMove) =>
      handle { game =>
        forecastApi.nextMove(game, lastMove) map { mOpt =>
          mOpt foreach { move =>
            this ! HumanPlay(PlayerId(game.player.id), move, blur = false)
          }
          Nil
        }
      }

    case LilaStop(promise) =>
      proxy.withGame { g =>
        g.playable ?? {
          proxy saveAndFlush {
            g.clock.fold(Progress(g)) { clock =>
              g.withClock {
                clock
                  .giveTime(g.turnColor, Centis(2000))
                  .giveTime(!g.turnColor, Centis(1000))
              }
            }
          }
        }
      } tap promise.completeWith

    case WsBoot =>
      handle { game =>
        game.playable ?? {
          messenger.system(game, "Lichess has been updated! Sorry for the inconvenience.")
          val progress = moretimer.give(game, Color.all, 20 seconds)
          proxy save progress inject progress.events
        }
      }

    case AbortForce =>
      handle { game =>
        game.playable ?? finisher.other(game, _.Aborted, None)
      }

    case BotConnected(color, v) =>
      fuccess {
        getPlayer(color) setBotConnected v
      }

    case NoStart =>
      handle { game =>
        game.timeBeforeExpiration.exists(_.centis == 0) ?? {
          if (game.isSwiss) game.startClock ?? { g =>
            proxy save g inject List(Event.Reload)
          }
          else finisher.noStart(game)
        }
      }

    case StartClock =>
      handle { game =>
        game.startClock ?? { g =>
          proxy save g inject List(Event.Reload)
        }
      }

    case FishnetStart =>
      proxy.withGame { g =>
        g.playableByAi ?? player.requestFishnet(g, this)
      }

    case Tick =>
      proxy.withGameOptionSync { g =>
        (g.forceResignable && g.bothPlayersHaveMoved) ?? fuccess {
          Color.all.foreach { c =>
            if (!getPlayer(c).isOnline && getPlayer(!c).isOnline) {
              getPlayer(c).showMillisToGone foreach {
                _ ?? { millis =>
                  if (millis <= 0) notifyGone(c, gone = true)
                  else g.clock.exists(_.remainingTime(c).millis > millis + 3000) ?? notifyGoneIn(c, millis)
                }
              }
            }
          }
        }
      } | funit

    case Stop => proxy.terminate() >>- socketSend(RP.Out.stop(roomId))
  }

  private def getPlayer(color: Color): Player = color.fold(whitePlayer, blackPlayer)

  private def recordLag(pov: Pov): Unit =
    if ((pov.game.playedTurns & 30) == 10) {
      // Triggers every 32 moves starting on ply 10.
      // i.e. 10, 11, 42, 43, 74, 75, ...
      for {
        user  <- pov.player.userId
        clock <- pov.game.clock
        lag   <- clock.lag(pov.color).lagMean
      } UserLagCache.put(user, lag)
    }

  private def notifyGone(color: Color, gone: Boolean): Funit =
    proxy.withPov(color) { pov =>
      fuccess {
        socketSend(Protocol.Out.gone(FullId(pov.fullId), gone))
      }
    }

  private def notifyGoneIn(color: Color, millis: Long): Funit =
    proxy.withPov(color) { pov =>
      fuccess {
        socketSend(Protocol.Out.goneIn(FullId(pov.fullId), millis))
      }
    }

  private def handle(op: Game => Fu[Events]): Funit =
    proxy.withGame { g =>
      handleAndPublish(op(g))
    }

  private def handle(playerId: PlayerId)(op: Pov => Fu[Events]): Funit =
    proxy.withPov(playerId) {
      _ ?? { pov =>
        handleAndPublish(op(pov))
      }
    }

  private def handle(color: Color)(op: Pov => Fu[Events]): Funit =
    proxy.withPov(color) { pov =>
      handleAndPublish(op(pov))
    }

  private def handleAndPublish(events: Fu[Events]): Funit =
    events dmap publish recover errorHandler("handle")

  private def handleAi(op: Pov => Fu[Events]): Funit =
    proxy.withGame {
      _.aiPov ?? { p =>
        handleAndPublish(op(p))
      }
    }

  private def publish[A](events: Events): Unit =
    if (events.nonEmpty) {
      events foreach { e =>
        version = version.inc
        socketSend {
          Protocol.Out.tellVersion(roomId, version, e)
        }
      }
      if (
        events exists {
          case e: Event.Move => e.threefold
          case _             => false
        }
      ) this ! Threefold
    }

  private def errorHandler(name: String): PartialFunction[Throwable, Unit] = {
    case e: ClientError =>
      logger.info(s"Round client error $name: ${e.getMessage}")
      lila.mon.round.error.client.increment().unit
    case e: FishnetError =>
      logger.info(s"Round fishnet error $name: ${e.getMessage}")
      lila.mon.round.error.fishnet.increment().unit
    case e: Exception =>
      logger.warn(s"$name: ${e.getMessage}")
      lila.mon.round.error.other.increment().unit
  }

  def roomId = RoomId(gameId)
}

object RoundDuct {

  case class HasUserId(userId: User.ID, promise: Promise[Boolean])
  case class SetGameInfo(game: lila.game.Game, goneWeights: (Float, Float))
  case object Tick
  case object Stop
  case object WsBoot
  case class LilaStop(promise: Promise[Unit])

  private[round] case class TakebackSituation(nbDeclined: Int, lastDeclined: Option[DateTime]) {

    def decline = TakebackSituation(nbDeclined + 1, DateTime.now.some)

    def delaySeconds = (math.pow(nbDeclined min 10, 2) * 10).toInt

    def offerable = lastDeclined.fold(true) { _ isBefore DateTime.now.minusSeconds(delaySeconds) }

    def reset = takebackSituationZero.zero
  }

  implicit private[round] val takebackSituationZero: Zero[TakebackSituation] =
    Zero.instance(TakebackSituation(0, none))

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
      val isSimulHost: IsSimulHost
  )
}
