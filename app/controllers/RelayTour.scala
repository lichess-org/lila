package controllers

import play.api.data.Form
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.relay.{ RelayRound => RoundModel, RelayTour => TourModel }
import lila.user.{ User => UserModel }
import views._

final class RelayTour(env: Env) extends LilaController(env) {

  def index(page: Int) =
    Open { implicit ctx =>
      Reasonable(page) {
        for {
          active <- (page == 1).??(env.relay.api.officialActive)
          pager  <- env.relay.pager.inactive(page)
        } yield Ok(html.relay.index(active, pager))
      }
    }

  def show(slug: String, id: String) = Open { implicit ctx =>
    WithTour(id) { tour =>
      if (tour.slug != slug) Redirect(routes.RelayTour.show(tour.slug, tour.id.value)).fuccess
      else
        env.relay.api.byTour(tour) map { relays =>
          Ok(html.relay.tour.show(tour, relays))
        }
    }
  }

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      Ok(html.relay.tourForm.create(env.relay.tourForm.create)).fuccess
    }
  }

  def create = AuthBody { implicit ctx => me =>
    NoLameOrBot {
      env.relay.tourForm.create
        .bindFromRequest()(ctx.body, formBinding)
        .fold(
          err => BadRequest(html.relay.tourForm.create(err)).fuccess,
          setup =>
            env.relay.api.tourCreate(setup, me) map { tour =>
              Redirect(routes.RelayTour.show(tour.slug, tour.id.value))
            }
        )
    }
  }

  def edit(id: String) = Auth { implicit ctx => me =>
    WithTour(id) { tour =>
      ???
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    WithTour(id) { tour =>
      ???
    }
  }

  private def WithTour(id: String)(
      f: TourModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.relay.api tourById TourModel.Id(id))(f)
}
