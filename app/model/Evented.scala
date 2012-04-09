package lila
package model

// events are kept in insertion/addition order
case class Evented(game: DbGame, events: List[Event] = Nil) {

  def +(event: Event) = copy(events = events :+ event)

  def ++(es: List[Event]) = copy(events = events ::: es)

  def map(f: DbGame ⇒ DbGame) = copy(game = f(game))

  def flatMap(f: DbGame ⇒ Evented) = f(game) match {
    case Evented(g2, e2) ⇒ copy(game = g2, events = events ::: e2)
  }
}
