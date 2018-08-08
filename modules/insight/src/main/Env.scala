package lidraughts.insight

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    getPref: String => Fu[lidraughts.pref.Pref],
    areFriends: (String, String) => Fu[Boolean],
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val settings = new {
    val CollectionEntry = config getString "collection.entry"
    val CollectionUserCache = config getString "collection.user_cache"
  }
  import settings._

  private val db = new lidraughts.db.Env("insight", config getConfig "mongodb", lifecycle)

  lazy val share = new Share(getPref, areFriends)

  lazy val jsonView = new JsonView

  private lazy val storage = new Storage(coll = db(CollectionEntry))

  private lazy val aggregationPipeline = new AggregationPipeline

  private lazy val indexer = new Indexer(
    storage = storage,
    sequencer = system.actorOf(Props(
      classOf[lidraughts.hub.Sequencer],
      None, None, logger
    ))
  )

  private lazy val userCacheApi = new UserCacheApi(coll = db(CollectionUserCache))

  lazy val api = new InsightApi(
    storage = storage,
    userCacheApi = userCacheApi,
    pipeline = aggregationPipeline,
    indexer = indexer
  )

  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.analyse.actorApi.AnalysisReady(game, _) => api updateGame game
    }
  })), 'analysisReady)
}

object Env {

  lazy val current: Env = "insight" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "insight",
    getPref = lidraughts.pref.Env.current.api.getPrefById,
    areFriends = lidraughts.relation.Env.current.api.fetchAreFriends,
    system = lidraughts.common.PlayApp.system,
    lifecycle = lidraughts.common.PlayApp.lifecycle
  )
}
