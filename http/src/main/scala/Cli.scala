package lila.http

object Cli {

  def main(args: Array[String]) {

    args.headOption flatMap makeEnv map { env ⇒
      args.tail.toList match {
        case "fixtures" :: Nil ⇒ fixtures(env)
        case _ => sys error "Invalid command"
      }
    } err "Invalid environment"
  }

  def makeEnv(name: String) = name match {
    case "test" ⇒ Env.test some
    case _      ⇒ none
  }

  def fixtures(env: Env) {
    println("Load %s fixtures" format env.name)
  }
}
