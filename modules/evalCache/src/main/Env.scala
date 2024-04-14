package lila.evalCache

import chess.variant.Variant
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.CollName

@Module
final class Env(
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler, play.api.Mode):

  private lazy val coll = yoloDb(CollName("eval_cache2")).failingSilently()

  lazy val api: EvalCacheApi = wire[EvalCacheApi]

  def getSinglePvEval: lila.tree.CloudEval.GetSinglePvEval = api.getSinglePvEval

  def cli = new lila.common.Cli:
    def process = { case "eval-cache" :: "drop" :: variantKey :: fenParts =>
      Variant(Variant.LilaKey(variantKey)).fold(fufail("Invalid variant")): variant =>
        api
          .drop(variant, chess.format.Fen.Full(fenParts.mkString(" ")))
          .inject("Done, but the eval can stay in cache for up to 5 minutes")
    }
