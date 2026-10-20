package lila.identity

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.userId.UserId

/** Global identity status as projected from signed IdP events. */
enum IdentityStatus:
  case Active, Banned, Suspended, Deleted

object IdentityStatus:
  val all = List(Active, Banned, Suspended, Deleted)

  def fromKey(key: String): Option[IdentityStatus] =
    all.find(_.key == key.trim.toLowerCase)

  extension (status: IdentityStatus)
    def key: String              = status.toString.toLowerCase
    def globallyEnabled: Boolean = status == IdentityStatus.Active

/** Projection document. `_id` is the canonical SSO `sub`. */
case class IdentityStatusRecord(
    @Key("_id") id: UserId,
    globallyEnabled: Boolean,
    status: IdentityStatus,
    version: Long,
    occurredAt: Instant,
    reasonCode: Option[String],
    updatedAt: Instant
)

/** Signed event delivered by the IdP. */
case class IdentityEvent(
    @Key("_id") eventId: String,
    issuer: String,
    subject: UserId,
    status: IdentityStatus,
    version: Long,
    occurredAt: Instant,
    reasonCode: Option[String]
):
  def globallyEnabled: Boolean = status.globallyEnabled
