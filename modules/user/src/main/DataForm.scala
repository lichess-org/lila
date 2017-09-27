package lila.user

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

object DataForm {

  val note = Form(mapping(
    "text" -> nonEmptyText(minLength = 3, maxLength = 2000),
    "mod" -> boolean
  )(NoteData.apply)(NoteData.unapply))

  case class NoteData(text: String, mod: Boolean)

  val profile = Form(mapping(
    "country" -> optional(nonEmptyText.verifying(Countries.codeSet contains _)),
    "location" -> optional(nonEmptyText(maxLength = 80)),
    "bio" -> optional(nonEmptyText(maxLength = 600)),
    "firstName" -> nameField,
    "lastName" -> nameField,
    "fideRating" -> optional(number(min = 600, max = 3000)),
    "uscfRating" -> optional(number(min = 600, max = 3000)),
    "ecfRating" -> optional(number(min = 0, max = 300)),
    "links" -> optional(nonEmptyText(maxLength = 3000))
  )(Profile.apply)(Profile.unapply))

  def profileOf(user: User) = profile fill user.profileOrDefault

  private def nameField = optional(nonEmptyText(minLength = 2, maxLength = 20))

  case class Passwd(
      oldPasswd: String,
      newPasswd1: String,
      newPasswd2: String
  ) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  def passwd(u: User) = UserRepo loginCandidate u map { candidate =>
    Form(mapping(
      "oldPasswd" -> nonEmptyText.verifying("incorrectPassword", { pass =>
        // ugly shit, but Play forms are synchronous.
        // make sure the form is ratelimited by user upstream
        candidate.check(pass) await 2.seconds
      }),
      "newPasswd1" -> nonEmptyText(minLength = 2),
      "newPasswd2" -> nonEmptyText(minLength = 2)
    )(Passwd.apply)(Passwd.unapply).verifying("the new passwords don't match", _.samePasswords))
  }

  val title = Form(single("title" -> optional(nonEmptyText)))
}
