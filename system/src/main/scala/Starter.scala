package lila.system

import model.{ DbGame, Entry, Standard }
import memo.{ VersionMemo, EntryMemo }
import db.{ GameRepo, EntryRepo }
import scalaz.effects._

final class Starter(
    val gameRepo: GameRepo,
    entryRepo: EntryRepo,
    val versionMemo: VersionMemo,
    entryMemo: EntryMemo,
    ai: () ⇒ Ai) extends IOTools {

  def start(game: DbGame, entryData: String): IO[DbGame] = for {
    _ ← if (game.variant == Standard) io() else gameRepo saveInitialFen game
    _ ← addEntry(game, entryData)
    g2 ← if (game.player.isHuman) io(game) else for {
      aiResult ← ai()(game) map (_.err)
      (newChessGame, move) = aiResult
    } yield game.update(newChessGame, move)
  } yield g2

  private def addEntry(game: DbGame, data: String): IO[Unit] =
    Entry.build(game, data).fold(
      f ⇒ (entryMemo++) map (id ⇒ entryRepo insert f(id)),
      io()
    )
}
