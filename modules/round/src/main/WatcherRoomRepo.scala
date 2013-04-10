package lila.round

import lila.db.api._
import tube.watcherRoomTube

private[round] object WatcherRoomRepo {

  def room(id: String): Fu[WatcherRoom] =
    $find byId id map (_ | WatcherRoom(id, Nil))

  def addMessage(
    id: String,
    userId: Option[String],
    text: String): Funit = $update(
      $select(id),
      $push("messages", WatcherRoom.encode(userId, text)),
      upsert = true) 
}
