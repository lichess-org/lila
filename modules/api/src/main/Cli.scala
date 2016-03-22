package lila.api

import akka.actor.ActorSelection
import akka.pattern.{ ask, pipe }
import play.twirl.api.Html

import lila.hub.actorApi.{ RemindDeploy, Deploy }
import makeTimeout.short

private[api] final class Cli(bus: lila.common.Bus, renderer: ActorSelection) extends lila.common.Cli {

  private val logger = lila.log("cli")

  def apply(args: List[String]): Fu[String] = run(args).map(_ + "\n") ~ {
    _.logFailure(logger, _ => args mkString " ") foreach { output =>
      logger.info("%s\n%s".format(args mkString " ", output))
    }
  }

  def process = {
    case "deploy" :: "pre" :: Nil  => remindDeploy(lila.hub.actorApi.RemindDeployPre)
    case "deploy" :: "post" :: Nil => remindDeploy(lila.hub.actorApi.RemindDeployPost)
    case "rating" :: "fest" :: Nil => RatingFest(
      lila.db.Env.current,
      lila.round.Env.current.perfsUpdater,
      lila.game.Env.current,
      lila.user.Env.current) inject "done"
  }

  private def remindDeploy(event: RemindDeploy): Fu[String] = {
    renderer ? event foreach {
      case html: Html => bus.publish(Deploy(event, html.body), 'deploy)
    }
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
      lila.wiki.Env.current.cli.process orElse
      lila.i18n.Env.current.cli.process orElse
      lila.game.Env.current.cli.process orElse
      lila.gameSearch.Env.current.cli.process orElse
      lila.teamSearch.Env.current.cli.process orElse
      lila.forum.Env.current.cli.process orElse
      lila.forumSearch.Env.current.cli.process orElse
      lila.team.Env.current.cli.process orElse
      lila.round.Env.current.cli.process orElse
      lila.puzzle.Env.current.cli.process orElse
      lila.tournament.Env.current.cli.process orElse
      lila.explorer.Env.current.cli.process orElse
      lila.fishnet.Env.current.cli.process orElse
      lila.blog.Env.current.cli.process orElse
      process
}
