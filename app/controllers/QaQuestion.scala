package controllers

import play.api.data.Form
import play.api.mvc._

import lila.api.Context
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

  private def renderAsk(form: Form[_], status: Results.Status)(implicit ctx: Context) =
    api.question popular 10 zip api.tag.all map {
      case (popular, tags) => status(views.html.qa.ask(form, tags, popular))
    }

  def ask = Auth { implicit ctx =>
    _ =>
      renderAsk(forms.question, Results.Ok)
  }

  def doAsk = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      forms.question.bindFromRequest.fold(
        err => renderAsk(err, Results.BadRequest),
        data => api.question.create(data, me) map { q =>
          Redirect(routes.QaQuestion.show(q.id, q.slug))
        }
      )
  }
}
