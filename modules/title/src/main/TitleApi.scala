package lila.title

import reactivemongo.api.bson.*

import lila.core.config.BaseUrl
import lila.core.id.TitleRequestId
import lila.core.msg.SystemMsg
import lila.core.perm.Granter
import lila.db.dsl.{ *, given }
import lila.memo.PicfitApi

final class TitleApi(coll: Coll, picfitApi: PicfitApi, baseUrl: BaseUrl)(using Executor):

  import TitleRequest.*

  private given BSONDocumentHandler[FormData] = Macros.handler
  given lila.db.BSON[Status] with
    def reads(r: lila.db.BSON.Reader) =
      r.str("n") match
        case "pending"  => Status.pending(r.strD("t"))
        case "approved" => Status.approved
        case "rejected" => Status.rejected
        case "feedback" => Status.feedback(r.str("t"))
        case "imported" => Status.imported
        case _          => Status.building
    def writes(w: lila.db.BSON.Writer, s: Status) =
      s.textOpt match
        case Some(t) => $doc("n" -> s.name, "t" -> t)
        case None    => $doc("n" -> s.name)
  private given BSONDocumentHandler[StatusAt]     = Macros.handler
  private given BSONDocumentHandler[TitleRequest] = Macros.handler
  private val statusField                         = "history.0.status"
  private val updatedAtField                      = "history.0.at"

  def getCurrent(using me: Me): Fu[Option[TitleRequest]] =
    coll.find($doc("userId" -> me.userId)).sort($sort.desc(updatedAtField)).one[TitleRequest]

  def getForMe(id: TitleRequestId)(using me: Me): Fu[Option[TitleRequest]] =
    coll
      .byId[TitleRequest](id)
      .map:
        _.filter(_.userId.is(me) || Granter(_.Admin))

  def allOf(u: User): Fu[List[TitleRequest]] =
    coll.list[TitleRequest]($doc("userId" -> u.id))

  def findSimilar(req: TitleRequest): Fu[List[TitleRequest]] =
    val search = List(
      ("data.realName" -> BSONString(req.data.realName)).some,
      req.data.fideId.map(id => "data.fideId" -> BSONInteger(id.value))
    ).flatten.map: (k, v) =>
      $doc(k -> v)
    coll
      .find($or(search*) ++ $doc("userId".$ne(req.userId), statusField -> $nin(Status.building)))
      .sort($sort.desc(updatedAtField))
      .cursor[TitleRequest]()
      .list(30)

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

  def queue(nb: Int): Fu[List[TitleRequest]] =
    coll
      .find($doc(s"$statusField.n" -> Status.pending.toString))
      .sort($sort.asc(updatedAtField))
      .cursor[TitleRequest]()
      .list(nb)

  def process(req: TitleRequest, data: TitleForm.ProcessData)(using me: Me): Fu[TitleRequest] =
    val newReq = req.pushStatus(data.status)
    newReq.status match
      case Status.feedback(feedback) => sendFeedback(req.userId, feedback)
      case _                         =>
    coll.update.one($id(req.id), newReq).inject(newReq)

  def tryAgain(req: TitleRequest) =
    coll.update.one($id(req.id), req.tryAgain).void

  private def sendFeedback(to: UserId, feedback: String): Unit =
    val pm = s"""
Your title request has been reviewed by the Lichess team.
Here is the feedback provided:

$feedback

$baseUrl/verify-title
"""
    lila.common.Bus.publish(SystemMsg(to, pm), "msgSystemSend")

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

  private[title] def cleanupOldPics: Funit = for
    oldPics <- coll
      .find(
        $doc(
          updatedAtField -> $lt(nowInstant.minusMonths(1)),
          $or("idDocument".$exists(true), "selfie".$exists(true))
        )
      )
      .sort($sort.asc(updatedAtField))
      .cursor[TitleRequest]()
      .list(20)
    _ <- oldPics.sequentiallyVoid(image.delete(_, "idDocument"))
    _ <- oldPics.sequentiallyVoid(image.delete(_, "selfie"))
  yield ()
