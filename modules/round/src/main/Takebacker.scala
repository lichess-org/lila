package lidraughts.round

import draughts.format.Forsyth
import lidraughts.game.{ Event, Game, GameRepo, Pov, Progress, Rewind, UciMemo }
import lidraughts.pref.{ Pref, PrefApi }
import RoundDuct.TakebackSituation

private final class Takebacker(
    messenger: Messenger,
    uciMemo: UciMemo,
    prefApi: PrefApi,
    bus: lidraughts.common.Bus
) {

  def yes(situation: TakebackSituation)(pov: Pov)(implicit proxy: GameProxy): Fu[(Events, TakebackSituation)] = IfAllowed(pov.game) {
    pov match {
      case Pov(game, color) if pov.opponent.isProposingTakeback => {
        if (pov.opponent.proposeTakebackAt == game.turns) rewindUntilPly(game, game.displayTurns - (if (game.situation.ghosts == 0 || game.turnColor != color) 1 else 2), pov.opponent.color)
        else rewindUntilPly(game, game.displayTurns - 2, pov.opponent.color)
      } map (_ -> situation.reset)
      case Pov(game, _) if pov.opponent.isAi =>
        rewindUntilPly(game, game.displayTurns - (if ((game.situation.ghosts == 0 && game.turnColor == pov.opponent.color) || (game.situation.ghosts != 0 && game.turnColor != pov.opponent.color)) 1 else 2), pov.player.color) map (_ -> situation)
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

  def no(situation: TakebackSituation)(pov: Pov)(implicit proxy: GameProxy): Fu[(Events, TakebackSituation)] = pov match {
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

  def isAllowedIn(game: Game): Fu[Boolean] =
    if (game.isMandatory) fuFalse
    else isAllowedByPrefs(game)

  private def isAllowedByPrefs(game: Game): Fu[Boolean] =
    if (game.hasAi) fuTrue
    else game.userIds.map {
      prefApi.getPref(_, (p: Pref) => p.takeback)
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
    else if (game.isMandatory) fufail(ClientError("[takebacker] game disallows it " + game.id))
    else isAllowedByPrefs(game) flatMap {
      case true => f
      case _ => fufail(ClientError("[takebacker] disallowed by preferences " + game.id))
    }

  private def rewindUntilPly(game: Game, ply: Int, takeBacker: draughts.Color)(implicit proxy: GameProxy): Fu[Events] =
    GameRepo initialFen game flatMap {
      fen =>
        Rewind(game, fen, game.turns, takeBacker, game.turns == ply).future flatMap {
          prog1 => rewindPly(game, fen, game.turns, takeBacker, ply, prog1, 1)
        }
    }

  private def rewindPly(game: Game, fen: Option[draughts.format.FEN], initialPly: Int, takeBacker: draughts.Color, targetPly: Int, prog: Progress, rewinds: Int)(implicit proxy: GameProxy): Fu[Events] = {
    if (rewinds > 10) {
      logger.info(s"rewindPly $rewinds - initialPly: $initialPly, takeBacker: $takeBacker, targetPly: $targetPly, fen: $fen")
      logger.info(s"rewindPly $rewinds - turns: ${prog.game.turns}, ghosts: ${prog.game.situation.ghosts}")
      logger.info(s"rewindPly $rewinds - fen: ${Forsyth >> prog.game.situation}, prog: $prog")
    }
    if (rewinds > 20) {
      logger.info(s"rewindPly Infinite rewinds - fen: ${Forsyth >> game.situation}, game $game")
      fuccess {
        uciMemo.drop(game, rewinds)
      } flatMap { _ => saveAndNotify(prog) }
    } else if (prog.game.turns + (if (prog.game.situation.ghosts > 0) 1 else 0) <= targetPly)
      fuccess {
        uciMemo.drop(game, rewinds)
      } flatMap { _ => saveAndNotify(prog) }
    else Rewind(prog.game, fen, initialPly, takeBacker, initialPly == targetPly).future map { progress =>
      prog withGame progress.game
    } flatMap { progn => rewindPly(game, fen, initialPly, takeBacker, targetPly, progn, rewinds + 1) }
  }

  private def saveAndNotify(p1: Progress)(implicit proxy: GameProxy): Fu[Events] = {
    val p2 = p1 + Event.Reload
    messenger.system(p2.game, _.takebackPropositionAccepted)
    proxy.save(p2) inject p2.events
  }
}
