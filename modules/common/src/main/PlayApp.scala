package lila.common

import com.typesafe.config.Config
import play.api.{ Play, Application, Mode }

object PlayApp {

  def loadConfig: Config = withApp(_.configuration.underlying)

  def loadConfig(prefix: String): Config = loadConfig getConfig prefix

  def withApp[A](op: Application => A): A =
    Play.maybeApplication map op err "Play application is not started!"

  def system = withApp { implicit app =>
    play.api.libs.concurrent.Akka.system
  }

  private def enableScheduler = !(loadConfig getBoolean "app.scheduler.disabled")

  def scheduler = new Scheduler(system.scheduler,
    enabled = enableScheduler && isServer,
    debug = loadConfig getBoolean "app.scheduler.debug")

  def isDev = isMode(_.Dev)
  def isTest = isMode(_.Test)
  def isProd = isMode(_.Prod)
  def isServer = !isTest
  def isMode(f: Mode.type => Mode.Mode) = withApp { _.mode == f(Mode) }
}
