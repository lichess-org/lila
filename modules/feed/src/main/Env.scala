package lila.feed

import com.softwaremill.macwire.*

import lila.core.config.CollName
import lila.core.lilaism.Lilaism.*

@Module
final class Env(cacheApi: lila.memo.CacheApi, db: lila.db.Db, flairApi: lila.core.user.FlairApi)(using
    Executor,
    Scheduler
):

  private val feedColl = db(CollName("daily_feed"))
  val api = wire[FeedApi]
  val paginator = wire[FeedPaginatorBuilder]

  export api.lastUpdate
