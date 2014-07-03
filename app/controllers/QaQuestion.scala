package controllers

import play.api.mvc._

import lila.app._
import lila.qa.{ QuestionId, Question, AnswerId, Answer, QuestionWithUsers, QaAuth }
import views._

object QaQuestion extends QaController {

  def index(page: Option[Int] = None) = Open { implicit ctx =>
    api.question.paginatorWithUsers(page getOrElse 1, 20) zip
      (api.question popular 10) map {
        case (questions, popular) => Ok(views.html.qa.index(questions, popular))
      }
  }

  def search = Open { implicit ctx =>
    val query = ~get("q")
    (query match {
      case "" => (api.question recent 20 flatMap api.question.zipWithUsers)
      case _  => Env.qa search query flatMap api.question.zipWithUsers
    }) zip (api.question popular 10) map {
      case (questions, popular) => Ok(views.html.qa.search(query, questions, popular))
    }
  }

  def byTag(tag: String) = Open { implicit ctx =>
    (api.question.byTag(tag, 20) flatMap api.question.zipWithUsers) zip
      (api.question popular 10) map {
        case (questions, popular) => Ok(views.html.qa.byTag(tag, questions, popular))
      }
  }

  def show(id: QuestionId, slug: String) = Open { implicit ctx =>
    WithQuestion(id, slug) { q =>
      (api.question incViews q) >> renderQuestion(q)
    }
  }

  def ask = TODO
  // WithUser { implicit req =>
  //   renderAsk(forms.question, Results.Ok)
  // }

  def doAsk = TODO
  // WithUser { implicit req =>
  //   forms.question.bindFromRequest.fold(
  //     err => renderAsk(err, Results.BadRequest),
  //     data => QaApi.question.create(data, req.ctx.user) map { q =>
  //       Redirect(routes.Question.show(q.id, q.slug))
  //     }
  //   )
  // }
}
