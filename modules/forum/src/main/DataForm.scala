package lila.forum

import play.api.data._
import play.api.data.Forms._

import lila.common.Form.clean

final private[forum] class DataForm(
    val captcher: lila.hub.actors.Captcher
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.hub.CaptchedForm {

  import DataForm._

  val postMapping = mapping(
    "text"    -> clean(text(minLength = 3)),
    "gameId"  -> text,
    "move"    -> text,
    "modIcon" -> optional(boolean)
  )(PostData.apply)(PostData.unapply)
    .verifying(captchaFailMessage, validateCaptcha _)

  val post = Form(postMapping)

  val postEdit = Form(mapping("changes" -> text(minLength = 3))(PostEdit.apply)(PostEdit.unapply))

  def postWithCaptcha = withCaptcha(post)

  val topic = Form(
    mapping(
      "name" -> clean(text(minLength = 3, maxLength = 100)),
      "post" -> postMapping
    )(TopicData.apply)(TopicData.unapply)
  )
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

    def looksLikeVenting =
      List(name, post.text) exists { txt =>
        mostlyUpperCase(txt) || ventingRegex.find(txt)
      }
  }

  private def mostlyUpperCase(text: String) =
    text.length > 5 && {
      import java.lang.Character._
      // true if >2/3 of the latin letters are upper
      (text take 300).foldLeft(0) { (i, c) =>
        getType(c) match {
          case UPPERCASE_LETTER => i + 1
          case LOWERCASE_LETTER => i - 2
          case _                => i
        }
      } > 0
    }

  private val ventingRegex = """cheat|engine|rating|loser|banned|abort""".r

  case class PostEdit(changes: String)
}
