package controllers

import play.api.mvc.Result
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.rating.PerfType
import lila.user.{ User => UserModel }
import lila.tutor.{ TutorPerfReport, TutorReport }
import lila.common.LilaOpeningFamily

final class Tutor(env: Env) extends LilaController(env) {

  def home =
    Secure(_.Beta) { implicit ctx => holder =>
      Redirect(routes.Tutor.user(holder.user.username)).fuccess
    }

  def user(username: String) = TutorPage(username) { implicit ctx => me =>
    env.tutor.api.latest(me) map { report =>
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

  def opening(username: String, perf: String, colName: String, opName: String) =
    TutorPerfPage(username, perf) { implicit ctx => me => report => perf =>
      chess.Color
        .fromName(colName)
        .fold(Redirect(routes.Tutor.openings(me.username, perf.perf.key)).fuccess) { color =>
          LilaOpeningFamily
            .find(opName)
            .fold(Redirect(routes.Tutor.openings(me.username, perf.perf.key)).fuccess) { family =>
              Ok(views.html.tutor.opening(report, perf, family as color, me)).fuccess
            }
        }
    }

  def phases(username: String, perf: String) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.phases(report, perf, me)).fuccess
  }

  private def TutorPage(username: String)(f: Context => UserModel => Fu[Result]) =
    Secure(_.Beta) { implicit ctx => holder =>
      if (!holder.user.is(username)) {
        if (isGranted(_.SeeInsight)) env.user.repo.named(username) flatMap { _ ?? f(ctx) }
        else redirHome(holder.user)
      } else f(ctx)(holder.user)
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
