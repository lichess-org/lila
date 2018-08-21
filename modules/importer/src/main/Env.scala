package lidraughts.importer

import com.typesafe.config.Config

final class Env(
    config: Config,
    scheduler: akka.actor.Scheduler
) {

  private val Delay = config duration "delay"

  lazy val forms = new DataForm

  lazy val importer = new Importer(Delay, scheduler)
}

object Env {

  lazy val current = "importer" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "importer",
    scheduler = lidraughts.common.PlayApp.system.scheduler
  )
}
