package lila.importer

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    roundMap: akka.actor.ActorRef,
    bookmark: akka.actor.ActorSelection) {

  private val Delay = config duration "delay"

  lazy val forms = new DataForm

  lazy val importer = new Importer(roundMap, bookmark, Delay)

  lazy val live = new Live(roundMap)
}

object Env {

  lazy val current = "[boot] importer" describes new Env(
    config = lila.common.PlayApp loadConfig "importer",
    roundMap = lila.round.Env.current.roundMap,
    bookmark = lila.hub.Env.current.actor.bookmark)
}
