package lila.title

import reactivemongo.api.bson.Macros.Annotations.Key
import chess.{ PlayerTitle, FideId }
import scalalib.ThreadLocalRandom
import monocle.syntax.all.*

import lila.core.id.ImageId
import io.mola.galimatias.URL
import lila.core.id.TitleRequestId

case class TitleRequest(
    @Key("_id") id: TitleRequestId,
    userId: UserId,
    data: TitleRequest.FormData,
    idDocument: Option[ImageId],
    selfie: Option[ImageId],
    history: NonEmptyList[TitleRequest.StatusAt], // latest first
    createdAt: Instant
):

  def focusImage(tag: String) =
    if tag == "idDocument" then this.focus(_.idDocument)
    else if tag == "selfie" then this.focus(_.selfie)
    else sys.error(s"Invalid image tag $tag")

object TitleRequest:

  case class FormData(
      realName: String,
      title: PlayerTitle,
      fideId: Option[FideId],
      federationUrl: Option[URL],
      public: Boolean,
      coach: Boolean,
      comment: Option[String]
  )
  enum Status:
    case building // until idDocument and selfie are uploaded
    case pending
    case approved
    case feedback(val text: String)
    case rejected
    def now: StatusAt = StatusAt(this, nowInstant)

  case class StatusAt(status: Status, at: Instant)

  def make(userId: UserId, data: FormData): TitleRequest =
    TitleRequest(
      id = TitleRequestId(ThreadLocalRandom.nextString(6)),
      userId = userId,
      data = data,
      idDocument = none,
      selfie = none,
      history = NonEmptyList.one(Status.building.now),
      createdAt = nowInstant
    )

  def update(req: TitleRequest, data: FormData): TitleRequest =
    req.copy(
      data = data,
      history = NonEmptyList.one(Status.building.now)
    )
