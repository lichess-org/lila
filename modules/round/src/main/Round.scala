package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._, round._
import lila.game.{ Game, GameRepo, Pov, PovRef, PlayerRef, Event, Progress }
import lila.hub.actorApi.map._
import lila.hub.SequentialActor
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import makeTimeout.large

private[round] final class Round(
    gameId: String,
    messenger: Messenger,
    takebacker: Takebacker,
    finisher: Finisher,
    rematcher: Rematcher,
    player: Player,
    drawer: Drawer,
    socketHub: ActorRef,
    moretimeDuration: Duration) extends SequentialActor {

  context setReceiveTimeout 30.seconds

  def process = {

    case ReceiveTimeout ⇒ fuccess {
      self ! SequentialActor.Terminate
    }

    case p: HumanPlay ⇒ handle(p.playerId) { pov ⇒
      pov.game.outoftimePlayer.fold(player.human(p)(pov))(outOfTime(pov.game))
    }

    case AiPlay ⇒ handle { game ⇒
      player ai game map (_.events)
    }

    case Abort(playerId) ⇒ handle(playerId) { pov ⇒
      pov.game.abortable ?? finisher(pov.game, _.Aborted)
    }

    case AbortForce ⇒ handle { game ⇒
      game.playable ?? finisher(game, _.Aborted)
    }

    case Resign(playerId) ⇒ handle(playerId) { pov ⇒
      pov.game.resignable ?? finisher(pov.game, _.Resign, Some(!pov.color))
    }

    case ResignColor(color) ⇒ handle(color) { pov ⇒
      pov.game.resignable ?? finisher(pov.game, _.Resign, Some(!pov.color))
    }

    case ResignForce(playerId) ⇒ handle(playerId) { pov ⇒
      (pov.game.resignable && !pov.game.hasAi) ?? {
        socketHub ? Ask(pov.gameId, IsGone(!pov.color)) flatMap {
          case true ⇒ finisher(pov.game, _.Timeout, Some(pov.color))
          case _    ⇒ fufail("[round] cannot force resign of " + pov)
        }
      }
    }

    case DrawForce(playerId) ⇒ handle(playerId) { pov ⇒
      (pov.game.drawable && !pov.game.hasAi) ?? {
        socketHub ? Ask(pov.gameId, IsGone(!pov.color)) flatMap {
          case true ⇒ finisher(pov.game, _.Timeout, None)
          case _    ⇒ fufail("[round] cannot force draw of " + pov)
        }
      }
    }

    case Outoftime ⇒ handle { game ⇒
      game.outoftimePlayer ?? outOfTime(game)
    }

    // exceptionally we don't block nor publish events
    // if the game is abandoned, then nobody is around to see it
    // we can also terminate this actor
    case Abandon ⇒ fuccess {
      GameRepo game gameId foreach { gameOption ⇒
        gameOption filter (_.abandoned) foreach { game ⇒
          if (game.abortable) finisher(game, _.Aborted)
          else finisher(game, _.Resign, Some(!game.player.color))
          self ! PoisonPill
        }
      }
    }

    case DrawYes(playerRef)  ⇒ handle(playerRef)(drawer.yes)
    case DrawNo(playerRef)   ⇒ handle(playerRef)(drawer.no)
    case DrawClaim(playerId) ⇒ handle(playerId)(drawer.claim)
    case DrawForce           ⇒ handle(drawer force _)
    case Cheat(color) ⇒ handle(color) { pov ⇒
      pov.game.playable ?? finisher(pov.game, _.Cheat, Some(!pov.color))
    }

    case RematchYes(playerRef)  ⇒ handle(playerRef)(rematcher.yes)
    case RematchNo(playerRef)   ⇒ handle(playerRef)(rematcher.no)

    case TakebackYes(playerRef) ⇒ handle(playerRef)(takebacker.yes)
    case TakebackNo(playerRef)  ⇒ handle(playerRef)(takebacker.no)

    case Moretime(playerRef) ⇒ handle(playerRef) { pov ⇒
      pov.game.clock.filter(_ ⇒ pov.game.moretimeable) ?? { clock ⇒
        val newClock = clock.giveTime(!pov.color, moretimeDuration.toSeconds)
        val progress = (pov.game withClock newClock) + Event.Clock(newClock)
        messenger.system(progress.game, (_.untranslated(
          "%s + %d seconds".format(!pov.color, moretimeDuration.toSeconds)
        )))
        GameRepo save progress inject progress.events
      }
    }
  }

  private def outOfTime(game: Game)(p: lila.game.Player) =
    finisher(game, _.Outoftime, Some(!p.color) filter {
      chess.InsufficientMatingMaterial(game.toChess.board, _)
    })

  protected def handle[A](op: Game ⇒ Fu[Events]): Funit =
    handleGame(GameRepo game gameId)(op)

  protected def handle(playerId: String)(op: Pov ⇒ Fu[Events]): Funit =
    handlePov(GameRepo pov PlayerRef(gameId, playerId))(op)

  protected def handle(color: chess.Color)(op: Pov ⇒ Fu[Events]): Funit =
    handlePov(GameRepo pov PovRef(gameId, color))(op)

  private def handlePov(pov: Fu[Option[Pov]])(op: Pov ⇒ Fu[Events]): Funit = publish {
    pov flatten "pov not found" flatMap { p ⇒
      if (p.player.isAi) fufail("player can't play AI") else op(p)
    }
  }

  private def handleGame(game: Fu[Option[Game]])(op: Game ⇒ Fu[Events]): Funit = publish {
    game flatten "game not found" flatMap op
  }

  private def publish[A](op: Fu[Events]) = op addEffect {
    events ⇒ if (events.nonEmpty) socketHub ! Tell(gameId, events)
  } addFailureEffect {
    case e: ClientErrorException ⇒
    case e                       ⇒ logwarn("[round] " + e)
  } void
}
