package lila.system
package db

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

import scalaz.effects._

final class HistoryRepo(collection: MongoCollection) {

  val entryType = 2

  def addEntry(username: String, elo: Int, gameId: String): IO[Unit] = io {
    val tsKey = (System.currentTimeMillis / 1000).toString
    collection.update(
      DBObject("_id" -> username),
      DBObject("$set" -> (("entries." + tsKey) -> DBObject(
        "t" -> entryType,
        "e" -> elo,
        "g" -> gameId
      ))),
      false, false
    )
  }
}
