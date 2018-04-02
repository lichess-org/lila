package lila.api

import play.api.libs.iteratee._
import org.joda.time.DateTime

import chess.format.pgn.Pgn
import lila.game.Game
import lila.game.PgnDump.WithFlags
import lila.game.{ GameRepo, Query }

final class PgnDump(
    dumper: lila.game.PgnDump,
    getSimulName: String => Fu[Option[String]],
    getTournamentName: String => Option[String]
) {

  def apply(game: Game, initialFen: Option[String], flags: WithFlags): Fu[Pgn] =
    (game.simulId ?? getSimulName) map { simulName =>
      val pgn = dumper(game, initialFen, flags)
      simulName.orElse(game.tournamentId flatMap getTournamentName).fold(pgn)(pgn.withEvent)
    }

  def filename(game: Game) = dumper filename game

  private val toPgn =
    Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game flatMap { initialFen =>
        apply(game, initialFen, WithFlags()).map(pgn => s"$pgn\n\n\n")
      }
    }

  def exportUserGames(userId: String, since: Option[DateTime], until: Option[DateTime], max: Int): Enumerator[String] = {
    import reactivemongo.play.iteratees.cursorProducer
    import lila.db.dsl._
    GameRepo.sortedCursor(
      Query.user(userId) ++ Query.createdBetween(since, until),
      Query.sortCreated
    ).enumerator(maxDocs = max) &> toPgn
  }

  def exportGamesFromIds(ids: List[String]): Enumerator[String] =
    Enumerator.enumerate(ids grouped 50) &>
      Enumeratee.mapM[List[String]].apply[List[Game]](GameRepo.gamesFromSecondary) &>
      Enumeratee.mapConcat(identity) &>
      toPgn
}
