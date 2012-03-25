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
  clock: Option[List[Int]])

object Entry {

  def build(game: DbGame, encodedData: String): Option[Int ⇒ Entry] =
    encodedData.split('$').toList match {
      case wu :: wue :: bu :: bue :: Nil ⇒ Some(
        (id: Int) ⇒
          new Entry(
            id = id,
            EntryGame(
              id = game.id,
              players = List(
                EntryPlayer(
                  u = wu.some filterNot (_.isEmpty),
                  ue = wue
                ),
                EntryPlayer(
                  u = bu.some filterNot (_.isEmpty),
                  ue = bue
                )
              ),
              variant = game.variant.name,
              rated = game.isRated,
              clock = game.clock map { c ⇒
                List(c.limitInMinutes, c.incrementInSeconds)
              }
            )
          )
      )
      case _ ⇒ None
    }
}
