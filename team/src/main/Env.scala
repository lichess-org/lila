package lila.team

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionTeam = config getString "collection.team"
    val CollectionMember = config getString "collection.member"
    val CollectionRequest = config getString "collection.request"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val PaginatorMaxUserPerPage = config getInt "paginator.max_user_per_page"
  }
  import settings._

  private[team] lazy val teamColl = db(CollectionTeam)
}

object Env {

  lazy val current = "[bookmark] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "bookmark",
    db = lila.db.Env.current)
}
