package lila.evalCache

import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder) {

  private val CollectionEvalCache = config getString "collection.eval_cache"

  private lazy val truster = new EvalCacheTruster

  lazy val api = new EvalCacheApi(
    coll = db(CollectionEvalCache),
    truster = truster,
    asyncCache = asyncCache)

  lazy val socketHandler = new EvalCacheSocketHandler(
    api = api,
    truster = truster)
}

object Env {

  lazy val current: Env = "evalCache" boot new Env(
    config = lila.common.PlayApp loadConfig "evalCache",
    db = lila.db.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache)
}
