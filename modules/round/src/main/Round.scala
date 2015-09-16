package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._, round._
import lila.game.{ Game, GameRepo, Pov, PovRef, PlayerRef, Event, Progress }
import lila.hub.actorApi.map._
import lila.hub.actorApi.{ Deploy, RemindDeployPost }
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
    monitorMove: Int => Unit,
    moretimeDuration: Duration,
    activeTtl: Duration) extends SequentialActor {

  context setReceiveTimeout activeTtl

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'deploy)
  }

  override def postStop() {
    context.system.lilaBus unsubscribe self
  }

  def process = {

    case ReceiveTimeout => fuccess {
      self ! SequentialActor.Terminate
    }

    case p: HumanPlay =>
      handle(p.playerId) { pov =>
        pov.game.outoftimePlayer.fold(player.human(p, self)(pov))(outOfTime(pov.game))
      } >>- monitorMove((nowMillis - p.atMillis).toInt)

    case AiPlay => handle { game =>
      game.playableByAi ?? {
        player ai game map (_.events)
      }
    }

    case Abort(playerId) => handle(playerId) { pov =>
      pov.game.abortable ?? finisher.abort(pov)
    }

    case Resign(playerId) => handle(playerId) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case ResignColor(color) => handle(color) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case GoBerserk(color) => handle(color) { pov =>
      pov.game.goBerserk(color) ?? { progress =>
        messenger.system(pov.game, (_.untranslated(
          s"${pov.color.name.capitalize} is going berserk!"
        )))
        GameRepo.save(progress) >> GameRepo.goBerserk(pov) inject progress.events
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
      game.outoftimePlayer ?? outOfTime(game)
    }

    // exceptionally we don't block nor publish events
    // if the game is abandoned, then nobody is around to see it
    // we can also terminate this actor
    case Abandon => fuccess {
      GameRepo game gameId foreach { gameOption =>
        gameOption filter (_.abandoned) foreach { game =>
          if (game.abortable) finisher.other(game, _.Aborted)
          else finisher.other(game, _.Resign, Some(!game.player.color))
          self ! PoisonPill
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

    case Threefold => GameRepo game gameId flatMap {
      _ ?? drawer.autoThreefold map {
        _ foreach { pov =>
          self ! DrawClaim(pov.player.id)
        }
      }
    }

    case HoldAlert(playerId, mean, sd) => handle(playerId) { pov =>
      !pov.player.hasHoldAlert ?? {
        loginfo(s"hold alert http://lichess.org/${pov.gameId}/${pov.color.name}#${pov.game.turns} ${pov.player.userId | "anon"} mean: $mean SD: $sd")
        GameRepo.setHoldAlert(pov, mean, sd) inject List[Event]()
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
        GameRepo save progress inject progress.events
      }
    }

    case ForecastPlay(lastMove) => handle { game =>
      forecastApi.nextMove(game, lastMove) map { mOpt =>
        mOpt foreach { move =>
          self ! HumanPlay(
            game.player.id, "127.0.0.1", move.orig.key, move.dest.key, move.promotion.map(_.name), false, 0.seconds, _ => ()
          )
        }
        Nil
      }
    }

    case Deploy(RemindDeployPost, _) => handle { game =>
      game.clock.filter(_ => game.playable) ?? { clock =>
        import chess.Color
        val freeSeconds = 15
        val newClock = clock.giveTime(Color.White, freeSeconds).giveTime(Color.Black, freeSeconds)
        val progress = (game withClock newClock) + Event.Clock(newClock)
        messenger.system(game, (_.untranslated("Lichess has been updated")))
        messenger.system(game, (_.untranslated("Sorry for the inconvenience!")))
        Color.all.foreach { c =>
          messenger.system(game, (_.untranslated(s"$c + $freeSeconds seconds")))
        }
        GameRepo save progress inject progress.events
      }
    }

    case AbortForMaintenance => handle { game =>
      messenger.system(game, (_.untranslated("Game aborted for server maintenance")))
      messenger.system(game, (_.untranslated("Sorry for the inconvenience!")))
      game.playable ?? finisher.other(game, _.Aborted)
    }
  }

  private def outOfTime(game: Game)(p: lila.game.Player) =
    finisher.other(game, _.Outoftime, Some(!p.color) filterNot { color =>
      game.toChess.board.variant.drawsOnInsufficientMaterial &&
        chess.InsufficientMatingMaterial(game.toChess.board, color)
    })

  protected def handle[A](op: Game => Fu[Events]): Funit =
    handleGame(GameRepo game gameId)(op)

  protected def handle(playerId: String)(op: Pov => Fu[Events]): Funit =
    handlePov(GameRepo pov PlayerRef(gameId, playerId))(op)

  protected def handle(color: chess.Color)(op: Pov => Fu[Events]): Funit =
    handlePov(GameRepo pov PovRef(gameId, color))(op)

  private def handlePov(pov: Fu[Option[Pov]])(op: Pov => Fu[Events]): Funit = publish {
    pov flatten "pov not found" flatMap { p =>
      if (p.player.isAi) fufail("player can't play AI") else op(p)
    }
  }

  private def handleGame(game: Fu[Option[Game]])(op: Game => Fu[Events]): Funit = publish {
    game flatten "game not found" flatMap op
  }

  private def publish[A](op: Fu[Events]) = op addEffect { events =>
    if (events.nonEmpty) socketHub ! Tell(gameId, EventList(events))
    if (events exists {
      case e: Event.Move => e.threefold
      case _             => false
    }) self ! Threefold
  } addFailureEffect {
    case e: ClientErrorException =>
    case e                       => logwarn(s"[round] ${gameId} $e")
  } void
}
