package lila.app
package round

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._

final class RoomRepo(collection: MongoCollection)
    extends SalatDAO[Room, String](collection) {

  def room(id: String): IO[Room] = io {
    findOneById(id) | Room(id, Nil)
  }

  def addMessage(id: String, author: String, message: String): IO[Unit] = io {
    collection.update(
      DBObject("_id" -> id),
      $push(Seq("messages" -> Room.encode(author, message))),
      upsert = true,
      multi = false
    )
  }

  def addSystemMessage(id: String, message: String) =
    addMessage(id, "system", message)

  def addSystemMessages(id: String, messages: Seq[String]): IO[Unit] =
    (messages map { addSystemMessage(id, _) }).sequence map (_ ⇒ Unit)

  def insertIO(room: Room) = io {
    insert(room)
  }
}
