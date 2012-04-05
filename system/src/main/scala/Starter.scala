package lila.system

import model.{ DbGame, Entry, Standard }
import memo.{ VersionMemo, EntryMemo }
import db.{ GameRepo, EntryRepo }
import scalaz.effects._

final class Starter(
  val gameRepo: GameRepo,
  entryRepo: EntryRepo,
  val versionMemo: VersionMemo,
  ai: Ai) extends IOTools {

  def start(game: DbGame, entryData: String): IO[DbGame] = for {
    _ ← if (game.variant == Standard) io() else gameRepo saveInitialFen game
    _ ← Entry(game, entryData).fold(entryRepo.add, io())
    g2 ← if (game.player.isHuman) io(game) else for {
      aiResult ← ai(game) map (_.err)
      (newChessGame, move) = aiResult
    } yield game.update(newChessGame, move)
  } yield g2
}
