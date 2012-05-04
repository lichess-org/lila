package lila.cli

import lila.SystemEnv
import scalaz.effects._

case class AverageElo(env: SystemEnv) extends Command {

  def apply: IO[Unit] = for {
    avg ← env.userRepo.averageElo
    _ ← putStrLn("Average elo is %f" format avg)
  } yield ()
}
