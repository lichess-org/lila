package lila.identity

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.db.dsl.{ *, given }

/** Maps a canonical SSO `sub` to an existing lila account whose `_id` predates the SSO migration (legacy
  * username-like id).
  */
case class IdentityAlias(@Key("_id") subject: String, userId: UserId)

object IdentityAlias:
  given reactivemongo.api.bson.BSONDocumentHandler[IdentityAlias] =
    reactivemongo.api.bson.Macros.handler

final class IdentityAliasRepo(coll: Coll)(using Executor):

  import IdentityAlias.given

  def userId(subject: String): Fu[Option[UserId]] =
    coll.primitiveOne[UserId]($id(subject), "userId")
