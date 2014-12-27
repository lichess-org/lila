package lila.security

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

import lila.db.api.$count
import lila.user.tube.userTube

final class DataForm(val captcher: akka.actor.ActorSelection) extends lila.hub.CaptchedForm {

  import DataForm._

  val signup = Form(mapping(
    "username" -> nonEmptyText.verifying(
      Constraints minLength 2,
      Constraints maxLength 20,
      Constraints.pattern(
        regex = """^[\w-]+$""".r,
        error = "Invalid username. Please use only letters, numbers and dash"),
      Constraints.pattern(
        regex = """^[^\d].+$""".r,
        error = "The username must not start with a number")
    ),
    "password" -> text(minLength = 4),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(SignupData.apply)(_ => None)
    .verifying("This user already exists", d => !userExists(d).await)
    .verifying("This username is not acceptable", d => !usernameSucks(d.username.toLowerCase))
    .verifying(captchaFailMessage, validateCaptcha _)
  )

  def signupWithCaptcha = withCaptcha(signup)

  val passwordReset = Form(mapping(
    "email" -> Forms.email,
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(PasswordReset.apply)(_ => None)
    .verifying(captchaFailMessage, validateCaptcha _)
  )

  def passwordResetWithCaptcha = withCaptcha(passwordReset)

  val newPassword = Form(single(
    "password" -> text(minLength = 4)
  ))

  case class PasswordResetConfirm(
      newPasswd1: String,
      newPasswd2: String) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  val passwdReset = Form(mapping(
    "newPasswd1" -> nonEmptyText(minLength = 2),
    "newPasswd2" -> nonEmptyText(minLength = 2)
  )(PasswordResetConfirm.apply)(PasswordResetConfirm.unapply).verifying(
      "the new passwords don't match",
      _.samePasswords
    ))

  private def userExists(data: SignupData) =
    $count.exists(data.username.toLowerCase)

  private def usernameSucks(u: String) =
    (lameUsernames exists u.contains) ||
      (lamePrefixes exists u.startsWith) ||
      (lameSuffixes exists u.endsWith)

  private val lamePrefixes = "_" :: "-" :: (for {
    title <- "nm cm fm im gm lm".split(' ').toList
    sep <- List("-", "_")
  } yield s"$title$sep") ::: (0 to 9).toList map (_.toString)

  private val lameSuffixes = List("-", "_")

  private val lameUsernames = List(
    "hitler",
    "fuck",
    "penis",
    "vagin",
    "anus",
    "bastard",
    "bitch",
    "shit",
    "shiz",
    "cunniling",
    "cunt",
    "kunt",
    "douche",
    "faggot",
    "jerk",
    "nigg",
    "piss",
    "poon",
    "prick",
    "pussy",
    "slut",
    "whore",
    "nazi")
}

object DataForm {

  case class SignupData(
    username: String,
    password: String,
    gameId: String,
    move: String)

  case class PasswordReset(
    email: String,
    gameId: String,
    move: String)
}
