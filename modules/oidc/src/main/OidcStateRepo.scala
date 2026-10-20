package lila.oidc

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.db.dsl.{ *, given }

/** Short-lived, single-use login state. Never trust the browser to carry the nonce. */
case class OidcLoginState(
    @Key("_id") state: String,
    nonce: String,
    codeVerifier: String,
    referrer: Option[String],
    createdAt: Instant,
    expiresAt: Instant
)

object OidcLoginState:
  given reactivemongo.api.bson.BSONDocumentHandler[OidcLoginState] =
    reactivemongo.api.bson.Macros.handler

final class OidcStateRepo(coll: Coll)(using Executor):

  import OidcLoginState.given

  def put(state: String, nonce: String, codeVerifier: String, referrer: Option[String]): Funit =
    coll.insert
      .one(
        OidcLoginState(
          state = state,
          nonce = nonce,
          codeVerifier = codeVerifier,
          referrer = referrer,
          createdAt = nowInstant,
          expiresAt = nowInstant.plusSeconds(600)
        )
      )
      .void

  /** Atomically consume the state: a replay finds nothing. */
  def consume(state: String): Fu[Option[OidcLoginState]] =
    coll
      .findAndModify($doc("_id" -> state), coll.removeModifier)
      .map(_.value.flatMap(_.asOpt[OidcLoginState]))
