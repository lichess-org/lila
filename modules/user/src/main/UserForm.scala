package lila.user

import play.api.data.*
import play.api.data.validation.Constraints
import play.api.data.Forms.*

import User.ClearPassword
import lila.common.LameName
import lila.common.Form.{ cleanNonEmptyText, cleanText, trim, into, given }

final class UserForm(authenticator: Authenticator):

  def username(user: User): Form[UserName] =
    Form(
      single(
        "username" -> cleanNonEmptyText
          .into[UserName]
          .verifying(
            "changeUsernameNotSame",
            name => name.id == user.username.id && name != user.username
          )
          .verifying(
            "usernameUnacceptable",
            name => !LameName.hasTitle(name.value) || LameName.hasTitle(user.username.value)
          )
      )
    ).fill(user.username)

  def usernameOf(user: User) = username(user) fill user.username

  val profile = Form(
    mapping(
      "country"    -> optional(text.verifying(Countries.codeSet contains _)),
      "location"   -> optional(cleanNonEmptyText(maxLength = 80)),
      "bio"        -> optional(cleanNonEmptyText(maxLength = 400)),
      "firstName"  -> nameField,
      "lastName"   -> nameField,
      "fideRating" -> optional(number(min = 600, max = 3000)),
      "uscfRating" -> optional(number(min = 100, max = 3000)),
      "ecfRating"  -> optional(number(min = 0, max = 3000)),
      "rcfRating"  -> optional(number(min = 0, max = 3000)),
      "cfcRating"  -> optional(number(min = 0, max = 3000)),
      "dsbRating"  -> optional(number(min = 0, max = 3000)),
      "links"      -> optional(cleanNonEmptyText(maxLength = 3000))
    )(Profile.apply)(unapply)
  )

  def profileOf(user: User) = profile fill user.profileOrDefault

  private def nameField = optional(cleanText(minLength = 1, maxLength = 20))

  case class Passwd(
      oldPasswd: String,
      newPasswd1: String,
      newPasswd2: String
  ):
    def samePasswords = newPasswd1 == newPasswd2

  def passwd(u: User)(using ec: scala.concurrent.ExecutionContext) =
    authenticator loginCandidate u map { candidate =>
      Form(
        mapping(
          "oldPasswd"  -> nonEmptyText.verifying("incorrectPassword", p => candidate.check(ClearPassword(p))),
          "newPasswd1" -> text(minLength = 2),
          "newPasswd2" -> text(minLength = 2)
        )(Passwd.apply)(unapply)
          .verifying("newPasswordsDontMatch", _.samePasswords)
      )
    }

object UserForm:

  val note = Form(
    mapping(
      "text" -> cleanText(minLength = 3, maxLength = 2000),
      "mod"  -> boolean,
      "dox"  -> optional(boolean)
    )(NoteData.apply)(unapply)
  )

  case class NoteData(text: String, mod: Boolean, dox: Option[Boolean])

  val title = Form(single("title" -> optional(of[UserTitle])))

  lazy val historicalUsernameConstraints = Seq(
    Constraints minLength 2,
    Constraints maxLength 30,
    Constraints.pattern(regex = User.historicalUsernameRegex)
  )
  lazy val historicalUsernameField =
    trim(text).verifying(historicalUsernameConstraints*).into[UserStr]
