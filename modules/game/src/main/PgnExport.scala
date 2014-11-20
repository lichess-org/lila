package lila.game

import lila.db.api.$query
import play.api.libs.iteratee._

import lila.db.Implicits._
import tube.gameTube

private[game] final class PgnExport(pgnDump: lila.game.PgnDump) {

  def apply(userId: String): Enumerator[String] = {
    val query = pimpQB($query(Query user userId)) sort Query.sortCreated
    val toPgn = Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game map { initialFen =>
        pgnDump(game, initialFen).toString + "\n\n\n"
      }
    }
    query.cursor[Game].enumerate() &> toPgn
  }
}
