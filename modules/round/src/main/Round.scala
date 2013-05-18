package lila.round

import actorApi._, round._
import lila.game.{ Game, GameRepo, PgnRepo, Pov, PovRef, PlayerRef, Event, Progress }
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import lila.socket.actorApi.Forward
import makeTimeout.large

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.{ ask, pipe }

private[round] final class Round(
    gameId: String,
    messenger: Messenger,
    takebacker: Takebacker,
    finisher: Finisher,
    rematcher: Rematcher,
    player: Player,
    drawer: Drawer,
    socketHub: ActorRef,
    moretimeDuration: Duration) extends Actor {

  context setReceiveTimeout 30.seconds

  def receive = {

    case ReceiveTimeout                 ⇒ self ! PoisonPill

    case Send(events)                   ⇒ socketHub ! Forward(gameId, events)

    case p: HumanPlay                   ⇒ handle(p.playerId)(player human p)

    case p: AiPlay                      ⇒ blockAndPublish(GameRepo game gameId, 10.seconds)(player ai p)

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
        socketHub ? IsGone(pov.game.id, !pov.color) flatMap {
          case true ⇒ finisher(pov.game, _.Timeout, Some(pov.color))
        }
      }
    }

    case Outoftime ⇒ handle { game ⇒
      game.outoftimePlayer ?? { player ⇒
        finisher(game, _.Outoftime, Some(!player.color) filter game.toChess.board.hasEnoughMaterialToMate)
      }
    }

    case DrawYes(playerRef)     ⇒ handle(playerRef)(drawer.yes)
    case DrawNo(playerRef)      ⇒ handle(playerRef)(drawer.no)
    case DrawClaim(playerId)    ⇒ handle(playerId)(drawer.claim)
    case DrawForce              ⇒ handle(drawer force _)

    case RematchYes(playerRef)  ⇒ handle(playerRef)(rematcher.yes)
    case RematchNo(playerRef)   ⇒ handle(playerRef)(rematcher.no)

    case TakebackYes(playerRef) ⇒ handle(playerRef)(takebacker.yes)
    case TakebackNo(playerRef)  ⇒ handle(playerRef)(takebacker.no)

    case Moretime(playerRef) ⇒ handle(playerRef) { pov ⇒
      pov.game.clock.filter(_ ⇒ pov.game.moretimeable) ?? { clock ⇒
        val newClock = clock.giveTime(!pov.color, moretimeDuration.toSeconds)
        val progress = pov.game withClock newClock
        messenger.systemMessage(progress.game, (_.untranslated(
          "%s + %d seconds".format(!pov.color, moretimeDuration.toSeconds)
        ))) flatMap { events ⇒
          val progress2 = progress ++ (Event.Clock(newClock) :: events)
          GameRepo save progress2 inject progress2.events
        }
      }
    }
  }

  protected def handle(playerId: String)(op: Pov ⇒ Fu[Events]) {
    blockAndPublish(GameRepo pov PlayerRef(gameId, playerId))(op)
  }

  protected def handle(color: chess.Color)(op: Pov ⇒ Fu[Events]) {
    blockAndPublish(GameRepo pov PovRef(gameId, color))(op)
  }

  protected def handle[A](op: Game ⇒ Fu[Events]) {
    blockAndPublish(GameRepo game gameId)(op)
  }

  private def blockAndPublish[A](context: Fu[Option[A]], timeout: FiniteDuration = 3.seconds)(op: A ⇒ Fu[Events]) {
    try {
      val events = {
        context flatten "[round] not found" flatMap op
      } await makeTimeout(timeout)
      if (events.nonEmpty) socketHub ! Forward(gameId, events)
    }
    catch {
      case e: lila.common.LilaException ⇒ logwarn("[round] " + e.message)
    }
  }
}
