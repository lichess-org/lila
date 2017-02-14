package lila.api

import chess.format.pgn.Pgn
import lila.game.Game
import lila.game.{ GameRepo, Query }
import play.api.libs.iteratee._

final class PgnDump(
    dumper: lila.game.PgnDump,
    getSimulName: String => Fu[Option[String]],
    getTournamentName: String => Option[String]
) {

  def apply(game: Game, initialFen: Option[String]): Fu[Pgn] =
    (game.simulId ?? getSimulName) map { simulName =>
      val pgn = dumper(game, initialFen)
      simulName.orElse(game.tournamentId flatMap getTournamentName).fold(pgn)(pgn.withEvent)
    }

  def filename(game: Game) = dumper filename game

  private val toPgn =
    Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game flatMap { initialFen =>
        apply(game, initialFen).map(pgn => s"$pgn\n\n\n")
      }
    }

  def exportUserGames(userId: String): Enumerator[String] = {
    import reactivemongo.play.iteratees.cursorProducer
    GameRepo.sortedCursor(Query user userId, Query.sortCreated).
      enumerator() &> toPgn
  }

  def exportGamesFromIds(ids: List[String]): Enumerator[String] =
    Enumerator.enumerate(ids grouped 50) &>
      Enumeratee.mapM[List[String]].apply[List[Game]](GameRepo.gamesFromSecondary) &>
      Enumeratee.mapConcat(identity) &>
      toPgn
}
