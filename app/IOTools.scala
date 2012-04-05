package lila

import db.GameRepo
import memo.VersionMemo
import model._
import chess.Color
import scalaz.effects._

trait IOTools {

  val gameRepo: GameRepo
  val versionMemo: VersionMemo

  def ioColor(colorName: String): IO[Color] = io {
    Color(colorName) err "Invalid color"
  }

  def save(g1: DbGame, g2: DbGame): IO[Unit] = for {
    _ ← gameRepo.applyDiff(g1, g2)
    _ ← versionMemo put g2
  } yield ()
}
