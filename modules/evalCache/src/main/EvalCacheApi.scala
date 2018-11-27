package lila.evalCache

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import chess.format.{ FEN, Forsyth }
import chess.variant.Variant
import lila.db.dsl._
import lila.socket.Socket

final class EvalCacheApi(
    coll: Coll,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import EvalCacheEntry._
  import BSONHandlers._

  def getEvalJson(variant: Variant, fen: FEN, multiPv: Int): Fu[Option[JsObject]] = getEval(
    id = Id(variant, SmallFen.make(variant, fen)),
    multiPv = multiPv
  ) map {
      _.map { JsonHandlers.writeEval(_, fen) }
    } addEffect { res =>
      Forsyth getPly fen.value foreach { ply =>
        lila.mon.evalCache.register(ply, res.isDefined)
      }
    }

  def put(trustedUser: TrustedUser, candidate: Input.Candidate, uid: Socket.Uid): Funit =
    candidate.input ?? { put(trustedUser, _, uid) }

  def shouldPut = truster shouldPut _

  def getSinglePvEval(variant: Variant, fen: FEN): Fu[Option[Eval]] = getEval(
    id = Id(variant, SmallFen.make(variant, fen)),
    multiPv = 1
  )

  private[evalCache] def drop(variant: Variant, fen: FEN): Funit = {
    val id = Id(chess.variant.Standard, SmallFen.make(variant, fen))
    coll.remove($id(id)).void >>- cache.put(id, none)
  }

  private val cache = asyncCache.multi[Id, Option[EvalCacheEntry]](
    name = "eval_cache",
    f = fetchAndSetAccess,
    expireAfter = _.ExpireAfterAccess(10 minutes)
  )

  private def getEval(id: Id, multiPv: Int): Fu[Option[Eval]] = getEntry(id) map {
    _.flatMap(_ makeBestMultiPvEval multiPv)
  }

  private def getEntry(id: Id): Fu[Option[EvalCacheEntry]] = cache get id

  private def fetchAndSetAccess(id: Id): Fu[Option[EvalCacheEntry]] =
    coll.find($id(id)).one[EvalCacheEntry] addEffect { res =>
      if (res.isDefined) coll.updateFieldUnchecked($id(id), "usedAt", DateTime.now)
    }

  private def put(trustedUser: TrustedUser, input: Input, uid: Socket.Uid): Funit = Validator(input) match {
    case Some(error) =>
      logger.info(s"Invalid from ${trustedUser.user.username} $error ${input.fen}")
      funit
    case None => getEntry(input.id) map {
      case None =>
        val entry = EvalCacheEntry(
          _id = input.id,
          nbMoves = destSize(input.fen),
          evals = List(input.eval),
          usedAt = DateTime.now
        )
        coll.insert(entry).recover(lila.db.recoverDuplicateKey(_ => ())) >>-
          cache.put(input.id, entry.some) >>-
          upgrade.onEval(input, uid)
      case Some(oldEntry) =>
        val entry = oldEntry add input.eval
        !(entry similarTo oldEntry) ?? {
          coll.update($id(entry.id), entry, upsert = true).void >>-
            cache.put(input.id, entry.some) >>-
            upgrade.onEval(input, uid)
        }

    }
  }

  private def destSize(fen: FEN): Int =
    chess.Game(chess.variant.Standard.some, fen.value.some).situation.destinations.size
}
