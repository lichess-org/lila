package lila.app
package cli

import lila.app.core.CoreEnv
import scalaz.effects._

private[cli] case class Games(env: CoreEnv) {

  def perDay(days: Int): IO[String] = for {
    nbs ← env.game.gameRepo.nbPerDay(days)
  } yield nbs mkString " "
}
