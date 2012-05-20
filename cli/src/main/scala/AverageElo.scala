package lila.cli

import lila.core.CoreEnv
import scalaz.effects._

case class AverageElo(env: CoreEnv) extends Command {

  def apply: IO[Unit] = for {
    avg ← env.user.userRepo.averageElo
    _ ← putStrLn("Average elo is %f" format avg)
  } yield ()
}
