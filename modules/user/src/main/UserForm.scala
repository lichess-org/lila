package lila.user

import play.api.data._
import play.api.data.validation.Constraints
import play.api.data.Forms._

import User.ClearPassword
import lila.common.Form.{ cleanNonEmptyText, cleanText }

final class UserForm(authenticator: Authenticator) {

  val note = Form(
    mapping(
      "text" -> cleanText(minLength = 3, maxLength = 2000),
      "mod"  -> boolean,
      "dox"  -> optional(boolean)
    )(NoteData.apply)(NoteData.unapply)
  )

  case class NoteData(text: String, mod: Boolean, dox: Option[Boolean])

  def username(user: User): Form[String] =
    Form(
      single(
        "username" -> cleanNonEmptyText.verifying(
          "changeUsernameNotSame",
          name => name.toLowerCase == user.username.toLowerCase && name != user.username
        )
      )
    ).fill(user.username)

  def usernameOf(user: User) = username(user) fill user.username

  val profile = Form(
    mapping(
      "country"    -> optional(text.verifying(Countries.codeSet contains _)),
      "location"   -> optional(cleanNonEmptyText(maxLength = 80)),
      "bio"        -> optional(cleanNonEmptyText(maxLength = 600)),
      "firstName"  -> nameField,
      "lastName"   -> nameField,
      "fideRating" -> optional(number(min = 600, max = 3000)),
      "uscfRating" -> optional(number(min = 100, max = 3000)),
      "ecfRating"  -> optional(number(min = 0, max = 3000)),
      "links"      -> optional(cleanNonEmptyText(maxLength = 3000))
    )(Profile.apply)(Profile.unapply)
  )

  def profileOf(user: User) = profile fill user.profileOrDefault

  private def nameField = optional(cleanText(minLength = 2, maxLength = 20))

  case class Passwd(
      oldPasswd: String,
      newPasswd1: String,
      newPasswd2: String
  ) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  def passwd(u: User)(implicit ec: scala.concurrent.ExecutionContext) =
    authenticator loginCandidate u map { candidate =>
      Form(
        mapping(
          "oldPasswd"  -> nonEmptyText.verifying("incorrectPassword", p => candidate.check(ClearPassword(p))),
          "newPasswd1" -> text(minLength = 2),
          "newPasswd2" -> text(minLength = 2)
        )(Passwd.apply)(Passwd.unapply).verifying("newPasswordsDontMatch", _.samePasswords)
      )
    }
}

object UserForm {

  val title = Form(single("title" -> optional(nonEmptyText)))

  lazy val historicalUsernameConstraints = Seq(
    Constraints minLength 2,
    Constraints maxLength 30,
    Constraints.pattern(regex = User.historicalUsernameRegex)
  )
  lazy val historicalUsernameField = text.verifying(historicalUsernameConstraints: _*)
}
