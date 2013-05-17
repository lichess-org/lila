package lila.round

import actorApi._, round._
import lila.ai.Ai
import lila.game.{ GameRepo, PgnRepo, Pov, PovRef, Handler, Event, Progress }
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import chess.{ Role, Color }
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
    socketHub: ActorRef,
    moretimeDuration: Duration) extends Actor with Handler {

  context setReceiveTimeout 30.seconds

  private type PlayResult = (Events, String, Option[String])

  def receive = {

    case ReceiveTimeout ⇒ self ! PoisonPill

    case Play(color, origS, destS, promS, blur, lag) ⇒ sender ! {
      val povRef = as(color)
      fromPov[PlayResult](povRef) {
        case Pov(g1, color) ⇒ PgnRepo get g1.id flatMap { pgnString ⇒
          (for {
            g2 ← g1.validIf(g1 playableBy color, "Game not playable %s %s, on move %d".format(origS, destS, g1.toChess.fullMoveNumber))
            orig ← posAt(origS) toValid "Wrong orig " + origS
            dest ← posAt(destS) toValid "Wrong dest " + destS
            promotion = Role promotable promS
            chessGame = g2.toChess withPgnMoves pgnString
            newChessGameAndMove ← chessGame(orig, dest, promotion, lag)
            (newChessGame, move) = newChessGameAndMove
          } yield g2.update(newChessGame, move, blur)).prefixFailuresWith(povRef + " - ").fold(fufail(_), {
            case (progress, pgn) ⇒
              if (progress.game.finished)
                (GameRepo save progress) >>
                  PgnRepo.save(povRef.gameId, pgn) >>
                  finisher.moveFinish(progress.game, color) map { finishEvents ⇒
                    playResult(progress.events ::: finishEvents, progress)
                  }
              else if (progress.game.player.isAi && progress.game.playable) for {
                initialFen ← progress.game.variant.exotic ?? {
                  GameRepo initialFen progress.game.id
                }
                aiResult ← ai.play(progress.game.toChess, pgn, initialFen, ~progress.game.aiLevel)
                eventsAndFen ← aiResult match {
                  case (newChessGame, move) ⇒ {
                    val (prog2, pgn2) = progress.game.update(newChessGame, move)
                    val progress2 = progress >> prog2
                    (GameRepo save progress2) >>
                      PgnRepo.save(povRef.gameId, pgn2) >>
                      finisher.moveFinish(progress2.game, !color) map { finishEvents ⇒
                        playResult(progress2.events ::: finishEvents, progress2)
                      }
                  }
                }
              } yield eventsAndFen
              else (GameRepo save progress) >>
                PgnRepo.save(povRef.gameId, pgn) inject
                playResult(progress.events, progress)
          })
        }
      }
    }.await

    case Abort(fullId)         ⇒ sender ! attempt(fullId, finisher.abort).await

    case Resign(fullId, false) ⇒ sender ! attempt(fullId, finisher.resign).await

    case Resign(fullId, true) ⇒ sender ! {
      GameRepo pov fullId flatMap {
        _.fold[FuEvents](fufail("No such game")) { pov ⇒
          (socketHub ? IsGone(pov.game.id, !pov.color) flatMap {
            case true ⇒ finisher resignForce pov
            case _    ⇒ fufail[Events]("Opponent is not gone")
          })
        }
      }
    }.await

    case Outoftime(color)   ⇒ sender ! attemptRef(as(color), finisher outoftime _.game).await

    case DrawClaim(fullId)  ⇒ sender ! attempt(fullId, finisher.drawClaim).await

    case DrawAccept(fullId) ⇒ sender ! attempt(fullId, finisher.drawAccept).await

    case DrawOffer(fullId) ⇒ sender ! attempt(fullId, {
      case pov @ Pov(g1, color) ⇒
        if (g1 playerCanOfferDraw color) {
          if (g1.player(!color).isOfferingDraw) finisher drawAccept pov
          else for {
            p1 ← messenger.systemMessage(g1, _.drawOfferSent) map { es ⇒
              Progress(g1, Event.ReloadTable(!color) :: es)
            }
            p2 = p1 map { g ⇒ g.updatePlayer(color, _ offerDraw g.turns) }
            _ ← GameRepo save p2
          } yield p2.events
        }
        else fufail("invalid draw offer " + fullId)
    }).await

    case DrawCancel(fullId) ⇒ sender ! attempt(fullId, {
      case pov @ Pov(g1, color) ⇒
        if (pov.player.isOfferingDraw) for {
          p1 ← messenger.systemMessage(g1, _.drawOfferCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeDrawOffer) }
          _ ← GameRepo save p2
        } yield p2.events
        else fufail("no draw offer to cancel " + fullId)
    }).await

    case DrawDecline(fullId) ⇒ sender ! attempt(fullId, {
      case pov @ Pov(g1, color) ⇒
        if (g1.player(!color).isOfferingDraw) for {
          p1 ← messenger.systemMessage(g1, _.drawOfferDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeDrawOffer) }
          _ ← GameRepo save p2
        } yield p2.events
        else fufail("no draw offer to decline " + fullId)
    }).await

    case RematchCancel(fullId) ⇒ sender ! attempt(fullId, {
      case pov @ Pov(g1, color) ⇒
        if (pov.player.isOfferingRematch) for {
          p1 ← messenger.systemMessage(g1, _.rematchOfferCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeRematchOffer) }
          _ ← GameRepo save p2
        } yield p2.events
        else fufail("no rematch offer to cancel " + fullId)
    }).await

    case RematchDecline(fullId) ⇒ sender ! attempt(fullId, {
      case pov @ Pov(g1, color) ⇒
        if (g1.player(!color).isOfferingRematch) for {
          p1 ← messenger.systemMessage(g1, _.rematchOfferDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeRematchOffer) }
          _ ← GameRepo save p2
        } yield p2.events
        else fufail("no rematch offer to decline " + fullId)
    }).await

    case TakebackAccept(fullId) ⇒ sender ! {
      fromPov(fullId) { pov ⇒
        if (pov.opponent.isProposingTakeback && pov.game.nonTournament) for {
          fen ← GameRepo initialFen pov.game.id
          pgn ← PgnRepo get pov.game.id
          res ← takeback(pov.game, pgn, fen)
        } yield res
        else fufail("opponent is not proposing a takeback")
      }
    }.await

    case TakebackOffer(fullId) ⇒ sender ! {
      fromPov(fullId) {
        case pov @ Pov(g1, color) ⇒
          if (g1.playable && g1.bothPlayersHaveMoved && g1.nonTournament) for {
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
          } yield result
          else fufail[Events]("invalid takeback proposition " + fullId)
      }
    }.await

    case TakebackCancel(fullId) ⇒ sender ! attempt(fullId, {
      case pov @ Pov(g1, color) ⇒
        if (pov.player.isProposingTakeback) for {
          p1 ← messenger.systemMessage(g1, _.takebackPropositionCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeTakebackProposition) }
          _ ← GameRepo save p2
        } yield p2.events
        else fufail("no takeback proposition to cancel " + fullId)
    }).await

    case TakebackDecline(fullId) ⇒ sender ! attempt(fullId, {
      case pov @ Pov(g1, color) ⇒
        if (g1.player(!color).isProposingTakeback) for {
          p1 ← messenger.systemMessage(g1, _.takebackPropositionDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeTakebackProposition) }
          _ ← GameRepo save p2
        } yield p2.events
        else fufail("no takeback proposition to decline " + fullId)
    }).await

    case Moretime(color) ⇒ sender ! attemptRef(as(color), pov ⇒
      pov.game.clock.filter(_ ⇒ pov.game.moretimeable).fold[FuEvents](fufail("cannot add moretime")) { clock ⇒
        val color = !pov.color
        val newClock = clock.giveTime(color, moretimeDuration.toSeconds)
        val progress = pov.game withClock newClock
        for {
          events ← messenger.systemMessage(
            progress.game, ((_.untranslated(
              "%s + %d seconds".format(color, moretimeDuration.toSeconds)
            )): SelectI18nKey)
          )
          progress2 = progress ++ (Event.Clock(newClock) :: events)
          _ ← GameRepo save progress2
        } yield progress2.events
      }
    ).await
  }

  private def as(color: Color) = PovRef(gameId, color)

  private def playResult(events: List[Event], progress: Progress) = (
    events,
    Forsyth exportBoard progress.game.toChess.board,
    progress.game.lastMove
  )
}
