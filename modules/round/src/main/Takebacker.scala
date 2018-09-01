package lidraughts.round

import lidraughts.game.{ GameRepo, Game, UciMemo, Pov, Rewind, Event, Progress }
import lidraughts.pref.{ Pref, PrefApi }

private[round] final class Takebacker(
    messenger: Messenger,
    uciMemo: UciMemo,
    prefApi: PrefApi,
    bus: lidraughts.common.Bus
) {

  def yes(situation: Round.TakebackSituation)(pov: Pov)(implicit proxy: GameProxy): Fu[(Events, Round.TakebackSituation)] = IfAllowed(pov.game) {
    pov match {
      case Pov(game, color) if pov.opponent.isProposingTakeback => {
        if (pov.opponent.proposeTakebackAt == game.turns) rewindUntilPly(game, game.displayTurns - (game.situation.ghosts == 0 || game.turnColor != color).fold(1, 2), pov.opponent.color)
        else rewindUntilPly(game, game.displayTurns - 2, pov.opponent.color)
      } map (_ -> situation.reset)
      //case Pov(game, _) if pov.opponent.isAi => double(game) map (_ -> situation)
      case Pov(game, color) if (game playerCanProposeTakeback color) && situation.offerable => {
        messenger.system(game, _.takebackPropositionSent)
        val progress = Progress(game) map { g =>
          g.updatePlayer(color, _ proposeTakeback (if (game.turnColor == color && g.turns > 1 && game.situation.ghosts == 0) g.turns - 1 else g.turns))
        }
        proxy.save(progress) >>- publishTakebackOffer(pov) inject
          List(Event.TakebackOffers(color.white, color.black))
      } map (_ -> situation)
      case _ => fufail(ClientError("[takebacker] invalid yes " + pov))
    }
  }

  def no(situation: Round.TakebackSituation)(pov: Pov)(implicit proxy: GameProxy): Fu[(Events, Round.TakebackSituation)] = pov match {
    case Pov(game, color) if pov.player.isProposingTakeback => proxy.save {
      messenger.system(game, _.takebackPropositionCanceled)
      Progress(game) map { g => g.updatePlayer(color, _.removeTakebackProposition) }
    } inject {
      List(Event.TakebackOffers(false, false)) -> situation.decline
    }
    case Pov(game, color) if pov.opponent.isProposingTakeback => proxy.save {
      messenger.system(game, _.takebackPropositionDeclined)
      Progress(game) map { g => g.updatePlayer(!color, _.removeTakebackProposition) }
    } inject {
      List(Event.TakebackOffers(false, false)) -> situation.decline
    }
    case _ => fufail(ClientError("[takebacker] invalid no " + pov))
  }

  def isAllowedByPrefs(game: Game): Fu[Boolean] =
    if (game.hasAi) fuTrue
    else game.userIds.map { userId =>
      prefApi.getPref(userId, (p: Pref) => p.takeback)
    }.sequenceFu map {
      _.forall { p =>
        p == Pref.Takeback.ALWAYS || (p == Pref.Takeback.CASUAL && game.casual)
      }
    }

  private def publishTakebackOffer(pov: Pov): Unit =
    if (pov.game.isCorrespondence && pov.game.nonAi) pov.player.userId foreach { userId =>
      bus.publish(
        lidraughts.hub.actorApi.round.CorresTakebackOfferEvent(pov.gameId),
        'offerEventCorres
      )
    }

  private def IfAllowed[A](game: Game)(f: => Fu[A]): Fu[A] =
    if (!game.playable) fufail(ClientError("[takebacker] game is over " + game.id))
    else isAllowedByPrefs(game) flatMap {
      _.fold(f, fufail(ClientError("[takebacker] disallowed by preferences " + game.id)))
    }

  private def rewindUntilPly(game: Game, ply: Int, takeBacker: draughts.Color)(implicit proxy: GameProxy): Fu[Events] =
    GameRepo initialFen game flatMap {
      fen =>
        Rewind(game, fen, game.turns, takeBacker, game.turns == ply).future flatMap {
          prog1 => rewindPly(game, fen, game.turns, takeBacker, ply, prog1, 1)
        }
    }

  private def rewindPly(game: Game, fen: Option[String], initialPly: Int, takeBacker: draughts.Color, targetPly: Int, prog: Progress, rewinds: Int)(implicit proxy: GameProxy): Fu[Events] =
    if (prog.game.turns + (prog.game.situation.ghosts > 0).fold(1, 0) <= targetPly)
      fuccess { uciMemo.drop(game, rewinds) } flatMap { _ => saveAndNotify(prog) }
    else Rewind(prog.game, fen, initialPly, takeBacker, initialPly == targetPly).future map { progress =>
      prog withGame progress.game
    } flatMap { progn => rewindPly(game, fen, initialPly, takeBacker, targetPly, progn, rewinds + 1) }

  private def saveAndNotify(p1: Progress)(implicit proxy: GameProxy): Fu[Events] = {
    val p2 = p1 + Event.Reload
    messenger.system(p2.game, _.takebackPropositionAccepted)
    proxy.save(p2) inject p2.events
  }
}
