package lila.http

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val moveForm = Form(tuple(
    "from" -> nonEmptyText,
    "to" -> nonEmptyText,
    "promotion" -> optional(text),
    "b" -> optional(number)
  ))

  val talkForm = Form(tuple(
    "author" -> nonEmptyText,
    "message" -> nonEmptyText
  ))

  val joinForm = Form(tuple(
    "redirect" -> nonEmptyText,
    "messages" -> nonEmptyText
  ))
}
