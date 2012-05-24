package lila
package user

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val bio = Form(single(
    "bio" -> text(maxLength = 400)
  ))
}
