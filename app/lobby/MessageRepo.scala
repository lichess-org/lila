package lila
package lobby

import mongodb.CappedRepo
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._

final class MessageRepo(collection: MongoCollection, max: Int)
    extends CappedRepo[Message](collection, max) {

  val all = io {
    collection.find(DBObject()).map(decode).toList collect {
      case Some(msg) if !msg.isEmpty ⇒ msg
    }
  }

  def add(message: Message): IO[Unit] = io {
    collection += encode(message)
  }

  def decode(obj: DBObject): Option[Message] = for {
    id ← obj.getAs[ObjectId]("_id")
    u ← obj.getAs[String]("u")
    t ← obj.getAs[String]("t")
  } yield Message(id, u, t)

  def encode(obj: Message): DBObject = DBObject(
    "u" -> obj.username,
    "t" -> obj.text)

  def censorUsername(username: String): IO[Unit] = io {
    collection.update(DBObject("u" -> username), $set(Seq("t" -> "")), upsert = false, multi = true)
  }

  def removeRegex(regex: util.matching.Regex): IO[Unit] = io {
    collection.update(DBObject("t" -> regex), $set(Seq("t" -> "")), upsert = false, multi = true)
  }
}
