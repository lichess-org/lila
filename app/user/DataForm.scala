package lila
package user

import play.api.data._
import play.api.data.Forms._

import ui.Color

object DataForm {

  val bio = Form(single(
    "bio" -> text(maxLength = 400)
  ))

  val sound = Form(single(
    "sound" -> nonEmptyText.verifying(Set("true", "false") contains _)
  ))

  val color = Form(single(
    "color" -> nonEmptyText.verifying(Color contains _)
  ))

}
