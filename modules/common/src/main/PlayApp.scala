package lila.common

import com.typesafe.config.Config
import org.joda.time.{ DateTime, Period }
import play.api.{ Play, Application, Mode }
import scala.collection.JavaConversions._

object PlayApp {

  val startedAt = DateTime.now
  val startedAtMillis = nowMillis

  def uptimeSeconds = nowSeconds - startedAt.getSeconds

  def startedSinceMinutes(minutes: Int) =
    startedSinceSeconds(minutes * 60)

  def startedSinceSeconds(seconds: Int) =
    startedAtMillis < (nowMillis - (seconds * 1000))

  def loadConfig: Config = withApp(_.configuration.underlying)

  def loadConfig(prefix: String): Config = loadConfig getConfig prefix

  def withApp[A](op: Application => A): A =
    Play.maybeApplication map op err "Play application is not started!"

  def system = withApp { implicit app =>
    play.api.libs.concurrent.Akka.system
  }

  private def enableScheduler = !(loadConfig getBoolean "app.scheduler.disabled")

  lazy val scheduler = new Scheduler(
    system.scheduler,
    enabled = enableScheduler && isServer,
    debug = loadConfig getBoolean "app.scheduler.debug"
  )

  def lifecycle = withApp(_.injector.instanceOf[play.api.inject.ApplicationLifecycle])

  lazy val isDev = isMode(_.Dev)
  lazy val isTest = isMode(_.Test)
  lazy val isProd = isMode(_.Prod) && !loadConfig.getBoolean("app.forcedev")
  def isServer = !isTest

  def isMode(f: Mode.type => Mode.Mode) = withApp { _.mode == f(Mode) }
}
