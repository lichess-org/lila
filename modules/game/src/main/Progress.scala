package lila.game

import lila.core.game.*

// events are kept in insertion/addition order
case class Progress(origin: Game, game: Game, events: List[Event] = Nil):

  def map(f: Game => Game) = copy(game = f(game))

  def flatMap(f: Game => Progress) =
    f(game) match
      case Progress(_, g2, e2) => copy(game = g2, events = events ::: e2)

  def >>(next: => Progress): Progress = flatMap(_ => next)

  def +(event: Event) = copy(events = events :+ event)

  def ++(es: List[Event]) = copy(events = events ::: es)

  def withGame(g: Game) = copy(game = g)

  def statusChanged = origin.status != game.status

  def dropEvents = copy(events = Nil)

  override def toString = s"Progress ${game.id}: ${origin.ply} -> ${game.ply} ${game.status}"

object Progress:

  def apply(game: Game): Progress = Progress(game, game)
  def apply(game: Game, events: List[Event]): Progress = Progress(game, game, events)
  def apply(game: Game, events: Event): Progress = Progress(game, game, events :: Nil)
