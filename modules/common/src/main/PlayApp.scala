package lila.common

import com.typesafe.config.Config
import org.joda.time.{ DateTime, Period }
import play.api.i18n.Lang
import play.api.{ Play, Application, Mode }
import scala.collection.JavaConverters._

object PlayApp {

  val startedAt = DateTime.now
  val startedAtMillis = nowMillis

  def uptime = new Period(startedAt, DateTime.now)

  def startedSinceMinutes(minutes: Int) =
    startedSinceSeconds(minutes * 60)

  def startedSinceSeconds(seconds: Int) =
    startedAtMillis < (nowMillis - (seconds * 1000))

  def loadConfig: Config = withApp(_.configuration.underlying)

  def loadConfig(prefix: String): Config = loadConfig getConfig prefix

  def withApp[A](op: Application => A): A =
    op(old.play.Env.application)

  // def system = withApp { implicit app =>
  //   old.play.api.libs.concurrent.Akka.system
  // }

  lazy val langs = loadConfig.getStringList("play.i18n.langs").asScala.map(Lang.apply)(scala.collection.breakOut)

  private def enableScheduler = !(loadConfig getBoolean "app.scheduler.disabled")

  lazy val scheduler = new Scheduler(
    old.play.Env.actorSystem.scheduler,
    enabled = enableScheduler && isServer,
    debug = loadConfig getBoolean "app.scheduler.debug"
  )

  def lifecycle = old.play.Env.lifecycle

  lazy val isDev = isMode(_.Dev)
  lazy val isTest = isMode(_.Test)
  lazy val isProd = isMode(_.Prod) && !loadConfig.getBoolean("app.forcedev")
  def isServer = !isTest

  def isMode(f: Mode.type => Mode) = withApp { _.mode == f(Mode) }
}
