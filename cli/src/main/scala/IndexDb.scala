package lila.cli

import lila.system.db.GameRepo
import scalaz.effects._

case class IndexDb(gameRepo: GameRepo) extends Command {

  def apply: IO[Unit] = for {
    _ ← putStrLn("- Drop indexes")
    _ ← gameRepo.dropIndexes
    _ ← putStrLn("- Ensure indexes")
    _ ← gameRepo.ensureIndexes
  } yield ()
}
