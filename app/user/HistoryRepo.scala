package lila
package user

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

import scalaz.effects._

final class HistoryRepo(collection: MongoCollection) {

  import HistoryRepo._

  def addEntry(
    username: String,
    elo: Int,
    gameId: Option[String] = None,
    entryType: Int = TYPE_GAME): IO[Unit] = io {
    val tsKey = (System.currentTimeMillis / 1000).toString
    collection.update(
      DBObject("_id" -> username),
      $set(("entries." + tsKey) -> DBObject(
        "t" -> entryType,
        "e" -> elo,
        "g" -> (gameId | null)
      )),
      false, false
    )
  }
}

object HistoryRepo {

  val TYPE_START = 1;
  val TYPE_GAME = 2;
  val TYPE_ADJUST = 3;
}
