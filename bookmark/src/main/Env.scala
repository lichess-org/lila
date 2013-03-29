package lila.bookmark

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionBookmark = config getString "collection.bookmark"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"

  private[bookmark] lazy val bookmarkColl = db(CollectionBookmark)

  private lazy val cached = new Cached

  lazy val paginator = new PaginatorBuilder(
    maxPerPage = PaginatorMaxPerPage)

  lazy val api = new BookmarkApi(
    cached = cached,
    paginator = paginator)
}

object Env {

  lazy val current = "[bookmark] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "bookmark",
    db = lila.db.Env.current)
}
