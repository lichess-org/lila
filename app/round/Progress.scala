package lila
package round

import game.DbGame

// events are kept in insertion/addition order
case class Progress(origin: DbGame, game: DbGame, events: List[Event] = Nil) {

  def map(f: DbGame ⇒ DbGame) = copy(game = f(game))

  def flatMap(f: DbGame ⇒ Progress) = f(game) match {
    case Progress(_, g2, e2) ⇒ copy(game = g2, events = events ::: e2)
  }

  def +(event: Event) = copy(events = events :+ event)

  def ++(es: List[Event]) = copy(events = events ::: es)

  def withGame(g: DbGame) = copy(game = g)
}

object Progress {

  def apply(game: DbGame): Progress =
    new Progress(game, game)

  def apply(game: DbGame, events: List[Event]): Progress =
    new Progress(game, game, events)
}
