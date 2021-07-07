package lila.oauth

import org.joda.time.DateTime
import reactivemongo.api.bson._
import com.roundeights.hasher.Algo

import lila.common.{ Bearer, SecureRandom }
import lila.user.User

case class AccessToken(
    id: AccessToken.Id,
    plain: Bearer,
    userId: User.ID,
    createdAt: Option[DateTime],
    description: Option[String], // for personal access tokens
    usedAt: Option[DateTime] = None,
    scopes: List[OAuthScope],
    clientOrigin: Option[String],
    expires: Option[DateTime]
) {
  def isBrandNew = createdAt.exists(DateTime.now.minusSeconds(5).isBefore)
}

object AccessToken {

  case class Id(value: String) extends AnyVal
  object Id {
    def from(bearer: Bearer) = Id(Algo.sha256(bearer.secret).hex)
  }

  case class ForAuth(userId: User.ID, scopes: List[OAuthScope])

  object BSONFields {
    val id           = "_id"
    val plain        = "plain"
    val userId       = "userId"
    val createdAt    = "created"
    val description  = "description"
    val usedAt       = "used"
    val scopes       = "scopes"
    val clientOrigin = "clientOrigin"
    val expires      = "expires"
  }

  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler
  import OAuthScope.scopeHandler

  private[oauth] val forAuthProjection = $doc(
    BSONFields.userId -> true,
    BSONFields.scopes -> true
  )

  implicit private[oauth] val idHandler     = stringAnyValHandler[Id](_.value, Id.apply)
  implicit private[oauth] val bearerHandler = stringAnyValHandler[Bearer](_.secret, Bearer.apply)

  implicit val ForAuthBSONReader = new BSONDocumentReader[ForAuth] {
    def readDocument(doc: BSONDocument) =
      for {
        userId <- doc.getAsTry[User.ID](BSONFields.userId)
        scopes <- doc.getAsTry[List[OAuthScope]](BSONFields.scopes)
      } yield ForAuth(userId, scopes)
  }

  implicit val AccessTokenBSONHandler = new BSON[AccessToken] {

    import BSONFields._

    def reads(r: BSON.Reader): AccessToken =
      AccessToken(
        id = r.get[Id](id),
        plain = r.get[Bearer](plain),
        userId = r str userId,
        createdAt = r.getO[DateTime](createdAt),
        description = r strO description,
        usedAt = r.getO[DateTime](usedAt),
        scopes = r.get[List[OAuthScope]](scopes),
        clientOrigin = r strO clientOrigin,
        expires = r.getO[DateTime](expires)
      )

    def writes(w: BSON.Writer, o: AccessToken) =
      $doc(
        id           -> o.id,
        plain        -> o.plain,
        userId       -> o.userId,
        createdAt    -> o.createdAt,
        description  -> o.description,
        usedAt       -> o.usedAt,
        scopes       -> o.scopes,
        clientOrigin -> o.clientOrigin,
        expires      -> o.expires
      )
  }
}
