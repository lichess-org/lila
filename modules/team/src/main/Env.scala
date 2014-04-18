package lila.team

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(config: Config, hub: lila.hub.Env, db: lila.db.Env) {

  private val settings = new {
    val CollectionTeam = config getString "collection.team"
    val CollectionMember = config getString "collection.member"
    val CollectionRequest = config getString "collection.request"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val PaginatorMaxUserPerPage = config getInt "paginator.max_user_per_page"
  }
  import settings._

  lazy val forms = new DataForm(hub.actor.captcher)

  lazy val api = new TeamApi(
    cached = cached,
    notifier = notifier,
    forum = hub.actor.forum,
    indexer = hub.actor.teamIndexer,
    timeline = hub.actor.timeline)

  lazy val paginator = new PaginatorBuilder(
    maxPerPage = PaginatorMaxPerPage,
    maxUserPerPage = PaginatorMaxUserPerPage)

  lazy val cli = new Cli(api)

  lazy val cached = new Cached

  private[team] lazy val teamColl = db(CollectionTeam)
  private[team] lazy val requestColl = db(CollectionRequest)
  private[team] lazy val memberColl = db(CollectionMember)

  private lazy val notifier = new Notifier(
    messenger = hub.actor.messenger,
    router = hub.actor.router)
}

object Env {

  lazy val current = "[boot] team" describes new Env(
    config = lila.common.PlayApp loadConfig "team",
    hub = lila.hub.Env.current,
    db = lila.db.Env.current)
}
