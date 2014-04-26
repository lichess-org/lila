package lila.hub

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  object actor {
    val game = select("actor.game.actor")
    val gameIndexer = select("actor.game.indexer")
    val renderer = select("actor.renderer")
    val captcher = select("actor.captcher")
    val forum = select("actor.forum.actor")
    val forumIndexer = select("actor.forum.indexer")
    val messenger = select("actor.messenger")
    val router = select("actor.router")
    val teamIndexer = select("actor.team.indexer")
    val ai = select("actor.ai")
    val monitor = select("actor.monitor")
    val tournamentOrganizer = select("actor.tournament.organizer")
    val timeline = select("actor.timeline.user")
    val bookmark = select("actor.bookmark")
    val roundMap = select("actor.round.map")
    val round = select("actor.round.actor")
    val lobby = select("actor.lobby")
    val relation = select("actor.relation")
    val challenger = select("actor.challenger")
    val report = select("actor.report")
    val mod = select("actor.mod")
    val evaluator = select("actor.evaluator")
    val chat = select("actor.chat")
    val analyser = select("actor.analyser")
  }

  object socket {
    val lobby = select("socket.lobby")
    val round = select("socket.round")
    val tournament = select("socket.tournament")
    val site = select("socket.site")
    val monitor = select("socket.monitor")
    val hub = select("socket.hub")
  }

  private def select(name: String) =
    system actorSelection ("/user/" + config.getString(name))
}

object Env {

  lazy val current = "[boot] hub" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = lila.common.PlayApp.system)
}
