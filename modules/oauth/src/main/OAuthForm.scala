package lila.oauth

import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import reactivemongo.api.bson.BSONObjectID

import lila.common.Form.absoluteUrl

object OAuthForm {

  private val scopesField = list(nonEmptyText.verifying(OAuthScope.byKey.contains _))

  object token {

    val form = Form(
      mapping(
        "description" -> text(minLength = 3, maxLength = 140),
        "scopes"      -> scopesField
      )(Data.apply)(Data.unapply)
    )

    def create = form

    case class Data(
        description: String,
        scopes: List[String]
    ) {
      def make(user: lila.user.User) =
        AccessToken(
          id = AccessToken.makeId,
          publicId = BSONObjectID.generate(),
          clientId = PersonalToken.clientId,
          userId = user.id,
          createdAt = DateTime.now.some,
          description = description.some,
          scopes = scopes.flatMap(OAuthScope.byKey.get),
          clientOrigin = None,
          expires = None,
        )
    }
  }

  object app {

    val form = Form(
      mapping(
        "name"        -> text(minLength = 3, maxLength = 90),
        "description" -> optional(nonEmptyText(maxLength = 400)),
        "homepageUri" -> absoluteUrl,
        "redirectUri" -> absoluteUrl
      )(Data.apply)(Data.unapply)
    )

    def create = form

    def edit(app: OAuthApp) = form fill Data.make(app)

    case class Data(
        name: String,
        description: Option[String],
        homepageUri: AbsoluteUrl,
        redirectUri: AbsoluteUrl
    ) {
      def make(user: lila.user.User) =
        OAuthApp(
          name = name,
          description = description,
          homepageUri = homepageUri,
          redirectUri = redirectUri,
          clientId = OAuthApp.makeId,
          clientSecret = OAuthApp.makeSecret,
          author = user.id,
          createdAt = DateTime.now
        )

      def update(app: OAuthApp) =
        app.copy(
          name = name,
          description = description,
          homepageUri = homepageUri,
          redirectUri = redirectUri
        )
    }

    object Data {

      def make(app: OAuthApp) =
        Data(
          name = app.name,
          description = app.description,
          homepageUri = app.homepageUri,
          redirectUri = app.redirectUri
        )
    }
  }
}
