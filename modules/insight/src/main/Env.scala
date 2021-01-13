package lila.insight

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    mongo: lila.db.Env
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  private lazy val db = mongo.asyncDb(
    "insight",
    appConfig.get[String]("insight.mongodb.uri")
  )

  lazy val share = wire[Share]

  lazy val jsonView = wire[JsonView]

  private lazy val storage = new Storage(db(CollName("insight")))

  private lazy val aggregationPipeline = wire[AggregationPipeline]

  private lazy val povToEntry = wire[PovToEntry]

  private lazy val indexer: InsightIndexer = wire[InsightIndexer]

  private lazy val insightUserApi = new InsightUserApi(db(CollName("insight_user_cache")))

  lazy val api = wire[InsightApi]

  lila.common.Bus.subscribeFun("analysisReady") { case lila.analyse.actorApi.AnalysisReady(game, _) =>
    api.updateGame(game).unit
  }
}
