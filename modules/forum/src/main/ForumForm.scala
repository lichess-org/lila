package lila.forum

import lila.common.Form.clean
import play.api.data._
import play.api.data.Forms._
import lila.user.User

final private[forum] class ForumForm(
    promotion: lila.security.PromotionApi,
    val captcher: lila.hub.actors.Captcher
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.hub.CaptchedForm {

  import ForumForm._

  def postMapping(user: User) =
    mapping(
      "text"    -> userTextMapping(user),
      "gameId"  -> text,
      "move"    -> text,
      "modIcon" -> optional(boolean)
    )(PostData.apply)(PostData.unapply)
      .verifying(captchaFailMessage, validateCaptcha _)

  def post(user: User) = Form(postMapping(user))

  def postEdit(user: User) =
    Form(
      mapping(
        "changes" -> userTextMapping(user)
      )(PostEdit.apply)(PostEdit.unapply)
    )

  def postWithCaptcha(user: User) = withCaptcha(post(user))

  def topic(user: User) =
    Form(
      mapping(
        "name" -> clean(text(minLength = 3, maxLength = 100)),
        "post" -> postMapping(user)
      )(TopicData.apply)(TopicData.unapply)
    )

  private def userTextMapping(user: User) =
    clean(text(minLength = 3))
      .verifying(
        "You have reached the maximum amount of links per day, which you can post to the forum",
        promotion.test(user) _
      )
}

object ForumForm {

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
    text.lengthIs > 5 && {
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
