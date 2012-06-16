package lila
package i18n

import play.api.data._
import play.api.data.Forms._

object DataForm {

  case class Translation(
    author: Option[String],
    comment: Option[String],
    messages: Map[String, String])

  val translation = Form(tuple(
    "author" -> optional(nonEmptyText),
    "comment" -> optional(nonEmptyText)
  ))
}
