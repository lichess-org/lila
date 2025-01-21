package lila.perfStat

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.*

@Module
final class Env(
    mongoCache: lila.memo.MongoCache.Api,
    lightUser: lila.core.LightUser.GetterSync,
    lightUserApi: lila.core.user.LightUserApi,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi,
    rankingRepo: lila.core.user.RankingRepo,
    rankingsOf: UserId => lila.rating.UserRankMap,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb
)(using Executor, Scheduler):

  private lazy val storage = PerfStatStorage:
    yoloDb(CollName("perf_stat")).failingSilently()

  lazy val indexer = wire[PerfStatIndexer]

  lazy val api = wire[PerfStatApi]

  lazy val jsonView = wire[JsonView]

  lila.common.Bus.subscribeFun("finishGame"):
    case lila.core.game.FinishGame(game, _) if !game.aborted =>
      indexer.addGame(game).addFailureEffect { e =>
        lila.log("perfStat").error(s"index game ${game.id}", e)
      }

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    storage.deleteAllFor(del.id)
