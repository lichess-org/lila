package lila.round

import actorApi._, round._
import lila.ai.Ai
import lila.game.{ Game, GameRepo, PgnRepo, Pov, PovRef, PlayerRef, Event, Progress }
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import lila.socket.actorApi.Forward
import chess.{ Status, Role, Color }
import chess.Pos.posAt
import chess.format.Forsyth
import makeTimeout.large

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.{ ask, pipe }

private[round] final class Round(
    gameId: String,
    messenger: Messenger,
    takebacker: Takebacker,
    ai: Ai,
    finisher: Finisher,
    rematcher: Rematcher,
    drawer: Drawer,
    socketHub: ActorRef,
    moretimeDuration: Duration) extends Actor {

  context setReceiveTimeout 30.seconds

  def receive = {

    case ReceiveTimeout                 ⇒ self ! PoisonPill

    // guaranty that all previous blocking events were performed
    case lila.hub.actorApi.map.Await(_) ⇒ sender ! ()

    case Send(events)                   ⇒ socketHub ! Forward(gameId, events)

    case Play(playerId, origS, destS, promS, blur, lag, onSuccess, onFailure) ⇒ handle(playerId) {
      case Pov(g1, color) ⇒ PgnRepo get g1.id flatMap { pgnString ⇒
        (for {
          g2 ← g1.validIf(g1 playableBy color, "Game not playable %s %s, on move %d".format(origS, destS, g1.toChess.fullMoveNumber))
          orig ← posAt(origS) toValid "Wrong orig " + origS
          dest ← posAt(destS) toValid "Wrong dest " + destS
          promotion = Role promotable promS
          chessGame = g2.toChess withPgnMoves pgnString
          newChessGameAndMove ← chessGame(orig, dest, promotion, lag)
          (newChessGame, move) = newChessGameAndMove
        } yield g2.update(newChessGame, move, blur)).prefixFailuresWith(playerId + " - ").fold(fufail(_), {
          case (progress, pgn) ⇒
            (GameRepo save progress) >> PgnRepo.save(gameId, pgn) >>
              progress.game.finished.fold(
                moveFinish(progress.game, color) map { finishEvents ⇒
                  playResult(progress.events ::: finishEvents, progress)
                }, {
                  if (progress.game.player.isAi && progress.game.playable)
                    self ! AiPlay(onSuccess, onFailure)
                  fuccess(playResult(progress.events, progress))
                })

        })
        //TODO test that line
      } addEffect onSuccess addFailureEffect onFailure map (_.events)
    }

    case AiPlay(onSuccess, onFailure) ⇒ handle { game ⇒
      game.player.isAi.fold(
        (game.variant.exotic ?? { GameRepo initialFen game.id }) zip
          (PgnRepo get game.id) flatMap {
            case (fen, pgn) ⇒
              ai.play(game.toChess, pgn, fen, ~game.aiLevel) flatMap {
                case (newChessGame, move) ⇒ {
                  val (progress, pgn2) = game.update(newChessGame, move)
                  (GameRepo save progress) >> PgnRepo.save(gameId, pgn2) >>
                    (moveFinish(progress.game, game.turnColor) map { finishEvents ⇒
                      playResult(progress.events ::: finishEvents, progress)
                    })
                }
              }
          } addEffect onSuccess addFailureEffect onFailure map (_.events),
        fufail("not AI turn")
      )
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

  private def moveFinish(game: Game, color: Color): Fu[List[Event]] = game.status match {
    case Status.Mate                               ⇒ finisher(game, _.Mate, Some(color))
    case status @ (Status.Stalemate | Status.Draw) ⇒ finisher(game, _ ⇒ status)
    case _                                         ⇒ fuccess(List[Event]())
  }

  private def playResult(events: List[Event], progress: Progress) = PlayResult(
    events,
    Forsyth exportBoard progress.game.toChess.board,
    progress.game.lastMove
  )

  protected def handle(playerId: String)(op: Pov ⇒ Fu[Events]) {
    blockAndPublish(GameRepo pov PlayerRef(gameId, playerId))(op)
  }

  protected def handle(color: Color)(op: Pov ⇒ Fu[Events]) {
    blockAndPublish(GameRepo pov PovRef(gameId, color))(op)
  }

  protected def handle[A](op: Game ⇒ Fu[Events]) {
    blockAndPublish(GameRepo game gameId)(op)
  }

  private def blockAndPublish[A](context: Fu[Option[A]])(op: A ⇒ Fu[List[Event]]) {
    try {
      val events = {
        context flatten "[round] not found" flatMap op
      } await 3.seconds
      if (events.nonEmpty) socketHub ! Forward(gameId, events)
    }
    catch {
      case e: lila.common.LilaException ⇒ logwarn("[round] " + e.message)
    }
  }
}
