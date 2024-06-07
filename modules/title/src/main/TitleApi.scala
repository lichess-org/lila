package lila.title

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.core.perm.Granter

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
        case _          => Status.building
    def writes(w: lila.db.BSON.Writer, s: Status) =
      s match
        case Status.feedback(t) => $doc("n" -> "feedback", "t" -> t)
        case status             => $doc("n" -> status.toString)
  private given BSONDocumentHandler[StatusAt]     = Macros.handler
  private given BSONDocumentHandler[TitleRequest] = Macros.handler

  def getForMe(id: String)(using me: Me): Fu[Option[TitleRequest]] =
    coll
      .byId[TitleRequest](id)
      .map:
        _.filter(_.userId.is(me) || Granter(_.SetTitle))

  def create(data: FormData)(using me: Me): Fu[TitleRequest] =
    val req = TitleRequest.make(me.userId, data)
    coll.insert.one(req).inject(req)

  def update(req: TitleRequest, data: FormData)(using me: Me): Funit =
    coll.update.one($id(req.id), TitleRequest.update(req, data)).void
