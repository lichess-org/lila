package controllers

import lila._
import user.{ User ⇒ UserModel }
import views._
import http.Context
import security.Granter

import scalaz.effects._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.templates.Html

object Team extends LilaController {

  private def repo = env.team.repo
  private def forms = env.team.forms
  private def api = env.team.api

  def home(page: Int) = Open { implicit ctx ⇒
    Ok(html.team.home(api popular page))
  }

  def show(id: String) = Open { implicit ctx ⇒
    IOptionOk(repo byId id) { html.team.show(_) }
  }

  def form = Auth { implicit ctx ⇒
    me ⇒ OnePerWeek(me) {
      Ok(html.team.form(forms.create, forms.captchaCreate))
    }
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒ OnePerWeek(me) {
      IOResult {
        implicit val req = ctx.body
        forms.create.bindFromRequest.fold(
          err ⇒ io(BadRequest(html.team.form(err, forms.captchaCreate))),
          setup ⇒ api.create(setup, me) map { team ⇒
            Redirect(routes.Team.show(team.id))
          })
      }
    }
  }

  def mine = Auth { implicit ctx ⇒
    me ⇒
      IOk(api mine me map { html.team.mine(_) })
  }

  private def OnePerWeek[A <: Result](me: UserModel)(a: ⇒ A)(implicit ctx: Context): Result = {
    !Granter.superAdmin(me) &&
      api.hasCreatedRecently(me).unsafePerformIO
  } fold (Forbidden(views.html.team.createLimit()), a)
}
