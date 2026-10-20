package lila.identity

import lila.db.dsl.{ *, given }

/** Idempotency log: an `eventId` is processed at most once. */
final class IdentityEventRepo(coll: Coll)(using Executor):

  import BsonHandlers.given

  /** Returns true when the event is new and must be applied. */
  def register(event: IdentityEvent): Fu[Boolean] =
    coll.insert
      .one(event)
      .map(_ => true)
      .recover(lila.db.recoverDuplicateKey(_ => false))

  def exists(eventId: String): Fu[Boolean] =
    coll.exists($id(eventId))
