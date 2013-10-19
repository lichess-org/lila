package lila.user

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val bio = Form(single(
    "bio" -> text(maxLength = 400)
  ))

  case class Passwd(
      oldPasswd: String,
      newPasswd1: String,
      newPasswd2: String) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  val passwd = Form(mapping(
    "oldPasswd" -> nonEmptyText,
    "newPasswd1" -> nonEmptyText(minLength = 2),
    "newPasswd2" -> nonEmptyText(minLength = 2)
  )(Passwd.apply)(Passwd.unapply).verifying(
      "the new passwords don't match",
      _.samePasswords
    ))
}
