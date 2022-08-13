package lila.perfStat

import com.softwaremill.macwire._
import com.softwaremill.tagging._
import play.api.Configuration

import lila.common.config._

@Module
final class Env(
    lightUser: lila.common.LightUser.GetterSync,
    lightUserApi: lila.user.LightUserApi,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    rankingsOf: lila.user.RankingsOf,
    rankingApi: lila.user.RankingApi,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
) {

  private lazy val storage = new PerfStatStorage(
    coll = yoloDb(CollName("perf_stat")).failingSilently()
  )

  lazy val indexer = wire[PerfStatIndexer]

  lazy val api = wire[PerfStatApi]

  lazy val jsonView = wire[JsonView]

  lila.common.Bus.subscribeFun("finishGame") {
    case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted =>
      indexer addGame game addFailureEffect { e =>
        lila.log("perfStat").error(s"index game ${game.id}", e)
      } unit
  }
}
