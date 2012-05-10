package lila.cli

import scalaz.effects._
import play.api.{ Mode, Application }

import java.io.File
import lila.SystemEnv

object Main {

  lazy val app = new Application(
    path = new File("."),
    classloader = this.getClass.getClassLoader(),
    sources = None,
    mode = Mode.Dev)

  lazy val env = SystemEnv(app)

  def main(args: Array[String]): Unit = sys exit {

    val command: Command = args.toList match {
      case "info" :: Nil  ⇒ Info(env)
      case "average-elo" :: Nil  ⇒ AverageElo(env)
      case "index" :: Nil ⇒ IndexDb(env.gameRepo)
      case "finish" :: Nil ⇒ new Command {
        def apply() = env.gameFinishCommand.apply()
      }
      case "eco" :: Nil => new Command {
        def apply() = putStrLn(lila.chess.Eco.tree.render())
      }
      case _ ⇒ new Command {
        def apply() = putStrLn("Usage: run command args")
      }
    }
    command.apply.unsafePerformIO
    0
  }
}
