package lila

import db.GameRepo
import model._
import chess.Color
import scalaz.effects._

trait IOTools {

  val gameRepo: GameRepo

  def ioColor(colorName: String): IO[Color] = io {
    Color(colorName) err "Invalid color"
  }

  def save(g1: DbGame, g2: DbGame): IO[Unit] = for {
    _ ← gameRepo.applyDiff(g1, g2)
  } yield ()

  def save(game: DbGame, evented: Evented): IO[Unit] = for {
    _ ← gameRepo.applyDiff(game, evented.game)
    // send events
  } yield ()
}
