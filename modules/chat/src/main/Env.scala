package lila.chat

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    flood: lila.security.Flood,
    prefApi: lila.pref.PrefApi,
    system: ActorSystem) {

  private val settings = new {
    val CollectionLine = config getString "collection.line"
    val NetDomain = config getString "net.domain"
  }
  import settings._

  private[chat] lazy val lineColl = db(CollectionLine)

  lazy val api = new Api(
    flood = flood,
    prefApi = prefApi,
    netDomain = NetDomain)

  system.actorOf(Props(new ChatActor(api, system.lilaBus)))
}

object Env {

  lazy val current: Env = "[boot] chat" describes new Env(
    config = lila.common.PlayApp loadConfig "chat",
    db = lila.db.Env.current,
    flood = lila.security.Env.current.flood,
    prefApi = lila.pref.Env.current.api,
    system = lila.common.PlayApp.system)
}
