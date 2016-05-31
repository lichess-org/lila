package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._, round._
import chess.Color
import lila.game.{ Game, Pov, PovRef, PlayerRef, Event, Progress }
import lila.hub.actorApi.DeployPost
import lila.hub.actorApi.map._
import lila.hub.actorApi.round.FishnetPlay
import lila.hub.SequentialActor
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import makeTimeout.large

private[round] final class Round(
    gameId: String,
    messenger: Messenger,
    takebacker: Takebacker,
    finisher: Finisher,
    rematcher: Rematcher,
    player: Player,
    drawer: Drawer,
    forecastApi: ForecastApi,
    socketHub: ActorRef,
    moretimeDuration: Duration,
    activeTtl: Duration) extends SequentialActor {

  context setReceiveTimeout activeTtl

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'deploy)
  }

  override def postStop() {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  implicit val proxy = new GameProxy(gameId)

  object lags { // player lag in millis
    var white = 0
    var black = 0
    def get(c: Color) = c.fold(white, black)
    def set(c: Color, v: Int) {
      if (c.white) white = v
      else black = v
    }
  }

  def process = {

    case ReceiveTimeout => fuccess {
      self ! SequentialActor.Terminate
    }

    case p: HumanPlay =>
      p.trace.finishFirstSegment()
      handleHumanPlay(p) { pov =>
        if (pov.game outoftime lags.get) finisher.outOfTime(pov.game)
        else {
          lags.set(pov.color, p.lag.toMillis.toInt)
          reportNetworkLag(pov)
          player.human(p, self)(pov)
        }
      } >>- {
        p.trace.finish()
        lila.mon.round.move.full.count()
      }

    case FishnetPlay(uci, currentFen) => handle { game =>
      player.fishnet(game, uci, currentFen)
    } >>- lila.mon.round.move.full.count()

    case Abort(playerId) => handle(playerId) { pov =>
      pov.game.abortable ?? finisher.abort(pov)
    }

    case Resign(playerId) => handle(playerId) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case GoBerserk(color) => handle(color) { pov =>
      pov.game.goBerserk(color) ?? { progress =>
        messenger.system(pov.game, (_.untranslated(
          s"${pov.color.name.capitalize} is going berserk!"
        )))
        proxy.save(progress) >> proxy.invalidating(_ goBerserk pov) inject progress.events
      }
    }

    case ResignForce(playerId) => handle(playerId) { pov =>
      (pov.game.resignable && !pov.game.hasAi && pov.game.hasClock) ?? {
        socketHub ? Ask(pov.gameId, IsGone(!pov.color)) flatMap {
          case true => finisher.rageQuit(pov.game, Some(pov.color))
          case _    => fuccess(List(Event.Reload))
        }
      }
    }

    case NoStartColor(color) => handle(color) { pov =>
      finisher.other(pov.game, _.NoStart, Some(!pov.color))
    }

    case DrawForce(playerId) => handle(playerId) { pov =>
      (pov.game.drawable && !pov.game.hasAi && pov.game.hasClock) ?? {
        socketHub ? Ask(pov.gameId, IsGone(!pov.color)) flatMap {
          case true => finisher.rageQuit(pov.game, None)
          case _    => fuccess(List(Event.Reload))
        }
      }
    }

    case Outoftime => handle { game =>
      game.outoftime(lags.get) ?? finisher.outOfTime(game)
    }

    // exceptionally we don't block nor publish events
    // if the game is abandoned, then nobody is around to see it
    // we can also terminate this actor
    case Abandon => fuccess {
      proxy withGame { game =>
        game.abandoned ?? {
          self ! PoisonPill
          if (game.abortable) finisher.other(game, _.Aborted)
          else finisher.other(game, _.Resign, Some(!game.player.color))
        }
      }
    }

    case DrawYes(playerRef)  => handle(playerRef)(drawer.yes)
    case DrawNo(playerRef)   => handle(playerRef)(drawer.no)
    case DrawClaim(playerId) => handle(playerId)(drawer.claim)
    case DrawForce           => handle(drawer force _)
    case Cheat(color) => handle { game =>
      (game.playable && !game.imported) ?? {
        finisher.other(game, _.Cheat, Some(!color))
      }
    }

    case Threefold => proxy withGame { game =>
      drawer autoThreefold game map {
        _ foreach { pov =>
          self ! DrawClaim(pov.player.id)
        }
      }
    }

    case HoldAlert(playerId, mean, sd, ip) => handle(playerId) { pov =>
      !pov.player.hasHoldAlert ?? {
        lila.log("cheat").info(s"hold alert $ip http://lichess.org/${pov.gameId}/${pov.color.name}#${pov.game.turns} ${pov.player.userId | "anon"} mean: $mean SD: $sd")
        lila.mon.cheat.holdAlert()
        proxy.bypass(_.setHoldAlert(pov, mean, sd)) inject List.empty[Event]
      }
    }

    case RematchYes(playerRef)  => handle(playerRef)(rematcher.yes)
    case RematchNo(playerRef)   => handle(playerRef)(rematcher.no)

    case TakebackYes(playerRef) => handle(playerRef)(takebacker.yes)
    case TakebackNo(playerRef)  => handle(playerRef)(takebacker.no)

    case Moretime(playerRef) => handle(playerRef) { pov =>
      pov.game.clock.ifTrue(pov.game moretimeable !pov.color) ?? { clock =>
        val newClock = clock.giveTime(!pov.color, moretimeDuration.toSeconds)
        val progress = (pov.game withClock newClock) + Event.Clock(newClock)
        messenger.system(pov.game, (_.untranslated(
          "%s + %d seconds".format(!pov.color, moretimeDuration.toSeconds)
        )))
        proxy save progress inject progress.events
      }
    }

    case ForecastPlay(lastMove) => handle { game =>
      forecastApi.nextMove(game, lastMove) map { mOpt =>
        mOpt foreach { move =>
          self ! HumanPlay(game.player.id, move, false, 0.seconds)
        }
        Nil
      }
    }

    case DeployPost => handle { game =>
      game.clock.filter(_ => game.playable) ?? { clock =>
        val freeSeconds = 15
        val newClock = clock.giveTime(Color.White, freeSeconds).giveTime(Color.Black, freeSeconds)
        val progress = (game withClock newClock) + Event.Clock(newClock)
        messenger.system(game, (_.untranslated("Lichess has been updated")))
        messenger.system(game, (_.untranslated("Sorry for the inconvenience!")))
        Color.all.foreach { c =>
          messenger.system(game, (_.untranslated(s"$c + $freeSeconds seconds")))
        }
        proxy save progress inject progress.events
      }
    }

    case AbortForMaintenance => handle { game =>
      messenger.system(game, (_.untranslated("Game aborted for server maintenance")))
      messenger.system(game, (_.untranslated("Sorry for the inconvenience!")))
      game.playable ?? finisher.other(game, _.Aborted)
    }
  }

  private def reportNetworkLag(pov: Pov) =
    if (pov.game.turns == 20 || pov.game.turns == 21) List(lags.white, lags.black).foreach { lag =>
      if (lag > 0) lila.mon.round.move.networkLag(lag)
    }

  protected def handle[A](op: Game => Fu[Events]): Funit =
    handleGame(proxy.game)(op)

  protected def handle(playerId: String)(op: Pov => Fu[Events]): Funit =
    handlePov((proxy playerPov playerId))(op)

  protected def handleHumanPlay(p: HumanPlay)(op: Pov => Fu[Events]): Funit =
    handlePov {
      p.trace.segment("fetch", "db") {
        proxy playerPov p.playerId
      }
    }(op)

  protected def handle(color: Color)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy pov color)(op)

  private def handlePov(pov: Fu[Option[Pov]])(op: Pov => Fu[Events]): Funit = publish {
    pov flatten "pov not found" flatMap { p =>
      if (p.player.isAi) fufail("player can't play AI") else op(p)
    }
  } recover errorHandler("handlePov")

  private def handleGame(game: Fu[Option[Game]])(op: Game => Fu[Events]): Funit = publish {
    game flatten "game not found" flatMap op
  } recover errorHandler("handleGame")

  private def publish[A](op: Fu[Events]): Funit = op.addEffect { events =>
    if (events.nonEmpty) socketHub ! Tell(gameId, EventList(events))
    if (events exists {
      case e: Event.Move => e.threefold
      case _             => false
    }) self ! Threefold
  }.void recover errorHandler("publish")

  private def errorHandler(name: String): PartialFunction[Throwable, Unit] = {
    case e: ClientError  => lila.mon.round.error.client()
    case e: FishnetError => lila.mon.round.error.fishnet()
    case e: Exception    => logger.warn(s"$name: ${e.getMessage}")
  }
}
