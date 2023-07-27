package lila.evalCache

import chess.format.Fen
import chess.variant.Variant
import play.api.libs.json.JsObject

import lila.db.AsyncCollFailingSilently
import lila.db.dsl.{ *, given }

final class EvalCacheApi(coll: AsyncCollFailingSilently, cacheApi: lila.memo.CacheApi)(using Executor):

  import EvalCacheEntry.*
  import BSONHandlers.given

  def getEvalJson(variant: Variant, fen: Fen.Epd, multiPv: MultiPv): Fu[Option[JsObject]] =
    getEval(Id(variant, SmallFen.make(variant, fen.simple)), multiPv) map {
      _.map { JsonView.writeEval(_, fen) }
    } addEffect { res =>
      Fen
        .readPly(fen)
        .foreach: ply =>
          lila.mon.evalCache.request(ply.value, res.isDefined).increment()
    }

  def getSinglePvEval(variant: Variant, fen: Fen.Epd): Fu[Option[Eval]] =
    getEval(Id(variant, SmallFen.make(variant, fen.simple)), MultiPv(1))

  private[evalCache] def drop(variant: Variant, fen: Fen.Epd): Funit =
    val id = Id(variant, SmallFen.make(variant, fen.simple))
    coll(_.delete.one($id(id)).void)

  private def getEval(id: Id, multiPv: MultiPv): Fu[Option[Eval]] =
    cache.get(id).map(_.flatMap(_ makeBestMultiPvEval multiPv))

  private val cache = cacheApi[Id, Option[EvalCacheEntry]](32_768, "evalCache"):
    _.expireAfterWrite(5 minutes).buildAsyncFuture: id =>
      coll: c =>
        c.one[EvalCacheEntry]($id(id))
          .addEffect: res =>
            if res.isDefined then c.updateFieldUnchecked($id(id), "usedAt", nowInstant)
