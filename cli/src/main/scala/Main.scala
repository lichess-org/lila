package lila.cli

import lila.system.SystemEnv

object Main {

  lazy val env = SystemEnv()

  def main(args: Array[String]): Unit = sys exit {
    args.toList match {
      case "import-db" :: args ⇒ {
        ImportDb(env.mongodb, env.gameRepo).apply.unsafePerformIO
      }
      case _ ⇒ println("Usage: run command args")
    }
    0
  }
}
