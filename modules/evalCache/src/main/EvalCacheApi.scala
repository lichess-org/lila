package lila.evalCache

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import chess.format.{ FEN, Uci }
import lila.db.dsl._
import lila.socket.Handler.Controller
import lila.user.User

final class EvalCacheApi(
    coll: Coll,
    truster: EvalCacheTruster) {

  import EvalCacheEntry._
  import BSONHandlers._

  def getEvalJson(fen: FEN, multiPv: Int): Fu[Option[JsObject]] = getEval(
    fen = lila.evalCache.EvalCacheEntry.SmallFen make fen,
    multiPv = multiPv
  ) map {
    _.map { lila.evalCache.JsonHandlers.writeEval(_, fen) }
  }

  def put(trustedUser: TrustedUser, candidate: Input.Candidate): Funit =
    candidate.input ?? { put(trustedUser, _) }

  def shouldPut = truster shouldPut _

  private def getEval(fen: SmallFen, multiPv: Int): Fu[Option[Eval]] = getEntry(fen) map {
    _.flatMap(_ bestMultiPvEval multiPv)
  }

  private def getEntry(fen: SmallFen): Fu[Option[EvalCacheEntry]] = coll.find($id(fen)).one[EvalCacheEntry]

  private def put(trustedUser: TrustedUser, input: Input): Funit = getEntry(input.fen) map {
    case None => coll.insert(input entry trustedUser.trust) recover lila.db.recoverDuplicateKey(_ => ()) void
    case Some(oldEntry) =>
      val entry = oldEntry add input.trusted(trustedUser.trust)
      !entry.similarTo(oldEntry) ?? coll.update($id(entry.fen), entry, upsert = true).void
  }

}
