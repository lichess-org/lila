package lila.api

private[api] final class Cli(env: Env) {

  def apply(args: List[String]): Fu[String] = run(args).map(_ + "\n") ~ {
    _ onSuccess {
      case output ⇒ loginfo("[cli] %s\n%s".format(args mkString " ", output))
    }
  }

  private def run(args: List[String]): Fu[String] = {
    (processors lift args) | fufail("Unknown command: " + args.mkString(" "))
  } recover {
    case throwable ⇒ "ERROR " + throwable
  }

  private def processors =
    lila.user.Env.current.cli.process orElse
      lila.security.Env.current.cli.process orElse
      lila.wiki.Env.current.cli.process orElse
      lila.i18n.Env.current.cli.process orElse
      lila.game.Env.current.cli.process orElse
      lila.gameSearch.Env.current.cli.process orElse
      lila.teamSearch.Env.current.cli.process orElse
      lila.forum.Env.current.cli.process orElse
      lila.forumSearch.Env.current.cli.process orElse
      lila.message.Env.current.cli.process
}
