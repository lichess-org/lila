package lila.api

import chess.format.pgn.Pgn
import lila.game.Game
import play.api.libs.iteratee._

final class PgnDump(
    dumper: lila.game.PgnDump,
    relayName: String => Option[String],
    simulName: String => Option[String],
    tournamentName: String => Option[String]) {

  def apply(game: Game, initialFen: Option[String]): Pgn = {
    val pgn = dumper(game, initialFen)
    game.relayId.flatMap(relayName).orElse {
      game.tournamentId.flatMap(tournamentName).orElse {
        game.simulId.flatMap(simulName)
      }
    }.fold(pgn)(pgn.withEvent)
  }

  def filename(game: Game) = dumper filename game

  def exportUserGames(userId: String): Enumerator[String] = {
    import lila.db.api.$query
    import lila.game.{ GameRepo, Query }
    import lila.game.BSONHandlers.gameBSONHandler
    import lila.db.Implicits._
    import lila.game.tube.gameTube
    val query = pimpQB($query(Query user userId)) sort Query.sortCreated
    val toPgn = Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game map { initialFen =>
        apply(game, initialFen).toString + "\n\n\n"
      }
    }
    query.cursor[Game]().enumerate() &> toPgn
  }
}
