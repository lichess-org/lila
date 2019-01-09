package lila.team

import akka.actor._
import com.typesafe.config.Config

import lila.common.MaxPerPage
import lila.mod.ModlogApi
import lila.notify.NotifyApi

final class Env(
    config: Config,
    hub: lila.hub.Env,
    modLog: ModlogApi,
    notifyApi: NotifyApi,
    system: ActorSystem,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
) {

  private val settings = new {
    val CollectionTeam = config getString "collection.team"
    val CollectionMember = config getString "collection.member"
    val CollectionRequest = config getString "collection.request"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val PaginatorMaxUserPerPage = config getInt "paginator.max_user_per_page"
  }
  import settings._

  private[team] lazy val colls = new Colls(
    team = db(CollectionTeam),
    request = db(CollectionRequest),
    member = db(CollectionMember)
  )

  lazy val forms = new DataForm(colls.team, hub.captcher)

  lazy val memberStream = new TeamMemberStream(colls.member)(system)

  lazy val api = new TeamApi(
    coll = colls,
    cached = cached,
    notifier = notifier,
    bus = system.lilaBus,
    indexer = hub.teamSearch,
    timeline = hub.timeline,
    modLog = modLog
  )

  lazy val paginator = new PaginatorBuilder(
    coll = colls,
    maxPerPage = MaxPerPage(PaginatorMaxPerPage),
    maxUserPerPage = MaxPerPage(PaginatorMaxUserPerPage)
  )

  lazy val cli = new Cli(api, colls)

  lazy val cached = new Cached(asyncCache)(system)

  private lazy val notifier = new Notifier(notifyApi = notifyApi)

  system.lilaBus.subscribeFun('shadowban) {
    case lila.hub.actorApi.mod.Shadowban(userId, true) => api deleteRequestsByUserId userId
  }
}

object Env {

  lazy val current = "team" boot new Env(
    config = lila.common.PlayApp loadConfig "team",
    hub = lila.hub.Env.current,
    modLog = lila.mod.Env.current.logApi,
    notifyApi = lila.notify.Env.current.api,
    system = lila.common.PlayApp.system,
    asyncCache = lila.memo.Env.current.asyncCache,
    db = lila.db.Env.current
  )
}
