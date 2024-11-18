package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.recap.{ Recap as RecapModel }
import lila.recap.Recap.Availability

final class Recap(env: Env) extends LilaController(env):

  def home = Secure(_.Beta) { _ ?=> me ?=>
    Redirect(routes.Recap.user(me.username))
  }

  def user(username: UserStr) = RecapPage(username) { _ ?=> user => r =>
    Ok.page(views.recap.home(r, user))
  }

  private def RecapPage(
      username: UserStr
  )(f: Context ?=> UserModel => Availability => Fu[Result]): EssentialAction =
    Secure(_.Beta) { ctx ?=> me ?=>
      def proceed(user: lila.core.user.User) = for
        av  <- env.recap.api.availability(user.id)
        res <- f(user)(av)
      yield res
      if me.is(username) then proceed(me)
      else
        Found(env.user.api.byId(username)): user =>
          isGranted(_.SeeInsight).so(proceed(user))
    }
