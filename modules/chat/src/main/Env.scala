package lila.chat

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    getUsername: String ⇒ Fu[String],
    flood: lila.security.Flood,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    modApi: lila.mod.ModApi,
    system: ActorSystem) {

  private val settings = new {
    val CollectionChat = config getString "collection.chat"
    val NetDomain = config getString "net.domain"
  }
  import settings._

  private[chat] lazy val chatColl = db(CollectionChat)
}

object Env {

  lazy val current: Env = "[boot] chat" describes new Env(
    config = lila.common.PlayApp loadConfig "chat",
    db = lila.db.Env.current,
    getUsername = lila.user.Env.current.usernameOrAnonymous,
    flood = lila.security.Env.current.flood,
    relationApi = lila.relation.Env.current.api,
    prefApi = lila.pref.Env.current.api,
    modApi = lila.mod.Env.current.api,
    system = lila.common.PlayApp.system)
}
