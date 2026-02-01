package lila.oauth

import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import com.roundeights.hasher.Algo

import lila.core.net.{ Bearer, UserAgent }
import lila.core.misc.oauth.AccessTokenId

case class AccessToken(
    @Key("_id") id: AccessTokenId,
    plain: Bearer,
    userId: UserId,
    created: Option[Instant],
    description: Option[String], // for personal access tokens
    usedAt: Option[Instant] = None,
    scopes: TokenScopes,
    clientOrigin: Option[String],
    userAgent: Option[UserAgent],
    expires: Option[Instant]
):
  def isBrandNew = created.exists(nowInstant.minusSeconds(5).isBefore)

  def isDangerous = scopes.intersects(OAuthScope.dangerList)

object AccessToken:

  case class ForAuth(
      userId: UserId,
      scopes: TokenScopes,
      tokenId: AccessTokenId,
      clientOrigin: Option[String]
  )

  case class Create(token: AccessToken)

  object BSONFields:
    val id = "_id"
    val userId = "userId"
    val created = "created"
    val usedAt = "used"
    val scopes = "scopes"
    val clientOrigin = "clientOrigin"

  def idFrom(bearer: Bearer) = AccessTokenId(Algo.sha256(bearer.value).hex)

  import lila.db.dsl.{ *, given }
  import OAuthScope.given

  private[oauth] val forAuthProjection = $doc(
    BSONFields.userId -> true,
    BSONFields.scopes -> true,
    BSONFields.clientOrigin -> true
  )

  given BSONDocumentReader[ForAuth] = new:
    def readDocument(doc: BSONDocument) = for
      tokenId <- doc.getAsTry[AccessTokenId](BSONFields.id)
      userId <- doc.getAsTry[UserId](BSONFields.userId)
      scopes <- doc.getAsTry[TokenScopes](BSONFields.scopes)
      origin = doc.getAsOpt[String](BSONFields.clientOrigin)
    yield ForAuth(userId, scopes, tokenId, origin)

  given BSONDocumentHandler[AccessToken] = Macros.handler
