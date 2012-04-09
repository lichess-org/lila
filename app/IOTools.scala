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

  //def save(game: DbGame, evented: Evented): IO[Unit] = for {
    //_ ← gameRepo.applyDiff(game, evented.game)
    //// send events
  //} yield ()

  def save(progress: Progress): IO[Unit] = progress match {
    case Progress(origin, game, events) ⇒ for {
      _ ← gameRepo.applyDiff(origin, game)
      // send events
    } yield ()
  }
}
