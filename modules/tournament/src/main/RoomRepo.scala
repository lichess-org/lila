package lila.tournament

import Room._

import lila.db.api._
import tube.roomTube

private[tournament] object RoomRepo {

  val maxMessages = 50

  def room(id: String): Fu[Room] = ($find byId id) map (_ | Room(id, Nil))

  def addMessage(id: String, msg: Message): Funit = $update(
    $select(id),
    $pushSlice("messages", Room encode msg, -maxMessages),
    upsert = true,
    multi = false)
}
