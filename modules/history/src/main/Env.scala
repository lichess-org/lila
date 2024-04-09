package lila.history

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.CollName
import lila.core.perf.PerfType
import lila.core.user.WithPerf
import lila.core.perf.PerfKey
import lila.core.data.Days

@Module
final class Env(
    mongoCache: lila.memo.MongoCache.Api,
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.AsyncDb @@ lila.db.YoloDb
)(using Executor, Scheduler, lila.core.i18n.Translator):

  private lazy val coll = db(CollName("history4")).failingSilently()

  lazy val api = wire[HistoryApi]

  lazy val ratingChartApi = wire[RatingChartApi]

  lila.common.Bus.subscribeFun("perfsUpdate"):
    case lila.game.actorApi.PerfsUpdate(game, bothPerfs) =>
      bothPerfs.mapList: (user, perfs) =>
        api.add(user, game, perfs)
