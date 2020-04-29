package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import views._

final class Swiss(
    env: Env
) extends LilaController(env) {

  def show(id: String) = Open { implicit ctx =>
    ???
  }

  def form(teamId: String) = Auth { implicit ctx => me =>
    Ok(html.swiss.form.create(env.swiss.forms.create, teamId)).fuccess
  }

  def create(teamId: String) = AuthBody { implicit ctx => me =>
    env.team.teamRepo.isLeader(teamId, me.id) flatMap {
      case false => notFound
      case _ =>
        env.swiss.forms.create
          .bindFromRequest()(ctx.body)
          .fold(
            err => BadRequest(html.swiss.form.create(err, teamId)).fuccess,
            data =>
              env.swiss.api.create(data, me, teamId) map { swiss =>
                Redirect(routes.Swiss.show(swiss.id.value))
              }
          )
    }
  }
}
