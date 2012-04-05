package lila.system
package db

import model.Entry

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class EntryRepo(collection: MongoCollection, max: Int)
    extends CappedRepo[Entry](collection, max) {

  def add(entry: Entry): IO[Unit] = io {
    collection += DBObject(
      "gameId" -> entry.gameId,
      "whiteName" -> entry.whiteName,
      "blackName" -> entry.blackName,
      "whiteId" -> entry.whiteId,
      "blackId" -> entry.blackId,
      "variant" -> entry.variant,
      "rated" -> entry.rated,
      "clock" -> entry.clock)
  }

  def decode(obj: DBObject): Option[Entry] = for {
    gameId ← obj.getAs[String]("gameId")
    whiteName ← obj.getAs[String]("whiteName")
    blackName ← obj.getAs[String]("blackName")
    whiteId = obj.getAs[String]("whiteId")
    blackId = obj.getAs[String]("blackId")
    variant ← obj.getAs[String]("variant")
    rated ← obj.getAs[Boolean]("rated")
    clock = obj.getAs[String]("clock")
  } yield Entry(gameId, whiteName, blackName, whiteId, blackId, variant, rated, clock)

  def encode(obj: Entry): DBObject = DBObject(
    "gameId" -> obj.gameId,
    "whiteName" -> obj.whiteName,
    "blackName" -> obj.blackName,
    "whiteId" -> obj.whiteId,
    "blackId" -> obj.blackId,
    "variant" -> obj.variant,
    "rated" -> obj.rated,
    "clock" -> obj.clock)
}
