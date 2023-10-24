package controllers

import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.rating.{ Perf, PerfType }
import lila.user.{ User as UserModel }
import lila.tutor.{ TutorFullReport, TutorPeriodReport, TutorPerfReport, TutorQueue }
import lila.tutor.TutorPeriodReport.Id
import lila.common.LilaOpeningFamily

final class Tutor(env: Env) extends LilaController(env):

  def home = Secure(_.Beta) { _ ?=> me ?=>
    Redirect(routes.Tutor.user(me.username))
  }

  def user(username: UserStr) = TutorPage(username) { _ ?=> reports =>
    Ok.page(views.html.tutor.home(reports))
  }

  def perf(username: UserStr, perf: Perf.Key, id: Id) =
    TutorReportPage(username, id) { _ ?=> reports => report =>
      Ok.page(views.html.tutor.perf(reports, report))
    }

  def openings(username: UserStr, perf: Perf.Key, id: Id) =
    TutorReportPage(username, id) { _ ?=> reports => report =>
      Ok.page(views.html.tutor.openings(reports, report))
    }

  def opening(username: UserStr, perf: Perf.Key, id: Id, colName: String, opName: String) =
    TutorReportPage(username, id) { _ ?=> reports => report =>
      chess.Color
        .fromName(colName)
        .fold(Redirect(routes.Tutor.openings(reports.user.username, report.perf.key)).toFuccess): color =>
          LilaOpeningFamily
            .find(opName)
            .flatMap(report.openings(color).find)
            .fold(Redirect(routes.Tutor.openings(reports.user.username, report.perf.key)).toFuccess):
              family =>
                env.puzzle.opening.find(family.family.key) flatMap { puzzle =>
                  Ok.page(views.html.tutor.opening(perf, family, color, user, puzzle))
                }
    }

  def skills(username: UserStr, perf: Perf.Key, id: Id) =
    TutorReportPage(username, id) { _ ?=> reports => report =>
      Ok.page(views.html.tutor.skills(reports, report))
    }

  def phases(username: UserStr, perf: Perf.Key, id: Id) =
    TutorReportPage(username, id) { _ ?=> reports => report =>
      Ok.page(views.html.tutor.phases(reports, report))
    }

  def time(username: UserStr, perf: Perf.Key, id: Id) =
    TutorReportPage(username, id) { _ ?=> reports => report =>
      Ok.page(views.html.tutor.time(reports, report))
    }

  private def TutorPage(
      username: UserStr
  )(f: Context ?=> TutorPeriodReport.UserReports => Fu[Result]): EssentialAction =
    Secure(_.Beta) { ctx ?=> me ?=>
      def proceed(user: UserModel.WithPerfs) = env.tutor.api.reports(user).flatMap(f(user.user))
      if me is username then env.user.api.withPerfs(me.value).flatMap(proceed)
      else
        Found(env.user.api.withPerfs(username)): user =>
          if isGranted(_.SeeInsight) then proceed(user)
          else
            (user.enabled.yes so env.clas.api.clas.isTeacherOf(me, user.id)) flatMap {
              if _ then proceed(user) else notFound
            }
    }

  private def TutorReportPage(username: UserStr, id: Id)(
      f: Context ?=> TutorPeriodReport.UserReports => TutorPeriodReport => Fu[Result]
  ): EssentialAction =
    TutorPage(username) { _ ?=> reports =>
      env.tutor.api
        .get(reports.user, id)
        .flatMap:
          _.fold(redirHome(reports.user)): report =>
            f(user)(reports)(report)
    }

  private def redirHome(user: UserModel) = Redirect(routes.Tutor.user(user.username))
