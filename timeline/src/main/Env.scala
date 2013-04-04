package lila.timeline

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    getUsername: String ⇒ Fu[String],
    lobby: ActorRef) {

  private val CollectionEntry = config getString "collection.entry"
  private val DisplayMax = config getString "display_max"

  lazy val push = new Push(
    lobby = lobby,
    getUsername = getUsername)

  private[timeline] lazy val entryColl = db(CollectionEntry)
}

object Env {

  lazy val current = "[boot] timeline" describes new Env(
    config = lila.common.PlayApp loadConfig "timeline",
    db = lila.db.Env.current,
    getUsername = lila.user.Env.current.usernameOrAnonymous _,
    lobby = lila.hub.Env.current.actor.lobby)
}
