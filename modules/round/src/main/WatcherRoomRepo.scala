package lila.round

import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter

import lila.db.api._
import tube.watcherRoomTube

object WatcherRoomRepo {

  private val maxMessages = 50

  def room(id: String): Fu[WatcherRoom] =
    $find byId id map (_ | WatcherRoom(id, Nil))

  def addMessage(
    id: String,
    username: Option[String],
    text: String): Funit = $update(
    $select(id),
    $pushSlice("messages", WatcherRoom.encode(username, text), -maxMessages),
    upsert = true)
}
