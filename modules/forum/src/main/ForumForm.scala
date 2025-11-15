package lila.forum

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.given
import lila.common.Form.{ cleanText, into }

final private[forum] class ForumForm(
    promotion: lila.core.security.PromotionApi,
    val captcha: lila.core.captcha.CaptchaApi
):

  import ForumForm.*

  def postMapping(inOwnTeam: Boolean)(using Me) =
    mapping(
      "text" -> userTextMapping(inOwnTeam),
      "gameId" -> of[GameId],
      "move" -> text,
      "modIcon" -> optional(boolean)
    )(PostData.apply)(unapply)
      .verifying(lila.core.captcha.failMessage, captcha.validateSync)

  def post(inOwnTeam: Boolean)(using Me) = Form(postMapping(inOwnTeam))

  def postEdit(inOwnTeam: Boolean, previousText: String)(using Me) =
    Form:
      mapping(
        "changes" -> userTextMapping(inOwnTeam, previousText.some)
      )(PostEdit.apply)(_.changes.some)

  def postWithCaptcha(inOwnTeam: Boolean)(using Me) = post(inOwnTeam) -> captcha.any

  def topic(inOwnTeam: Boolean)(using Me) =
    Form:
      mapping(
        "name" -> cleanText(minLength = 3, maxLength = 100),
        "post" -> postMapping(inOwnTeam)
      )(TopicData.apply)(unapply)

  val deleteWithReason = Form:
    single("reason" -> optional(nonEmptyText))

  val relocateTo = Form:
    single("categ" -> nonEmptyText.into[ForumCategId])

  private def userTextMapping(inOwnTeam: Boolean, previousText: Option[String] = None)(using me: Me) =
    cleanText(minLength = 3, 5_000_000) // high cap only used for bot move dumps
      .verifying(
        "You have reached the daily maximum for links in forum posts.",
        t => inOwnTeam || promotion.test(me, t, previousText)
      )
      .transform(_.replace("\r\n", "\n").replace('\r', '\n'), identity)

  val diagnostic = Form(
    mapping(
      "text" -> nonEmptyText(maxLength = 5_000_000), // high cap only used for bot move dumps
      "plaintext" -> optional(boolean)
    )(DiagnosticData.apply)(unapply)
  )

object ForumForm:

  case class PostData(
      text: String,
      gameId: GameId,
      move: String,
      modIcon: Option[Boolean]
  ) extends lila.core.captcha.WithCaptcha

  case class TopicData(
      name: String,
      post: PostData
  ):
    def automodText = s"$name\n${post.text}"

  case class PostEdit(changes: String)
  case class DiagnosticData(text: String, plaintext: Option[Boolean])
