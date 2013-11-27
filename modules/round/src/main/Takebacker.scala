package lila.round

import lila.game.{ GameRepo, Game, PgnRepo, UciMemo, Pov, Rewind, Event, Progress }

private[round] final class Takebacker(
  messenger: Messenger,
  uciMemo: UciMemo) {

  def yes(pov: Pov): Fu[Events] = pov match {
    case Pov(game, _) if pov.opponent.isProposingTakeback ⇒ single(game)
    case Pov(game, _) if pov.opponent.isAi                ⇒ double(game)
    case Pov(game, color) if (game playerCanProposeTakeback color) ⇒ for {
      p1 ← messenger.systemMessage(game, _.takebackPropositionSent) map { es ⇒
        Progress(game, Event.ReloadTablesOwner :: es)
      }
      p2 = p1 map { g ⇒ g.updatePlayer(color, _.proposeTakeback) }
      _ ← GameRepo save p2
    } yield p2.events
    case _ ⇒ fufail("[takebacker] invalid yes " + pov)
  }
  def no(pov: Pov): Fu[Events] = pov match {
    case Pov(game, color) if pov.player.isProposingTakeback ⇒ for {
      p1 ← messenger.systemMessage(game, _.takebackPropositionCanceled) map { es ⇒
        Progress(game, Event.ReloadTablesOwner :: es)
      }
      p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeTakebackProposition) }
      _ ← GameRepo save p2
    } yield p2.events
    case Pov(game, color) if pov.opponent.isProposingTakeback ⇒ for {
      p1 ← messenger.systemMessage(game, _.takebackPropositionDeclined) map { es ⇒
        Progress(game, Event.ReloadTablesOwner :: es)
      }
      p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeTakebackProposition) }
      _ ← GameRepo save p2
    } yield p2.events
    case _ ⇒ fufail("[takebacker] invalid no " + pov)
  }

  private def extras(gameId: String): Fu[(Option[String], List[String])] =
    (GameRepo initialFen gameId) zip (PgnRepo get gameId)

  private def single(game: Game): Fu[Events] = extras(game.id) flatMap {
    case (fen, moves) ⇒ Rewind(game, moves, fen).future flatMap {
      case (progress, newPgn) ⇒ 
        PgnRepo.save(game.id, newPgn) >>-
        uciMemo.drop(game, 1)
        save(progress) 
    }
  }

  private def double(game: Game): Fu[Events] = extras(game.id) flatMap {
    case (fen, pgn) ⇒ for {
      first ← Rewind(game, pgn, fen).future
      (prog1, pgn1) = first
      second ← Rewind(prog1.game, pgn1, fen).future map {
        case (progress, newPgn) ⇒ (prog1 withGame progress.game, newPgn)
      }
      (prog2, pgn2) = second
      _ ← PgnRepo.save(game.id, pgn2) >>- uciMemo.drop(game, 2)
      events ← save(prog2)
    } yield events
  }

  private def save(p1: Progress): Fu[Events] = {
    val p2 = p1 + Event.Reload
    messenger.systemMessage(p1.game, _.takebackPropositionAccepted) >>
      (GameRepo save p2) inject p2.events
  }
}
