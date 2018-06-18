package lila.importer

import com.typesafe.config.Config

final class Env(
    config: Config,
    scheduler: akka.actor.Scheduler,
    roundMap: akka.actor.ActorRef
) {

  private val Delay = config duration "delay"

  lazy val forms = new DataForm

  lazy val importer = new Importer(roundMap, Delay, scheduler)
}

object Env {

  lazy val current = "importer" boot new Env(
    config = lila.common.PlayApp loadConfig "importer",
    scheduler = old.play.Env.actorSystem.scheduler,
    roundMap = lila.round.Env.current.roundMap
  )
}
