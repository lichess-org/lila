package lila.oauth

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.cleanText

object OAuthTokenForm:

  private val scopesField = list(nonEmptyText.verifying(OAuthScope.byKey.contains))

  private val descriptionField = cleanText(minLength = 3, maxLength = 140)

  def create = Form(
    mapping(
      "description" -> descriptionField,
      "scopes"      -> scopesField
    )(Data.apply)(unapply)
  )

  case class Data(description: String, scopes: List[String])

  def adminChallengeTokens(max: Int = 1000) = Form(
    mapping(
      "description" -> descriptionField,
      "users"       -> cleanText.verifying(s"No more than $max users", _.split(',').sizeIs <= max)
    )(AdminChallengeTokensData.apply)(unapply)
  )

  case class AdminChallengeTokensData(description: String, usersStr: String):

    def usernames = usersStr.split(',').flatMap(UserStr.read).distinct.filter(_.couldBeUsername).toList
