package lila.oauth

import org.joda.time.DateTime

import lila.user.User
import io.lemonlabs.uri.AbsoluteUrl

case class OAuthApp(
    name: String,
    clientId: OAuthApp.Id,
    clientSecret: OAuthApp.Secret,
    homepageUri: AbsoluteUrl,
    redirectUri: AbsoluteUrl,
    author: User.ID,
    createdAt: DateTime,
    description: Option[String] = None
)

object OAuthApp {

  case class Id(value: String)     extends AnyVal
  case class Secret(value: String) extends AnyVal

  def makeId     = Id(ornicar.scalalib.Random secureString 16)
  def makeSecret = Secret(ornicar.scalalib.Random secureString 32)

  object BSONFields {
    val clientId     = "client_id"
    val clientSecret = "client_secret"
    val name         = "name"
    val homepageUri  = "homepage_uri"
    val redirectUri  = "redirect_uri"
    val author       = "author"
    val createdAt    = "create_date"
    val description  = "description"
  }

  import reactivemongo.api.bson._
  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler

  implicit private[oauth] val AppIdHandler     = stringAnyValHandler[Id](_.value, Id.apply)
  implicit private[oauth] val AppSecretHandler = stringAnyValHandler[Secret](_.value, Secret.apply)

  implicit val AppBSONHandler = new BSON[OAuthApp] {

    import BSONFields._

    def reads(r: BSON.Reader): OAuthApp =
      OAuthApp(
        clientId = r.get[Id](clientId),
        clientSecret = r.get[Secret](clientSecret),
        name = r str name,
        homepageUri = r.get[AbsoluteUrl](homepageUri),
        redirectUri =
          r.get[List[AbsoluteUrl]](redirectUri).headOption err "Missing OAuthApp.redirectUri array",
        author = r str author,
        createdAt = r.get[DateTime](createdAt),
        description = r strO description
      )

    def writes(w: BSON.Writer, o: OAuthApp) =
      $doc(
        clientId     -> o.clientId,
        clientSecret -> o.clientSecret,
        name         -> o.name,
        homepageUri  -> o.homepageUri,
        redirectUri  -> $arr(o.redirectUri),
        author       -> o.author,
        createdAt    -> o.createdAt,
        description  -> o.description
      )
  }
}
