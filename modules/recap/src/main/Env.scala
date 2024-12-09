package lila.recap

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.CollName
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    puzzleColls: lila.puzzle.PuzzleColls,
    cacheApi: CacheApi,
    lightUserApi: lila.core.user.LightUserApi,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor, Scheduler, akka.stream.Materializer):

  lazy val parallelismSetting = settingStore[Int](
    "recapParallelism",
    default = 8,
    text = "Number of yearly recaps to build in parallel".some
  ).taggedWith[Parallelism]

  private val colls = RecapColls(db(CollName("recap_report")), db(CollName("recap_queue")))

  private val json = wire[RecapJson]

  private val repo = wire[RecapRepo]

  private val builder = wire[RecapBuilder]

  private val queue = lila.memo.ParallelMongoQueue[UserId](
    coll = colls.queue,
    parallelism = () => parallelismSetting.get(),
    computationTimeout = 2.minutes,
    name = "recap"
  ): uid =>
    builder.compute(uid)

  lazy val api = wire[RecapApi]

trait Parallelism
final private class RecapColls(val recap: Coll, val queue: Coll)
