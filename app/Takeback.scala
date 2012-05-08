package lila

import db.GameRepo
import model.{ DbGame, Event, ReloadEvent, Progress }

import scalaz.effects._

final class Takeback(
    gameRepo: GameRepo,
    messenger: Messenger) {

  def apply(game: DbGame): Valid[IO[List[Event]]] =
    game.rewind map save

  def double(game: DbGame): Valid[IO[List[Event]]] = for {
    p1 ← game.rewind
    p2 ← p1.game.rewind map { p ⇒
      p1 withGame p.game
    }
  } yield save(p2)

  private def save(p1: Progress): IO[List[Event]] = for {
    _ ← messenger.systemMessage(p1.game, "Takeback proposition accepted")
    p2 = p1 + ReloadEvent()
    _ ← gameRepo save p2
  } yield p2.events
}
