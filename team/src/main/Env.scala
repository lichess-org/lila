package lila.team

import akka.actor.ActorRef
import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    captcher: ActorRef,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionTeam = config getString "collection.team"
    val CollectionMember = config getString "collection.member"
    val CollectionRequest = config getString "collection.request"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val PaginatorMaxUserPerPage = config getInt "paginator.max_user_per_page"
    val CacheCapacity = config getInt "cache.capacity"
  }
  import settings._

  lazy val forms = new DataForm(captcher)

  private[team] lazy val teamColl = db(CollectionTeam)
  private[team] lazy val requestColl = db(CollectionRequest)
  private[team] lazy val memberColl = db(CollectionMember)

  private[team] lazy val cached = new Cached(CacheCapacity)
}

object Env {

  lazy val current = "[bookmark] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "bookmark",
    captcher = lila.hub.Env.current.actor.captcher,
    db = lila.db.Env.current)
}
