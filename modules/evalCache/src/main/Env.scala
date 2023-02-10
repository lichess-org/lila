package lila.evalCache

import chess.variant.Variant
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.common.config.CollName

@Module
final class Env(
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler, play.api.Mode):

  private lazy val coll = yoloDb(CollName("eval_cache")).failingSilently()

  lazy val api: EvalCacheApi = wire[EvalCacheApi]

  def cli = new lila.common.Cli:
    def process = { case "eval-cache" :: "drop" :: variantKey :: fenParts =>
      Variant(Variant.LilaKey(variantKey)).fold(fufail("Invalid variant")) { variant =>
        api.drop(variant, chess.format.Fen.Epd(fenParts mkString " ")) inject
          "Done, but the eval can stay in cache for up to 5 minutes"
      }
    }
