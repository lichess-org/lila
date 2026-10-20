package lila.identity

import lila.db.dsl.{ *, given }

final class IdentityStatusRepo(coll: Coll)(using Executor):

  import BsonHandlers.given

  def get(id: UserId): Fu[Option[IdentityStatusRecord]] =
    coll.byId[IdentityStatusRecord](id)

  /** None means there is no projection yet; callers fall back to the ID token claim. */
  def globallyEnabled(id: UserId): Fu[Option[Boolean]] =
    coll.primitiveOne[Boolean]($id(id), "globallyEnabled")

  /** Apply an event only if its version is newer than the stored one. Returns true when the projection
    * changed.
    */
  def applyEvent(event: IdentityEvent): Fu[Boolean] =
    val record = IdentityStatusRecord(
      id = event.subject,
      globallyEnabled = event.globallyEnabled,
      status = event.status,
      version = event.version,
      occurredAt = event.occurredAt,
      reasonCode = event.reasonCode,
      updatedAt = nowInstant
    )
    coll.update
      .one(
        $id(event.subject) ++ $doc("version".$lt(event.version)),
        $doc(
          "$set" -> $doc(
            "globallyEnabled" -> event.globallyEnabled,
            "status"          -> event.status.key,
            "version"         -> event.version,
            "occurredAt"      -> event.occurredAt,
            "reasonCode"      -> event.reasonCode,
            "updatedAt"       -> record.updatedAt
          )
        )
      )
      .flatMap {
        case res if res.nModified == 1 => fuccess(true)
        case _ =>
          coll.insert
            .one(record)
            .map(_ => true)
            .recover(lila.db.recoverDuplicateKey(_ => false))
      }
