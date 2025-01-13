package controllers

import lila.app.*

final class TournamentCrud(env: Env) extends LilaController(env):

  import env.tournament.crudApi as crud
  import views.tournament.form.crud as crudView

  def index(page: Int) = Secure(_.ManageTournament) { _ ?=> _ ?=>
    Ok.async:
      crud
        .paginator(page)
        .map(crudView.index)
  }

  def edit(id: TourId) = Secure(_.ManageTournament) { ctx ?=> _ ?=>
    FoundPage(crud.one(id)): tour =>
      crudView.edit(tour, crud.editForm(tour))
  }

  def update(id: TourId) = SecureBody(_.ManageTournament) { ctx ?=> _ ?=>
    Found(crud.one(id)): tour =>
      bindForm(crud.editForm(tour))(
        err => BadRequest.page(crudView.edit(tour, err)),
        data => crud.update(tour, data).inject(Redirect(routes.TournamentCrud.edit(id)).flashSuccess)
      )
  }

  def form = Secure(_.ManageTournament) { ctx ?=> _ ?=>
    Ok.page(crudView.create(crud.createForm))
  }

  def create = SecureBody(_.ManageTournament) { ctx ?=> me ?=>
    bindForm(crud.createForm)(
      err => BadRequest.page(crudView.create(err)),
      data =>
        crud.create(data).map { tour =>
          Redirect {
            if tour.isTeamBattle then routes.Tournament.teamBattleEdit(tour.id)
            else routes.TournamentCrud.edit(tour.id)
          }.flashSuccess
        }
    )
  }

  def cloneT(id: TourId) = Secure(_.ManageTournament) { ctx ?=> _ ?=>
    FoundPage(crud.one(id)): old =>
      crudView.create(crud.editForm(crud.clone(old)))
  }
