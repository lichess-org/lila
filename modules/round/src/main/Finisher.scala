package lila.round

import lila.game.{ GameRepo, Game, Pov, Event }
import lila.user.{ User, UserRepo, EloUpdater }
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import chess.{ EloCalculator, Status, Color }
import Status._
import Color._
import lila.game.tube.gameTube
import lila.user.tube.userTube
import lila.db.api._
import lila.hub.actorApi.round._

import scalaz.{ OptionTs, Success }

private[round] final class Finisher(
    tournamentOrganizer: lila.hub.ActorLazyRef,
    messenger: Messenger,
    eloUpdater: EloUpdater,
    eloCalculator: EloCalculator,
    indexer: lila.hub.ActorLazyRef) extends OptionTs {

  def apply(
    game: Game,
    status: Status.type ⇒ Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None): Fu[Events] = for {
    p1 ← fuccess {
      game.finish(status(Status), winner)
    }
    p2 ← message.fold(fuccess(p1)) { m ⇒
      messenger.systemMessage(p1.game, m) map p1.++
    }
    _ ← GameRepo save p2
    g = p2.game
    winnerId = winner flatMap (g.player(_).userId)
    _ ← GameRepo.finish(g.id, winnerId) >>
      updateElo(g) >>
      ((g.status >= Status.Mate) ?? incNbGames(g, White)) >>
      ((g.status >= Status.Mate) ?? incNbGames(g, Black)) >>-
      (indexer ! lila.game.actorApi.InsertGame(g)) >>-
      (tournamentOrganizer ! FinishGame(g.id))
      // TODO send redirection events
  } yield p2.events

  private def incNbGames(game: Game, color: Color): Funit =
    game.player(color).userId ?? { id ⇒
      UserRepo.incNbGames(id, game.rated, game.hasAi,
        result = game.wonBy(color).fold(0)(_.fold(1, -1)).some filterNot (_ ⇒ game.hasAi || game.aborted)
      )
    }

  private def updateElo(game: Game): Funit = (game.finished && game.rated && game.turns >= 2) ?? ~{
    for {
      whiteUserId ← game.player(White).userId
      blackUserId ← game.player(Black).userId
      if whiteUserId != blackUserId
    } yield (for {
      whiteUser ← optionT($find.byId[User](whiteUserId))
      blackUser ← optionT($find.byId[User](blackUserId))
      _ ← optionT {
        val (whiteElo, blackElo) = eloCalculator.calculate(whiteUser, blackUser, game.winnerColor)
        val (whiteDiff, blackDiff) = (whiteElo - whiteUser.elo, blackElo - blackUser.elo)
        val cheaterWin = (whiteDiff > 0 && whiteUser.engine) || (blackDiff > 0 && blackUser.engine)
        (!cheaterWin) ?? {
          GameRepo.setEloDiffs(game.id, whiteDiff, blackDiff) >>
            eloUpdater.game(whiteUser, whiteElo, blackUser.elo) >>
            eloUpdater.game(blackUser, blackElo, whiteUser.elo)
        } inject true.some
      }
    } yield ()).value.void
  }
}
