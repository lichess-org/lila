package lila.ask

import com.softwaremill.macwire.*
import com.softwaremill.tagging.@@
import lila.core.config.*

@Module
final class Env(
    db: lila.db.AsyncDb @@ lila.db.YoloDb,
    // timeline: lila.hub.actors.Timeline,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):
  private lazy val askColl = db(CollName("ask"))
  lazy val repo            = wire[AskRepo]
  lazy val api             = wire[AskApi]
