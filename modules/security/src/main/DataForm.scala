package lila.security

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

import lila.common.{ LameName, EmailAddress }
import lila.user.{ User, UserRepo }

final class DataForm(
    val captcher: akka.actor.ActorSelection,
    emailValidator: EmailAddressValidator
) extends lila.hub.CaptchedForm {

  import DataForm._

  case class Empty(gameId: String, move: String)

  val empty = Form(mapping(
    "gameId" -> text,
    "move" -> text
  )(Empty.apply)(_ => None)
    .verifying(captchaFailMessage, validateCaptcha _))

  def emptyWithCaptcha = withCaptcha(empty)

  private val anyEmail = nonEmptyText.verifying(Constraints.emailAddress)
  private val acceptableEmail = anyEmail.verifying(emailValidator.acceptableConstraint)
  private def acceptableUniqueEmail(forUser: Option[User]) =
    acceptableEmail.verifying(emailValidator uniqueConstraint forUser)

  object signup {

    private val username = nonEmptyText.verifying(
      Constraints minLength 2,
      Constraints maxLength 20,
      Constraints.pattern(
        regex = User.usernameRegex,
        error = "usernameInvalid"
      )
    ).verifying("usernameUnacceptable", u => !LameName(u))
      .verifying("usernameAlreadyUsed", u => !UserRepo.nameExists(u).awaitSeconds(4))

    val website = Form(mapping(
      "username" -> username,
      "password" -> text(minLength = 4),
      "email" -> acceptableUniqueEmail(none),
      "g-recaptcha-response" -> optional(nonEmptyText)
    )(SignupData.apply)(_ => None))

    val mobile = Form(mapping(
      "username" -> username,
      "password" -> text(minLength = 4),
      "email" -> acceptableUniqueEmail(none),
      "can-confirm" -> optional(boolean)
    )(MobileSignupData.apply)(_ => None))
  }

  val passwordReset = Form(mapping(
    "email" -> anyEmail, // allow unacceptable emails for BC
    "gameId" -> text,
    "move" -> text
  )(PasswordReset.apply)(_ => None)
    .verifying(captchaFailMessage, validateCaptcha _))

  def passwordResetWithCaptcha = withCaptcha(passwordReset)

  val newPassword = Form(single(
    "password" -> text(minLength = 4)
  ))

  case class PasswordResetConfirm(newPasswd1: String, newPasswd2: String) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  val passwdReset = Form(mapping(
    "newPasswd1" -> nonEmptyText(minLength = 2),
    "newPasswd2" -> nonEmptyText(minLength = 2)
  )(PasswordResetConfirm.apply)(PasswordResetConfirm.unapply).verifying(
      "the new passwords don't match",
      _.samePasswords
    ))

  def changeEmail(u: User, old: Option[EmailAddress]) = UserRepo loginCandidate u map { candidate =>
    Form(mapping(
      "passwd" -> nonEmptyText.verifying("incorrectPassword", candidate.check),
      "email" -> acceptableUniqueEmail(candidate.user.some).verifying(emailValidator differentConstraint old)
    )(ChangeEmail.apply)(ChangeEmail.unapply)).fill(ChangeEmail(
      passwd = "",
      email = old.??(_.value)
    ))
  }

  def modEmail(user: User) = Form(single("email" -> acceptableUniqueEmail(user.some)))

  val closeAccount = Form(single("passwd" -> nonEmptyText))
}

object DataForm {

  case class SignupData(
      username: String,
      password: String,
      email: String,
      `g-recaptcha-response`: Option[String]
  ) {
    def recaptchaResponse = `g-recaptcha-response`

    def realEmail = EmailAddress(email)
  }

  case class MobileSignupData(
      username: String,
      password: String,
      email: String,
      canConfirm: Option[Boolean]
  ) {
    def realEmail = EmailAddress(email)
  }

  case class PasswordReset(
      email: String,
      gameId: String,
      move: String
  ) {
    def realEmail = EmailAddress(email)
  }

  case class ChangeEmail(passwd: String, email: String) {
    def realEmail = EmailAddress(email)
  }
}
