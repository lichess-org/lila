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
    takeback: Takeback,
    ai: Ai,
    finisher: Finisher,
    rematcher: Rematcher,
    notifyMove: (String, String, Option[String]) ⇒ Unit,
    socketHub: ActorRef,
    moretimeDuration: Duration) extends Actor {

  context setReceiveTimeout 30.seconds

  def receive = {

    case ReceiveTimeout ⇒ self ! PoisonPill

    // guaranty that all previous blocking events were performed
    case Await          ⇒ sender ! ()

    case Send(events)   ⇒ publish(events)

    case Play(playerId, origS, destS, promS, blur, lag) ⇒ blocking[PlayResult](playerId) {
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
            if (progress.game.finished)
              (GameRepo save progress) >>
                PgnRepo.save(gameId, pgn) >>
                moveFinish(progress.game, color) map { finishEvents ⇒
                  playResult(progress.events ::: finishEvents, progress)
                }
            else if (progress.game.player.isAi && progress.game.playable) for {
              initialFen ← progress.game.variant.exotic ?? {
                GameRepo initialFen progress.game.id
              }
              // TODO unblock AI
              aiResult ← ai.play(progress.game.toChess, pgn, initialFen, ~progress.game.aiLevel)
              eventsAndFen ← aiResult match {
                case (newChessGame, move) ⇒ {
                  val (prog2, pgn2) = progress.game.update(newChessGame, move)
                  val progress2 = progress >> prog2
                  (GameRepo save progress2) >>
                    PgnRepo.save(gameId, pgn2) >>
                    moveFinish(progress2.game, !color) map { finishEvents ⇒
                      playResult(progress2.events ::: finishEvents, progress2)
                    }
                }
              }
            } yield eventsAndFen
            else (GameRepo save progress) >>
              PgnRepo.save(gameId, pgn) inject
              playResult(progress.events, progress)
        })
      }
    } ~ {
      case PlayResult(events, fen, lastMove) ⇒ {
        publish(events)
        notifyMove(gameId, fen, lastMove)
      }
    }

    case Abort(playerId) ⇒ publishing(playerId) { pov ⇒
      pov.game.abortable ?? finisher(pov.game, _.Aborted)
    }

    case AbortForce ⇒ publishing { game ⇒
      game.playable ?? finisher(game, _.Aborted)
    }

    case Resign(playerId) ⇒ publishing(playerId) { pov ⇒
      pov.game.resignable ?? finisher(pov.game, _.Resign, Some(!pov.color))
    }

    case ResignColor(color) ⇒ publishing(color) { pov ⇒
      pov.game.resignable ?? finisher(pov.game, _.Resign, Some(!pov.color))
    }

    case ResignForce(playerId) ⇒ publishing(playerId) { pov ⇒
      (pov.game.resignable && !pov.game.hasAi) ?? {
        socketHub ? IsGone(pov.game.id, !pov.color) flatMap {
          case true ⇒ finisher(pov.game, _.Timeout, Some(pov.color))
        }
      }
    }

    case Outoftime ⇒ publishing { game ⇒
      game.outoftimePlayer ?? { player ⇒
        finisher(game, _.Outoftime, Some(!player.color) filter game.toChess.board.hasEnoughMaterialToMate)
      }
    }

    case DrawClaim(playerId) ⇒ publishing(playerId) { pov ⇒
      (pov.game.playable &&
        pov.game.player.color == pov.color &&
        pov.game.toChessHistory.threefoldRepetition
      ) ?? finisher(pov.game, _.Draw)
    }

    case DrawAccept(playerId) ⇒ publishing(playerId) { pov ⇒
      pov.opponent.isOfferingDraw ?? finisher(pov.game, _.Draw, None, Some(_.drawOfferAccepted))
    }

    case DrawForce ⇒ publishing { game ⇒
      finisher(game, _.Draw, None, None)
    }

    case DrawOffer(playerId) ⇒ publishing(playerId) {
      case pov @ Pov(g1, color) ⇒ (g1 playerCanOfferDraw color) ?? {
        if (g1.player(!color).isOfferingDraw)
          finisher(pov.game, _.Draw, None, Some(_.drawOfferAccepted))
        else for {
          p1 ← messenger.systemMessage(g1, _.drawOfferSent) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _ offerDraw g.turns) }
          _ ← GameRepo save p2
        } yield p2.events
      }
    }

    case DrawCancel(playerRef) ⇒ publishing(playerRef) {
      case pov @ Pov(g1, color) ⇒ (pov.player.isOfferingDraw) ?? (for {
        p1 ← messenger.systemMessage(g1, _.drawOfferCanceled) map { es ⇒
          Progress(g1, Event.ReloadTable(!color) :: es)
        }
        p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeDrawOffer) }
        _ ← GameRepo save p2
      } yield p2.events)
    }

    case DrawDecline(playerRef) ⇒ publishing(playerRef) {
      case pov @ Pov(g1, color) ⇒ (g1.player(!color).isOfferingDraw) ?? (for {
        p1 ← messenger.systemMessage(g1, _.drawOfferDeclined) map { es ⇒
          Progress(g1, Event.ReloadTable(!color) :: es)
        }
        p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeDrawOffer) }
        _ ← GameRepo save p2
      } yield p2.events)
    }

    case RematchYes(playerRef) ⇒ publishing(playerRef)(rematcher.yes) 
    case RematchNo(playerRef)  ⇒ publishing(playerRef)(rematcher.no) 

    case TakebackAccept(playerRef) ⇒ publishing(playerRef) { pov ⇒
      (pov.opponent.isProposingTakeback && pov.game.nonTournament) ?? (for {
        fen ← GameRepo initialFen pov.game.id
        pgn ← PgnRepo get pov.game.id
        res ← takeback(pov.game, pgn, fen)
      } yield res)
    }

    case TakebackOffer(playerRef) ⇒ publishing(playerRef) {
      case pov @ Pov(g1, color) ⇒
        (g1.playable && g1.bothPlayersHaveMoved && g1.nonTournament) ?? (for {
          fen ← GameRepo initialFen pov.game.id
          pgn ← PgnRepo get pov.game.id
          result ← if (g1.player(!color).isAi)
            takeback.double(pov.game, pgn, fen)
          else if (g1.player(!color).isProposingTakeback)
            takeback(pov.game, pgn, fen)
          else for {
            p1 ← messenger.systemMessage(g1, _.takebackPropositionSent) map { es ⇒
              Progress(g1, Event.ReloadTable(!color) :: es)
            }
            p2 = p1 map { g ⇒ g.updatePlayer(color, _.proposeTakeback) }
            _ ← GameRepo save p2
          } yield p2.events
        } yield result)
    }

    case TakebackCancel(playerRef) ⇒ publishing(playerRef) {
      case pov @ Pov(g1, color) ⇒
        (pov.player.isProposingTakeback) ?? (for {
          p1 ← messenger.systemMessage(g1, _.takebackPropositionCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeTakebackProposition) }
          _ ← GameRepo save p2
        } yield p2.events)
    }

    case TakebackDecline(playerRef) ⇒ publishing(playerRef) {
      case pov @ Pov(g1, color) ⇒
        (g1.player(!color).isProposingTakeback) ?? (for {
          p1 ← messenger.systemMessage(g1, _.takebackPropositionDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeTakebackProposition) }
          _ ← GameRepo save p2
        } yield p2.events)
    }

    case Moretime(playerRef) ⇒ publishing(playerRef) { pov ⇒
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

  private def publish(events: List[Event]) {
    if (events.nonEmpty) socketHub ! Forward(gameId, events)
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

  protected def blocking[A](playerId: String)(op: Pov ⇒ Fu[A]): A = {
    GameRepo pov PlayerRef(gameId, playerId) flatten "No such game" flatMap op
  }.await

  protected def publishing(playerId: String)(op: Pov ⇒ Fu[Events]) {
    blocking(playerId)(op) ~ publish
  }

  protected def publishing(color: Color)(op: Pov ⇒ Fu[Events]) {
    {
      GameRepo pov PovRef(gameId, color) flatten "No such game" flatMap op
    }.await ~ publish
  }

  protected def publishing[A](op: Game ⇒ Fu[Events]) {
    {
      GameRepo game gameId flatten "No such game" flatMap op
    }.await ~ publish
  }
}
