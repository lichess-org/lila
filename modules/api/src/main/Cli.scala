package lila.api

import akka.pattern.{ ask, pipe }
import makeTimeout.short
import play.api.templates.Html

import lila.hub.actorApi.{ RemindDeploy, Deploy }

private[api] final class Cli(hub: lila.hub.Env) extends lila.common.Cli {

  def apply(args: List[String]): Fu[String] = run(args).map(_ + "\n") ~ {
    _ logFailure ("[cli] " + args.mkString(" ")) foreach { output ⇒
      loginfo("[cli] %s\n%s".format(args mkString " ", output))
    }
  }

  def process = {
    case "deploy" :: "pre" :: Nil  ⇒ remindDeploy(lila.hub.actorApi.RemindDeployPre)
    case "deploy" :: "post" :: Nil ⇒ remindDeploy(lila.hub.actorApi.RemindDeployPost)
  }

  private def remindDeploy(event: RemindDeploy): Fu[String] = {
    hub.actor.renderer ? event map {
      case html: Html ⇒ Deploy(event, html.body)
    } pipeToSelection hub.socket.hub
    fuccess("Deploy in progress")
  }

  private def run(args: List[String]): Fu[String] = {
    (processors lift args) | fufail("Unknown command: " + args.mkString(" "))
  } recover {
    case e: Exception ⇒ "ERROR " + e
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
      lila.message.Env.current.cli.process orElse
      lila.tournament.Env.current.cli.process orElse
      lila.analyse.Env.current.cli.process orElse
      lila.team.Env.current.cli.process orElse
      process
}
