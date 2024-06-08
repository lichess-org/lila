package lila.title

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.core.perm.Granter
import lila.core.id.TitleRequestId
import lila.memo.PicfitApi

final class TitleApi(coll: Coll, picfitApi: PicfitApi)(using Executor):

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
  private val statusField                         = "history.0.status"

  def getCurrent(using me: Me): Fu[Option[TitleRequest]] =
    coll.one[TitleRequest]($doc("userId" -> me.userId))

  def getForMe(id: TitleRequestId)(using me: Me): Fu[Option[TitleRequest]] =
    coll
      .byId[TitleRequest](id)
      .map:
        _.filter(_.userId.is(me) || Granter(_.SetTitle))

  def create(data: FormData)(using me: Me): Fu[TitleRequest] =
    val req = TitleRequest.make(me.userId, data)
    coll.insert.one(req).inject(req)

  def update(req: TitleRequest, data: FormData)(using me: Me): Fu[TitleRequest] =
    val newReq = req.update(data)
    coll.update.one($id(req.id), newReq).inject(newReq)

  def delete(req: TitleRequest): Funit =
    coll.delete.one($id(req.id)).void

  def countPending: Fu[Int] =
    coll.countSel($doc(s"$statusField.n" -> Status.pending.toString))

  def queue: Fu[List[TitleRequest]] =
    coll
      .find($doc(s"$statusField.n" -> Status.pending.toString))
      .sort($sort.asc(s"$statusField.at"))
      .cursor[TitleRequest]()
      .list(30)

  object image:
    def rel(req: TitleRequest, tag: String) =
      s"title-request.$tag:${req.id}"

    def upload(req: TitleRequest, picture: PicfitApi.FilePart, tag: String)(using me: Me): Fu[TitleRequest] =
      if !Set("idDocument", "selfie").contains(tag) then fufail(s"Invalid tag $tag")
      else
        for
          image <- picfitApi.uploadFile(rel(req, tag), picture, userId = me.userId)
          _     <- coll.updateField($id(req.id), tag, image.id)
        yield req.focusImage(tag).replace(image.id.some)

    def delete(req: TitleRequest, tag: String): Fu[TitleRequest] = for
      _ <- picfitApi.deleteByRel(rel(req, tag))
      _ <- coll.unsetField($id(req.id), tag)
    yield req.focusImage(tag).replace(none)
