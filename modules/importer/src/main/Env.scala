package lila.importer

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    roundMap: akka.actor.ActorRef) {

  private val Delay = config duration "delay"

  lazy val forms = new DataForm

  lazy val importer = new Importer(roundMap, Delay)

  lazy val live = new Live(roundMap)
}

object Env {

  lazy val current = "[boot] importer" describes new Env(
    config = lila.common.PlayApp loadConfig "importer",
    roundMap = lila.round.Env.current.roundMap)
}
