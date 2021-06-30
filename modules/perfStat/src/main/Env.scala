package lila.perfStat

import akka.actor._
import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._

@Module
final class Env(
    appConfig: Configuration,
    lightUser: lila.common.LightUser.GetterSync,
    lightUserApi: lila.user.LightUserApi,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    rankingsOf: lila.user.RankingsOf,
    rankingApi: lila.user.RankingApi,
    db: lila.db.Db
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private lazy val storage = new PerfStatStorage(
    coll = db(appConfig.get[CollName]("perfStat.collection.perf_stat"))
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
