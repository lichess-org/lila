package lila.forum

import play.api.data._
import play.api.data.Forms._

private[forum] final class DataForm(val captcher: akka.actor.ActorSelection) extends lila.hub.CaptchedForm {

  import DataForm._

  val postMapping = mapping(
    "text" -> text(minLength = 3),
    "gameId" -> text,
    "move" -> text,
    "modIcon" -> optional(boolean)
  )(PostData.apply)(PostData.unapply)
    .verifying(captchaFailMessage, validateCaptcha _)

  val post = Form(postMapping)

  val postEdit = Form(mapping("changes" -> text(minLength = 3))(PostEdit.apply)(PostEdit.unapply))

  def postWithCaptcha = withCaptcha(post)

  val topic = Form(mapping(
    "name" -> text(minLength = 3, maxLength = 100),
    "post" -> postMapping
  )(TopicData.apply)(TopicData.unapply))
}

object DataForm {

  case class PostData(
      text: String,
      gameId: String,
      move: String,
      modIcon: Option[Boolean]
  )

  case class TopicData(
      name: String,
      post: PostData
  ) {

    def looksLikeVenting = List(name, post.text) exists { txt =>
      mostlyUpperCase(txt) || ventingPattern.matcher(txt).find
    }
  }

  private def mostlyUpperCase(txt: String) = {
    val extract = txt.take(300)
    (extract.contains(' ') || extract.size > 5) && {
      extract.count(_.isUpper) > extract.count(_.isLower) * 2
    }
  }

  private val ventingPattern = """cheat|engine|rating|loser|banned|abort""".r.pattern

  case class PostEdit(changes: String)
}
