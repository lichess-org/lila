package lidraughts.api

import akka.actor.ActorSelection

import lidraughts.hub.actorApi.Deploy

private[api] final class Cli(bus: lidraughts.common.Bus) extends lidraughts.common.Cli {

  private val logger = lidraughts.log("cli")

  def apply(args: List[String]): Fu[String] = run(args).map(_ + "\n") ~ {
    _.logFailure(logger, _ => args mkString " ") foreach { output =>
      logger.info("%s\n%s".format(args mkString " ", output))
    }
  }

  def process = {
    case "deploy" :: "pre" :: Nil => remindDeploy(lidraughts.hub.actorApi.DeployPre)
    case "deploy" :: "post" :: Nil => remindDeploy(lidraughts.hub.actorApi.DeployPost)
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
    lidraughts.security.Env.current.cli.process orElse
      lidraughts.i18n.Env.current.cli.process orElse
      lidraughts.teamSearch.Env.current.cli.process orElse
      lidraughts.forumSearch.Env.current.cli.process orElse
      lidraughts.team.Env.current.cli.process orElse
      lidraughts.puzzle.Env.current.cli.process orElse
      lidraughts.tournament.Env.current.cli.process orElse
      lidraughts.explorer.Env.current.cli.process orElse
      lidraughts.draughtsnet.Env.current.cli.process orElse
      lidraughts.study.Env.current.cli.process orElse
      lidraughts.studySearch.Env.current.cli.process orElse
      lidraughts.evalCache.Env.current.cli.process orElse
      lidraughts.report.Env.current.cli.process orElse
      lidraughts.game.Env.current.cli.process orElse
      lidraughts.gameSearch.Env.current.cli.process orElse
      lidraughts.plan.Env.current.cli.process orElse
      process
}
