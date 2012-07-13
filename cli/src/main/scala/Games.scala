package lila.cli

import lila.core.CoreEnv
import scalaz.effects._

case class Games(env: CoreEnv) {

  def perDay(days: Int): IO[Unit] = for {
    nbs ← env.game.gameRepo.nbPerDay(days)
    _ ← putStrLn(nbs mkString " ")
  } yield ()
}
