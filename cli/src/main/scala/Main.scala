package lila.cli

import scalaz.effects._
import play.api.{ Mode, Application }

import java.io.File
import lila.core.CoreEnv

object Main {

  lazy val app = new Application(
    path = new File("."),
    classloader = this.getClass.getClassLoader(),
    sources = None,
    mode = Mode.Dev)

  lazy val env = CoreEnv(app)

  def main(args: Array[String]): Unit = sys exit {

    val command: Command = args.toList match {
      case "info" :: Nil  ⇒ Info(env)
      case "average-elo" :: Nil  ⇒ AverageElo(env)
      case "index" :: Nil ⇒ IndexDb(env.gameRepo)
      case "trans-js-dump" :: Nil ⇒ TransJsDump(
        path = new File(env.app.path.getCanonicalPath + "/public/trans"),
        pool = env.i18nPool, 
        keys = env.i18nKeys)
      case "finish" :: Nil ⇒ new Command {
        def apply() = env.gameFinishCommand.apply()
      }
      case "eco" :: Nil => new Command {
        def apply() = putStrLn(chess.OpeningExplorer.tree.render())
      }
      case _ ⇒ new Command {
        def apply() = putStrLn("Usage: run command args")
      }
    }
    command.apply.unsafePerformIO
    0
  }
}
