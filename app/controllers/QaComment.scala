package controllers

import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.qa.{ QuestionId, AnswerId, QaAuth }

object QaComment extends QaController {

  def question(id: QuestionId) = AuthBody { implicit ctx => me =>
    IfCanComment {
      WithQuestion(id) { q =>
        implicit val req = ctx.body
        forms.comment.bindFromRequest.fold(
          err => renderQuestion(q, None),
          data => api.comment.create(data, Left(q), me) map { comment =>
            Redirect(routes.QaQuestion.show(q.id, q.slug) + "#comment-" + comment.id)
          }
        )
      }
    }
  }

  def answer(questionId: QuestionId, answerId: AnswerId) = AuthBody { implicit ctx => me =>
    IfCanComment {
      (api.question findById questionId) zip (api.answer findById answerId) flatMap {
        case (Some(q), Some(a)) =>
          implicit val req = ctx.body
          forms.comment.bindFromRequest.fold(
            err => renderQuestion(q, None),
            data => api.comment.create(data, Right(a), me) map { comment =>
              Redirect(routes.QaQuestion.show(q.id, q.slug) + "#comment-" + comment.id)
            }
          )
        case _ => notFound
      }
    }
  }

  def remove(questionId: QuestionId, commentId: String) = Secure(_.ModerateQa) { implicit ctx => me =>
    api.comment.remove(questionId, commentId) inject
      Redirect(routes.QaQuestion.show(questionId, "redirect"))
  }

  private def IfCanComment(block: => Fu[Result])(implicit ctx: Context) =
    if (QaAuth.canComment) block else fuccess(Forbidden)
}
