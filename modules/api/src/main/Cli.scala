package lila.api

import akka.actor.ActorSelection

import lila.hub.actorApi.Deploy

private[api] final class Cli(bus: lila.common.Bus) extends lila.common.Cli {

  private val logger = lila.log("cli")

  def apply(args: List[String]): Fu[String] = run(args).map(_ + "\n") ~ {
    _.logFailure(logger, _ => args mkString " ") foreach { output =>
      logger.info("%s\n%s".format(args mkString " ", output))
    }
  }

  def process = {
    case "uptime" :: Nil => fuccess(s"${lila.common.PlayApp.uptimeSeconds} seconds")
    case "deploy" :: "pre" :: Nil => remindDeploy(lila.hub.actorApi.DeployPre)
    case "deploy" :: "post" :: Nil => remindDeploy(lila.hub.actorApi.DeployPost)
    case "change" :: ("asset" | "assets") :: "version" :: Nil =>
      import lila.common.AssetVersion
      AssetVersion.change
      fuccess(s"Changed to ${AssetVersion.current}")
    case "gdpr" :: "erase" :: username :: "forever" :: Nil =>
      lila.user.UserRepo named username map {
        case None => "No such user."
        case Some(user) if user.enabled => "That user account is not closed. Can't erase."
        case Some(user) =>
          bus.publish(lila.user.User.GDPRErase(user), 'gdprErase)
          s"Erasing all data about ${user.username} now"
      }
    case "announce" :: msgWords =>
      val msg = msgWords mkString " "
      bus.publish(lila.hub.actorApi.Announce(msg), 'announce)
      fuccess(s"Announcing: $msg")
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
    lila.security.Env.current.cli.process orElse
      lila.i18n.Env.current.cli.process orElse
      lila.teamSearch.Env.current.cli.process orElse
      lila.forumSearch.Env.current.cli.process orElse
      lila.team.Env.current.cli.process orElse
      lila.puzzle.Env.current.cli.process orElse
      lila.tournament.Env.current.cli.process orElse
      lila.explorer.Env.current.cli.process orElse
      lila.fishnet.Env.current.cli.process orElse
      lila.study.Env.current.cli.process orElse
      lila.studySearch.Env.current.cli.process orElse
      lila.coach.Env.current.cli.process orElse
      lila.evalCache.Env.current.cli.process orElse
      lila.plan.Env.current.cli.process orElse
      process
}
