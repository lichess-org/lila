package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketHubName = config getString "socket.hub.name"
  private val SocketHubTimeout = config duration "socket.hub.timeout"

  object actor {
    lazy val game = actorSelection("game.actor")
    lazy val gameIndexer = actorSelection("game.indexer")
    lazy val renderer = actorSelection("renderer")
    lazy val captcher = actorSelection("captcher")
    lazy val forum = actorSelection("forum.actor")
    lazy val forumIndexer = actorSelection("forum.indexer")
    lazy val messenger = actorSelection("messenger")
    lazy val router = actorSelection("router")
    lazy val teamIndexer = actorSelection("team.indexer")
    lazy val ai = actorSelection("ai")
    lazy val monitor = actorSelection("monitor")
    lazy val tournamentOrganizer = actorSelection("tournament.organizer")
    lazy val timeline = actorSelection("timeline")
    lazy val bookmark = actorSelection("bookmark")
  }

  object socket {
    lazy val lobby = socketSelection("lobby")
    lazy val monitor = socketSelection("monitor")
    lazy val site = socketSelection("site")
    lazy val round = socketSelection("round")
    lazy val tournament = socketSelection("tournament")
    // this one must load after its broadcasted actors
    lazy val hub = system.actorOf(Props(new Broadcast(List(
      lobby, site, round, tournament
    ))(makeTimeout(SocketHubTimeout))), name = SocketHubName)
  }

  private def actorSelection(name: String) =
    system.actorSelection("/user/" + config.getString("actor." + name))

  private def socketSelection(name: String) =
    system.actorSelection("/user/" + config.getString("socket." + name))
}

object Env {

  lazy val current = "[boot] hub" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
