package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.recap.{ Recap as RecapModel }
import lila.recap.Recap.Availability

final class Recap(env: Env) extends LilaController(env):

  def home = Auth { _ ?=> me ?=>
    Redirect(routes.Recap.user(me.username))
  }

  def user(username: UserStr) = RecapPage(username) { _ ?=> user => data =>
    negotiate(
      html = Ok.page(views.recap.home(data, user)),
      json = data match
        case Availability.Available(data) => Ok(data).toFuccess
        case Availability.Queued(_)       => env.recap.api.awaiter(user).map(Ok(_))
    )
  }

  private def RecapPage(
      username: UserStr
  )(f: Context ?=> UserModel => Availability => Fu[Result]): EssentialAction =
    Auth { ctx ?=> me ?=>
      def proceed(user: lila.core.user.User) = for
        av  <- env.recap.api.availability(user)
        res <- f(using ctx.updatePref(_.forceDarkBg))(user)(av)
      yield res
      if me.is(username) then proceed(me)
      else
        Found(env.user.api.byId(username)): user =>
          val canView = isGranted(_.SeeInsight) || !env.net.isProd
          canView.so(proceed(user))
    }
