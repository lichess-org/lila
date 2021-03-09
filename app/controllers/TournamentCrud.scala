package controllers

import lila.app._
import views._

final class TournamentCrud(env: Env) extends LilaController(env) {

  private def crud = env.tournament.crudApi

  def index(page: Int) =
    Secure(_.ManageTournament) { implicit ctx => _ =>
      crud.paginator(page) map { paginator =>
        html.tournament.crud.index(paginator)
      }
    }

  def edit(id: String) =
    Secure(_.ManageTournament) { implicit ctx => _ =>
      OptionOk(crud one id) { tour =>
        html.tournament.crud.edit(tour, crud editForm tour)
      }
    }

  def update(id: String) =
    SecureBody(_.ManageTournament) { implicit ctx => _ =>
      OptionFuResult(crud one id) { tour =>
        implicit val req = ctx.body
        crud
          .editForm(tour)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.tournament.crud.edit(tour, err)).fuccess,
            data => crud.update(tour, data) inject Redirect(routes.TournamentCrud.edit(id)).flashSuccess
          )
      }
    }

  def form =
    Secure(_.ManageTournament) { implicit ctx => _ =>
      Ok(html.tournament.crud.create(crud.createForm)).fuccess
    }

  def create =
    SecureBody(_.ManageTournament) { implicit ctx => me =>
      implicit val req = ctx.body
      crud.createForm
        .bindFromRequest()
        .fold(
          err => BadRequest(html.tournament.crud.create(err)).fuccess,
          data =>
            crud.create(data, me.user) map { tour =>
              Redirect {
                if (tour.isTeamBattle) routes.Tournament.teamBattleEdit(tour.id)
                else routes.TournamentCrud.edit(tour.id)
              }.flashSuccess
            }
        )
    }

  def cloneT(id: String) =
    Secure(_.ManageTournament) { implicit ctx => _ =>
      OptionFuResult(crud one id) { old =>
        val tour = crud clone old
        Ok(html.tournament.crud.create(crud editForm tour)).fuccess
      }
    }

}
