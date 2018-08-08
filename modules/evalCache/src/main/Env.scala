package lidraughts.evalCache

import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  private val CollectionEvalCache = config getString "collection.eval_cache"

  private lazy val truster = new EvalCacheTruster

  lazy val api = new EvalCacheApi(
    coll = db(CollectionEvalCache),
    truster = truster,
    asyncCache = asyncCache
  )

  lazy val socketHandler = new EvalCacheSocketHandler(
    api = api,
    truster = truster
  )

  def cli = new lidraughts.common.Cli {
    def process = {
      case "eval-cache" :: "drop" :: fenParts =>
        api.drop(draughts.variant.Standard, draughts.format.FEN(fenParts mkString " ")) inject "done!"
    }
  }
}

object Env {

  lazy val current: Env = "evalCache" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "evalCache",
    db = lidraughts.db.Env.current,
    asyncCache = lidraughts.memo.Env.current.asyncCache
  )
}
