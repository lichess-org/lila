package lila.hub

import akka.actor.*
import com.softwaremill.macwire.*
import com.typesafe.config.Config
import play.api.Configuration

object actors:
  trait Actor:
    val actor: ActorSelection
    val ! = actor.!
  final class GameSearch(val actor: ActorSelection)  extends Actor
  final class ForumSearch(val actor: ActorSelection) extends Actor
  final class TeamSearch(val actor: ActorSelection)  extends Actor
  final class Fishnet(val actor: ActorSelection)     extends Actor
  final class Bookmark(val actor: ActorSelection)    extends Actor
  final class Shutup(val actor: ActorSelection)      extends Actor
  final class Timeline(val actor: ActorSelection)    extends Actor
  final class Report(val actor: ActorSelection)      extends Actor
  final class Renderer(val actor: ActorSelection)    extends Actor
  final class Captcher(val actor: ActorSelection)    extends Actor

@Module
final class Env(
    appConfig: Configuration,
    system: ActorSystem
):

  import actors.*

  private val config = appConfig.get[Config]("hub")

  val gameSearch  = GameSearch(select("actor.game.search"))
  val renderer    = Renderer(select("actor.renderer"))
  val captcher    = Captcher(select("actor.captcher"))
  val forumSearch = ForumSearch(select("actor.forum.search"))
  val teamSearch  = TeamSearch(select("actor.team.search"))
  val fishnet     = Fishnet(select("actor.fishnet"))
  val timeline    = Timeline(select("actor.timeline.user"))
  val bookmark    = Bookmark(select("actor.bookmark"))
  val report      = Report(select("actor.report"))
  val shutup      = Shutup(select("actor.shutup"))

  private def select(name: String) =
    system.actorSelection("/user/" + config.getString(name))
