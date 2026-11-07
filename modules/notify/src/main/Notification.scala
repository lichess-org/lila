package lila.notify
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom
import scalalib.paginator.Paginator

import lila.core.notify.{ NotificationContent, UnreadCount }
import lila.notify.Notification.*

private[notify] case class Notification(
    @Key("_id") id: Notification.Id,
    notifies: UserId,
    content: NotificationContent,
    read: NotificationRead,
    createdAt: Instant,
    expiresAt: Option[Instant] = None
):
  def to = notifies

object Notification:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  opaque type NotificationRead = Boolean
  object NotificationRead extends YesNo[NotificationRead]

  case class AndUnread(pager: Paginator[Notification], unread: UnreadCount)

  def make[U: UserIdOf](
      to: U,
      content: NotificationContent,
      expiresIn: Option[FiniteDuration] = none
  ): Notification =
    Notification(
      id = ThreadLocalRandom.nextString(8),
      notifies = to.id,
      content = content,
      read = NotificationRead(false),
      createdAt = nowInstant,
      expiresAt = expiresIn.map(nowInstant.plus(_))
    )
