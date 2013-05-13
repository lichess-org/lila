package lila.socket

import com.typesafe.config.Config
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.{ ask, pipe }

import lila.common.PimpedConfig._
import lila.hub.actorApi.GetNbMembers
import actorApi._
import makeTimeout.short

final class Env(
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    hub: lila.hub.Env) {

  scheduler.effect(5 seconds, "") {
    hub.socket.hub ! actorApi.Broom
  }

  scheduler.effect(2 seconds, "") {
    hub.socket.hub ! GetNbMembers
  }
}

object Env {

  lazy val current = "[boot] socket" describes new Env(
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    hub = lila.hub.Env.current)
}
