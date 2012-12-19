package lila
package cli

import lila.core.CoreEnv
import scalaz.effects._

private[cli] case class Games(env: CoreEnv) {

  def perDay(days: Int): IO[Unit] = for {
    nbs ← env.game.gameRepo.nbPerDay(days)
    _ ← putStrLn(nbs mkString " ")
  } yield ()
}
