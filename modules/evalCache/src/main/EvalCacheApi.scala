package lidraughts.evalCache

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import draughts.format.{ FEN, Forsyth }
import draughts.variant.Variant
import lidraughts.db.dsl._
import lidraughts.socket.Socket

final class EvalCacheApi(
    coll: Coll,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade,
    asyncCache: lidraughts.memo.AsyncCache.Builder
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
        lidraughts.mon.evalCache.register(ply, res.isDefined)
      }
    }

  def put(trustedUser: TrustedUser, candidate: Input.Candidate, uid: Socket.Uid): Funit =
    candidate.input ?? { put(trustedUser.user.id, _, uid.some) }

  def put(trustedUserId: String, candidate: Input.Candidate, uid: Option[Socket.Uid]): Funit =
    candidate.input ?? { put(trustedUserId, _, uid) }

  def shouldPut = truster shouldPut _

  def getSinglePvEval(variant: Variant, fen: FEN, minNodes: Int = 0): Fu[Option[Eval]] = getEval(
    id = Id(variant, SmallFen.make(variant, fen)),
    multiPv = 1,
    minNodes = minNodes
  )

  private[evalCache] def drop(variant: Variant, fen: FEN): Funit = {
    val id = Id(draughts.variant.Standard, SmallFen.make(variant, fen))
    coll.remove($id(id)).void >>- cache.put(id, none)
  }

  private val cache = asyncCache.multi[Id, Option[EvalCacheEntry]](
    name = "eval_cache",
    f = fetchAndSetAccess,
    expireAfter = _.ExpireAfterAccess(10 minutes)
  )

  private def getEval(id: Id, multiPv: Int, minNodes: Int = 0): Fu[Option[Eval]] = getEntry(id) map {
    _.flatMap(_.makeBestMultiPvEval(multiPv, minNodes))
  }

  private def getEntry(id: Id): Fu[Option[EvalCacheEntry]] = cache get id

  private def fetchAndSetAccess(id: Id): Fu[Option[EvalCacheEntry]] =
    coll.find($id(id)).one[EvalCacheEntry] addEffect { res =>
      if (res.isDefined) coll.updateFieldUnchecked($id(id), "usedAt", DateTime.now)
    }

  private def put(trustedUserId: String, input: Input, uid: Option[Socket.Uid]): Funit = Validator(input) match {
    case Some(error) =>
      logger.info(s"Invalid ${input.id.variant.key} from $trustedUserId $error ${input.fen} ${input.eval}")
      funit
    case None => getEntry(input.id) map {
      case None =>
        val entry = EvalCacheEntry(
          _id = input.id,
          nbMoves = destSize(input.fen),
          evals = List(input.eval),
          usedAt = DateTime.now
        )
        coll.insert(entry).recover(lidraughts.db.recoverDuplicateKey(_ => ())) >>-
          cache.put(input.id, entry.some) >>-
          uid ?? { upgrade.onEval(input, _) }
      case Some(oldEntry) =>
        val entry = oldEntry add input.eval
        !(entry similarTo oldEntry) ?? {
          coll.update($id(entry.id), entry, upsert = true).void >>-
            cache.put(input.id, entry.some) >>-
            uid ?? { upgrade.onEval(input, _) }
        }

    }
  }

  private def destSize(fen: FEN): Int =
    draughts.DraughtsGame(draughts.variant.Standard.some, fen.value.some).situation.allDestinations.size
}
