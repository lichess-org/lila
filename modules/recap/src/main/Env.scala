package lila.recap

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.CollName
import lila.db.dsl.Coll
import lila.memo.CacheApi

@Module
final class Env(db: lila.db.Db, cacheApi: CacheApi, settingStore: lila.memo.SettingStore.Builder)(using
    Executor,
    akka.stream.Materializer
)(using mode: play.api.Mode, scheduler: Scheduler):

  lazy val parallelismSetting = settingStore[Int](
    "recapParallelism",
    default = 5,
    text = "Number of yearly recaps to build in parallel".some
  ).taggedWith[Parallelism]

  private val colls = RecapColls(db(CollName("recap_report")), db(CollName("recap_queue")))

trait Parallelism
final private class RecapColls(val recap: Coll, val queue: Coll)
