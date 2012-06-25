package lila
package round

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._

class WatcherRoomRepo(collection: MongoCollection)
    extends SalatDAO[WatcherRoom, String](collection) {

  import WatcherRoom.Message

  def room(id: String): IO[WatcherRoom] = io {
    findOneById(id) | WatcherRoom(id, Nil)
  }

  def addMessage(id: String, username: Option[String], text: String): IO[Message] = io {
    Message(username, text) ~ { message ⇒
      collection.update(
        DBObject("_id" -> id),
        $push("messages" -> (WatcherRoom encode message)),
        upsert = true,
        multi = false
      )
    }
  }

  def insertIO(room: WatcherRoom) = io {
    insert(room)
  }
}
