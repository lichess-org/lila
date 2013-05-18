package lila.importer

import lila.common.PimpedConfig._

import com.typesafe.config.Config

final class Env(
    config: Config,
    roundMap: akka.actor.ActorRef,
    bookmark: lila.hub.ActorLazyRef) {

  private val Delay = config duration "delay"

  lazy val forms = new DataForm

  lazy val importer = new Importer(roundMap, bookmark, Delay)
}

object Env {

  lazy val current = "[boot] importer" describes new Env(
    config = lila.common.PlayApp loadConfig "importer",
    roundMap = lila.round.Env.current.roundMap,
    bookmark = lila.hub.Env.current.actor.bookmark)
}
