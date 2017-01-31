package lila.evalCache

import scala.concurrent.duration._

import chess.format.{ FEN, Uci }
import lila.db.dsl._
import lila.socket.Handler.Controller
import lila.user.User

final class EvalCacheApi(coll: Coll) {

  import EvalCacheEntry._
  import BSONHandlers._

  def getEval(fen: String, multiPv: Int): Fu[Option[Eval]] = Id(fen, multiPv) ?? getEval

  def put(candidate: Input.Candidate): Funit = candidate.input ?? put

  def socketHandler(user: Option[User]): Controller =
    user.filter(canPut).fold(lila.socket.Handler.emptyController)(makeController)

  private def canPut(user: User) = true

  private def makeController(user: User): Controller = {
    case ("evalPut", o) => EvalCacheParser.parsePut(user, o.pp).pp foreach put
  }

  private def getEval(id: Id): Fu[Option[Eval]] = getEntry(id) map {
    _.flatMap(_.bestEval)
  }

  private def getEntry(id: Id): Fu[Option[EvalCacheEntry]] = coll.find($id(id)).one[EvalCacheEntry]

  private def put(input: Input): Funit =
    getEntry(input.id) map {
      _.fold(input entry Trust(1))(_ add input.eval)
    } flatMap { entry =>
      coll.update($id(entry.id), entry, upsert = true).void
    }
}
