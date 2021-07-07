package lila.oauth

import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import reactivemongo.api.bson.BSONObjectID

import lila.common.Bearer
import lila.common.Form.absoluteUrl

object OAuthTokenForm {

  private val scopesField = list(nonEmptyText.verifying(OAuthScope.byKey.contains _))

  def create = Form(
    mapping(
      "description" -> text(minLength = 3, maxLength = 140),
      "scopes"      -> scopesField
    )(Data.apply)(Data.unapply)
  )

  case class Data(description: String, scopes: List[String])
}
