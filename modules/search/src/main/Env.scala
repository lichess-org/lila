package lila.search

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.{ Success, Failure }

import akka.actor.ActorSystem
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  lazy val client = ESClient.make
}

object Env {

  lazy val current = "[boot] search" describes new Env(
    config = lila.common.PlayApp loadConfig "search",
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
