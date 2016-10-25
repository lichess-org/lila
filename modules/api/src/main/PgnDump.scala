package lila.api

import chess.format.pgn.{ Pgn, Parser }
import lila.db.dsl._
import lila.game.Game
import lila.game.{ GameRepo, Query }
import play.api.libs.iteratee._

final class PgnDump(
    dumper: lila.game.PgnDump,
    simulName: String => Option[String],
    tournamentName: String => Option[String]) {

  def apply(game: Game, initialFen: Option[String]): Pgn = {
    val pgn = dumper(game, initialFen)
    game.tournamentId.flatMap(tournamentName).orElse {
      game.simulId.flatMap(simulName)
    }.fold(pgn)(pgn.withEvent)
  }

  def filename(game: Game) = dumper filename game

  private val toPgn =
    Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game map { initialFen =>
        apply(game, initialFen).toString + "\n\n\n"
      }
    }

  def exportUserGames(userId: String): Enumerator[String] =
    GameRepo.sortedCursor(Query user userId, Query.sortCreated).enumerate() &> toPgn

  def exportGamesFromIds(ids: List[String]): Enumerator[String] =
    Enumerator.enumerate(ids grouped 50) &>
      Enumeratee.mapM[List[String]].apply[List[Game]](GameRepo.gamesFromSecondary) &>
      Enumeratee.mapConcat(identity) &>
      toPgn
}
