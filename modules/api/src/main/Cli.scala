package lila.api

import akka.actor.ActorSelection

import lila.common.Bus
import lila.hub.actorApi.Deploy

private[api] final class Cli extends lila.common.Cli {

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
          Bus.publish(lila.user.User.GDPRErase(user), 'gdprErase)
          s"Erasing all data about ${user.username} now"
      }
    case "announce" :: "cancel" :: Nil =>
      AnnounceStore set none
      Bus.publish(AnnounceStore.cancel, 'announce)
      fuccess("Removed announce")
    case "announce" :: msgWords => AnnounceStore.set(msgWords mkString " ") match {
      case Some(announce) =>
        Bus.publish(announce, 'announce)
        fuccess(announce.json.toString)
      case None =>
        fuccess("Invalid announce. Format: `announce <length> <unit> <words...>` or just `announce cancel` to cancel it")
    }
  }

  private def remindDeploy(event: Deploy): Fu[String] = {
    Bus.publish(event, 'deploy)
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
