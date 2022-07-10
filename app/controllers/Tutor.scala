package controllers

import play.api.mvc.Result
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.rating.PerfType
import lila.user.{ User => UserModel }
import lila.tutor.{ TutorFullReport, TutorPerfReport, TutorQueue }
import lila.common.LilaOpeningFamily

final class Tutor(env: Env) extends LilaController(env) {

  def home =
    Secure(_.Beta) { implicit ctx => holder =>
      Redirect(routes.Tutor.user(holder.user.username)).fuccess
    }

  def user(username: String) = TutorPage(username) { implicit ctx => me => av =>
    Ok(views.html.tutor.home(av, me)).fuccess
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
            .flatMap(perf.openings(color).find)
            .fold(Redirect(routes.Tutor.openings(me.username, perf.perf.key)).fuccess) { family =>
              env.puzzle.opening.find(family.family) map { puzzle =>
                Ok(views.html.tutor.opening(report, perf, family, color, me, puzzle))
              }
            }
        }
    }

  def phases(username: String, perf: String) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.phases(report, perf, me)).fuccess
  }

  def time(username: String, perf: String) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.time(report, perf, me)).fuccess
  }

  def refresh(username: String) = TutorPageAvailability(username) { ctx => user => availability =>
    env.tutor.api.request(user, availability) >> redirHome(user)
  }

  private def TutorPageAvailability(
      username: String
  )(f: Context => UserModel => TutorFullReport.Availability => Fu[Result]) =
    Secure(_.Beta) { implicit ctx => holder =>
      def proceed(user: UserModel) = env.tutor.api.availability(user) flatMap f(ctx)(user)
      if (!holder.user.is(username)) {
        if (isGranted(_.SeeInsight)) env.user.repo.named(username) flatMap { _ ?? proceed }
        else redirHome(holder.user)
      } else proceed(holder.user)
    }

  private def TutorPage(
      username: String
  )(f: Context => UserModel => TutorFullReport.Available => Fu[Result]) =
    TutorPageAvailability(username) { implicit ctx => user => availability =>
      availability match {
        case TutorFullReport.InsufficientGames =>
          BadRequest(views.html.tutor.empty.insufficientGames).fuccess
        case TutorFullReport.Empty(in: TutorQueue.InQueue) =>
          Accepted(views.html.tutor.empty.queued(in, user)).fuccess
        case TutorFullReport.Empty(_)             => Accepted(views.html.tutor.empty.start(user)).fuccess
        case available: TutorFullReport.Available => f(ctx)(user)(available)
      }
    }

  private def TutorPerfPage(username: String, perf: String)(
      f: Context => UserModel => TutorFullReport.Available => TutorPerfReport => Fu[Result]
  ) =
    TutorPage(username) { ctx => me => availability =>
      PerfType(perf).fold(redirHome(me)) { perf =>
        availability match {
          case full @ TutorFullReport.Available(report, _) =>
            report(perf).fold(redirHome(me)) { perfReport =>
              f(ctx)(me)(full)(perfReport)
            }
        }
      }
    }

  private def redirHome(user: UserModel) =
    Redirect(routes.Tutor.user(user.username)).fuccess
}
