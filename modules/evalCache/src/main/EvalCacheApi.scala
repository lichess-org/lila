package lila.evalCache

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import shogi.format.forsyth.Sfen
import shogi.variant.Variant
import lila.db.dsl._
import lila.memo.CacheApi._
import lila.socket.Socket

final class EvalCacheApi(
    coll: Coll,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import EvalCacheEntry._
  import BSONHandlers._

  def getEvalJson(variant: Variant, sfen: Sfen, multiPv: Int): Fu[Option[JsObject]] =
    getEval(
      id = Id(variant, SmallSfen.make(sfen)),
      multiPv = multiPv
    ) map {
      _.map { JsonHandlers.writeEval(_, sfen) }
    }

  def put(trustedUser: TrustedUser, candidate: Input.Candidate, sri: Socket.Sri): Funit =
    candidate.input ?? { put(trustedUser, _, sri) }

  def shouldPut = truster shouldPut _

  def getSinglePvEval(variant: Variant, sfen: Sfen): Fu[Option[Eval]] =
    getEval(
      id = Id(variant, SmallSfen.make(sfen)),
      multiPv = 1
    )

  private[evalCache] def drop(variant: Variant, sfen: Sfen): Funit = {
    val id = Id(variant, SmallSfen.make(sfen))
    coll.delete.one($id(id)).void >>- cache.invalidate(id)
  }

  private val cache = cacheApi[Id, Option[EvalCacheEntry]](1024, "evalCache") {
    _.expireAfterAccess(5 minutes)
      .buildAsyncFuture(fetchAndSetAccess)
  }

  private def getEval(id: Id, multiPv: Int): Fu[Option[Eval]] =
    getEntry(id) map {
      _.flatMap(_ makeBestMultiPvEval multiPv)
    }

  private def getEntry(id: Id): Fu[Option[EvalCacheEntry]] = cache get id

  private def fetchAndSetAccess(id: Id): Fu[Option[EvalCacheEntry]] =
    coll.ext.find($id(id)).one[EvalCacheEntry] addEffect { res =>
      if (res.isDefined) coll.updateFieldUnchecked($id(id), "usedAt", DateTime.now)
    }

  private def put(trustedUser: TrustedUser, input: Input, sri: Socket.Sri): Funit =
    Validator(input) match {
      case Some(error) =>
        logger.info(s"Invalid from ${trustedUser.user.username} $error ${input.sfen}")
        funit
      case None =>
        getEntry(input.id) flatMap {
          case None =>
            val entry = EvalCacheEntry(
              _id = input.id,
              nbMoves = destSize(input.id.variant, input.sfen),
              evals = List(input.eval),
              usedAt = DateTime.now
            )
            coll.insert.one(entry).void.recover(lila.db.ignoreDuplicateKey) >>-
              cache.put(input.id, fuccess(entry.some)) >>-
              upgrade.onEval(input, sri)
          case Some(oldEntry) =>
            val entry = oldEntry add input.eval
            !(entry similarTo oldEntry) ?? {
              coll.update.one($id(entry.id), entry, upsert = true).void >>-
                cache.put(input.id, fuccess(entry.some)) >>-
                upgrade.onEval(input, sri)
            }

        }
    }

  private def destSize(variant: shogi.variant.Variant, sfen: Sfen): Int =
    ~(sfen.toSituation(variant) map { sit =>
      sit.moveDestinations.view.map(_._2.size).sum +
        sit.dropDestinations.view.map(_._2.size).sum
    })
}
