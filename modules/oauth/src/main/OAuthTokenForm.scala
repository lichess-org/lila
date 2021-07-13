package lila.oauth

import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import reactivemongo.api.bson.BSONObjectID

import lila.common.Bearer
import lila.common.Form.{ absoluteUrl, cleanText }
import lila.user.User

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

  def adminChallengeTokens = Form(
    mapping(
      "description" -> descriptionField,
      "users" -> cleanText
        .verifying("No more than 500 users", _.split(',').size <= 500)
    )(AdminChallengeTokensData.apply)(AdminChallengeTokensData.unapply _)
  )

  case class AdminChallengeTokensData(description: String, usersStr: String) {

    def usernames = usersStr.split(',').map(_.trim).distinct.filter(User.couldBeUsername).toList
  }
}
