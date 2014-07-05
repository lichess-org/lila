package lila.qa

import play.api.data._
import play.api.data.Forms._

object DataForms {

  lazy val question = Form(
    mapping(
      "title" -> nonEmptyText(minLength = 10, maxLength = 150),
      "body" -> nonEmptyText(minLength = 10, maxLength = 10000),
      "hidden-tags" -> text
    )(QuestionData.apply)(QuestionData.unapply)
  )

  def editQuestion(q: Question) = question fill QuestionData(
    title = q.title,
    body = q.body,
    `hidden-tags` = q.tags mkString ",")

  case class QuestionData(title: String, body: String, `hidden-tags`: String) {

    def tags = `hidden-tags`.split(',').toList.map(_.trim).filter(_.nonEmpty)
  }

  lazy val answer = Form(
    mapping(
      "body" -> nonEmptyText(minLength = 30)
    )(AnswerData.apply)(AnswerData.unapply)
  )

  case class AnswerData(body: String)

  lazy val comment = Form(
    mapping(
      "body" -> nonEmptyText(minLength = 20)
    )(CommentData.apply)(CommentData.unapply)
  )

  case class CommentData(body: String)

  val vote = Form(single(
    "vote" -> number
  ))
}
