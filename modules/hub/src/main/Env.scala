package lila.hub

import akka.actor._
import com.softwaremill.macwire._
import com.typesafe.config.Config
import play.api.Configuration

object actors {
  trait Actor {
    val actor: ActorSelection
    val ! = actor ! _
  }
  case class GameSearch(actor: ActorSelection)    extends Actor
  case class ForumSearch(actor: ActorSelection)   extends Actor
  case class TeamSearch(actor: ActorSelection)    extends Actor
  case class Fishnet(actor: ActorSelection)       extends Actor
  case class TournamentApi(actor: ActorSelection) extends Actor
  case class Bookmark(actor: ActorSelection)      extends Actor
  case class Shutup(actor: ActorSelection)        extends Actor
  case class Notification(actor: ActorSelection)  extends Actor
  case class Timeline(actor: ActorSelection)      extends Actor
  case class Report(actor: ActorSelection)        extends Actor
  case class Renderer(actor: ActorSelection)      extends Actor
  case class Captcher(actor: ActorSelection)      extends Actor
}

@Module
final class Env(
    appConfig: Configuration,
    system: ActorSystem
) {

  import actors._

  private val config = appConfig.get[Config]("hub")

  val gameSearch    = GameSearch(select("actor.game.search"))
  val renderer      = Renderer(select("actor.renderer"))
  val captcher      = Captcher(select("actor.captcher"))
  val forumSearch   = ForumSearch(select("actor.forum.search"))
  val teamSearch    = TeamSearch(select("actor.team.search"))
  val fishnet       = Fishnet(select("actor.fishnet"))
  val tournamentApi = TournamentApi(select("actor.tournament.api"))
  val timeline      = Timeline(select("actor.timeline.user"))
  val bookmark      = Bookmark(select("actor.bookmark"))
  val report        = Report(select("actor.report"))
  val shutup        = Shutup(select("actor.shutup"))
  val notification  = Notification(select("actor.notify"))

  private def select(name: String) =
    system.actorSelection("/user/" + config.getString(name))
}
