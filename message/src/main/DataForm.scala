package lila.message

import lila.user.{ User, UserRepo }

import play.api.data._
import play.api.data.Forms._

final class DataForm(userRepo: UserRepo) {

  import DataForm._

  val thread = Form(mapping(
    "username" -> nonEmptyText.verifying("Unknown username", usernameExists _),
    "subject" -> text(minLength = 3),
    "text" -> text(minLength = 3)
  )({
      case (username, subject, text) ⇒ ThreadData(
        user = fetchUser(username) err "Unknown username " + username,
        subject = subject,
        text = text)
    })(_.export.some))

  val post = Form(single(
    "text" -> text(minLength = 3)
  ))

  private def fetchUser(username: String) = (userRepo.find byId username).await

  private def usernameExists(username: String) = fetchUser(username).isDefined
}

object DataForm {

  case class ThreadData(
      user: User,
      subject: String,
      text: String) {

    def export = (user.username, subject, text)
  }
}
