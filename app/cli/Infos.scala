package lila
package cli

import lila.core.CoreEnv
import scalaz.effects._

private[cli] case class Infos(env: CoreEnv) {

  def averageElo: IO[Unit] = for {
    avg ← env.user.userRepo.averageElo
    _ ← putStrLn("Average elo is %f" format avg)
  } yield ()
}
