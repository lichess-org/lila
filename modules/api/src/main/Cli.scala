package lila.api

import lila.common.Bus

final private[api] class Cli(
    security: lila.security.Env,
    tournament: lila.tournament.Env,
    fishnet: lila.fishnet.Env,
    study: lila.study.Env,
    fide: lila.fide.Env,
    evalCache: lila.evalCache.Env,
    plan: lila.plan.Env,
    msg: lila.msg.Env,
    video: lila.video.Env,
    puzzle: lila.puzzle.Env,
    team: lila.team.Env,
    notify: lila.notify.Env,
    manifest: lila.web.AssetManifest
)(using Executor)
    extends lila.common.Cli:

  import play.api.data.Forms.*
  val form = play.api.data.Form(single("command" -> nonEmptyText))

  def apply(args: List[String]): Fu[String] =
    run(args)
      .map(_ + "\n")
      .logFailure(lila.log("cli"), _ => args.mkString(" "))

  def process =
    case "uptime" :: Nil => fuccess(s"${lila.common.Uptime.seconds} seconds")
    case "change" :: ("asset" | "assets") :: "version" :: Nil =>
      manifest.update()
      import lila.core.net.AssetVersion
      val current = AssetVersion.change()
      Bus.pub(AssetVersion.Changed(current))
      fuccess(s"Changed to ${AssetVersion.current}")
    case "announce" :: words => lila.web.AnnounceApi.cli(words)
    case "threads" :: Nil =>
      fuccess:
        val threads = scalalib.Jvm.threadGroups()
        s"${threads.map(_.total).sum} threads\n\n${threads.mkString("\n")}"

  private def run(args: List[String]): Fu[String] = {
    (processors.lift(args)) | fufail("Unknown command: " + args.mkString(" "))
  }.recover { case e: Exception =>
    "ERROR " + e
  }

  private def processors =
    security.cli.process
      .orElse(tournament.cli.process)
      .orElse(fishnet.cli.process)
      .orElse(study.cli.process)
      .orElse(evalCache.cli.process)
      .orElse(plan.cli.process)
      .orElse(puzzle.cli.process)
      .orElse(msg.cli.process)
      .orElse(video.cli.process)
      .orElse(team.cli.process)
      .orElse(notify.cli.process)
      .orElse(fide.cli.process)
      .orElse(process)
