package lila.evalCache

import chess.format.FEN
import chess.variant.Variant
import org.joda.time.DateTime
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import lila.db.AsyncCollFailingSilently
import lila.db.dsl._
import lila.memo.CacheApi._
import lila.socket.Socket

final class EvalCacheApi(
    coll: AsyncCollFailingSilently,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import EvalCacheEntry._
  import BSONHandlers._

  def getEvalJson(variant: Variant, fen: FEN, multiPv: Int): Fu[Option[JsObject]] =
    getEval(
      id = Id(variant, SmallFen.make(variant, fen)),
      multiPv = multiPv
    ) map {
      _.map { JsonHandlers.writeEval(_, fen) }
    } addEffect { res =>
      fen.ply foreach { ply =>
        lila.mon.evalCache.request(ply, res.isDefined).increment()
      }
    }

  def put(trustedUser: TrustedUser, candidate: Input.Candidate, sri: Socket.Sri): Funit =
    candidate.input ?? { put(trustedUser, _, sri) }

  def shouldPut = truster shouldPut _

  def getSinglePvEval(variant: Variant, fen: FEN): Fu[Option[Eval]] =
    getEval(
      id = Id(variant, SmallFen.make(variant, fen)),
      multiPv = 1
    )

  private[evalCache] def drop(variant: Variant, fen: FEN): Funit = {
    val id = Id(variant, SmallFen.make(variant, fen))
    coll(_.delete.one($id(id)).void) >>- cache.invalidate(id)
  }

  private val cache = cacheApi[Id, Option[EvalCacheEntry]](65536, "evalCache") {
    _.expireAfterAccess(5 minutes)
      .buildAsyncFuture(fetchAndSetAccess)
  }

  private def getEval(id: Id, multiPv: Int): Fu[Option[Eval]] =
    getEntry(id) map {
      _.flatMap(_ makeBestMultiPvEval multiPv)
    }

  private def getEntry(id: Id): Fu[Option[EvalCacheEntry]] = cache get id

  private def fetchAndSetAccess(id: Id): Fu[Option[EvalCacheEntry]] = coll { c =>
    c.one[EvalCacheEntry]($id(id)) addEffect { res =>
      if (res.isDefined) c.updateFieldUnchecked($id(id), "usedAt", DateTime.now)
    }
  }

  private def put(trustedUser: TrustedUser, input: Input, sri: Socket.Sri): Funit = coll { c =>
    Validator(input) match {
      case Some(error) =>
        logger.info(s"Invalid from ${trustedUser.user.username} $error ${input.fen}")
        funit
      case None =>
        getEntry(input.id) flatMap {
          case None =>
            val entry = EvalCacheEntry(
              _id = input.id,
              nbMoves = destSize(input.fen),
              evals = List(input.eval),
              usedAt = DateTime.now,
              updatedAt = DateTime.now
            )
            c.insert.one(entry).recover(lila.db.ignoreDuplicateKey).void >>-
              cache.put(input.id, fuccess(entry.some)) >>-
              upgrade.onEval(input, sri)
          case Some(oldEntry) =>
            val entry = oldEntry add input.eval
            !(entry similarTo oldEntry) ?? {
              c.update.one($id(entry.id), entry, upsert = true).void >>-
                cache.put(input.id, fuccess(entry.some)) >>-
                upgrade.onEval(input, sri)
            }

        }
    }
  }

  private def destSize(fen: FEN): Int =
    chess.Game(chess.variant.Standard.some, fen.some).situation.moves.view.map(_._2.size).sum
}
