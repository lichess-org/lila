package lila
package forum

import play.api.data._
import play.api.data.Forms._

object DataForm {

  case class PostData(
    text: String,
    author: Option[String])

  lazy val postMapping = mapping(
    "text" -> text(minLength = 3),
    "author" -> optional(text)
  )(PostData.apply)(PostData.unapply)

  lazy val post = Form(postMapping)

  case class TopicData(
    name: String,
    post: PostData)

  lazy val topic = Form(mapping(
    "name" -> text(minLength = 3),
    "post" -> postMapping
  )(TopicData.apply)(TopicData.unapply))
}
