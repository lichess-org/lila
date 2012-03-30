package lila.cli

import lila.system.SystemEnv
import scalaz.effects._

object Main {

  lazy val env = SystemEnv()

  def main(args: Array[String]): Unit = sys exit {

    val command: Command = args.toList match {
      //case "compact" :: Nil ⇒ CompactDb(env.mongodb)
      case "index" :: Nil ⇒ IndexDb(env.gameRepo)
      case "migrate-rooms" :: Nil ⇒ MigrateRooms(env.mongodb)
      case "import-games" :: Nil ⇒ ImportGames(env.mongodb, env.gameRepo)
      case _ ⇒ new Command {
        def apply() = putStrLn("Usage: run command args")
      }
    }
    command.apply.unsafePerformIO
    0
  }
}
