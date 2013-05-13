package lila.security

import lila.user.tube.userTube
import lila.db.api.$count

import akka.actor.ActorRef
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

final class DataForm(val captcher: lila.hub.ActorLazyRef) extends lila.hub.CaptchedForm {

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
    .verifying("This user already exists", d ⇒ !userExists(d).await)
    .verifying(captchaFailMessage, validateCaptcha _)
  )

  def signupWithCaptcha = withCaptcha(signup)

  private def userExists(data: SignupData) =
    $count.exists(data.username.toLowerCase)
}

object DataForm {

  case class SignupData(
    username: String,
    password: String,
    gameId: String,
    move: String)
}
