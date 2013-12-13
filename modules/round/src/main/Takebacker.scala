package lila.round

import lila.game.{ GameRepo, Game, UciMemo, Pov, Rewind, Event, Progress }

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
    case _ ⇒ ClientErrorException.future("[takebacker] invalid yes " + pov)
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
    case _ ⇒ ClientErrorException.future("[takebacker] invalid no " + pov)
  }

  private def single(game: Game): Fu[Events] = for {
    fen <- GameRepo initialFen game.id 
    progress <- Rewind(game, fen).future 
    _ <- fuccess { uciMemo.drop(game, 1) }
    events <- save(progress)
  } yield events

  private def double(game: Game): Fu[Events] = for {
    fen ← GameRepo initialFen game.id
    prog1 ← Rewind(game, fen).future
    prog2 ← Rewind(prog1.game, fen).future map { progress =>
      prog1 withGame progress.game
    }
    _ ← fuccess { uciMemo.drop(game, 2) }
    events ← save(prog2)
  } yield events

  private def save(p1: Progress): Fu[Events] = {
    val p2 = p1 + Event.Reload
    messenger.systemMessage(p2.game, _.takebackPropositionAccepted) >>
      (GameRepo save p2) inject p2.events
  }
}
