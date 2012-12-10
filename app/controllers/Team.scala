package controllers

import lila._
import views._
import http.Context

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
    me ⇒
        Ok(html.team.form(forms.create, forms.captchaCreate))
  }

  def create = TODO
  // AuthBody { implicit ctx ⇒
  //   implicit me ⇒
  //     NoEngine {
  //       IOResult {
  //         implicit val req = ctx.body
  //         forms.create.bindFromRequest.fold(
  //           err ⇒ io(BadRequest(html.tournament.form(err, forms))),
  //           setup ⇒ api.createTournament(setup, me) map { tour ⇒
  //             Redirect(routes.Tournament.show(tour.id))
  //           })
  //       }
  //     }
  // }
}
