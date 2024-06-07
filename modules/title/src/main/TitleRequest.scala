package lila.title

import reactivemongo.api.bson.Macros.Annotations.Key
import chess.{ PlayerTitle, FideId }
import scalalib.ThreadLocalRandom

import lila.core.id.ImageId
import io.mola.galimatias.URL

case class TitleRequest(
    @Key("_id") id: String,
    userId: UserId,
    data: TitleRequest.FormData,
    idDocument: Option[ImageId],
    selfie: Option[ImageId],
    history: NonEmptyList[TitleRequest.Status], // latest first
    createdAt: Instant
)

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

  def make(userId: UserId, data: FormData): TitleRequest =
    TitleRequest(
      id = ThreadLocalRandom.nextString(6),
      userId = userId,
      data = data,
      idDocument = none,
      selfie = none,
      history = NonEmptyList.one(Status.building),
      createdAt = nowInstant
    )
