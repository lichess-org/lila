package lidraughts.team

import com.typesafe.config.Config
import akka.actor._

import lidraughts.notify.NotifyApi
import lidraughts.common.MaxPerPage

final class Env(
    config: Config,
    hub: lidraughts.hub.Env,
    notifyApi: NotifyApi,
    system: ActorSystem,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    db: lidraughts.db.Env
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

  lazy val forms = new DataForm(colls.team, hub.actor.captcher)

  lazy val pager = new MemberPager(colls.member)

  lazy val api = new TeamApi(
    coll = colls,
    cached = cached,
    notifier = notifier,
    bus = system.lidraughtsBus,
    indexer = hub.actor.teamSearch,
    timeline = hub.actor.timeline
  )

  lazy val paginator = new PaginatorBuilder(
    coll = colls,
    maxPerPage = MaxPerPage(PaginatorMaxPerPage),
    maxUserPerPage = MaxPerPage(PaginatorMaxUserPerPage)
  )

  lazy val cli = new Cli(api, colls)

  lazy val cached = new Cached(asyncCache)(system)

  private lazy val notifier = new Notifier(notifyApi = notifyApi)

  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.hub.actorApi.mod.Shadowban(userId, true) => api deleteRequestsByUserId userId
    }
  })), 'shadowban)
}

object Env {

  lazy val current = "team" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "team",
    hub = lidraughts.hub.Env.current,
    notifyApi = lidraughts.notify.Env.current.api,
    system = lidraughts.common.PlayApp.system,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    db = lidraughts.db.Env.current
  )
}
