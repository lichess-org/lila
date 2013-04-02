package lila.security

import lila.user.{ User, UserRepo }
import lila.db.api.$exists

import akka.actor.ActorRef
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

final class DataForm(val captcher: ActorRef) extends lila.hub.CaptchedForm {

  import DataForm._
  import UserRepo.tube

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
    .verifying("This user already exists", d ⇒ !userExists(d).await)
    .verifying(captchaFailMessage, validateCaptcha)
  )

  def signupWithCaptcha = withCaptcha(signup)

  private def userExists(data: SignupData) =
    $count.exists[User](data.username.toLowerCase)
}

object DataForm {

  case class SignupData(
    username: String,
    password: String,
    gameId: String,
    move: String)
}
