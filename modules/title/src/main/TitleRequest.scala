package lila.title

import chess.{ FideId, PlayerTitle }
import io.mola.galimatias.URL
import monocle.syntax.all.*
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom

import lila.core.id.{ ImageId, TitleRequestId }

case class TitleRequest(
    @Key("_id") id: TitleRequestId,
    userId: UserId,
    data: TitleRequest.FormData,
    idDocument: Option[ImageId],
    selfie: Option[ImageId],
    history: NonEmptyList[TitleRequest.StatusAt], // newest first
    createdAt: Instant
):
  import TitleRequest.*

  def status = history.head.status

  def approved = status == Status.approved

  def isRejectedButCanTryAgain = status.is(_.rejected) &&
    history.head.at.isBefore(nowInstant.minus(14.days))

  def tryAgain =
    if isRejectedButCanTryAgain then pushStatus(Status.building)
    else this

  def pushStatus(s: TitleRequest.Status): TitleRequest = copy(
    history =
      if status != s then s.now :: history
      else history.copy(head = s.now)
  )

  def update(data: FormData): TitleRequest =
    copy(data = data).pushStatus:
      if !hasImages then Status.building
      else if status.is(_.building) || status.isFeedback then Status.pending(data.comment | "")
      else status

  def hasImages = idDocument.isDefined && selfie.isDefined

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
    case pending(val comment: String) // needs moderator review
    case approved
    case feedback(val text: String)
    case rejected
    case imported
    def name = this match
      case pending(_) => "pending"
      case feedback(_) => "feedback"
      case s => s.toString
    def now: StatusAt = StatusAt(this, nowInstant)
    def is(s: Status.type => Status) = this == s(Status)
    def isPending = name == "pending"
    def isFeedback = name == "feedback"
    def textOpt = this match
      case pending(t) => t.some
      case feedback(t) => t.some
      case _ => none

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
