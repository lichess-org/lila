package lila.qa

import play.api.data._
import play.api.data.Forms._

private[qa] final class DataForm(val captcher: akka.actor.ActorSelection) extends lila.hub.CaptchedForm {

  lazy val question = Form(
    mapping(
      "title" -> nonEmptyText(minLength = 10, maxLength = 150),
      "body" -> nonEmptyText(minLength = 10, maxLength = 10000),
      "hidden-tags" -> text,
      "gameId" -> text,
      "move" -> text
    )(QuestionData.apply)(QuestionData.unapply)
      .verifying(captchaFailMessage, validateCaptcha _))

  def editQuestion(q: Question) = question fill QuestionData(
    title = q.title,
    body = q.body,
    `hidden-tags` = q.tags mkString ",",
    gameId = "",
    move = "")

  lazy val answer = Form(
    mapping(
      "body" -> nonEmptyText(minLength = 30),
      "gameId" -> text,
      "move" -> text
    )(AnswerData.apply)(AnswerData.unapply)
      .verifying(captchaFailMessage, validateCaptcha _))

  lazy val editAnswer = Form(
    single(
      "body" -> nonEmptyText(minLength = 30)
    ))

  lazy val comment = Form(
    mapping(
      "body" -> nonEmptyText(minLength = 20)
    )(CommentData.apply)(CommentData.unapply)
  )

  val vote = Form(single(
    "vote" -> number
  ))
}

private[qa] case class QuestionData(
    title: String,
    body: String,
    `hidden-tags`: String,
    gameId: String,
    move: String) {

  def tags = `hidden-tags`.split(',').toList.map(_.trim).filter(_.nonEmpty)
}

private[qa] case class AnswerData(
  body: String,
  gameId: String,
  move: String)

private[qa] case class CommentData(body: String)
