package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketHubName = config getString "socket.hub.name"
  private val SocketHubTimeout = config duration "socket.hub.timeout"

  object actor {
    val gameIndexer = actorFor("game.indexer")
    val lobby = actorFor("lobby")
    val renderer = actorFor("renderer")
    val captcher = actorFor("captcher")
    val forumIndexer = actorFor("forum.indexer")
    val messenger = actorFor("messenger")
    val router = actorFor("router")
    val forum = actorFor("forum")
    val teamIndexer = actorFor("team.indexer")
    val ai = actorFor("ai")
    val monitor = actorFor("monitor")
    val tournamentOrganizer = actorFor("tournament.organizer")
    val timeline = actorFor("timeline")
    val bookmark = actorFor("bookmark")
  }

  object socket {
    val lobby = socketFor("lobby")
    val monitor = socketFor("monitor")
    val hub = system.actorOf(Props(new Broadcast(List(
      socket.lobby
    ))(makeTimeout(SocketHubTimeout))), name = SocketHubName)
  }

  private def actorFor(name: String) =
    system.actorFor("/user/" + config.getString("actor/" + name))

  private def socketFor(name: String) =
    system.actorFor("/user/" + config.getString("socket/" + name))
}

object Env {

  lazy val current = "[boot] hub" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
