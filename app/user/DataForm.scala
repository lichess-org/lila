package lila
package user

import play.api.data._
import play.api.data.Forms._

import ui.Color

object DataForm {

  val bio = Form(single(
    "bio" -> text(maxLength = 400)
  ))

  val chat = Form(single(
    "chat" -> jsBoolean
  ))

  val sound = Form(single(
    "sound" -> jsBoolean
  ))

  val color = Form(single(
    "color" -> nonEmptyText.verifying(Color contains _)
  ))

  private def jsBoolean = nonEmptyText.verifying(Set("true", "false") contains _)
}
