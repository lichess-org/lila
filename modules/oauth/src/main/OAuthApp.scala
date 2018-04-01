package lila.oauth

import org.joda.time.DateTime

import lila.user.User

case class OAuthApp(
    name: String,
    clientId: OAuthApp.Id,
    clientSecret: OAuthApp.Secret,
    homepageUri: String,
    redirectUri: String,
    author: User.ID,
    createdAt: DateTime,
    scopes: List[OAuthScope],
    description: Option[String] = None
)

object OAuthApp {

  case class Id(value: String) extends AnyVal
  case class Secret(value: String) extends AnyVal

  def makeId = Id(ornicar.scalalib.Random secureString 16)
  def makeSecret = Secret(ornicar.scalalib.Random secureString 32)

  object BSONFields {
    val clientId = "client_id"
    val clientSecret = "client_secret"
    val name = "name"
    val homepageUri = "homepage_uri"
    val redirectUri = "redirect_uri"
    val author = "author"
    val createdAt = "create_date"
    val scopes = "scopes"
    val description = "description"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler
  import OAuthScope.scopeHandler

  private[oauth] implicit val AppIdHandler = stringAnyValHandler[Id](_.value, Id.apply)
  private[oauth] implicit val AppSecretHandler = stringAnyValHandler[Secret](_.value, Secret.apply)

  implicit val AppBSONHandler = new BSON[OAuthApp] {

    import BSONFields._

    def reads(r: BSON.Reader): OAuthApp = OAuthApp(
      clientId = r.get[Id](clientId),
      clientSecret = r.get[Secret](clientSecret),
      name = r str name,
      homepageUri = r str homepageUri,
      redirectUri = r str redirectUri,
      author = r str author,
      createdAt = r.get[DateTime](createdAt),
      scopes = r.get[List[OAuthScope]](scopes),
      description = r strO description
    )

    def writes(w: BSON.Writer, o: OAuthApp) = $doc(
      clientId -> o.clientId,
      clientSecret -> o.clientSecret,
      name -> o.name,
      homepageUri -> o.homepageUri,
      redirectUri -> o.redirectUri,
      author -> o.author,
      createdAt -> o.createdAt,
      scopes -> o.scopes,
      description -> o.description
    )
  }
}
