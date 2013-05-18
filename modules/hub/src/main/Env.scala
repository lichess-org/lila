package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketHubName = config getString "socket.hub.name"
  private val SocketHubTimeout = config duration "socket.hub.timeout"

  object actor {
    val game = actorLazyRef("game.actor")
    val gameIndexer = actorLazyRef("game.indexer")
    val renderer = actorLazyRef("renderer")
    val captcher = actorLazyRef("captcher")
    val forum = actorLazyRef("forum.actor")
    val forumIndexer = actorLazyRef("forum.indexer")
    val messenger = actorLazyRef("messenger")
    val router = actorLazyRef("router")
    val teamIndexer = actorLazyRef("team.indexer")
    val ai = actorLazyRef("ai")
    val monitor = actorLazyRef("monitor")
    val tournamentOrganizer = actorLazyRef("tournament.organizer")
    val timeline = actorLazyRef("timeline")
    val bookmark = actorLazyRef("bookmark")
    val roundMap = actorLazyRef("round.map")
    val lobby = actorLazyRef("lobby")
  }

  object socket {
    val lobby = socketLazyRef("lobby")
    val monitor = socketLazyRef("monitor")
    val site = socketLazyRef("site")
    val round = socketLazyRef("round")
    val tournament = socketLazyRef("tournament")
    val hub = lazyRef(SocketHubName)
  }

  system.actorOf(Props(new Broadcast(List(
    socket.lobby,
    socket.site, 
    socket.round, 
    socket.tournament
  ))(makeTimeout(SocketHubTimeout))), name = SocketHubName)

  private val lazyRef = ActorLazyRef(system) _

  private def actorLazyRef(name: String) =
    lazyRef(config.getString("actor." + name))

  private def socketLazyRef(name: String) =
    lazyRef(config.getString("socket." + name))
}

object Env {

  lazy val current = "[boot] hub" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
