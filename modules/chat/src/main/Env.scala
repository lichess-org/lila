package lila.chat

import akka.actor.{ ActorSystem, Props }
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    flood: lila.security.Flood,
    system: ActorSystem) {

  private val settings = new {
    val CollectionChat = config getString "collection.chat"
    val MaxLinesPerChat = config getInt "max_lines"
    val NetDomain = config getString "net.domain"
    val ActorName = config getString "actor.name"
  }
  import settings._

  lazy val api = new ChatApi(
    coll = chatColl,
    flood = flood,
    maxLinesPerChat = MaxLinesPerChat,
    netDomain = NetDomain)

  system.actorOf(Props(new FrontActor(api)), name = ActorName)

  private[chat] lazy val chatColl = db(CollectionChat)
}

object Env {

  lazy val current: Env = "[boot] chat" describes new Env(
    config = lila.common.PlayApp loadConfig "chat",
    db = lila.db.Env.current,
    flood = lila.security.Env.current.flood,
    system = lila.common.PlayApp.system)
}
