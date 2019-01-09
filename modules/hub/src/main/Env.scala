package lila.hub

import akka.actor._
import com.typesafe.config.Config

final class Env(config: Config, system: ActorSystem) {

  val gameSearch = select("actor.game.search")
  val renderer = select("actor.renderer")
  val captcher = select("actor.captcher")
  val forumSearch = select("actor.forum.search")
  val teamSearch = select("actor.team.search")
  val fishnet = select("actor.fishnet")
  val tournamentApi = select("actor.tournament.api")
  val timeline = select("actor.timeline.user")
  val bookmark = select("actor.bookmark")
  val relation = select("actor.relation")
  val report = select("actor.report")
  val shutup = select("actor.shutup")
  val mod = select("actor.mod")
  val chat = select("actor.chat")
  val notification = select("actor.notify")

  val bus = system.lilaBus

  private def select(name: String) =
    system actorSelection ("/user/" + config.getString(name))
}

object Env {

  lazy val current = "hub" boot new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = lila.common.PlayApp.system
  )
}
