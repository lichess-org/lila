package lila.common

import com.typesafe.config.Config
import org.joda.time.{ DateTime, Period }
import play.api.i18n.{ Lang, Messages }
import play.api.{ Play, Application, Mode }
import scala.collection.JavaConversions._

object PlayApp {

  val startedAt = DateTime.now

  def uptime = new Period(startedAt, DateTime.now)

  def startedSinceMinutes(minutes: Int) =
    startedAt.isBefore(DateTime.now minusMinutes minutes)

  def startedSinceSeconds(seconds: Int) =
    startedAt.isBefore(DateTime.now minusSeconds seconds)

  def loadConfig: Config = withApp(_.configuration.underlying)

  def loadConfig(prefix: String): Config = loadConfig getConfig prefix

  def withApp[A](op: Application => A): A =
    Play.maybeApplication map op err "Play application is not started!"

  def system = withApp { implicit app =>
    play.api.libs.concurrent.Akka.system
  }

  lazy val langs = loadConfig.getStringList("play.i18n.langs").toList map Lang.apply

  protected def loadMessages(file: String): Map[String, String] = withApp { app =>
    import scala.collection.JavaConverters._
    import play.utils.Resources
    app.classloader.getResources(file).asScala.toList
      .filterNot(url => Resources.isDirectory(app.classloader, url)).reverse
      .map { messageFile =>
        Messages.parse(
          Messages.UrlMessageSource(messageFile),
          messageFile.toString
        ).fold(e => throw e, identity)
      }.foldLeft(Map.empty[String, String]) { _ ++ _ }
  }

  lazy val messages: Map[String, Map[String, String]] = lila.common.Chronometer.syncEffect(
    langs.map(_.code).map { lang =>
      (lang, loadMessages("messages." + lang))
    }.toMap
      .+("default" -> loadMessages("messages"))
      .+("default.play" -> loadMessages("messages.default"))
  ) { lap =>
      lila.log.boot.info(s"${lap.millis}ms PlayApp.messages")
    }

  private def enableScheduler = !(loadConfig getBoolean "app.scheduler.disabled")

  def scheduler = new Scheduler(system.scheduler,
    enabled = enableScheduler && isServer,
    debug = loadConfig getBoolean "app.scheduler.debug")

  def lifecycle = withApp(_.injector.instanceOf[play.api.inject.ApplicationLifecycle])

  lazy val isDev = isMode(_.Dev)
  lazy val isTest = isMode(_.Test)
  lazy val isProd = isMode(_.Prod) && !loadConfig.getBoolean("forcedev")
  def isServer = !isTest

  def isMode(f: Mode.type => Mode.Mode) = withApp { _.mode == f(Mode) }
}
