package lila.security

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

import lila.common.LameName
import lila.db.api.$count
import lila.user.tube.userTube
import lila.user.User

final class DataForm(
    val captcher: akka.actor.ActorSelection,
    emailAddress: EmailAddress) extends lila.hub.CaptchedForm {

  import DataForm._

  case class Empty(gameId: String, move: String)

  val empty = Form(mapping(
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(Empty.apply)(_ => None)
    .verifying(captchaFailMessage, validateCaptcha _)
  )

  def emptyWithCaptcha = withCaptcha(empty)

  private val anyEmail = nonEmptyText.verifying(Constraints.emailAddress)
  private val acceptableEmail = anyEmail.verifying(emailAddress.acceptableConstraint)
  private def acceptableUniqueEmail(forUser: Option[User]) =
    acceptableEmail.verifying(emailAddress uniqueConstraint forUser)

  object signup {

    private val username = nonEmptyText.verifying(
      Constraints minLength 2,
      Constraints maxLength 20,
      Constraints.pattern(
        regex = """^[\w-]+$""".r,
        error = "Invalid username. Please use only letters, numbers and dash"),
      Constraints.pattern(
        regex = """^[^\d].+$""".r,
        error = "The username must not start with a number")
    ).verifying("This user already exists", u => !$count.exists(u.toLowerCase) awaitSeconds 2)
      .verifying("This username is not acceptable", u => !LameName(u))

    val website = Form(mapping(
      "username" -> username,
      "password" -> text(minLength = 4),
      "email" -> acceptableUniqueEmail(none),
      "g-recaptcha-response" -> nonEmptyText,
      "gameId" -> nonEmptyText,
      "move" -> nonEmptyText
    )(SignupData.apply)(_ => None)
      .verifying(captchaFailMessage, validateCaptcha _))

    val mobile = Form(mapping(
      "username" -> username,
      "password" -> text(minLength = 4),
      "email" -> optional(acceptableUniqueEmail(none))
    )(MobileSignupData.apply)(_ => None))

    def websiteWithCaptcha = withCaptcha(website)
  }

  val passwordReset = Form(mapping(
    "email" -> anyEmail, // allow unacceptable emails for BC
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(PasswordReset.apply)(_ => None)
    .verifying(captchaFailMessage, validateCaptcha _)
  )

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

  def changeEmail(user: User) = Form(mapping(
    "email" -> acceptableUniqueEmail(user.some),
    "passwd" -> nonEmptyText
  )(ChangeEmail.apply)(ChangeEmail.unapply))

  def modEmail(user: User) = Form(single("email" -> acceptableUniqueEmail(user.some)))

  val closeAccount = Form(single("passwd" -> nonEmptyText))
}

object DataForm {

  case class SignupData(
      username: String,
      password: String,
      email: String,
      `g-recaptcha-response`: String,
      gameId: String,
      move: String) {
    def recaptchaResponse = `g-recaptcha-response`
  }

  case class MobileSignupData(
    username: String,
    password: String,
    email: Option[String])

  case class PasswordReset(
    email: String,
    gameId: String,
    move: String)

  case class ChangeEmail(email: String, passwd: String)
}
