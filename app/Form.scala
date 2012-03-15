package lila.http

import play.api.data._
import play.api.data.Forms._

object LilaForm {

  val move = Form(tuple(
    "from" -> text,
    "to" -> text
  ))
}
