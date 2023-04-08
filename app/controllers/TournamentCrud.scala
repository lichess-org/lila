package controllers

import lila.app.{ given, * }
import views.*

final class TournamentCrud(env: Env) extends LilaController(env):

  private def crud = env.tournament.crudApi

  def index(page: Int) =
    Secure(_.ManageTournament) { implicit ctx => _ =>
      crud.paginator(page) map { paginator =>
        html.tournament.crud.index(paginator)
      }
    }

  def edit(id: TourId) =
    Secure(_.ManageTournament) { implicit ctx => _ =>
      OptionOk(crud one id) { tour =>
        html.tournament.crud.edit(tour, crud editForm tour)
      }
    }

  def update(id: TourId) =
    SecureBody(_.ManageTournament) { implicit ctx => _ =>
      OptionFuResult(crud one id) { tour =>
        given play.api.mvc.Request[?] = ctx.body
        crud
          .editForm(tour)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.tournament.crud.edit(tour, err)).toFuccess,
            data => crud.update(tour, data) inject Redirect(routes.TournamentCrud.edit(id)).flashSuccess
          )
      }
    }

  def form =
    Secure(_.ManageTournament) { implicit ctx => _ =>
      Ok(html.tournament.crud.create(crud.createForm)).toFuccess
    }

  def create =
    SecureBody(_.ManageTournament) { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      crud.createForm
        .bindFromRequest()
        .fold(
          err => BadRequest(html.tournament.crud.create(err)).toFuccess,
          data =>
            crud.create(data, me.user) map { tour =>
              Redirect {
                if (tour.isTeamBattle) routes.Tournament.teamBattleEdit(tour.id)
                else routes.TournamentCrud.edit(tour.id)
              }.flashSuccess
            }
        )
    }

  def cloneT(id: TourId) =
    Secure(_.ManageTournament) { implicit ctx => _ =>
      OptionFuResult(crud one id) { old =>
        val tour = crud clone old
        Ok(html.tournament.crud.create(crud editForm tour)).toFuccess
      }
    }
