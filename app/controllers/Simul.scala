package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Pov, GameRepo }
import lila.simul.{ Simul => Sim }
import lila.user.UserRepo
import views._

object Simul extends LilaController {

  private def env = Env.simul

  private def simulNotFound(implicit ctx: Context) = NotFound(html.simul.notFound())

  val home = Open { implicit ctx =>
    ???
    // fetchTournaments zip repo.scheduled zip UserRepo.allSortToints(10) map {
    //   case ((((created, started), finished), scheduled), leaderboard) =>
    //     Ok(html.tournament.home(created, started, finished, scheduled, leaderboard))
    // }
  }

  private def newForm(me: lila.user.User) =
    env.forms.create(s"${me.username}'s simul")

  def form = Auth { implicit ctx =>
    me =>
      NoEngine {
        Ok(html.simul.form(newForm(me), env.forms)).fuccess
      }
  }

  def create = AuthBody { implicit ctx =>
    implicit me =>
      NoEngine {
        implicit val req = ctx.body
        newForm(me).bindFromRequest.fold(
          err => BadRequest(html.simul.form(err.pp, env.forms)).fuccess,
          setup => env.api.createSimul(setup, me) map { simul =>
            println(simul)
            ???
            // Redirect(routes.Simul.show(simul.id))
          })
      }
  }
}
