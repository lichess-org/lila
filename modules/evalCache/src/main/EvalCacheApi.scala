package lila.evalCache

import org.joda.time.DateTime
import scala.concurrent.duration._

import chess.format.{ FEN, Uci }
import lila.db.dsl._
import lila.socket.Handler.Controller
import lila.user.User

final class EvalCacheApi(
    coll: Coll,
    truster: Truster) {

  import EvalCacheEntry._
  import BSONHandlers._

  def getEval(fen: String, multiPv: Int): Fu[Option[Eval]] = Id(fen, multiPv) ?? getEval

  def put(trustedUser: TrustedUser, candidate: Input.Candidate): Funit =
    candidate.input ?? { put(trustedUser, _) }

  def socketHandler(userOption: Option[User]): Controller = {
    userOption.fold(lila.socket.Handler.emptyController) { user =>
      val trust = truster(user)
      if (trust.isTooLow) lila.socket.Handler.emptyController
      else makeController(TrustedUser(trust, user))
    }
  }

  def shouldPut = truster shouldPut _

  private def makeController(trustedUser: TrustedUser): Controller = {
    case ("evalPut", o) => EvalCacheParser.parsePut(trustedUser.user, o) foreach { put(trustedUser, _) }
  }

  private def getEval(id: Id): Fu[Option[Eval]] = getEntry(id) map {
    _.flatMap(_.bestEval)
  }

  private def getEntry(id: Id): Fu[Option[EvalCacheEntry]] = coll.find($id(id)).one[EvalCacheEntry]

  private def put(trustedUser: TrustedUser, input: Input): Funit = {
    getEntry(input.id) map {
      _.fold(input entry trustedUser.trust)(_ add input.trusted(trustedUser.trust))
    } flatMap { entry =>
      coll.update($id(entry.id), entry, upsert = true).void
    }
  }
}
