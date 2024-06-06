package lila.title

import reactivemongo.api.bson.Macros.Annotations.Key
import chess.{ PlayerTitle, FideId }
import scalalib.ThreadLocalRandom

import lila.core.id.ImageId
import java.time.Instant
import io.mola.galimatias.URL

case class TitleRequest(
    @Key("_id") id: String,
    userId: UserId,
    data: TitleRequest.FormData,
    status: TitleRequest.Status,
    createdAt: Instant
)

object TitleRequest:

  case class FormData(
      realName: String,
      title: PlayerTitle,
      fideId: Option[FideId],
      nationalFederationUrl: Option[URL],
      idDocument: ImageId,
      selfie: ImageId,
      public: Boolean,
      coach: Boolean,
      comment: Option[String]
  )
  enum Status:
    case Pending
    case Approved
    case Feedback(val text: String)
    case Rejected

  def make(
      userId: UserId,
      data: FormData
  ): TitleRequest =
    TitleRequest(
      id = ThreadLocalRandom.nextString(6),
      userId = userId,
      data = data,
      status = Status.Pending,
      createdAt = Instant.now
    )
