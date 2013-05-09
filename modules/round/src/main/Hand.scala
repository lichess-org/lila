package lila.round

import lila.ai.Ai
import lila.game.{ GameRepo, PgnRepo, Pov, PovRef, Handler, Event, Progress }
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import chess.Role
import chess.Pos.posAt
import chess.format.Forsyth
import actorApi._
import makeTimeout.large

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration.Duration

final class Hand(
    messenger: Messenger,
    takeback: Takeback,
    ai: Ai,
    finisher: Finisher,
    socketHub: ActorRef,
    moretimeDuration: Duration) extends Handler {

  type FuValidEvents = Fu[Valid[List[Event]]]
  type PlayResult = Fu[Valid[(List[Event], String, Option[String])]]

  def play(
    povRef: PovRef,
    origString: String,
    destString: String,
    promString: Option[String] = None,
    blur: Boolean = false,
    lag: Int = 0): PlayResult = fromPovFu(povRef) {
    case Pov(g1, color) ⇒ PgnRepo get g1.id flatMap { pgnString ⇒
      (for {
        g2 ← g1.validIf(g1 playableBy color, "Game not playable %s %s, on move %d".format(origString, destString, g1.toChess.fullMoveNumber))
        orig ← posAt(origString) toValid "Wrong orig " + origString
        dest ← posAt(destString) toValid "Wrong dest " + destString
        promotion = Role promotable promString
        chessGame = g2.toChess withPgnMoves pgnString
        newChessGameAndMove ← chessGame(orig, dest, promotion, lag)
        (newChessGame, move) = newChessGameAndMove
      } yield g2.update(newChessGame, move, blur)).prefixFailuresWith(povRef + " - ").fold(
        e ⇒ fuccess(failure(e)), {
          case (progress, pgn) ⇒ if (progress.game.finished)
            (GameRepo save progress) >>
              PgnRepo.save(povRef.gameId, pgn) >>
              finisher.moveFinish(progress.game, color) map { finishEvents ⇒
                playResult(progress.events ::: finishEvents, progress)
              }
          else if (progress.game.player.isAi && progress.game.playable) for {
            initialFen ← progress.game.variant.exotic ?? {
              GameRepo initialFen progress.game.id
            }
            aiResult ← ai.play(progress.game.toChess, pgn.pp, initialFen, ~progress.game.aiLevel)
            eventsAndFen ← aiResult.fold(
              err ⇒ fuccess(failure(err)), {
                case (newChessGame, move) ⇒ {
                  val (prog2, pgn2) = progress.game.update(newChessGame, move)
                  val progress2 = progress >> prog2
                  (GameRepo save progress2) >>
                    PgnRepo.save(povRef.gameId, pgn2) >>
                    finisher.moveFinish(progress2.game, !color) map { finishEvents ⇒
                      playResult(progress2.events ::: finishEvents, progress2)
                    }
                }
              }): PlayResult
          } yield eventsAndFen
          else (GameRepo save progress) >>
            PgnRepo.save(povRef.gameId, pgn) inject
            playResult(progress.events, progress)
        })
    }
  }

  private def playResult(events: List[Event], progress: Progress) = success((
    events,
    Forsyth exportBoard progress.game.toChess.board,
    progress.game.lastMove
  ))

  def abort(fullId: String): FuValidEvents = attempt(fullId, finisher.abort)

  def resign(fullId: String): FuValidEvents = attempt(fullId, finisher.resign)

  def resignForce(fullId: String): FuValidEvents =
    GameRepo pov fullId flatMap { povOption ⇒
      povOption.fold(fuccess(!!("No such game")): FuValidEvents) { pov ⇒
        (socketHub ? IsGone(pov.game.id, !pov.color) flatMap {
          case true ⇒ finisher resignForce pov fold (
            err ⇒ fuccess(failure(err)): FuValidEvents,
            fu ⇒ fu map { success(_) }
          )
          case _ ⇒ fuccess(!!("Opponent is not gone")): FuValidEvents
        })
      }
    }

  def outoftime(ref: PovRef): FuValidEvents = attemptRef(ref, finisher outoftime _.game)

  def drawClaim(fullId: String): FuValidEvents = attempt(fullId, finisher.drawClaim)

  def drawAccept(fullId: String): FuValidEvents = attempt(fullId, finisher.drawAccept)

  def drawOffer(fullId: String): FuValidEvents = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (g1 playerCanOfferDraw color) {
        if (g1.player(!color).isOfferingDraw) finisher drawAccept pov
        else success {
          for {
            p1 ← messenger.systemMessage(g1, _.drawOfferSent) map { es ⇒
              Progress(g1, Event.ReloadTable(!color) :: es)
            }
            p2 = p1 map { g ⇒ g.updatePlayer(color, _ offerDraw g.turns) }
            _ ← GameRepo save p2
          } yield p2.events
        }
      }
      else !!("invalid draw offer " + fullId)
  })

  def drawCancel(fullId: String): FuValidEvents = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (pov.player.isOfferingDraw) success {
        for {
          p1 ← messenger.systemMessage(g1, _.drawOfferCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeDrawOffer) }
          _ ← GameRepo save p2
        } yield p2.events
      }
      else !!("no draw offer to cancel " + fullId)
  })

  def drawDecline(fullId: String): FuValidEvents = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (g1.player(!color).isOfferingDraw) success {
        for {
          p1 ← messenger.systemMessage(g1, _.drawOfferDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeDrawOffer) }
          _ ← GameRepo save p2
        } yield p2.events
      }
      else !!("no draw offer to decline " + fullId)
  })

  def rematchCancel(fullId: String): FuValidEvents = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      pov.player.isOfferingRematch.fold(
        success(for {
          p1 ← messenger.systemMessage(g1, _.rematchOfferCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeRematchOffer) }
          _ ← GameRepo save p2
        } yield p2.events),
        !!("no rematch offer to cancel " + fullId)
      )
  })

  def rematchDecline(fullId: String): FuValidEvents = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      g1.player(!color).isOfferingRematch.fold(
        success(for {
          p1 ← messenger.systemMessage(g1, _.rematchOfferDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeRematchOffer) }
          _ ← GameRepo save p2
        } yield p2.events),
        !!("no rematch offer to decline " + fullId)
      )
  })

  def takebackAccept(fullId: String): FuValidEvents = fromPov(fullId) { pov ⇒
    if (pov.opponent.isProposingTakeback && pov.game.nonTournament) for {
      fen ← GameRepo initialFen pov.game.id
      pgn ← PgnRepo get pov.game.id
      res ← takeback(pov.game, pgn, fen).sequence
    } yield res
    else fuccess { !!("opponent is not proposing a takeback") }
  }

  def takebackOffer(fullId: String): FuValidEvents = fromPov(fullId) {
    case pov @ Pov(g1, color) ⇒
      if (g1.playable && g1.bothPlayersHaveMoved && g1.nonTournament) {
        for {
          fen ← GameRepo initialFen pov.game.id
          pgn ← PgnRepo get pov.game.id
          result ← if (g1.player(!color).isAi)
            takeback.double(pov.game, pgn, fen).sequence
          else if (g1.player(!color).isProposingTakeback)
            takeback(pov.game, pgn, fen).sequence
          else for {
            p1 ← messenger.systemMessage(g1, _.takebackPropositionSent) map { es ⇒
              Progress(g1, Event.ReloadTable(!color) :: es)
            }
            p2 = p1 map { g ⇒ g.updatePlayer(color, _.proposeTakeback) }
            _ ← GameRepo save p2
          } yield success(p2.events)
        } yield result
      }
      else fuccess { !!("invalid takeback proposition " + fullId) }
  }

  def takebackCancel(fullId: String): FuValidEvents = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (pov.player.isProposingTakeback) success {
        for {
          p1 ← messenger.systemMessage(g1, _.takebackPropositionCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeTakebackProposition) }
          _ ← GameRepo save p2
        } yield p2.events
      }
      else !!("no takeback proposition to cancel " + fullId)
  })

  def takebackDecline(fullId: String): FuValidEvents = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (g1.player(!color).isProposingTakeback) success {
        for {
          p1 ← messenger.systemMessage(g1, _.takebackPropositionDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeTakebackProposition) }
          _ ← GameRepo save p2
        } yield p2.events
      }
      else !!("no takeback proposition to decline " + fullId)
  })

  def moretime(ref: PovRef): FuValidEvents = attemptRef(ref, pov ⇒
    pov.game.clock filter (_ ⇒ pov.game.moretimeable) map { clock ⇒
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
    } toValid "cannot add moretime"
  )
}
