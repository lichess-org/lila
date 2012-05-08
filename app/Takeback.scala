package lila

import model._

import scalaz.effects._

final class Takeback {

  def perform(game: DbGame): Valid[IO[List[Event]]] =
    success(io(Nil))
}
