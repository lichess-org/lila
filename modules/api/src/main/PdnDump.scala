package lidraughts.api

import play.api.libs.iteratee._
import org.joda.time.DateTime

import draughts.format.pdn.Pdn
import lidraughts.game.Game
import lidraughts.game.PdnDump.WithFlags
import lidraughts.game.{ GameRepo, Query }
import play.api.libs.iteratee._

final class PdnDump(
    dumper: lidraughts.game.PdnDump,
    getSimulName: String => Fu[Option[String]],
    getTournamentName: String => Option[String]
) {

  def apply(game: Game, initialFen: Option[String], flags: WithFlags, draughtsResult: Boolean): Fu[Pdn] =
    (game.simulId ?? getSimulName) map { simulName =>
      val pdn = dumper(game, initialFen, flags, draughtsResult)
      simulName.orElse(game.tournamentId flatMap getTournamentName).fold(pdn)(pdn.withEvent)
    }

  def filename(game: Game) = dumper filename game

  private def toPdn(draughtsResult: Boolean) =
    Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game flatMap { initialFen =>
        apply(game, initialFen, WithFlags(), draughtsResult).map(pdn => s"$pdn\n\n\n")
      }
    }

  def exportUserGames(userId: String, since: Option[DateTime], until: Option[DateTime], draughtsResult: Boolean): Enumerator[String] = {
    import reactivemongo.play.iteratees.cursorProducer
    import lidraughts.db.dsl._
    GameRepo.sortedCursor(
      Query.user(userId) ++ Query.createdBetween(since, until),
      Query.sortCreated
    ).enumerator() &> toPdn(draughtsResult)
  }

  def exportGamesFromIds(ids: List[String], draughtsResult: Boolean): Enumerator[String] =
    Enumerator.enumerate(ids grouped 50) &>
      Enumeratee.mapM[List[String]].apply[List[Game]](GameRepo.gamesFromSecondary) &>
      Enumeratee.mapConcat(identity) &>
      toPdn(draughtsResult)
}
