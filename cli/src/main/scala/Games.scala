package lila.cli

import lila.game.GameRepo
import scalaz.effects._

case class Games(gameRepo: GameRepo) {

  def index: IO[Unit] = for {
    _ ← putStrLn("- Drop indexes")
    _ ← gameRepo.dropIndexes
    _ ← putStrLn("- Ensure indexes")
    _ ← gameRepo.ensureIndexes
  } yield ()
}
