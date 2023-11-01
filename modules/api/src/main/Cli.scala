package lila.api

import lila.common.Bus

final private[api] class Cli(
    security: lila.security.Env,
    teamSearch: lila.teamSearch.Env,
    forumSearch: lila.forumSearch.Env,
    tournament: lila.tournament.Env,
    fishnet: lila.fishnet.Env,
    study: lila.study.Env,
    studySearch: lila.studySearch.Env,
    evalCache: lila.evalCache.Env,
    plan: lila.plan.Env,
    msg: lila.msg.Env,
    video: lila.video.Env,
    puzzle: lila.puzzle.Env,
    team: lila.team.Env,
    notify: lila.notify.Env,
    accountClosure: AccountClosure
)(using Executor)
    extends lila.common.Cli:

  private val logger = lila.log("cli")

  def apply(args: List[String]): Fu[String] =
    run(args)
      .map(_ + "\n")
      .logFailure(logger, _ => args mkString " ")

  def process =
    case "uptime" :: Nil => fuccess(s"${lila.common.Uptime.seconds} seconds")
    case "change" :: ("asset" | "assets") :: "version" :: Nil =>
      import lila.common.AssetVersion
      AssetVersion.change()
      fuccess(s"Changed to ${AssetVersion.current}")
    case "announce" :: "cancel" :: Nil =>
      AnnounceStore set none
      Bus.publish(AnnounceStore.cancel, "announce")
      fuccess("Removed announce")
    case "announce" :: msgWords =>
      AnnounceStore.set(msgWords mkString " ") match
        case Some(announce) =>
          Bus.publish(announce, "announce")
          fuccess(announce.json.toString)
        case None =>
          fuccess(
            "Invalid announce. Format: `announce <length> <unit> <words...>` or just `announce cancel` to cancel it"
          )
    case "puzzle" :: "opening" :: "recompute" :: "all" :: Nil =>
      puzzle.opening.recomputeAll
      fuccess("started in background")
    case "threads" :: Nil =>
      fuccess:
        val threads = ornicar.scalalib.Jvm.threadGroups()
        s"${threads.map(_.total).sum} threads\n\n${threads.mkString("\n")}"

  private def run(args: List[String]): Fu[String] = {
    (processors lift args) | fufail("Unknown command: " + args.mkString(" "))
  } recover { case e: Exception =>
    "ERROR " + e
  }

  private def processors =
    security.cli.process orElse
      teamSearch.cli.process orElse
      forumSearch.cli.process orElse
      tournament.cli.process orElse
      fishnet.cli.process orElse
      study.cli.process orElse
      studySearch.cli.process orElse
      evalCache.cli.process orElse
      plan.cli.process orElse
      msg.cli.process orElse
      video.cli.process orElse
      team.cli.process orElse
      notify.cli.process orElse
      process
