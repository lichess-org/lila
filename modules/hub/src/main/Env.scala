package lila.hub

import akka.actor._
import com.typesafe.config.Config
import play.api.Configuration

object actors {
  trait Actor {
    val actor: ActorSelection
    val ! = actor ! _
  }
  case class Relation(actor: ActorSelection) extends Actor
  case class Timeline(actor: ActorSelection) extends Actor
  case class Report(actor: ActorSelection) extends Actor
  case class Renderer(actor: ActorSelection) extends Actor
}

final class Env(
    appConfig: Configuration,
    system: ActorSystem
) {

  import actors._

  val config = appConfig.get[Config]("hub")

  val gameSearch = select("actor.game.search")
  val renderer = Renderer(select("actor.renderer"))
  val captcher = select("actor.captcher")
  val forumSearch = select("actor.forum.search")
  val teamSearch = select("actor.team.search")
  val fishnet = select("actor.fishnet")
  val tournamentApi = select("actor.tournament.api")
  val timeline = Timeline(select("actor.timeline.user"))
  val bookmark = select("actor.bookmark")
  val relation = Relation(select("actor.relation"))
  val report = Report(select("actor.report"))
  val shutup = select("actor.shutup")
  val mod = select("actor.mod")
  val notification = select("actor.notify")

  private def select(name: String) =
    system.actorSelection("/user/" + config.getString(name))
}
