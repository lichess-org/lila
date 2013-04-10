package lila.round

import lila.game.{ GameRepo, Game, Rewind, Event, Progress }
import lila.db.api._

private[round] final class Takeback(messenger: Messenger) {

  private type ValidFuEvents = Valid[Fu[List[Event]]]

  def apply(game: Game, pgn: String, initialFen: Option[String]): ValidFuEvents =
    Rewind(game, pgn, initialFen) map {
      case (progress, newPgn) ⇒ savePgn(game.id, newPgn) >> save(progress)
    } mapFail failInfo(game)

  def double(game: Game, pgn: String, initialFen: Option[String]): ValidFuEvents = {
    for {
      first ← Rewind(game, pgn, initialFen)
      (prog1, pgn1) = first
      second ← Rewind(prog1.game, pgn1, initialFen) map {
        case (progress, newPgn) ⇒ (prog1 withGame progress.game, newPgn)
      }
      (prog2, pgn2) = second
    } yield savePgn(game.id, pgn2) >> save(prog2)
  } mapFail failInfo(game)

  def failInfo(game: Game) =
    (failures: Failures) ⇒ "Takeback %s".format(game.id) <:: failures

  private def savePgn(gameId: String, pgn: String): Funit = {
    import lila.game.tube.pgnTube
    $update.field(gameId, "p", pgn, upsert = true)
  }

  private def save(p1: Progress): Fu[List[Event]] = for {
    _ ← messenger.systemMessage(p1.game, _.takebackPropositionAccepted)
    p2 = p1 + Event.Reload
    _ ← GameRepo save p2
  } yield p2.events
}
