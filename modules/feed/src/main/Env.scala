package lila.feed

import com.softwaremill.macwire.*

import lila.core.config.{ CollName, NetDomain, RouteUrl }
import lila.core.lilaism.Lilaism.*

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db,
    flairApi: lila.core.user.FlairApi,
    routeUrl: RouteUrl,
    elastic: lila.search.SearchClient
)(using Executor, Scheduler, NetDomain):

  private val feedColl = db(CollName("daily_feed"))

  val markdown = wire[FeedMarkdown]

  private val jsonView = wire[FeedJsonView]

  val api = wire[FeedApi]

  val paginator = wire[FeedPaginatorBuilder]

  export api.lastUpdates
