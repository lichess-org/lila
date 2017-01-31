package lila.evalCache

import scala.concurrent.duration._

import chess.format.{ FEN, Uci }
import lila.db.dsl._

final class EvalCacheApi(coll: Coll) {

  import EvalCacheEntry._
  import BSONHandlers._

  def get(id: Id): Fu[Option[EvalCacheEntry]] = coll.find($id(id)).one[EvalCacheEntry]

  def put(input: Input): Funit =
    get(input.id) map {
      _.fold(input.entry)(_ add input.eval)
    } flatMap { entry =>
      coll.update($id(entry.id), entry, upsert = true).void
    }
}
