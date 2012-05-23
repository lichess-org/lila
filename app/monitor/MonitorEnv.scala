package lila
package monitor

import akka.actor._
import play.api.libs.concurrent._
import play.api.Application

import core.Settings

final class MonitorEnv(
  app: Application,
  settings: Settings) {

  implicit val ctx = app
  import settings._

  lazy val reporting = Akka.system.actorOf(
    Props(new Reporting(
      rpsProvider = rpsProvider
    )), name = ActorReporting)

  val rpsProvider = new RpsProvider(
    timeout = MonitorTimeout)

  val stream = new Stream(
    reporting = reporting,
    timeout = MonitorTimeout)
}
