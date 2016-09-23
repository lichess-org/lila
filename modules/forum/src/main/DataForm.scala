package lila.forum

import play.api.data._
import play.api.data.Forms._

private[forum] final class DataForm(val captcher: akka.actor.ActorSelection) extends lila.hub.CaptchedForm {

  import DataForm._

  val postMapping = mapping(
    "text" -> text(minLength = 3),
    "author" -> optional(text),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(PostData.apply)(PostData.unapply)
    .verifying(captchaFailMessage, validateCaptcha _)

  val post = Form(postMapping)

  val postEdit = Form(mapping("changes" -> text(minLength=3))(PostEdit.apply)(PostEdit.unapply))

  def postWithCaptcha = withCaptcha(post)

  val topic = Form(mapping(
    "name" -> text(minLength = 3, maxLength = 100),
    "post" -> postMapping
  )(TopicData.apply)(TopicData.unapply))
}

object DataForm {

  case class PostData(
    text: String,
    author: Option[String],
    gameId: String,
    move: String)

  case class TopicData(
    name: String,
    post: PostData)

    case class PostEdit(changes: String)
}
