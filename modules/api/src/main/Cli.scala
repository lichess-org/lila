package lila.api

import akka.actor.ActorSelection
import akka.pattern.{ ask, pipe }
import play.twirl.api.Html

import lila.hub.actorApi.Deploy
import makeTimeout.short

private[api] final class Cli(bus: lila.common.Bus, renderer: ActorSelection) extends lila.common.Cli {

  private val logger = lila.log("cli")

  def apply(args: List[String]): Fu[String] = run(args).map(_ + "\n") ~ {
    _.logFailure(logger, _ => args mkString " ") foreach { output =>
      logger.info("%s\n%s".format(args mkString " ", output))
    }
  }

  def process = {
    case "deploy" :: "pre" :: Nil  => remindDeploy(lila.hub.actorApi.DeployPre)
    case "deploy" :: "post" :: Nil => remindDeploy(lila.hub.actorApi.DeployPost)
  }

  private def remindDeploy(event: Deploy): Fu[String] = {
    bus.publish(event, 'deploy)
    fuccess("Deploy in progress")
  }

  private def run(args: List[String]): Fu[String] = {
    (processors lift args) | fufail("Unknown command: " + args.mkString(" "))
  } recover {
    case e: Exception => "ERROR " + e
  }

  private def processors =
    lila.user.Env.current.cli.process orElse
      lila.security.Env.current.cli.process orElse
      lila.i18n.Env.current.cli.process orElse
      lila.game.Env.current.cli.process orElse
      lila.gameSearch.Env.current.cli.process orElse
      lila.teamSearch.Env.current.cli.process orElse
      lila.forumSearch.Env.current.cli.process orElse
      lila.team.Env.current.cli.process orElse
      lila.puzzle.Env.current.cli.process orElse
      lila.tournament.Env.current.cli.process orElse
      lila.explorer.Env.current.cli.process orElse
      lila.fishnet.Env.current.cli.process orElse
      lila.blog.Env.current.cli.process orElse
      process
}
