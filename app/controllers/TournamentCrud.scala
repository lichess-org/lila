package controllers

import lila.app.{ given, * }
import views.*

final class TournamentCrud(env: Env) extends LilaController(env):

  private def crud = env.tournament.crudApi

  def index(page: Int) = Secure(_.ManageTournament) { _ ?=> _ =>
    crud
      .paginator(page)
      .map:
        html.tournament.crud.index
  }

  def edit(id: TourId) = Secure(_.ManageTournament) { ctx ?=> _ =>
    OptionOk(crud one id): tour =>
      html.tournament.crud.edit(tour, crud editForm tour)
  }

  def update(id: TourId) = SecureBody(_.ManageTournament) { ctx ?=> _ =>
    OptionFuResult(crud one id): tour =>
      crud
        .editForm(tour)
        .bindFromRequest()
        .fold(
          err => BadRequest(html.tournament.crud.edit(tour, err)).toFuccess,
          data => crud.update(tour, data) inject Redirect(routes.TournamentCrud.edit(id)).flashSuccess
        )
  }

  def form = Secure(_.ManageTournament) { ctx ?=> _ =>
    Ok(html.tournament.crud.create(crud.createForm)).toFuccess
  }

  def create = SecureBody(_.ManageTournament) { ctx ?=> me =>
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

  def cloneT(id: TourId) = Secure(_.ManageTournament) { ctx ?=> _ =>
    OptionFuResult(crud one id): old =>
      val tour = crud clone old
      Ok(html.tournament.crud.create(crud editForm tour)).toFuccess
  }
