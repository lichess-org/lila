package lila.insight

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import reactivemongo.api.MongoConnection.ParsedURI
import scala.concurrent.duration.FiniteDuration

import lila.common.config._
import lila.db.DbConfig.uriLoader

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    mongo: lila.db.Env
)(implicit system: akka.actor.ActorSystem) {

  private lazy val db = mongo.asyncDb(
    "insight",
    appConfig.get[ParsedURI]("insight.mongodb.uri")
  )

  lazy val share = wire[Share]

  lazy val jsonView = wire[JsonView]

  private lazy val storage = new Storage(db(CollName("insight")))

  private lazy val aggregationPipeline = wire[AggregationPipeline]

  private def sequencer = new lila.hub.FutureSequencer(
    executionTimeout = None,
    logger = logger
  )

  private lazy val povToEntry = wire[PovToEntry]

  private lazy val indexer = wire[Indexer]

  private lazy val userCacheApi = new UserCacheApi(db(CollName("insight_user_cache")))

  lazy val api = wire[InsightApi]

  lila.common.Bus.subscribeFun("analysisReady") {
    case lila.analyse.actorApi.AnalysisReady(game, _) => api updateGame game
  }
}
