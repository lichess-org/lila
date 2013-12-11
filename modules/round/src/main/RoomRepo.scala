package lila.round

import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter

import lila.db.api._
import tube.roomTube

object RoomRepo {

  private val maxMessages = 50

  def room(id: String): Fu[Room] = $find byId id map (_ | Room(id, Nil))

  def addMessage(id: String, author: String, text: String): Funit =
    $update(
      $select(id),
      $pushSlice("messages", Room.encode(author, text), -maxMessages),
      upsert = true)

  def addSystemMessage(id: String, text: String) =
    addMessage(id, "system", text)

  def addSystemMessages(id: String, texts: Seq[String]): Funit =
    (texts map { addSystemMessage(id, _) }).sequenceFu.void
}
