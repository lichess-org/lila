package lila.hub

import com.typesafe.config.Config
import akka.actor._
import lila.common.PimpedConfig._

final class Env(config: Config, system: ActorSystem) {

  private val SocketsTimeout = config duration "sockets.timeout"
  private val SocketsName = config getString "sockets.name"

  object actor {
    val lobby = actorFor(config getString "actor.lobby.name")
    val renderer = actorFor(config getString "actor.renderer.name")
    val captcher = actorFor(config getString "actor.captcher.name")
    val forumIndexer = actorFor(config getString "actor.forum_indexer.name")
    val messenger = actorFor(config getString "actor.messenger.name")
    val router = actorFor(config getString "actor.router.name")
    val forum = actorFor(config getString "actor.forum.name")
  }

  val sockets = system.actorOf(Props(new Broadcast(List(
    actor.lobby
  ), SocketsTimeout)), name = SocketsName)

  private def actorFor(name: String) = system.actorFor("/user/" + name)
}

object Env {

  lazy val current = "[hub] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "hub",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current))
}
