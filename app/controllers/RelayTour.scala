package controllers

import play.api.data.Form
import play.api.mvc._
import scala.annotation.nowarn
import views._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.relay.{ RelayRound => RoundModel, RelayTour => TourModel }
import lila.user.{ User => UserModel }

final class RelayTour(env: Env, apiC: => Api) extends LilaController(env) {

  def index(page: Int) =
    Open { implicit ctx =>
      Reasonable(page) {
        for {
          active <- (page == 1).??(env.relay.api.officialActive)
          pager  <- env.relay.pager.inactive(page)
        } yield Ok(html.relay.tour.index(active, pager))
      }
    }

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      Ok(html.relay.tourForm.create(env.relay.tourForm.create)).fuccess
    }
  }

  def create =
    AuthOrScopedBody(_.Study.Write)(
      auth = implicit ctx =>
        me =>
          NoLameOrBot {
            env.relay.tourForm.create
              .bindFromRequest()(ctx.body, formBinding)
              .fold(
                err => BadRequest(html.relay.tourForm.create(err)).fuccess,
                setup =>
                  env.relay.api.tourCreate(setup, me) map { tour =>
                    Redirect(routes.RelayRound.form(tour.id.value)).flashSuccess
                  }
              )
          },
      scoped = req =>
        me =>
          NoLameOrBot(me) {
            env.relay.tourForm.create
              .bindFromRequest()(req, formBinding)
              .fold(
                err => BadRequest(apiFormError(err)).fuccess,
                setup =>
                  JsonOk {
                    env.relay.api.tourCreate(setup, me) map { tour =>
                      env.relay.jsonView(tour.withRounds(Nil), withUrls = true)
                    }
                  }
              )
          }
    )

  def edit(id: String) = Auth { implicit ctx => me =>
    WithTourCanUpdate(id) { tour =>
      Ok(html.relay.tourForm.edit(tour, env.relay.tourForm.edit(tour))).fuccess
    }
  }

  def update(id: String) =
    AuthOrScopedBody(_.Study.Write)(
      auth = implicit ctx =>
        me =>
          WithTourCanUpdate(id) { tour =>
            env.relay.tourForm
              .edit(tour)
              .bindFromRequest()(ctx.body, formBinding)
              .fold(
                err => BadRequest(html.relay.tourForm.edit(tour, err)).fuccess,
                setup =>
                  env.relay.api.tourUpdate(tour, setup, me) inject
                    Redirect(routes.RelayTour.redirectOrApiTour(tour.slug, tour.id.value))
              )
          },
      scoped = implicit req =>
        me =>
          env.relay.api tourById TourModel.Id(id) flatMap {
            _ ?? { tour =>
              env.relay.api.canUpdate(me, tour) flatMap {
                _ ?? env.relay.tourForm
                  .edit(tour)
                  .bindFromRequest()
                  .fold(
                    err => BadRequest(apiFormError(err)).fuccess,
                    setup => env.relay.api.tourUpdate(tour, setup, me) inject jsonOkResult
                  )
              }
            }
          }
    )

  def redirectOrApiTour(@nowarn("msg=unused") slug: String, anyId: String) = Open { implicit ctx =>
    env.relay.api byIdWithTour RoundModel.Id(anyId) flatMap {
      case Some(rt) => Redirect(rt.path).fuccess // BC old broadcast URLs
      case None =>
        env.relay.api tourById TourModel.Id(anyId) flatMap {
          _ ?? { tour =>
            render.async {
              case Accepts.Json() =>
                JsonOk {
                  env.relay.api.withRounds(tour) map { trs =>
                    env.relay.jsonView(trs, withUrls = true)
                  }
                }
              case _ => redirectToTour(tour)
            }
          }
        }
    }
  }

  def apiIndex =
    Action.async { implicit req =>
      apiC.jsonStream {
        env.relay.api
          .officialTourStream(MaxPerSecond(20), getInt("nb", req) | 20)
          .map(env.relay.jsonView.apply(_, withUrls = true))
      }.fuccess
    }

  private def redirectToTour(tour: TourModel)(implicit ctx: Context): Fu[Result] =
    env.relay.api.activeTourNextRound(tour) orElse env.relay.api.tourLastRound(tour) flatMap {
      case None =>
        ctx.me.?? { env.relay.api.canUpdate(_, tour) } flatMap {
          _ ?? Redirect(routes.RelayRound.form(tour.id.value)).fuccess
        }
      case Some(round) => Redirect(round.withTour(tour).path).fuccess
    }

  private def WithTour(id: String)(
      f: TourModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.relay.api tourById TourModel.Id(id))(f)

  private def WithTourCanUpdate(id: String)(
      f: TourModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    WithTour(id) { tour =>
      ctx.me.?? { env.relay.api.canUpdate(_, tour) } flatMap {
        _ ?? f(tour)
      }
    }
}
