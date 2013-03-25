package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  val MetaTimeout = config duration "meta.timeout"
  val MetaName = config getString "meta.name"

  val LobbyName = config getString "lobby.name"
  val RendererName = config getString "renderer.name"

  val lobbyActor = system.actorFor("/user/" + LobbyName)
  val rendererActor = system.actorFor("/user/" + RendererName)

  val meta = new MetaHub(Nil, MetaTimeout)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
