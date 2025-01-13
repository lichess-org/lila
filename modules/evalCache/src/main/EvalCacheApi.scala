package lila.evalCache

import chess.format.Fen
import chess.variant.Variant
import play.api.libs.json.JsObject

import lila.core.chess.MultiPv
import lila.db.AsyncCollFailingSilently
import lila.db.dsl.{ *, given }
import lila.tree.CloudEval

final class EvalCacheApi(coll: AsyncCollFailingSilently, cacheApi: lila.memo.CacheApi)(using Executor):

  import BSONHandlers.given

  def getEvalJson(variant: Variant, fen: Fen.Full, multiPv: MultiPv): Fu[Option[JsObject]] =
    Id.from(variant, fen)
      .so: id =>
        getEval(id, multiPv)
          .map:
            _.map { JsonView.writeEval(_, fen) }
          .addEffect(monitorRequest(fen))

  val getSinglePvEval: CloudEval.GetSinglePvEval = sit => getEval(Id(sit), MultiPv(1))

  private def monitorRequest(fen: Fen.Full)(res: Option[Any]) =
    Fen
      .readPly(fen)
      .foreach: ply =>
        lila.mon.evalCache.request(ply.value, res.isDefined).increment()

  private[evalCache] def drop(variant: Variant, fen: Fen.Full): Funit =
    Id.from(variant, fen)
      .so: id =>
        coll(_.delete.one($id(id)).void)

  private def getEval(id: Id, multiPv: MultiPv): Fu[Option[CloudEval]] =
    cache.get(id).map(_.flatMap(_.makeBestMultiPvEval(multiPv)))

  private val cache = cacheApi[Id, Option[EvalCacheEntry]](16_384, "evalCache"):
    _.expireAfterWrite(5.minutes).buildAsyncFuture: id =>
      coll: c =>
        c.one[EvalCacheEntry]($id(id))
          .addEffect: res =>
            if res.isDefined then c.updateFieldUnchecked($id(id), "usedAt", nowInstant)
