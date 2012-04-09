package lila

import model.{ DbGame, Entry, Standard, Evented }
import db.{ GameRepo, EntryRepo }
import scalaz.effects._

final class Starter(
    val gameRepo: GameRepo,
    entryRepo: EntryRepo,
    lobbySocket: lobby.Socket,
    ai: () ⇒ Ai) extends IOTools {

  def start(game: DbGame, entryData: String): IO[Evented] = for {
    _ ← if (game.variant == Standard) io() else gameRepo saveInitialFen game
    _ ← Entry(game, entryData).fold(
      entry ⇒ entryRepo add entry flatMap { _ ⇒ lobbySocket addEntry entry },
      io())
    evented ← if (game.player.isHuman) io(Evented(game)) else for {
      aiResult ← ai()(game) map (_.err)
      (newChessGame, move) = aiResult
    } yield game.update(newChessGame, move)
  } yield evented
}
