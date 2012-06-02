package lila
package lobby

import mongodb.CappedRepo
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class MessageRepo(collection: MongoCollection, max: Int)
extends CappedRepo[Message](collection, max) {

  def add(message: Message): IO[Unit] = io {
    collection += encode(message)
  }

  def decode(obj: DBObject): Option[Message] = for {
    u ← obj.getAs[String]("u")
    t ← obj.getAs[String]("t")
  } yield Message(u, t)

  def encode(obj: Message): DBObject = DBObject(
    "u" -> obj.username,
    "t" -> obj.text)

  def censorUsername(username: String): IO[Unit] = io {
    collection.update(
      DBObject("u" -> username),
      $set("t" -> ""),
      upsert = false,
      multi = true
    )
  }
}
