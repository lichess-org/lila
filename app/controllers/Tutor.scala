package controllers

import play.api.mvc.Result
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.rating.PerfType
import lila.user.{ User => UserModel }
import lila.tutor.{ TutorPerfReport, TutorReport }

final class Tutor(env: Env) extends LilaController(env) {

  def user(username: String) = TutorPage(username) { implicit ctx => me =>
    env.tutor.builder.getOrMake(me, ctx.ip) map { report =>
      Ok(views.html.tutor.home(report, me))
    }
  }

  def perf(username: String, perf: String) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.perf(report, perf, me)).fuccess
  }

  def openings(username: String, perf: String) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.openings(report, perf, me)).fuccess
  }

  def phases(username: String, perf: String) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.phases(report, perf, me)).fuccess
  }

  private def TutorPage(username: String)(f: Context => UserModel => Fu[Result]) =
    Secure(_.Beta) { ctx => holder =>
      if (!holder.user.is(username)) redirHome(holder.user)
      else f(ctx)(holder.user)
    }

  private def TutorPerfPage(username: String, perf: String)(
      f: Context => UserModel => TutorReport => TutorPerfReport => Fu[Result]
  ) =
    TutorPage(username) { ctx => me =>
      PerfType(perf).fold(redirHome(me)) { perf =>
        env.tutor.builder.get(me) flatMap {
          _.fold(redirHome(me)) { report =>
            report(perf).fold(redirHome(me)) { perfReport =>
              f(ctx)(me)(report)(perfReport)
            }
          }
        }
      }
    }

  private def redirHome(user: UserModel) =
    Redirect(routes.Tutor.user(user.username)).fuccess
}
