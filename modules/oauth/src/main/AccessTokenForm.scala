package lila.oauth

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

object AccessTokenForm {

  val form = Form(mapping(
    "description" -> nonEmptyText(minLength = 3, maxLength = 140)
  )(Data.apply)(Data.unapply))

  def create = form

  case class Data(
      description: String
  ) {
    def make(user: lila.user.User) = AccessToken(
      id = AccessToken.makeId,
      clientId = PersonalToken.clientId,
      userId = user.id,
      expiresAt = DateTime.now plusYears 100,
      createdAt = DateTime.now.some,
      description = description.some,
      scopes = Nil
    )
  }
}
