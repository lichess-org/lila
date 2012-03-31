package lila.cli

import lila.system.SystemEnv
import scalaz.effects._

case class Info(env: SystemEnv) extends Command {

  def apply: IO[Unit] = for {
    nb <- nbGames
    _ ← putStrLn("%d games in DB" format nb)
  } yield ()

  def nbGames = io {
    env.gameRepo.count()
  }
}
