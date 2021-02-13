package lila.oauth

import org.joda.time.DateTime

import lila.user.User

case class AccessToken(
    id: AccessToken.Id,
    clientId: String,
    userId: User.ID,
    createdAt: Option[DateTime] = None, // for personal access tokens
    description: Option[String] = None, // for personal access tokens
    usedAt: Option[DateTime] = None,
    scopes: List[OAuthScope]
) {
  def isBrandNew = createdAt.exists(DateTime.now.minusSeconds(5).isBefore)
}

object AccessToken {

  val idSize = 16

  case class Id(value: String) extends AnyVal {
    def isPersonal = value.lengthIs == idSize
  }

  def makeId = Id(ornicar.scalalib.Random secureString idSize)

  case class ForAuth(userId: User.ID, scopes: List[OAuthScope])

  case class WithApp(token: AccessToken, app: OAuthApp)

  object BSONFields {
    val id          = "access_token_id"
    val clientId    = "client_id"
    val userId      = "user_id"
    val createdAt   = "create_date"
    val description = "description"
    val usedAt      = "used_at"
    val scopes      = "scopes"
  }

  import reactivemongo.api.bson._
  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler
  import OAuthScope.scopeHandler

  private[oauth] val forAuthProjection = $doc(
    BSONFields.userId -> true,
    BSONFields.scopes -> true
  )

  implicit private[oauth] val accessTokenIdHandler = stringAnyValHandler[Id](_.value, Id.apply)

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
        clientId = r str clientId,
        userId = r str userId,
        createdAt = r.getO[DateTime](createdAt),
        description = r strO description,
        usedAt = r.getO[DateTime](usedAt),
        scopes = r.get[List[OAuthScope]](scopes)
      )

    def writes(w: BSON.Writer, o: AccessToken) =
      $doc(
        id          -> o.id,
        clientId    -> o.clientId,
        userId      -> o.userId,
        createdAt   -> o.createdAt,
        description -> o.description,
        usedAt      -> o.usedAt,
        scopes      -> o.scopes
      )
  }
}
