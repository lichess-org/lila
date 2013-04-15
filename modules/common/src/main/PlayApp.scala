package lila.common

import play.api.{ Play, Application, Mode }
import com.typesafe.config.Config

object PlayApp {

  def loadConfig: Config = withApp(_.configuration.underlying)

  def loadConfig(prefix: String): Config = loadConfig getConfig prefix

  def withApp[A](op: Application ⇒ A): A =
    Play.maybeApplication map op err "Play application is not started!"

  def system = withApp { implicit app ⇒
    play.api.libs.concurrent.Akka.system
  }

  def isDev = withApp { _.mode == Mode.Dev }
}
