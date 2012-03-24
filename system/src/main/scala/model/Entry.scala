package lila.system
package model

import com.novus.salat.annotations._

case class Entry(
    @Key("_id") id: Int,
    data: EntryGame) {
}

case class EntryPlayer(u: Option[String], ue: String)

case class EntryGame(
  id: String,
  players: List[EntryPlayer],
  variant: String,
  rated: Boolean,
  clock: List[Int] = Nil)
