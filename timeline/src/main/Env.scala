package lila.timeline

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    getUsername: String ⇒ Fu[String],
    sockets: ActorRef) {

  val CollectionEntry = config getString "collection.entry"
  val DisplayMax = config getString "display_max"

}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "timeline",
    db = lila.db.Env.current,
    getUsername = lila.user.Env.current.usernameOrAnonymous _,
    sockets = lila.hub.Env.current.sockets)
}
