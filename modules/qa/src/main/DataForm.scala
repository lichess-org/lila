package lila.qa

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.Lang

private[qa] final class DataForm(
    val captcher: akka.actor.ActorSelection,
    detectLanguage: lila.common.DetectLanguage
) extends lila.hub.CaptchedForm {

  lazy val question = Form(
    mapping(
      "title" -> text(minLength = 10, maxLength = 150)
        .verifying(languageMessage, validateLanguage _),
      "body" -> text(minLength = 10, maxLength = 10000)
        .verifying(languageMessage, validateLanguage _),
      "hidden-tags" -> text,
      "gameId" -> text,
      "move" -> text
    )(QuestionData.apply)(QuestionData.unapply)
      .verifying(captchaFailMessage, validateCaptcha _)
  )

  def editQuestion(q: Question) = question fill QuestionData(
    title = q.title,
    body = q.body,
    `hidden-tags` = q.tags mkString ",",
    gameId = "",
    move = ""
  )

  lazy val answer = Form(
    mapping(
      "body" -> text(minLength = 30, maxLength = 10000)
        .verifying(languageMessage, validateLanguage _),
      "gameId" -> text,
      "move" -> text,
      "modIcon" -> optional(boolean)
    )(AnswerData.apply)(AnswerData.unapply)
      .verifying(captchaFailMessage, validateCaptcha _)
  )

  lazy val editAnswer = Form(
    single(
      "body" -> text(minLength = 30, maxLength = 50000)
        .verifying(languageMessage, validateLanguage _)
    )
  )

  lazy val moveAnswer = Form(single(
    "to" -> nonEmptyText
  ))

  lazy val comment = Form(
    mapping(
      "body" -> text(minLength = 20, maxLength = 10000)
        .verifying(languageMessage, validateLanguage _)
    )(CommentData.apply)(CommentData.unapply)
  )

  val vote = Form(single(
    "vote" -> number
  ))

  private val languageMessage = "I didn't understand that. Is it written in English?"

  private def validateLanguage(str: String) =
    detectLanguage(str).awaitSeconds(5).??(_ == Lang("en"))
}

private[qa] case class QuestionData(
    title: String,
    body: String,
    `hidden-tags`: String,
    gameId: String,
    move: String
) {

  def tags = `hidden-tags`.split(',').toList.map(_.trim.toLowerCase).filter(_.nonEmpty)
}

private[qa] case class AnswerData(
    body: String,
    gameId: String,
    move: String,
    modIcon: Option[Boolean]
)

private[qa] case class CommentData(body: String)
