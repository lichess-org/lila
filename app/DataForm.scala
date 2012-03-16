package lila.http

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val moveForm = Form(tuple(
    "from" -> text,
    "to" -> text,
    "promotion" -> optional(text),
    "b" -> optional(number)
  ))
}
