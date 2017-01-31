package lila.evalCache

import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionEvalCache = config getString "collection.eval_cache"

  lazy val api = new EvalCacheApi(
    coll = db(CollectionEvalCache),
    truster = new EvalCacheTruster)
}

object Env {

  lazy val current: Env = "evalCache" boot new Env(
    config = lila.common.PlayApp loadConfig "evalCache",
    db = lila.db.Env.current)
}
