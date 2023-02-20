package lila.oauth

import play.api.data._
import play.api.data.Forms._

import lila.common.Form.cleanText

object OAuthTokenForm {

  private val scopesField = list(nonEmptyText.verifying(OAuthScope.byKey.contains _))

  private val descriptionField = cleanText(minLength = 3, maxLength = 140)

  def create = Form(
    mapping(
      "description" -> descriptionField,
      "scopes"      -> scopesField
    )(Data.apply)(Data.unapply)
  )

  case class Data(description: String, scopes: List[String])
}