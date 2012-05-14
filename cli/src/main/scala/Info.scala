package lila.cli

import lila.core.CoreEnv
import scalaz.effects._

case class Info(env: CoreEnv) extends Command {

  def apply: IO[Unit] = for {
    nb <- nbGames
    _ ← putStrLn("%d games in DB" format nb)
  } yield ()

  def nbGames = io {
    env.gameRepo.count()
  }
}
