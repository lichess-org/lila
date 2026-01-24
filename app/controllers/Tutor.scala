package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.LilaOpeningFamily
import lila.core.perf.UserWithPerfs
import lila.rating.PerfType
import lila.tutor.{ TutorFullReport, TutorPerfReport, TutorQueue }

final class Tutor(env: Env) extends LilaController(env):

  def home = Secure(_.Beta) { _ ?=> me ?=>
    Redirect(routes.Tutor.user(me.username))
  }

  def user(username: UserStr) = TutorPage(username) { _ ?=> user => av =>
    Ok.page(views.tutor.home(av, user))
  }

  def perf(username: UserStr, perf: PerfKey) = TutorPerfPage(username, perf) { _ ?=> user => full => perf =>
    Ok.page(views.tutor.perf(full.report, perf, user))
  }

  def openings(username: UserStr, perf: PerfKey) =
    TutorPerfPage(username, perf) { _ ?=> user => full => perf =>
      Ok.page(views.tutor.openingUi.openings(full.report, perf, user))
    }

  def opening(username: UserStr, perf: PerfKey, color: Color, opName: String) =
    TutorPerfPage(username, perf) { _ ?=> user => full => perf =>
      LilaOpeningFamily
        .find(opName)
        .flatMap(perf.openings(color).find)
        .fold(Redirect(routes.Tutor.openings(user.username, perf.perf.key)).toFuccess): family =>
          env.puzzle.opening.find(family.family.key).flatMap { puzzle =>
            Ok.page(views.tutor.opening(full.report, perf, family, color, user, puzzle))
          }
    }

  def skills(username: UserStr, perf: PerfKey) = TutorPerfPage(username, perf) { _ ?=> user => full => perf =>
    Ok.page(views.tutor.perf.skills(full.report, perf, user))
  }

  def phases(username: UserStr, perf: PerfKey) = TutorPerfPage(username, perf) { _ ?=> user => full => perf =>
    Ok.page(views.tutor.perf.phases(full.report, perf, user))
  }

  def time(username: UserStr, perf: PerfKey) = TutorPerfPage(username, perf) { _ ?=> user => full => perf =>
    Ok.page(views.tutor.perf.time(full.report, perf, user))
  }

  def refresh(username: UserStr) = TutorPageAvailability(username) { _ ?=> user => availability =>
    env.tutor.api.request(user, availability).inject(redirHome(user))
  }

  private def TutorPageAvailability(
      username: UserStr
  )(f: Context ?=> UserModel => TutorFullReport.Availability => Fu[Result]): EssentialAction =
    Secure(_.Beta) { ctx ?=> me ?=>
      def proceed(user: UserWithPerfs) = env.tutor.api.availability(user).flatMap(f(user.user))
      if me.is(username) then env.user.api.withPerfs(me.value).flatMap(proceed)
      else
        Found(env.user.api.withPerfs(username)): user =>
          if isGranted(_.SeeInsight) then proceed(user)
          else
            user.enabled.yes
              .so(env.clas.api.clas.isTeacherOf(me, user.id))
              .flatMap:
                if _ then proceed(user) else notFound
    }

  private def TutorPage(
      username: UserStr
  )(f: Context ?=> UserModel => TutorFullReport.Available => Fu[Result]): EssentialAction =
    TutorPageAvailability(username) { _ ?=> user => availability =>
      availability match
        case TutorFullReport.InsufficientGames =>
          BadRequest.page(views.tutor.home.empty.insufficientGames(user))
        case TutorFullReport.Empty(in: TutorQueue.InQueue) =>
          for
            waitGames <- env.tutor.queue.waitingGames(user)
            user <- env.user.api.withPerfs(user)
            page <- renderPage(views.tutor.home.empty.queued(in, user, waitGames))
          yield Accepted(page)
        case TutorFullReport.Empty(_) => Accepted.page(views.tutor.home.empty.start(user))
        case available: TutorFullReport.Available => f(user)(available)
    }

  private def TutorPerfPage(username: UserStr, perf: PerfKey)(
      f: Context ?=> UserModel => TutorFullReport.Available => TutorPerfReport => Fu[Result]
  ): EssentialAction =
    TutorPage(username) { _ ?=> user => availability =>
      availability match
        case full @ TutorFullReport.Available(report, _) =>
          report(perf).fold(redirHome(user).toFuccess):
            f(user)(full)
    }

  private def redirHome(user: UserModel) = Redirect(routes.Tutor.user(user.username))
