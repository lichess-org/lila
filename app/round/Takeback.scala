package lila
package round

import game.{ GameRepo, DbGame }
import scalaz.effects._

final class Takeback(
    gameRepo: GameRepo,
    messenger: Messenger) {

  def apply(game: DbGame, initialFen: Option[String]): Valid[IO[List[Event]]] =
    game rewind initialFen map save mapFail failInfo(game)

  def double(game: DbGame, initialFen: Option[String]): Valid[IO[List[Event]]] = {
    for {
      p1 ← game rewind initialFen
      p2 ← p1.game rewind initialFen map { p ⇒
        p1 withGame p.game
      }
    } yield save(p2)
  } mapFail failInfo(game)

  def failInfo(game: DbGame) =
    (failures: Failures) ⇒ "Takeback %s".format(game.id) <:: failures

  private def save(p1: Progress): IO[List[Event]] = for {
    _ ← messenger.systemMessage(p1.game, _.takebackPropositionAccepted)
    p2 = p1 + Event.Reload()
    _ ← gameRepo save p2
  } yield p2.events
}
