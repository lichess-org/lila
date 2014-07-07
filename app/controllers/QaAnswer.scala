package controllers

import play.api.data.Form
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.qa.{ QuestionId, Question, AnswerId, Answer, QaAuth }
import views._

object QaAnswer extends QaController {

  def create(id: QuestionId) = AuthBody { implicit ctx =>
    me =>
      WithQuestion(id) { q =>
        implicit val req = ctx.body
        forms.answer.bindFromRequest.fold(
          err => renderQuestion(q, Some(err)),
          data => api.answer.create(data, q, me) map { answer =>
            Redirect(routes.QaQuestion.show(q.id, q.slug) + "#answer-" + answer.id)
          }
        )
      }
  }

  def accept(questionId: QuestionId, answerId: AnswerId) = AuthBody { implicit ctx =>
    me =>
      (api.question findById questionId) zip (api.answer findById answerId) flatMap {
        case (Some(q), Some(a)) if (QaAuth canEdit q) =>
          api.answer.accept(q, a) inject Redirect(routes.QaQuestion.show(q.id, q.slug))
        case _ => notFound
      }
  }

  def vote(questionId: QuestionId, answerId: AnswerId) = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      forms.vote.bindFromRequest.fold(
        err => fuccess(BadRequest),
        v => api.answer.vote(answerId, me, v == 1) map {
          case Some(vote) => Ok(html.qa.vote(routes.QaAnswer.vote(questionId, answerId).url, vote))
          case None       => NotFound
        }
      )
  }

  def doEdit(questionId: QuestionId, answerId: AnswerId) = AuthBody { implicit ctx =>
    me =>
      WithOwnAnswer(questionId, answerId) { q =>
        a =>
          implicit val req = ctx.body
          forms.editAnswer.bindFromRequest.fold(
            err => renderQuestion(q),
            body => api.answer.edit(body, a.id) map {
              case None     => NotFound
              case Some(a2) => Redirect(routes.QaQuestion.show(q.id, q.slug) + "#answer-" + a2.id)
            }
          )
      }
  }

  def remove(questionId: QuestionId, answerId: AnswerId) = Secure(_.ModerateQa) { implicit ctx =>
    me =>
      OptionFuRedirect(api.answer findById answerId) { a =>
        (api.answer remove a.id) >>
          Env.mod.logApi.deleteQaAnswer(me.id, a.userId, a.body) inject
          routes.QaQuestion.show(questionId, "redirect")
      }
  }
}
