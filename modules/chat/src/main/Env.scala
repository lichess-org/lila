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
    system: ActorSystem) {

  private val settings = new {
    val CollectionLine = config getString "collection.line"
    val NetDomain = config getString "net.domain"
  }
  import settings._

  private[chat] lazy val lineColl = db(CollectionLine)

  lazy val api = new Api(
    namer = namer,
    flood = flood,
    relationApi = relationApi,
    prefApi = prefApi,
    netDomain = NetDomain)

  lazy val namer = new Namer(getUsername)

  system.actorOf(Props(new ChatActor(
    api = api,
    namer = namer,
    bus = system.lilaBus,
    relationApi = relationApi,
    prefApi = prefApi)))
}

object Env {

  lazy val current: Env = "[boot] chat" describes new Env(
    config = lila.common.PlayApp loadConfig "chat",
    db = lila.db.Env.current,
    getUsername = lila.user.Env.current.usernameOrAnonymous,
    flood = lila.security.Env.current.flood,
    relationApi = lila.relation.Env.current.api,
    prefApi = lila.pref.Env.current.api,
    system = lila.common.PlayApp.system)
}
