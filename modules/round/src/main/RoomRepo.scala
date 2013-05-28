package lila.round

import lila.db.api._
import tube.roomTube

object RoomRepo {

  def room(id: String): Fu[Room] = $find byId id map (_ | Room(id, Nil))

  def addMessage(id: String, author: String, text: String): Funit =
    $update(
      $select(id),
      $push("messages", Room.encode(author, text)),
      upsert = true)

  // false if skipped because previous message is similar
  def addSystemMessage(id: String, text: String): Fu[Boolean] =
    room(id) flatMap {
      _.decodedMessages.lastOption ?? { _ == ("system", text) } ?? {
        addMessage(id, "system", text) inject true
      }
    }

  def addSystemMessages(id: String, texts: Seq[String]): Funit =
    (texts map { addSystemMessage(id, _) }).sequenceFu.void
}
