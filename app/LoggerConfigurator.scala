package lila.app

import java.io.File
import java.net.URL

import ch.qos.logback.classic._
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.classic.util.ContextInitializer
import ch.qos.logback.core.util._
import org.slf4j.ILoggerFactory
import org.slf4j.LoggerFactory
import org.slf4j.bridge._
import play.api._

class LoggerConfigurator {
  def loggerFactory: ILoggerFactory = {
    LoggerFactory.getILoggerFactory
  }

  /** Initialize the Logger when there's no application ClassLoader available.
    */
  def init(rootPath: java.io.File, mode: Mode): Unit = {
    val properties   = Map("application.home" -> rootPath.getAbsolutePath)
    val resourceName = if (mode == Mode.Dev) "logback-play-dev.xml" else "logback-play-default.xml"
    val resourceUrl  = Option(this.getClass.getClassLoader.getResource(resourceName))
    configure(properties, resourceUrl)
  }

  def configure(): Unit = {

    // Get an explicitly configured file URL
    def explicitFileUrl = sys.props.get("logger.file").map(new File(_).toURI.toURL)

    // Get an explicitly configured URL
    def explicitUrl = sys.props.get("logger.url").map(new URL(_))

    val configUrl = explicitFileUrl.orElse(explicitUrl)

    val props = Map("application.home" -> new File(".").getAbsolutePath)

    configure(props, configUrl)
  }

  def configure(properties: Map[String, String], config: Option[URL]): Unit = {
    // Touching LoggerContext is not thread-safe, and so if you run several
    // application tests at the same time (spec2 / scalatest with "new WithApplication()")
    // then you will see NullPointerException as the array list loggerContextListenerList
    // is accessed concurrently from several different threads.
    //
    // The workaround is to use a synchronized block around a singleton
    // instance -- in this case, we use the StaticLoggerBinder's loggerFactory.
    // loggerFactory.synchronized {
      // Redirect JUL -> SL4FJ

      // Remove existing handlers from JUL
      SLF4JBridgeHandler.removeHandlersForRootLogger()

      // Configure logback
      val ctx = loggerFactory.asInstanceOf[LoggerContext]

      ctx.reset()

      // Set a level change propagator to minimize the overhead of JUL
      //
      // Please note that translating a java.util.logging event into SLF4J incurs the
      // cost of constructing LogRecord instance regardless of whether the SLF4J logger
      // is disabled for the given level. Consequently, j.u.l. to SLF4J translation can
      // seriously increase the cost of disabled logging statements (60 fold or 6000%
      // increase) and measurably impact the performance of enabled log statements
      // (20% overall increase). Please note that as of logback-version 0.9.25,
      // it is possible to completely eliminate the 60 fold translation overhead for
      // disabled log statements with the help of LevelChangePropagator.
      //
      // https://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html
      // https://logback.qos.ch/manual/configuration.html#LevelChangePropagator
      val levelChangePropagator = new LevelChangePropagator()
      levelChangePropagator.setContext(ctx)
      levelChangePropagator.setResetJUL(true)
      ctx.addListener(levelChangePropagator)
      SLF4JBridgeHandler.install()

      // Ensure that play.Logger and play.api.Logger are ignored when detecting file name and line number for
      // logging
      val frameworkPackages = ctx.getFrameworkPackages
      frameworkPackages.add(classOf[play.api.Logger].getName)

      properties.foreach { case (k, v) => ctx.putProperty(k, v) }

      config match {
        case Some(url) =>
          val initializer = new ContextInitializer(ctx)
          initializer.configureByResource(url)
        case None =>
          System.err.println("Could not detect a logback configuration file, not configuring logback")
      }

      StatusPrinter.printIfErrorsOccured(ctx)
    // }
  }

  /** Shutdown the logger infrastructure.
    */
  def shutdown(): Unit = {
    val ctx = loggerFactory.asInstanceOf[LoggerContext]
    ctx.stop()

    org.slf4j.bridge.SLF4JBridgeHandler.uninstall()

    // Unset the global application mode for logging
    play.api.Logger.unsetApplicationMode()
  }
}
