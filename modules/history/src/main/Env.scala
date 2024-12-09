package lila.history

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.CollName

@Module
final class Env(
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.AsyncDb @@ lila.db.YoloDb
)(using Executor, Scheduler, lila.core.i18n.Translator):

  private lazy val coll = db(CollName("history4")).failingSilently()

  lazy val api = wire[HistoryApi]

  lazy val ratingChartApi = wire[RatingChartApi]

  lila.common.Bus.subscribeFun("perfsUpdate"):
    case lila.core.game.PerfsUpdate(game, bothPerfs) =>
      bothPerfs.mapList: uwp =>
        api.add(uwp.user, game, uwp.perfs)
