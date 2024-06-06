package lila.title

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class TitleApi(coll: Coll)(using Executor):

  import TitleRequest.*

  private given BSONDocumentHandler[FormData] = Macros.handler
  given lila.db.BSON[Status] with
    def reads(r: lila.db.BSON.Reader) =
      r.str("n") match
        case "pending"  => Status.pending
        case "approved" => Status.approved
        case "rejected" => Status.rejected
        case "feedback" => Status.feedback(r.str("t"))
    def writes(w: lila.db.BSON.Writer, s: Status) =
      s match
        case Status.feedback(t) => $doc("n" -> "feedback", "t" -> t)
        case status             => $doc("n" -> status.toString)
  private given BSONDocumentHandler[TitleRequest] = Macros.handler

  def create(userId: UserId, data: FormData): Fu[TitleRequest] =
    val req = TitleRequest.make(userId, data)
    coll.insert.one(req).inject(req)
