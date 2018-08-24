package lila.insight

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    getPref: String => Fu[lila.pref.Pref],
    areFriends: (String, String) => Fu[Boolean],
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val settings = new {
    val CollectionEntry = config getString "collection.entry"
    val CollectionUserCache = config getString "collection.user_cache"
  }
  import settings._

  private val db = new lila.db.Env("insight", config getConfig "mongodb", lifecycle)

  lazy val share = new Share(getPref, areFriends)

  lazy val jsonView = new JsonView

  private lazy val storage = new Storage(coll = db(CollectionEntry))

  private lazy val aggregationPipeline = new AggregationPipeline

  private lazy val indexer = new Indexer(
    storage = storage,
    sequencer = new lila.hub.FutureSequencer(
      system = system,
      executionTimeout = None,
      logger = logger
    )
  )

  private lazy val userCacheApi = new UserCacheApi(coll = db(CollectionUserCache))

  lazy val api = new InsightApi(
    storage = storage,
    userCacheApi = userCacheApi,
    pipeline = aggregationPipeline,
    indexer = indexer
  )

  system.lilaBus.subscribeFun('analysisReady) {
    case lila.analyse.actorApi.AnalysisReady(game, _) => api updateGame game
  }
}

object Env {

  lazy val current: Env = "insight" boot new Env(
    config = lila.common.PlayApp loadConfig "insight",
    getPref = lila.pref.Env.current.api.getPrefById,
    areFriends = lila.relation.Env.current.api.fetchAreFriends,
    system = lila.common.PlayApp.system,
    lifecycle = lila.common.PlayApp.lifecycle
  )
}
