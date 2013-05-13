package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketHubName = config getString "socket.hub.name"
  private val SocketHubTimeout = config duration "socket.hub.timeout"

  object actor {
    lazy val game = actorFor("game.actor")
    lazy val gameIndexer = actorFor("game.indexer")
    lazy val renderer = actorFor("renderer")
    lazy val captcher = actorFor("captcher")
    lazy val forum = actorFor("forum.actor")
    lazy val forumIndexer = actorFor("forum.indexer")
    lazy val messenger = actorFor("messenger")
    lazy val router = actorFor("router")
    lazy val teamIndexer = actorFor("team.indexer")
    lazy val ai = actorFor("ai")
    lazy val monitor = actorFor("monitor")
    lazy val tournamentOrganizer = actorFor("tournament.organizer")
    lazy val timeline = actorFor("timeline")
    lazy val bookmark = actorFor("bookmark")
  }

  object socket {
    val lobby = socketLazyRef("lobby")
    val monitor = socketLazyRef("monitor")
    val site = socketLazyRef("site")
    val round = socketLazyRef("round")
    val tournament = socketLazyRef("tournament")
    val hub = system.actorOf(Props(new Broadcast(List(
      lobby, site, round, tournament
    ))(makeTimeout(SocketHubTimeout))), name = SocketHubName)
  }

  private def actorFor(name: String) =
    system.actorFor("/user/" + config.getString("actor." + name))

  private def socketLazyRef(name: String) =
    ActorLazyRef(system)(config.getString("socket." + name))
}

object Env {

  lazy val current = "[boot] hub" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
