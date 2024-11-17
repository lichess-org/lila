package lila.recap

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.CollName
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

@Module
final class Env(
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    cacheApi: CacheApi,
    settingStore: lila.memo.SettingStore.Builder
)(using
    Executor,
    akka.stream.Materializer
)(using mode: play.api.Mode, scheduler: Scheduler):

  lazy val parallelismSetting = settingStore[Int](
    "recapParallelism",
    default = 5,
    text = "Number of yearly recaps to build in parallel".some
  ).taggedWith[Parallelism]

  private val colls = RecapColls(db(CollName("recap_report")), db(CollName("recap_queue")))

  private val builder = wire[RecapBuilder]

  private val queue = lila.memo.ParallelMongoQueue[UserId](
    coll = colls.queue,
    parallelism = () => parallelismSetting.get(),
    computationTimeout = 15.seconds,
    name = "recap"
  )(builder.compute)

  lazy val api = wire[RecapApi]

trait Parallelism
final private class RecapColls(val recap: Coll, val queue: Coll)
