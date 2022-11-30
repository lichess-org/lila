package controllers

import play.api.mvc.Result
import scala.concurrent.duration.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.rating.{ Perf, PerfType }
import lila.user.{ User as UserModel }
import lila.tutor.{ TutorFullReport, TutorPerfReport, TutorQueue }
import lila.common.LilaOpeningFamily

final class Tutor(env: Env) extends LilaController(env):

  def home =
    Secure(_.Beta) { implicit ctx => holder =>
      Redirect(routes.Tutor.user(holder.user.username)).toFuccess
    }

  def user(username: UserStr) = TutorPage(username) { implicit ctx => me => av =>
    Ok(views.html.tutor.home(av, me)).toFuccess
  }

  def perf(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.perf(report, perf, me)).toFuccess: Fu[Result]
  }

  def openings(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.openings(report, perf, me)).toFuccess
  }

  def opening(username: UserStr, perf: Perf.Key, colName: String, opName: String) =
    TutorPerfPage(username, perf) { implicit ctx => me => report => perf =>
      chess.Color
        .fromName(colName)
        .fold(Redirect(routes.Tutor.openings(me.username, perf.perf.key)).toFuccess) { color =>
          LilaOpeningFamily
            .find(opName)
            .flatMap(perf.openings(color).find)
            .fold(Redirect(routes.Tutor.openings(me.username, perf.perf.key)).toFuccess) { family =>
              env.puzzle.opening.find(family.family.key) map { puzzle =>
                Ok(views.html.tutor.opening(report, perf, family, color, me, puzzle))
              }
            }
        }
    }

  def phases(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.phases(report, perf, me)).toFuccess
  }

  def time(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) {
    implicit ctx => me => report => perf =>
      Ok(views.html.tutor.time(report, perf, me)).toFuccess
  }

  def refresh(username: UserStr) = TutorPageAvailability(username) { ctx => user => availability =>
    env.tutor.api.request(user, availability) >> redirHome(user)
  }

  private def TutorPageAvailability(
      username: UserStr
  )(f: Context => UserModel => TutorFullReport.Availability => Fu[Result]) =
    Secure(_.Beta) { implicit ctx => holder =>
      def proceed(user: UserModel) = env.tutor.api.availability(user) flatMap f(ctx)(user)
      if (!holder.user.is(username))
        if (isGranted(_.SeeInsight)) env.user.repo.byId(username) flatMap { _ ?? proceed }
        else redirHome(holder.user)
      else proceed(holder.user)
    }

  private def TutorPage(
      username: UserStr
  )(f: Context => UserModel => TutorFullReport.Available => Fu[Result]) =
    TutorPageAvailability(username) { implicit ctx => user => availability =>
      availability match
        case TutorFullReport.InsufficientGames =>
          BadRequest(views.html.tutor.empty.insufficientGames).toFuccess
        case TutorFullReport.Empty(in: TutorQueue.InQueue) =>
          Accepted(views.html.tutor.empty.queued(in, user)).toFuccess
        case TutorFullReport.Empty(_)             => Accepted(views.html.tutor.empty.start(user)).toFuccess
        case available: TutorFullReport.Available => f(ctx)(user)(available)
    }

  private def TutorPerfPage(username: UserStr, perf: Perf.Key)(
      f: Context => UserModel => TutorFullReport.Available => TutorPerfReport => Fu[Result]
  ) =
    TutorPage(username) { ctx => me => availability =>
      PerfType(perf).fold(redirHome(me)) { perf =>
        availability match
          case full @ TutorFullReport.Available(report, _) =>
            report(perf).fold(redirHome(me)) { perfReport =>
              f(ctx)(me)(full)(perfReport)
            }
      }
    }

  private def redirHome(user: UserModel) =
    Redirect(routes.Tutor.user(user.username)).toFuccess
