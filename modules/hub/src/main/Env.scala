package lila.hub

import akka.actor._
import com.typesafe.config.Config
import play.api.Configuration

final class Env(
    appConfig: Configuration,
    system: ActorSystem
) {

  val config = appConfig.get[Config]("hub")

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
  val notification = select("actor.notify")

  private def select(name: String) =
    system.actorSelection("/user/" + config.getString(name))
}
