package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketHubName = config getString "socket.hub.name"
  private val SocketHubTimeout = config duration "socket.hub.timeout"

  object actor {
    lazy val game = actorLazyRef("game.actor")
    lazy val gameIndexer = actorLazyRef("game.indexer")
    lazy val renderer = actorLazyRef("renderer")
    lazy val captcher = actorLazyRef("captcher")
    lazy val forum = actorLazyRef("forum.actor")
    lazy val forumIndexer = actorLazyRef("forum.indexer")
    lazy val messenger = actorLazyRef("messenger")
    lazy val router = actorLazyRef("router")
    lazy val teamIndexer = actorLazyRef("team.indexer")
    lazy val ai = actorLazyRef("ai")
    lazy val monitor = actorLazyRef("monitor")
    lazy val tournamentOrganizer = actorLazyRef("tournament.organizer")
    lazy val timeline = actorLazyRef("timeline")
    lazy val bookmark = actorLazyRef("bookmark")
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
