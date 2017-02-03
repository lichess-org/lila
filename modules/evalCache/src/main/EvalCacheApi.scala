package lila.evalCache

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import chess.format.{ FEN, Uci, Forsyth }
import lila.db.dsl._
import lila.socket.Handler.Controller
import lila.user.User

final class EvalCacheApi(
    coll: Coll,
    truster: EvalCacheTruster,
    asyncCache: lila.memo.AsyncCache.Builder) {

  import EvalCacheEntry._
  import BSONHandlers._

  def getEvalJson(fen: FEN, multiPv: Int): Fu[Option[JsObject]] = getEval(
    fen = lila.evalCache.EvalCacheEntry.SmallFen make fen,
    multiPv = multiPv
  ) map {
    _.map { lila.evalCache.JsonHandlers.writeEval(_, fen) }
  } addEffect { res =>
    Forsyth getPly fen.value foreach { ply =>
      lila.mon.evalCache.register(ply, res.isDefined)
    }
  }

  def put(trustedUser: TrustedUser, candidate: Input.Candidate): Funit =
    candidate.input ?? { put(trustedUser, _) }

  def shouldPut = truster shouldPut _

  private val cache = asyncCache.multi[SmallFen, Option[EvalCacheEntry]](
    name = "eval_cache",
    f = fetchAndSetAccess,
    expireAfter = _.ExpireAfterAccess(10 minutes))

  private def getEval(fen: SmallFen, multiPv: Int): Fu[Option[Eval]] = getEntry(fen) map {
    _.flatMap(_ makeBestMultiPvEval multiPv)
  }

  private def getEntry(fen: SmallFen): Fu[Option[EvalCacheEntry]] = cache get fen

  private def fetchAndSetAccess(fen: SmallFen): Fu[Option[EvalCacheEntry]] =
    coll.find($id(fen)).one[EvalCacheEntry] addEffect { res =>
      if (res.isDefined) coll.updateFieldUnchecked($id(fen), "usedAt", DateTime.now)
    }

  private def put(trustedUser: TrustedUser, input: Input): Funit = getEntry(input.smallFen) map {
    case None =>
      val entry = EvalCacheEntry(
        _id = input.smallFen,
        nbMoves = destSize(input.fen),
        evals = List(input.eval),
        usedAt = DateTime.now)
      coll.insert(entry).recover(lila.db.recoverDuplicateKey(_ => ())) >>-
        cache.put(input.smallFen, entry.some)
    case Some(oldEntry) =>
      val entry = oldEntry add input.eval
      !(entry similarTo oldEntry) ?? {
        coll.update($id(entry.fen), entry, upsert = true).void >>-
          cache.put(input.smallFen, entry.some)
      }

  }

  private def destSize(fen: FEN): Int =
    chess.Game(chess.variant.Standard.some, fen.value.some).situation.destinations.size
}
