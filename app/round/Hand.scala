package lila
package round

import ai.Ai
import game.{ GameRepo, PgnRepo, Pov, PovRef, Handler }
import i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import chess.Role
import chess.Pos.posAt
import chess.format.Forsyth

import scalaz.effects._
import akka.actor._
import akka.dispatch.{ Future, Await }
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout

final class Hand(
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    messenger: Messenger,
    takeback: Takeback,
    ai: () ⇒ Ai,
    finisher: Finisher,
    hubMaster: ActorRef,
    moretimeSeconds: Int) extends Handler(gameRepo) {

  type IOValidEvents = IO[Valid[List[Event]]]
  type PlayResult = Future[Valid[(List[Event], String, Option[String])]]

  def play(
    povRef: PovRef,
    origString: String,
    destString: String,
    promString: Option[String] = None,
    blur: Boolean = false,
    lag: Int = 0): PlayResult = fromPovFuture(povRef) {
    case Pov(g1, color) ⇒ (for {
      g2 ← g1.validIf(g1 playableBy color, "Game not playable %s %s, on move %d - %s".format(origString, destString, g1.toChess.fullMoveNumber, g1))
      orig ← posAt(origString) toValid "Wrong orig " + origString
      dest ← posAt(destString) toValid "Wrong dest " + destString
      promotion = Role promotable promString
      chessGame = g2.toChess withPgnMoves (pgnRepo unsafeGet g2.id)
      newChessGameAndMove ← chessGame(orig, dest, promotion, lag)
      (newChessGame, move) = newChessGameAndMove
    } yield g2.update(newChessGame, move, blur)).prefixFailuresWith(povRef + " - ").fold(
      e ⇒ Future(failure(e)), {
        case (progress, pgn) ⇒ if (progress.game.finished) (for {
          _ ← gameRepo save progress
          _ ← pgnRepo.save(povRef.gameId, pgn)
          finishEvents ← finisher.moveFinish(progress.game, color)
          events = progress.events ::: finishEvents
        } yield playResult(events, progress)).toFuture
        else if (progress.game.player.isAi && progress.game.playable) for {
          initialFen ← progress.game.variant.standard.fold(
            io(none[String]),
            gameRepo initialFen progress.game.id).toFuture
          aiResult ← ai().play(progress.game, pgn, initialFen)
          eventsAndFen ← aiResult.fold(
            err ⇒ Future(failure(err)), {
              case (newChessGame, move) ⇒ {
                val (prog2, pgn2) = progress.game.update(newChessGame, move)
                val progress2 = progress flatMap { _ ⇒ prog2 }
                (for {
                  _ ← gameRepo save progress2
                  _ ← pgnRepo.save(povRef.gameId, pgn2)
                  finishEvents ← finisher.moveFinish(progress2.game, !color)
                  events = progress2.events ::: finishEvents
                } yield playResult(events, progress2)).toFuture
              }
            }): PlayResult
        } yield eventsAndFen
        else (for {
          _ ← gameRepo save progress
          _ ← pgnRepo.save(povRef.gameId, pgn)
          events = progress.events
        } yield playResult(events, progress)).toFuture
      })
  }

  private def playResult(events: List[Event], progress: Progress) = success((
    events,
    Forsyth exportBoard progress.game.toChess.board,
    progress.game.lastMove
  ))

  def abort(fullId: String): IOValidEvents = attempt(fullId, finisher.abort)

  def resign(fullId: String): IOValidEvents = attempt(fullId, finisher.resign)

  def resignForce(fullId: String): IO[Valid[List[Event]]] =
    gameRepo pov fullId flatMap { povOption ⇒
      (povOption toValid "No such game" flatMap { pov ⇒
        implicit val timeout = Timeout(1 second)
        Await.result(
          hubMaster ? round.IsGone(pov.game.id, !pov.color) map {
            case true ⇒ finisher resignForce pov
            case _    ⇒ !!("Opponent is not gone")
          },
          1 second
        )
      }).fold(err ⇒ io(failure(err)), _ map success)
    }

  def outoftime(ref: PovRef): IOValidEvents = attemptRef(ref, finisher outoftime _.game)

  def drawClaim(fullId: String): IOValidEvents = attempt(fullId, finisher.drawClaim)

  def drawAccept(fullId: String): IOValidEvents = attempt(fullId, finisher.drawAccept)

  def drawOffer(fullId: String): IOValidEvents = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (g1 playerCanOfferDraw color) {
        if (g1.player(!color).isOfferingDraw) finisher drawAccept pov
        else success {
          for {
            p1 ← messenger.systemMessage(g1, _.drawOfferSent) map { es ⇒
              Progress(g1, Event.ReloadTable(!color) :: es)
            }
            p2 = p1 map { g ⇒ g.updatePlayer(color, _ offerDraw g.turns) }
            _ ← gameRepo save p2
          } yield p2.events
        }
      }
      else !!("invalid draw offer " + fullId)
  })

  def drawCancel(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (pov.player.isOfferingDraw) success {
        for {
          p1 ← messenger.systemMessage(g1, _.drawOfferCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeDrawOffer) }
          _ ← gameRepo save p2
        } yield p2.events
      }
      else !!("no draw offer to cancel " + fullId)
  })

  def drawDecline(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (g1.player(!color).isOfferingDraw) success {
        for {
          p1 ← messenger.systemMessage(g1, _.drawOfferDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeDrawOffer) }
          _ ← gameRepo save p2
        } yield p2.events
      }
      else !!("no draw offer to decline " + fullId)
  })

  def rematchCancel(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      pov.player.isOfferingRematch.fold(
        success(for {
          p1 ← messenger.systemMessage(g1, _.rematchOfferCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeRematchOffer) }
          _ ← gameRepo save p2
        } yield p2.events),
        !!("no rematch offer to cancel " + fullId)
      )
  })

  def rematchDecline(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      g1.player(!color).isOfferingRematch.fold(
        success(for {
          p1 ← messenger.systemMessage(g1, _.rematchOfferDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeRematchOffer) }
          _ ← gameRepo save p2
        } yield p2.events),
        !!("no rematch offer to decline " + fullId)
      )
  })

  def takebackAccept(fullId: String): IOValidEvents = fromPov(fullId) { pov ⇒
    if (pov.opponent.isProposingTakeback && pov.game.nonTournament) for {
      fen ← gameRepo initialFen pov.game.id
      pgn ← pgnRepo get pov.game.id
      res ← takeback(pov.game, pgn, fen).sequence
    } yield res
    else io {
      !!("opponent is not proposing a takeback")
    }
  }

  def takebackOffer(fullId: String): IOValidEvents = fromPov(fullId) {
    case pov @ Pov(g1, color) ⇒
      if (g1.playable && g1.bothPlayersHaveMoved && g1.nonTournament) {
        for {
          fen ← gameRepo initialFen pov.game.id
          pgn ← pgnRepo get pov.game.id
          result ← if (g1.player(!color).isAi)
            takeback.double(pov.game, pgn, fen).sequence
          else if (g1.player(!color).isProposingTakeback)
            takeback(pov.game, pgn, fen).sequence
          else for {
            p1 ← messenger.systemMessage(g1, _.takebackPropositionSent) map { es ⇒
              Progress(g1, Event.ReloadTable(!color) :: es)
            }
            p2 = p1 map { g ⇒ g.updatePlayer(color, _.proposeTakeback) }
            _ ← gameRepo save p2
          } yield success(p2.events)
        } yield result
      }
      else io {
        !!("invalid takeback proposition " + fullId)
      }
  }

  def takebackCancel(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (pov.player.isProposingTakeback) success {
        for {
          p1 ← messenger.systemMessage(g1, _.takebackPropositionCanceled) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeTakebackProposition) }
          _ ← gameRepo save p2
        } yield p2.events
      }
      else !!("no takeback proposition to cancel " + fullId)
  })

  def takebackDecline(fullId: String): IO[Valid[List[Event]]] = attempt(fullId, {
    case pov @ Pov(g1, color) ⇒
      if (g1.player(!color).isProposingTakeback) success {
        for {
          p1 ← messenger.systemMessage(g1, _.takebackPropositionDeclined) map { es ⇒
            Progress(g1, Event.ReloadTable(!color) :: es)
          }
          p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeTakebackProposition) }
          _ ← gameRepo save p2
        } yield p2.events
      }
      else !!("no takeback proposition to decline " + fullId)
  })

  def moretime(ref: PovRef): IO[Valid[List[Event]]] = attemptRef(ref, pov ⇒
    pov.game.clock filter (_ ⇒ pov.game.moretimeable) map { clock ⇒
      val color = !pov.color
      val newClock = clock.giveTime(color, moretimeSeconds)
      val progress = pov.game withClock newClock
      for {
        events ← messenger.systemMessage(
          progress.game, ((_.untranslated(
            "%s + %d seconds".format(color, moretimeSeconds)
          )): SelectI18nKey)
        )
        progress2 = progress ++ (Event.Clock(newClock) :: events)
        _ ← gameRepo save progress2
      } yield progress2.events
    } toValid "cannot add moretime"
  )
}
