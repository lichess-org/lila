package lila.chat

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    getUsername: String ⇒ Fu[String],
    getTeamName: String ⇒ Fu[Option[String]],
    getTeamIds: String ⇒ Fu[List[String]],
    flood: lila.security.Flood,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    modApi: lila.mod.ModApi,
    system: ActorSystem) {

  private val settings = new {
    val CollectionLine = config getString "collection.line"
    val NetDomain = config getString "net.domain"
  }
  import settings._

  private[chat] lazy val lineColl = db(CollectionLine)

  lazy val api = new Api(
    namer = namer,
    chanVoter = new ChanVoter,
    flood = flood,
    relationApi = relationApi,
    prefApi = prefApi,
    getTeamIds = getTeamIds,
    netDomain = NetDomain)

  lazy val namer = new Namer(
    getUsername = getUsername,
    getTeamName = getTeamName)

  system.actorOf(Props(new ChatActor(
    api = api,
    namer = namer,
    bus = system.lilaBus,
    relationApi = relationApi,
    prefApi = prefApi,
    makeCommander = () ⇒ new Commander(
      modApi = modApi,
      namer = namer,
      getTeamIds = getTeamIds
    ))))
}

object Env {

  lazy val current: Env = "[boot] chat" describes new Env(
    config = lila.common.PlayApp loadConfig "chat",
    db = lila.db.Env.current,
    getUsername = lila.user.Env.current.usernameOrAnonymous,
    getTeamName = lila.team.Env.current.api.teamName,
    getTeamIds = lila.team.Env.current.api.teamIds,
    flood = lila.security.Env.current.flood,
    relationApi = lila.relation.Env.current.api,
    prefApi = lila.pref.Env.current.api,
    modApi = lila.mod.Env.current.api,
    system = lila.common.PlayApp.system)
}
