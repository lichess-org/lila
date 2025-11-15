package lila.api

import lila.common.{ Cli, CliCommand, Bus }
import scalalib.data.LazyFu
import scalalib.future.TimeoutException

final private class Cli(manifest: lila.web.AssetManifest)(using Executor, Scheduler):

  import play.api.data.Forms.*
  val form = play.api.data.Form(single("command" -> nonEmptyText))

  def run(args: List[String]): Fu[String] =
    Bus
      .ask(CliCommand(args, _), timeout = 1.second) // quickly look for a handler
      .mapFailure:
        case e: TimeoutException => e.copy(msg = s"Unknown command: ${args.mkString(" ")}")
        case e => e
      .flatMap(_.value) // executing the handler can take a very long time
      .map(_ + "\n")
      .logFailure(lila.log("cli"), _ => args.mkString(" "))
      .recover:
        case e: Exception => s"ERROR $e\n"

  Cli.handle:
    case "uptime" :: Nil => fuccess(s"${lila.common.Uptime.seconds} seconds")
    case "announce" :: words => lila.web.AnnounceApi.cli(words)
    case "change" :: ("asset" | "assets") :: "version" :: Nil =>
      manifest.update()
      import lila.core.net.AssetVersion
      val current = AssetVersion.change()
      Bus.pub(AssetVersion.Changed(current))
      fuccess(s"Changed to ${AssetVersion.current}")
