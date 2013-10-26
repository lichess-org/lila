package lila.socket

import akka.actor.ActorSystem
import akka.pattern.{ ask, pipe }
import com.typesafe.config.Config

import actorApi._
import lila.common.PimpedConfig._
import lila.hub.actorApi.GetNbMembers
import makeTimeout.short

final class Env(
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    hub: lila.hub.Env) {

  import scala.concurrent.duration._

  scheduler.once(5 seconds) {
    scheduler.effect(4 seconds, "publish broom to event bus") {
      system.eventStream.publish(actorApi.Broom)
    }
    scheduler.messageToSelection(1 seconds) {
      hub.socket.hub -> GetNbMembers
    }
  }
}

object Env {

  lazy val current = "[boot] socket" describes new Env(
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    hub = lila.hub.Env.current)
}
