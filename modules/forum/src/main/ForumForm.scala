package lila.forum

import lila.common.Form.cleanText
import play.api.data.*
import play.api.data.Forms.*
import lila.user.User
import lila.common.Form.given
import lila.user.Me

final private[forum] class ForumForm(
    promotion: lila.security.PromotionApi,
    val captcher: lila.hub.actors.Captcher
)(using Executor)
    extends lila.hub.CaptchedForm:

  import ForumForm.*

  def postMapping(inOwnTeam: Boolean)(using Me) =
    mapping(
      "text"    -> userTextMapping(inOwnTeam),
      "gameId"  -> of[GameId],
      "move"    -> text,
      "modIcon" -> optional(boolean)
    )(PostData.apply)(unapply)
      .verifying(captchaFailMessage, validateCaptcha)

  def post(inOwnTeam: Boolean)(using Me) = Form(postMapping(inOwnTeam))

  def postEdit(inOwnTeam: Boolean, previousText: String)(using Me) =
    Form:
      mapping(
        "changes" -> userTextMapping(inOwnTeam, previousText.some)
      )(PostEdit.apply)(_.changes.some)

  def postWithCaptcha(inOwnTeam: Boolean)(using Me) = withCaptcha(post(inOwnTeam))

  def topic(inOwnTeam: Boolean)(using Me) =
    Form:
      mapping(
        "name" -> cleanText(minLength = 3, maxLength = 100),
        "post" -> postMapping(inOwnTeam)
      )(TopicData.apply)(unapply)

  val deleteWithReason = Form:
    single("reason" -> optional(nonEmptyText))

  private def userTextMapping(inOwnTeam: Boolean, previousText: Option[String] = None)(using Me) =
    cleanText(minLength = 3)
      .verifying(
        "You have reached the daily maximum for links in forum posts.",
        t => inOwnTeam || promotion.test(t, previousText)
      )

object ForumForm:

  case class PostData(
      text: String,
      gameId: GameId,
      move: String,
      modIcon: Option[Boolean]
  )

  case class TopicData(
      name: String,
      post: PostData
  )

  case class PostEdit(changes: String)
