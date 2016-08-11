package lila.team

import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.notify.NotifyApi

final class Env(config: Config, hub: lila.hub.Env, notifyApi: NotifyApi, db: lila.db.Env) {

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
    member = db(CollectionMember))

  lazy val forms = new DataForm(colls.team, hub.actor.captcher)

  lazy val pager = new MemberPager(colls.member)

  lazy val api = new TeamApi(
    coll = colls,
    cached = cached,
    notifier = notifier,
    forum = hub.actor.forum,
    indexer = hub.actor.teamSearch,
    timeline = hub.actor.timeline)

  lazy val paginator = new PaginatorBuilder(
    coll = colls,
    maxPerPage = PaginatorMaxPerPage,
    maxUserPerPage = PaginatorMaxUserPerPage)

  lazy val cli = new Cli(api, colls)

  lazy val cached = new Cached

  private lazy val notifier = new Notifier(notifyApi = notifyApi)
}

object Env {

  lazy val current = "team" boot new Env(
    config = lila.common.PlayApp loadConfig "team",
    hub = lila.hub.Env.current,
    notifyApi = lila.notify.Env.current.api,
    db = lila.db.Env.current)
}
