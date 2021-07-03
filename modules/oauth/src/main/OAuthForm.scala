package lila.oauth

import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import reactivemongo.api.bson.BSONObjectID

import lila.common.Bearer
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
          id = Bearer.randomPersonal(),
          publicId = BSONObjectID.generate(),
          userId = user.id,
          createdAt = DateTime.now.some,
          description = description.some,
          scopes = scopes.flatMap(OAuthScope.byKey.get),
          clientOrigin = None,
          expires = None
        )
    }
  }

}
