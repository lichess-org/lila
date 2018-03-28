package controllers

import play.api.data.Form
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.qa.{ QuestionId, Question, QaAuth }
import views._

object QaQuestion extends QaController {

  def index(page: Option[Int] = None) = Open { implicit ctx =>
    pageHit
    for {
      pag <- api.question.recentPaginator(page getOrElse 1, lila.common.MaxPerPage(20))
      popular <- fetchPopular
      _ <- preloadUsers(pag.currentPageResults)
    } yield Ok(html.qa.index(pag, popular))
  }

  def search = Open { implicit ctx =>
    val query = (~get("q")).trim
    if (query.isEmpty) fuccess {
      Redirect(routes.QaQuestion.index())
    }
    else for {
      questions <- Env.qa search query
      popular <- fetchPopular
      _ <- preloadUsers(questions)
    } yield Ok(html.qa.search(query, questions, popular))
  }

  def byTag(tag: String) = Open { implicit ctx =>
    for {
      questions <- api.question.byTag(tag, 20)
      popular <- fetchPopular
      _ <- preloadUsers(questions)
    } yield Ok(html.qa.byTag(tag, questions, popular))
  }

  def show(id: QuestionId, slug: String) = Open { implicit ctx =>
    WithQuestion(id, slug) { q =>
      (api.question incViews q) >> renderQuestion(q)
    }
  }

  private def preloadUsers(questions: Seq[Question]) =
    Env.user.lightUserApi preloadMany questions.map(_.userId)

  private def renderAsk(form: Form[_], status: Results.Status)(implicit ctx: Context) =
    fetchPopular zip api.tag.all zip forms.anyCaptcha map {
      case ((popular, tags), captcha) => status(html.qa.ask(form, tags, popular, captcha))
    }

  def ask = Auth { implicit ctx => _ =>
    if (QaAuth.canAsk) renderAsk(forms.question, Results.Ok)
    else renderN00b
  }

  def doAsk = AuthBody { implicit ctx => me =>
    if (QaAuth.canAsk) {
      implicit val req = ctx.body
      forms.question.bindFromRequest.fold(
        err => renderAsk(err, Results.BadRequest),
        data => api.question.create(data, me) map { q =>
          Redirect(routes.QaQuestion.show(q.id, q.slug))
        }
      )
    } else renderN00b
  }

  def edit(id: QuestionId, slug: String) = Auth { implicit ctx => me =>
    WithOwnQuestion(id, slug) { q =>
      renderEdit(forms editQuestion q, q, Results.Ok)
    }
  }

  def doEdit(id: QuestionId) = AuthBody { implicit ctx => me =>
    WithOwnQuestion(id) { q =>
      implicit val req = ctx.body
      forms.question.bindFromRequest.fold(
        err => renderEdit(err, q, Results.BadRequest),
        data => api.question.edit(data, q.id) map {
          case None => NotFound
          case Some(q2) => Redirect(routes.QaQuestion.show(q2.id, q2.slug))
        }
      )
    }
  }

  private def renderEdit(form: Form[_], q: Question, status: Results.Status)(implicit ctx: Context) =
    fetchPopular zip api.tag.all zip forms.anyCaptcha map {
      case ((popular, tags), captcha) => status(html.qa.edit(form, q, tags, popular, captcha))
    }

  def vote(id: QuestionId) = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    forms.vote.bindFromRequest.fold(
      err => fuccess(BadRequest),
      v => api.question.vote(id, me, v == 1) map {
        case Some(vote) => Ok(html.qa.vote(routes.QaQuestion.vote(id).url, vote))
        case None => NotFound
      }
    )
  }

  def remove(questionId: QuestionId) = Secure(_.ModerateQa) { implicit ctx => me =>
    WithQuestion(questionId) { q =>
      (api.question remove q.id) >>
        Env.mod.logApi.deleteQaQuestion(me.id, q.userId, q.title) inject
        Redirect(routes.QaQuestion.index())
    }
  }

  def lock(questionId: QuestionId) = Secure(_.ModerateQa) { implicit ctx => me =>
    WithQuestion(questionId) { q =>
      (api.question.lock(q.id, !q.isLocked option me)) inject
        Redirect(routes.QaQuestion.show(q.id, q.slug))
    }
  }
}
