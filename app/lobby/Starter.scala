package lila
package lobby

import model.{ DbGame, Standard, Progress }
import db.{ GameRepo }
import timeline._
import scalaz.effects._

final class Starter(
    gameRepo: GameRepo,
    entryRepo: EntryRepo,
    socket: Socket,
    ai: () ⇒ Ai) {

  def start(game: DbGame, entryData: String): IO[Progress] = for {
    _ ← if (game.variant == Standard) io() else gameRepo saveInitialFen game
    _ ← Entry(game, entryData).fold(
      entry ⇒ entryRepo add entry flatMap { _ ⇒ socket addEntry entry },
      io())
    progress ← if (game.player.isHuman) io(Progress(game)) else for {
      aiResult ← ai()(game) map (_.err)
      (newChessGame, move) = aiResult
    } yield game.update(newChessGame, move)
  } yield progress
}
