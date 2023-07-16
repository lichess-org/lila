package lila.perfStat

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.common.config.*

@Module
final class Env(
    lightUser: lila.common.LightUser.GetterSync,
    lightUserApi: lila.user.LightUserApi,
    gameRepo: lila.game.GameRepo,
    userApi: lila.user.UserApi,
    rankingsOf: lila.user.RankingsOf,
    rankingApi: lila.user.RankingApi,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb
)(using Executor, Scheduler):

  private lazy val storage = PerfStatStorage:
    yoloDb(CollName("perf_stat")).failingSilently()

  lazy val indexer = wire[PerfStatIndexer]

  lazy val api = wire[PerfStatApi]

  lazy val jsonView = wire[JsonView]

  lila.common.Bus.subscribeFun("finishGame"):
    case lila.game.actorApi.FinishGame(game, _) if !game.aborted =>
      indexer addGame game addFailureEffect { e =>
        lila.log("perfStat").error(s"index game ${game.id}", e)
      }
