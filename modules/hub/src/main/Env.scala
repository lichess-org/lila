package lila.hub

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketHubName = config getString "socket.hub.name"
  private val SocketHubTimeout = config duration "socket.hub.timeout"

  object actor {
    val game = actorNamed("game.actor")
    val gameIndexer = actorNamed("game.indexer")
    val renderer = actorNamed("renderer")
    val captcher = actorNamed("captcher")
    val forum = actorNamed("forum.actor")
    val forumIndexer = actorNamed("forum.indexer")
    val messenger = actorNamed("messenger")
    val router = actorNamed("router")
    val teamIndexer = actorNamed("team.indexer")
    val ai = actorNamed("ai")
    val monitor = actorNamed("monitor")
    val tournamentOrganizer = actorNamed("tournament.organizer")
    val gameTimeline = actorNamed("timeline.game")
    val timeline = actorNamed("timeline.user")
    val bookmark = actorNamed("bookmark")
    val roundMap = actorNamed("round.map")
    val round = actorNamed("round.actor")
    val lobby = actorNamed("lobby")
    val relation = actorNamed("relation")
    val challenger = actorNamed("challenger")
  }

  object socket {
    val lobby = socketNamed("lobby")
    val monitor = socketNamed("monitor")
    val site = socketNamed("site")
    val round = socketNamed("round")
    val tournament = socketNamed("tournament")
    val hub = system actorSelection SocketHubName
  }

  system.actorOf(Props(new Broadcast(List(
    socket.lobby,
    socket.site,
    socket.round,
    socket.tournament
  ))(makeTimeout(SocketHubTimeout))), name = SocketHubName)

  private def actorNamed(name: String) =
    system actorSelection config.getString("actor." + name)

  private def socketNamed(name: String) =
    system actorSelection config.getString("socket." + name)
}

object Env {

  lazy val current = "[boot] hub" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
