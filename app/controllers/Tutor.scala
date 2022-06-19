package controllers

import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.rating.PerfType

final class Tutor(env: Env) extends LilaController(env) {

  def user(username: String) = Secure(_.Beta) { implicit ctx => holder =>
    val me = holder.user
    if (!me.is(username)) redirHome(me)
    else
      env.tutor.builder.getOrMake(me, ctx.ip) map { report =>
        Ok(views.html.tutor.home(report))
      }
  }

  def perf(username: String, perf: String) = Secure(_.Beta) { implicit ctx => holder =>
    val me = holder.user
    if (!me.is(username)) redirHome(me)
    else
      PerfType(perf).fold(redirHome(me)) { pt =>
        env.tutor.builder.get(me) flatMap {
          _.fold(redirHome(me)) { report =>
            report(pt).fold(redirHome(me)) { perfReport =>
              Ok(views.html.tutor.perf(report, perfReport)).fuccess
            }
          }
        }
      }
  }

  private def redirHome(user: lila.user.User) =
    Redirect(routes.Tutor.user(user.username)).fuccess
}
