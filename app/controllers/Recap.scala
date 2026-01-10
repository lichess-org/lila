package controllers

import play.api.mvc.*

import lila.app.*
import lila.recap.Recap.Availability
import lila.plan.LichessJson.given

final class Recap(env: Env) extends LilaController(env):

  def home = Auth { _ ?=> me ?=>
    Redirect(routes.Recap.user(me.username))
  }

  def user(username: UserStr) = RecapPage(username) { _ ?=> user => data =>
    negotiate(
      html = Ok.page(views.recap.home(data, user)),
      json = data match
        case Availability.Available(data) => Ok(data).toFuccess
        case Availability.Queued(_) => env.recap.api.awaiter(user)(getCosts).map(Ok(_))
    )
  }

  private def RecapPage(
      username: UserStr
  )(f: Context ?=> UserModel => Availability => Fu[Result]): EssentialAction =
    Auth { ctx ?=> me ?=>
      val availableAt = instantOf(lila.recap.yearToRecap, 12, 15, 0, 0)
      if env.mode.isProd && nowInstant.isBefore(availableAt) then
        Ok.page(views.recap.notAvailable(lila.recap.yearToRecap))
      else
        def proceed(user: lila.core.user.User) = for
          av <- env.recap.api.availability(user)(getCosts)
          res <- f(using ctx.updatePref(_.forceDarkBg))(user)(av)
        yield res
        if me.is(username) then proceed(me)
        else if isGranted(_.SeeInsight) || !env.mode.isProd then Found(env.user.api.byId(username))(proceed)
        else Redirect(routes.Recap.home).toFuccess
    }

  private def getCosts(using ctx: Context) =
    env.plan.currencyApi.yearlyCostsFor(env.security.geoIP(ctx.ip).flatMap(_.countryCode))
