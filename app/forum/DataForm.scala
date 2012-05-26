package lila
package forum

import play.api.data._
import play.api.data.Forms._

object DataForm {

  case class PostData(
    text: String,
    author: Option[String])

  val post = Form(mapping(
    "text" -> text(minLength = 3),
    "author" -> optional(text)
  )(PostData.apply)(PostData.unapply))
}
