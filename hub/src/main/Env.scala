package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketHubName = config getString "socket.hub.name"
  private val SocketHubTimeout = config duration "socket.hub.timeout"

  object actor {
    val lobby = actorFor("actor.lobby")
    val renderer = actorFor("actor.renderer")
    val captcher = actorFor("actor.captcher")
    val forumIndexer = actorFor("actor.forum_indexer")
    val messenger = actorFor("actor.messenger")
    val router = actorFor("actor.router")
    val forum = actorFor("actor.forum")
    val teamIndexer = actorFor("actor.team_indexer")
    val ai = actorFor("actor.ai")
    val monitor = actorFor("actor.monitor")
  }

  object socket {
    val lobby = actorFor("socket.lobby")
    val monitor = actorFor("socket.monitor")
    val hub = system.actorOf(Props(new Broadcast(List(
      socket.lobby
    ), SocketHubTimeout)), name = SocketHubName)
  }

  private def actorFor(name: String) =
    system.actorFor("/user/" + config.getString(name))
}

object Env {

  lazy val current = "[boot] hub" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
