package controllers

import lila.app.{ given, * }
import views.*

final class TournamentCrud(env: Env) extends LilaController(env):

  private def crud = env.tournament.crudApi

  def index(page: Int) = Secure(_.ManageTournament) { _ ?=> _ ?=>
    Ok.pageAsync:
      crud
        .paginator(page)
        .map(html.tournament.crud.index)
  }

  def edit(id: TourId) = Secure(_.ManageTournament) { ctx ?=> _ ?=>
    FoundPage(crud one id): tour =>
      html.tournament.crud.edit(tour, crud editForm tour)
  }

  def update(id: TourId) = SecureBody(_.ManageTournament) { ctx ?=> _ ?=>
    Found(crud one id): tour =>
      crud
        .editForm(tour)
        .bindFromRequest()
        .fold(
          err => BadRequest.page(html.tournament.crud.edit(tour, err)),
          data => crud.update(tour, data) inject Redirect(routes.TournamentCrud.edit(id)).flashSuccess
        )
  }

  def form = Secure(_.ManageTournament) { ctx ?=> _ ?=>
    Ok.page(html.tournament.crud.create(crud.createForm))
  }

  def create = SecureBody(_.ManageTournament) { ctx ?=> me ?=>
    crud.createForm
      .bindFromRequest()
      .fold(
        err => BadRequest.page(html.tournament.crud.create(err)),
        data =>
          crud.create(data) map { tour =>
            Redirect {
              if tour.isTeamBattle then routes.Tournament.teamBattleEdit(tour.id)
              else routes.TournamentCrud.edit(tour.id)
            }.flashSuccess
          }
      )
  }

  def cloneT(id: TourId) = Secure(_.ManageTournament) { ctx ?=> _ ?=>
    FoundPage(crud one id): old =>
      html.tournament.crud.create(crud editForm crud.clone(old))
  }
