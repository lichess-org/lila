package lila.user

import play.api.data.*
import play.api.data.validation.Constraints
import play.api.data.Forms.*

import lila.common.LameName
import lila.common.Form.{ cleanNonEmptyText, cleanText, trim, into, given }

final class UserForm:

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

  val profile: Form[Profile] = Form(
    mapping(
      "flag"       -> optional(text.verifying(Flags.codeSet contains _)),
      "location"   -> optional(cleanNonEmptyText(maxLength = 80)),
      "bio"        -> optional(cleanNonEmptyText(maxLength = 400)),
      "firstName"  -> nameField,
      "lastName"   -> nameField,
      "fideRating" -> optional(number(min = 1000, max = 3000)),
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

object UserForm:

  val note = Form(
    mapping(
      "text"     -> cleanText(minLength = 3, maxLength = 2000),
      "noteType" -> text
    )((text, noteType) => NoteData(text, noteType == "mod" || noteType == "dox", noteType == "dox"))(_ =>
      none
    )
  )

  val apiNote = Form(
    mapping(
      "text" -> cleanText(minLength = 3, maxLength = 2000),
      "mod"  -> boolean,
      "dox"  -> default(boolean, false)
    )(NoteData.apply)(unapply)
  )

  case class NoteData(text: String, mod: Boolean, dox: Boolean)

  val title = Form(single("title" -> optional(of[UserTitle])))

  lazy val historicalUsernameConstraints = Seq(
    Constraints minLength 2,
    Constraints maxLength 30,
    Constraints.pattern(regex = User.historicalUsernameRegex)
  )
  lazy val historicalUsernameField =
    trim(text).verifying(historicalUsernameConstraints*).into[UserStr]
