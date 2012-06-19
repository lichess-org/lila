package lila
package user

import play.api.data._
import play.api.data.Forms._

import ui.Theme

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

  val theme = Form(single(
    "theme" -> nonEmptyText.verifying(Theme contains _)
  ))

  val bg = Form(single(
    "bg" -> text.verifying(Set("light", "dark") contains _)
  ))

  private def jsBoolean = nonEmptyText.verifying(Set("true", "false") contains _)
}
