package controllers

import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.game.{ Pov, GameRepo }
import lila.tournament.{ System, TournamentRepo, PairingRepo, Tournament => Tourney, VisibleTournaments }
import lila.user.UserRepo
import views._

object TournamentCrud extends LilaController {

  private def env = Env.tournament
  private def crud = Env.tournament.crudApi

  def index = Secure(_.ManageTournament) { implicit ctx =>
    me =>
      crud.list map { tours =>
        html.tournament.crud.index(tours)
      }
  }

  def edit(id: String) = Secure(_.ManageTournament) { implicit ctx =>
    me =>
      OptionOk(crud one id) { tour =>
        html.tournament.crud.edit(tour, crud editForm tour)
      }
  }

  def update(id: String) = SecureBody(_.ManageTournament) { implicit ctx =>
    me =>
      OptionFuResult(crud one id) { tour =>
        implicit val req = ctx.body
        crud.editForm(tour).bindFromRequest.fold(
          err => BadRequest(html.tournament.crud.edit(tour, err.pp)).fuccess,
          data => crud.update(tour, data) inject Redirect(routes.TournamentCrud.edit(id))
        )
      }
  }

  def form = Secure(_.ManageTournament) { implicit ctx =>
    me =>
      Ok(html.tournament.crud.create(crud.createForm)).fuccess
  }

  def create = SecureBody(_.ManageTournament) { implicit ctx =>
    me =>
      implicit val req = ctx.body
      crud.createForm.bindFromRequest.fold(
        err => BadRequest(html.tournament.crud.create(err)).fuccess,
        data => crud.create(data, me) map { tour =>
          Redirect(routes.TournamentCrud.edit(tour.id))
        }
      )
  }
}
