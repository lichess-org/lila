package lila.perfStat

import akka.actor._
import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._

final class Env(
    appConfig: Configuration,
    lightUser: lila.common.LightUser.GetterSync,
    gameRepo: lila.game.GameRepo,
    db: lila.db.Db
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  lazy val storage = new PerfStatStorage(
    coll = db(appConfig.get[CollName]("perfStat.collection.perf_stat"))
  )

  lazy val indexer = wire[PerfStatIndexer]

  lazy val jsonView = wire[JsonView]

  def get(user: lila.user.User, perfType: lila.rating.PerfType): Fu[PerfStat] =
    storage.find(user.id, perfType) getOrElse indexer.userPerf(user, perfType)

  lila.common.Bus.subscribeFun("finishGame") {
    case lila.game.actorApi.FinishGame(game, _, _) if !game.aborted =>
      indexer addGame game addFailureEffect { e =>
        lila.log("perfStat").error(s"index game ${game.id}", e)
      } unit
  }
}
