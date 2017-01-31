package lila.evalCache

import org.joda.time.DateTime
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

  def getEval(fen: SmallFen, multiPv: Int): Fu[Option[Eval]] = getEntry(fen) map {
    _.flatMap(_ bestMultiPvEval multiPv)
  }

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
    case ("evalPut", o) => JsonHandlers.readPut(trustedUser.user, o) foreach { put(trustedUser, _) }
  }

  private def getEntry(fen: SmallFen): Fu[Option[EvalCacheEntry]] = coll.find($id(fen)).one[EvalCacheEntry]

  private def put(trustedUser: TrustedUser, input: Input): Funit = {
    getEntry(input.fen) map {
      _.fold(input entry trustedUser.trust)(_ add input.trusted(trustedUser.trust))
    } flatMap { entry =>
      coll.update($id(entry.fen), entry, upsert = true).void
    }
  }
}
