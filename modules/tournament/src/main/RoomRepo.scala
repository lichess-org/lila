package lila.tournament

import Room._
import lila.db.api._
import tube.roomTube

private[tournament] object RoomRepo {

  def room(id: String): Fu[Room] = ($find byId id) map (_ | Room(id, Nil))

  def addMessage(id: String, msg: Message): Funit = $update(
    $select(id),
    $push("messages", Room encode msg),
    upsert = true,
    multi = false)
}
