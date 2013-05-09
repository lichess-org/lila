package lila.round

import lila.game.{ GameRepo, Game, Rewind, Event, Progress }
import lila.db.api._

private[round] final class Takeback(messenger: Messenger) {

  def apply(game: Game, pgn: String, initialFen: Option[String]): FuEvents =
    (Rewind(game, pgn, initialFen) map {
      case (progress, newPgn) ⇒ savePgn(game.id, newPgn) >> save(progress)
    }) ||| fail(game)

  def double(game: Game, pgn: String, initialFen: Option[String]): FuEvents = {
    for {
      first ← Rewind(game, pgn, initialFen)
      (prog1, pgn1) = first
      second ← Rewind(prog1.game, pgn1, initialFen) map {
        case (progress, newPgn) ⇒ (prog1 withGame progress.game, newPgn)
      }
      (prog2, pgn2) = second
    } yield savePgn(game.id, pgn2) >> save(prog2)
  } ||| fail(game)

  def fail[A](game: Game)(err: Failures) =
    fufail[A]("Takeback %s".format(game.id) <:: err)

  private def savePgn(gameId: String, pgn: String): Funit = {
    import lila.game.tube.pgnTube
    $update.field(gameId, "p", pgn, upsert = true)
  }

  private def save(p1: Progress): FuEvents = {
    val p2 = p1 + Event.Reload
    messenger.systemMessage(p1.game, _.takebackPropositionAccepted) >>
    (GameRepo save p2) inject p2.events
  }
}
