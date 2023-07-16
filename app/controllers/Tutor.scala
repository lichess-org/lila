package controllers

import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.rating.{ Perf, PerfType }
import lila.user.{ User as UserModel }
import lila.tutor.{ TutorFullReport, TutorPerfReport, TutorQueue }
import lila.common.LilaOpeningFamily

final class Tutor(env: Env) extends LilaController(env):

  def home = Secure(_.Beta) { _ ?=> me ?=>
    Redirect(routes.Tutor.user(me.username))
  }

  def user(username: UserStr) = TutorPage(username) { _ ?=> user => av =>
    Ok.page(views.html.tutor.home(av, user))
  }

  def perf(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.html.tutor.perf(perf, user))
  }

  def openings(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.html.tutor.openings(perf, user))
  }

  def opening(username: UserStr, perf: Perf.Key, colName: String, opName: String) =
    TutorPerfPage(username, perf) { _ ?=> user => _ => perf =>
      chess.Color
        .fromName(colName)
        .fold(Redirect(routes.Tutor.openings(user.username, perf.perf.key)).toFuccess): color =>
          LilaOpeningFamily
            .find(opName)
            .flatMap(perf.openings(color).find)
            .fold(Redirect(routes.Tutor.openings(user.username, perf.perf.key)).toFuccess): family =>
              env.puzzle.opening.find(family.family.key) flatMap { puzzle =>
                Ok.page(views.html.tutor.opening(perf, family, color, user, puzzle))
              }
    }

  def skills(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.html.tutor.skills(perf, user))
  }

  def phases(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.html.tutor.phases(perf, user))
  }

  def time(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> user => _ => perf =>
    Ok.page(views.html.tutor.time(perf, user))
  }

  def refresh(username: UserStr) = TutorPageAvailability(username) { _ ?=> user => availability =>
    env.tutor.api.request(user, availability) inject redirHome(user)
  }

  private def TutorPageAvailability(
      username: UserStr
  )(f: Context ?=> UserModel => TutorFullReport.Availability => Fu[Result]): EssentialAction =
    Secure(_.Beta) { ctx ?=> me ?=>
      def proceed(user: UserModel.WithPerfs) = env.tutor.api.availability(user).flatMap(f(user.user))
      if me is username then env.user.api.withPerfs(me.value).flatMap(proceed)
      else
        Found(env.user.api.withPerfs(username)): user =>
          if isGranted(_.SeeInsight) then proceed(user)
          else
            (user.enabled.yes so env.clas.api.clas.isTeacherOf(me, user.id)) flatMap {
              if _ then proceed(user) else notFound
            }
    }

  private def TutorPage(
      username: UserStr
  )(f: Context ?=> UserModel => TutorFullReport.Available => Fu[Result]): EssentialAction =
    TutorPageAvailability(username) { _ ?=> user => availability =>
      availability match
        case TutorFullReport.InsufficientGames =>
          BadRequest.page(views.html.tutor.empty.insufficientGames(user))
        case TutorFullReport.Empty(in: TutorQueue.InQueue) =>
          for
            waitGames <- env.tutor.queue.waitingGames(user)
            user      <- env.user.api.withPerfs(user)
            page      <- renderPage(views.html.tutor.empty.queued(in, user, waitGames))
          yield Accepted(page)
        case TutorFullReport.Empty(_)             => Accepted.page(views.html.tutor.empty.start(user))
        case available: TutorFullReport.Available => f(user)(available)
    }

  private def TutorPerfPage(username: UserStr, perf: Perf.Key)(
      f: Context ?=> UserModel => TutorFullReport.Available => TutorPerfReport => Fu[Result]
  ): EssentialAction =
    TutorPage(username) { _ ?=> user => availability =>
      PerfType(perf).fold(redirHome(user).toFuccess): perf =>
        availability match
          case full @ TutorFullReport.Available(report, _) =>
            report(perf).fold(redirHome(user).toFuccess):
              f(user)(full)
    }

  private def redirHome(user: UserModel) = Redirect(routes.Tutor.user(user.username))
