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

  def addSystemMessage(id: String, text: String) =
    addMessage(id, "system", text)

  def addSystemMessages(id: String, texts: Seq[String]): Funit =
    (texts map { addSystemMessage(id, _) }).sequence.void
}
