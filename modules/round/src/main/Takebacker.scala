package lila.round

import lila.game.{ GameRepo, Game, UciMemo, Pov, Rewind, Event, Progress }
import lila.pref.{ Pref, PrefApi }

private[round] final class Takebacker(
    messenger: Messenger,
    uciMemo: UciMemo,
    prefApi: PrefApi) {

  def yes(pov: Pov): Fu[Events] = IfAllowed(pov.game) {
    pov match {
      case Pov(game, _) if pov.opponent.isProposingTakeback =>
        if (pov.opponent.proposeTakebackAt == pov.game.turns) single(game)
        else double(game)
      case Pov(game, _) if pov.opponent.isAi => double(game)
      case Pov(game, color) if (game playerCanProposeTakeback color) =>
        messenger.system(game, _.takebackPropositionSent)
        val progress = Progress(game) map { g =>
          g.updatePlayer(color, _ proposeTakeback g.turns)
        }
        GameRepo save progress inject List(Event.TakebackOffers(color.white, color.black))
      case _ => fufail(ClientError("[takebacker] invalid yes " + pov))
    }
  }

  def no(pov: Pov): Fu[Events] = IfAllowed(pov.game) {
    pov match {
      case Pov(game, color) if pov.player.isProposingTakeback => GameRepo save {
        messenger.system(game, _.takebackPropositionCanceled)
        Progress(game) map { g => g.updatePlayer(color, _.removeTakebackProposition) }
      } inject List(Event.TakebackOffers(false, false))
      case Pov(game, color) if pov.opponent.isProposingTakeback => GameRepo save {
        messenger.system(game, _.takebackPropositionDeclined)
        Progress(game) map { g => g.updatePlayer(!color, _.removeTakebackProposition) }
      } inject List(Event.TakebackOffers(false, false))
      case _ => fufail(ClientError("[takebacker] invalid no " + pov))
    }
  }

  def isAllowedByPrefs(game: Game): Fu[Boolean] =
    if (game.hasAi) fuccess(true)
    else game.userIds.map { userId =>
      prefApi.getPref(userId, (p: Pref) => p.takeback)
    }.sequenceFu map {
      _.forall { p =>
        p == Pref.Takeback.ALWAYS || (p == Pref.Takeback.CASUAL && game.casual)
      }
    }

  private def IfAllowed(game: Game)(f: => Fu[Events]): Fu[Events] =
    if (!game.playable) fufail(ClientError("[takebacker] game is over " + game.id))
    else isAllowedByPrefs(game) flatMap {
      _.fold(f, fufail(ClientError("[takebacker] disallowed by preferences " + game.id)))
    }

  private def single(game: Game): Fu[Events] = for {
    fen ← GameRepo initialFen game
    progress ← Rewind(game, fen).future
    _ ← fuccess { uciMemo.drop(game, 1) }
    events ← save(progress)
  } yield events

  private def double(game: Game): Fu[Events] = for {
    fen ← GameRepo initialFen game
    prog1 ← Rewind(game, fen).future
    prog2 ← Rewind(prog1.game, fen).future map { progress =>
      prog1 withGame progress.game
    }
    _ ← fuccess { uciMemo.drop(game, 2) }
    events ← save(prog2)
  } yield events

  private def save(p1: Progress): Fu[Events] = {
    val p2 = p1 + Event.Reload
    messenger.system(p2.game, _.takebackPropositionAccepted)
    (GameRepo save p2) inject p2.events
  }
}
