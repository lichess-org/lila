package lila.activity

import scala.concurrent.duration._

import akka.actor._

final class Env(
    system: akka.actor.ActorSystem
) {
}

object Env {

  lazy val current: Env = "activity" boot new Env(
    system = lila.common.PlayApp.system
  )
}
