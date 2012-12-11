package controllers

import lila._
import team.{ Joined, Motivate }
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

  private def teamRepo = env.team.teamRepo
  private def forms = env.team.forms
  private def api = env.team.api
  private def paginator = env.team.paginator

  def home(page: Int) = Open { implicit ctx ⇒
    Ok(html.team.home(paginator popularTeams page))
  }

  def show(id: String, page: Int) = Open { implicit ctx ⇒
    IOptionIOk(teamRepo byId id) { team ⇒
      api relationTo team map { relation ⇒
        html.team.show(team, paginator.teamMembers(team, page), relation)
      }
    }
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
    me ⇒ IOk(api mine me map { html.team.mine(_) })
  }

  def join(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒ IOResult(api join id map {
      case Some(Joined(team))   ⇒ Redirect(routes.Team.show(team.id))
      case Some(Motivate(team)) ⇒ Redirect(routes.Team.requestForm(team.id))
      case _                    ⇒ notFound
    })
  }

  def requestForm(id: String) = Auth { implicit ctx ⇒
    me ⇒ IOptionOk(api.requestable(id, me)) { team ⇒
      html.team.requestForm(team, forms.request, forms.captchaCreate)
    }
  }

  def requestCreate(id: String) = AuthBody { implicit ctx ⇒
    me ⇒ IOptionIOResult(api.requestable(id, me)) { team ⇒
      implicit val req = ctx.body
      forms.request.bindFromRequest.fold(
        err ⇒ io(BadRequest(html.team.requestForm(team, err, forms.captchaCreate))),
        setup ⇒ api.createRequest(team, setup, me) inject Redirect(routes.Team.show(team.id))
      )
    }
  }

  def quit(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒ IOResult(api quit id map {
      case Some(team) ⇒ Redirect(routes.Team.show(team.id))
      case _          ⇒ notFound
    })
  }

  private def OnePerWeek[A <: Result](me: UserModel)(a: ⇒ A)(implicit ctx: Context): Result = {
    !Granter.superAdmin(me) &&
      api.hasCreatedRecently(me).unsafePerformIO
  } fold (Forbidden(views.html.team.createLimit()), a)
}
