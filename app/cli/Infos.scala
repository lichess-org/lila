package lila
package cli

import lila.core.CoreEnv
import scalaz.effects._

private[cli] case class Infos(env: CoreEnv) {

  def averageElo: IO[String] = for {
    avg ← env.user.userRepo.averageElo
  } yield "Average elo is %f" 
}
