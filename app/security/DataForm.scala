package lila.app
package security

import user.{ User, UserRepo }
import site.Captcha

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

final class DataForm(
    userRepo: UserRepo,
    captcher: Captcha) {

  import DataForm._

  val signup = Form(mapping(
    "username" -> nonEmptyText.verifying(
      Constraints minLength 2,
      Constraints maxLength 20,
      Constraints.pattern(
        regex = """^[\w-]+$""".r,
        error = "Invalid username. Please use only letters, numbers and dash")
    ),
    "password" -> text(minLength = 4),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(SignupData.apply)(_ ⇒ None)
    .verifying("This user already exists", d ⇒ !userExists(d))
    .verifying(
      "Not a checkmate",
      data ⇒ captcher get data.gameId valid data.move.trim.toLowerCase
    )
  )

  def signupWithCaptcha = signup -> captchaCreate

  def captchaCreate: Captcha.Challenge = captcher.create

  private def userExists(data: SignupData) =
    userRepo.exists(data.username).unsafePerformIO
}

object DataForm {

  case class SignupData(
    username: String,
    password: String,
    gameId: String,
    move: String)
}
