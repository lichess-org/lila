package lila.app
package round

import game.{ GameRepo, PgnRepo, DbGame, Rewind }
import scalaz.effects._

final class Takeback(
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    rewind: Rewind,
    messenger: Messenger) {

  def apply(game: DbGame, pgn: String, initialFen: Option[String]): Valid[IO[List[Event]]] =
    rewind(game, pgn, initialFen) map {
      case (progress, newPgn) ⇒ pgnRepo.save(game.id, newPgn) flatMap { _ ⇒ save(progress) }
    } mapFail failInfo(game)

  def double(game: DbGame, pgn: String, initialFen: Option[String]): Valid[IO[List[Event]]] = {
    for {
      first ← rewind(game, pgn, initialFen)
      (prog1, pgn1) = first
      second ← rewind(prog1.game, pgn1, initialFen) map {
        case (progress, newPgn) ⇒ (prog1 withGame progress.game, newPgn)
      }
      (prog2, pgn2) = second
    } yield pgnRepo.save(game.id, pgn2) flatMap { _ ⇒ save(prog2) }
  } mapFail failInfo(game)

  def failInfo(game: DbGame) =
    (failures: Failures) ⇒ "Takeback %s".format(game.id) <:: failures

  private def save(p1: Progress): IO[List[Event]] = for {
    _ ← messenger.systemMessage(p1.game, _.takebackPropositionAccepted)
    p2 = p1 + Event.Reload()
    _ ← gameRepo save p2
  } yield p2.events
}
