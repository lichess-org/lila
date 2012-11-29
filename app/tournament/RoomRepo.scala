package lila
package tournament

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._

class RoomRepo(collection: MongoCollection)
    extends SalatDAO[Room, String](collection) {

  import Room._

  def room(id: String): IO[Room] = io {
    findOneById(id) | Room(id, Nil)
  }

  def addMessage(id: String, msg: Message): IO[Unit] = io {
    collection.update(
      DBObject("_id" -> id),
      $push(Seq("messages" -> (Room encode msg))),
      upsert = true,
      multi = false
    )
  }

  def insertIO(room: Room) = io {
    insert(room)
  }
}
