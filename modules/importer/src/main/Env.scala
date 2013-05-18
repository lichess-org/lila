package lila.importer

import lila.common.PimpedConfig._

import com.typesafe.config.Config

final class Env(
    config: Config,
    roundMap: akka.actor.ActorRef,
    finisher: lila.round.Finisher,
    bookmark: lila.hub.ActorLazyRef) {

  private val Delay = config duration "delay"

  lazy val forms = new DataForm

  lazy val importer = new Importer(roundMap, finisher, bookmark, Delay)
}

object Env {

  lazy val current = "[boot] importer" describes new Env(
    config = lila.common.PlayApp loadConfig "importer",
    roundMap = lila.round.Env.current.roundMap,
    finisher = lila.round.Env.current.finisher,
    bookmark = lila.hub.Env.current.actor.bookmark)
}
