package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val settings = new {
    val SocketsTimeout = config duration "sockets.timeout"
    val SocketsName = config getString "sockets.name"

    val LobbyName = config getString "lobby.name"
    val RendererName = config getString "renderer.name"
    val CaptcherName = config getString "captcher.name"
  }
  import settings._

  object actor {
    val lobby = actorFor(LobbyName)
    val renderer = actorFor(RendererName)
    val captcher = actorFor(CaptcherName)
  }

  val sockets = system.actorOf(Props(new Broadcast(List(
    actorFor(LobbyName)
  ), SocketsTimeout)), name = SocketsName)

  private def actorFor(name: String) = system.actorFor("/user/" + name)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
