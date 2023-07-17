package lila.app

import java.io.*

import play.api.{ Application, Environment, Play, Configuration, Mode }
import play.core.server.{ RealServerProcess, ServerProcess, Server, ServerStartException, ServerConfig }
import play.core.server.NettyServer
import play.api.inject.DefaultApplicationLifecycle

// The program entry point.
// To run with bloop:
// /path/to/bloop run lila -m lila.app.Lila -c /path/to/lila/.bloop
object Lila:

  def main(args: Array[String]): Unit = start(new RealServerProcess(args.toIndexedSeq))

  /** Starts a Play server and application for the given process. The settings for the server are based on
    * values passed on the command line and in various system properties. Crash out by exiting the given
    * process if there are any problems.
    *
    * @param process
    *   The process (real or abstract) to use for starting the server.
    */
  def start(process: ServerProcess): Server = try
    // Configure logback early - before play invokes Logger
    LoggerConfigurator.configure()

    val config: ServerConfig = readServerConfigSettings(process)

    // Start the application
    val application: Application =
      val environment = Environment(config.rootDir, process.classLoader, config.mode)
      LilaComponents(
        environment,
        DefaultApplicationLifecycle(),
        Configuration.load(environment)
      ).application

    Play.start(application)

    val server = NettyServer(
      config,
      application,
      stopHook = () => funit,
      application.actorSystem
    )(application.materializer)

    process.addShutdownHook:
      // Only run server stop if the shutdown reason is not defined. That means the
      // process received a SIGTERM (or other acceptable signal) instead of being
      // stopped because of CoordinatedShutdown, for example when downing a cluster.
      // The reason for that is we want to avoid calling coordinated shutdown from
      // inside a JVM shutdown hook if the trigger of the JVM shutdown hook was
      // coordinated shutdown.
      if application.coordinatedShutdown.shutdownReason().isEmpty then server.stop()

    lila.common.Lilakka.shutdown(
      application.coordinatedShutdown,
      _.PhaseBeforeActorSystemTerminate,
      "Shut down logging"
    ): () =>
      fuccess(LoggerConfigurator.shutdown())

    server
  catch
    case ServerStartException(message, cause) => process.exit(message, cause)
    case e: Throwable                         => process.exit("Oops, cannot start the server.", Some(e))

  def readServerConfigSettings(process: ServerProcess): ServerConfig =
    val configuration: Configuration =
      val rootDirArg    = process.args.headOption.map(new File(_))
      val rootDirConfig = rootDirArg.so(ServerConfig.rootDirConfig(_))
      Configuration.load(process.classLoader, process.properties, rootDirConfig, true)

    val rootDir: File =
      val path = configuration
        .getOptional[String]("play.server.dir")
        .getOrElse(throw ServerStartException("No root server path supplied"))
      val file = File(path)
      if !file.isDirectory then throw ServerStartException(s"Bad root server path: $path")
      file

    def parsePort(portType: String): Option[Int] =
      configuration
        .getOptional[String](s"play.server.$portType.port")
        .filter(_ != "disabled")
        .map: str =>
          try Integer.parseInt(str)
          catch
            case _: NumberFormatException =>
              throw ServerStartException(s"Invalid ${portType.toUpperCase} port: $str")

    parsePort("http") match
      case None => throw ServerStartException("Must provide an HTTP port")
      case Some(httpPort) =>
        val address = configuration.getOptional[String]("play.server.http.address").getOrElse("0.0.0.0")

        val mode =
          if configuration.getOptional[String]("play.mode").contains("prod") then Mode.Prod
          else Mode.Dev

        ServerConfig(rootDir, httpPort, address, mode, process.properties, configuration)
