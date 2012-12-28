package lila
package cli

import lila.core.CoreEnv
import scalaz.effects._

private[cli] case class Games(env: CoreEnv) {

  def perDay(days: Int): IO[String] = for {
    nbs ← env.game.gameRepo.nbPerDay(days)
  } yield nbs mkString " "
}
