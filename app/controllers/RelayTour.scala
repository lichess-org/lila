package controllers

import play.api.data.Form
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.relay.{ Relay => RelayModel, RelayTour => TourModel, RelayForm }
import lila.user.{ User => UserModel }
import views._

final class RelayTour(env: Env) extends LilaController(env) {

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
    ???
  }

  def create = Auth { implicit ctx => me =>
    ???
  }

  private def WithTour(id: String)(
      f: TourModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.relay.api tourById TourModel.Id(id))(f)
}
