package lila.evalCache

import scala.concurrent.duration._

import chess.format.{ FEN, Uci }
import lila.db.dsl._

final class EvalCacheApi(coll: Coll) {

  import EvalCacheEntry._
  import BSONHandlers._

  def get(fen: FEN): Fu[Option[EvalCacheEntry]] =
    coll.find($id(fen.value)).one[EvalCacheEntry]

  def put(fen: FEN, eval: Eval): Funit =
    get(fen) map {
      _.fold(EvalCacheEntry(fen, List(eval)))(_ add eval)
    } flatMap { entry =>
      coll.update($id(fen.value), entry, upsert = true).void
    }
}
