package lila.evalCache

import scala.concurrent.duration._

import chess.format.{ FEN, Uci }
import lila.db.dsl._

final class EvalCacheApi(coll: Coll) {

  import EvalCacheEntry._
  import BSONHandlers._

  def getEval(fen: String, multiPv: Int): Fu[Option[Eval]] = Id(fen, multiPv) ?? getEval

  def put(candidate: Input.Candidate): Funit = candidate.input ?? put

  private def getEval(id: Id): Fu[Option[Eval]] = getEntry(id) map {
    _.flatMap(_.bestEval)
  }

  private def getEntry(id: Id): Fu[Option[EvalCacheEntry]] = coll.find($id(id)).one[EvalCacheEntry]

  private def put(input: Input): Funit =
    getEntry(input.id) map {
      _.fold(input.entry)(_ add input.eval)
    } flatMap { entry =>
      coll.update($id(entry.id), entry, upsert = true).void
    }
}
