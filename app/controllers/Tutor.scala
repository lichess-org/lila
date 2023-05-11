package controllers

import play.api.mvc.Result
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.rating.{ Perf, PerfType }
import lila.user.{ User as UserModel }
import lila.tutor.{ TutorFullReport, TutorPerfReport, TutorQueue }
import lila.common.LilaOpeningFamily

final class Tutor(env: Env) extends LilaController(env):

  def home = Secure(_.Beta) { _ ?=> holder =>
    Redirect(routes.Tutor.user(holder.user.username)).toFuccess
  }

  def user(username: UserStr) = TutorPage(username) { ctx ?=> me => av =>
    Ok(views.html.tutor.home(av, me)).toFuccess
  }

  def perf(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { ctx ?=> me => _ => perf =>
    Ok(views.html.tutor.perf(perf, me)).toFuccess: Fu[Result]
  }

  def openings(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> me => _ => perf =>
    Ok(views.html.tutor.openings(perf, me)).toFuccess
  }

  def opening(username: UserStr, perf: Perf.Key, colName: String, opName: String) =
    TutorPerfPage(username, perf) { _ ?=> me => _ => perf =>
      chess.Color
        .fromName(colName)
        .fold(Redirect(routes.Tutor.openings(me.username, perf.perf.key)).toFuccess) { color =>
          LilaOpeningFamily
            .find(opName)
            .flatMap(perf.openings(color).find)
            .fold(Redirect(routes.Tutor.openings(me.username, perf.perf.key)).toFuccess) { family =>
              env.puzzle.opening.find(family.family.key) map { puzzle =>
                Ok(views.html.tutor.opening(perf, family, color, me, puzzle))
              }
            }
        }
    }

  def skills(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> me => _ => perf =>
    Ok(views.html.tutor.skills(perf, me)).toFuccess
  }

  def phases(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> me => _ => perf =>
    Ok(views.html.tutor.phases(perf, me)).toFuccess
  }

  def time(username: UserStr, perf: Perf.Key) = TutorPerfPage(username, perf) { _ ?=> me => _ => perf =>
    Ok(views.html.tutor.time(perf, me)).toFuccess
  }

  def refresh(username: UserStr) = TutorPageAvailability(username) { _ ?=> user => availability =>
    env.tutor.api.request(user, availability) >> redirHome(user)
  }

  private def TutorPageAvailability(
      username: UserStr
  )(f: Context ?=> UserModel => TutorFullReport.Availability => Fu[Result]) =
    Secure(_.Beta) { ctx ?=> holder =>
      def proceed(user: UserModel) = env.tutor.api.availability(user) flatMap f(user)
      if holder.user is username then proceed(holder.user)
      else
        env.user.repo.byId(username) flatMap {
          _.fold(notFound): user =>
            if isGranted(_.SeeInsight) then proceed(user)
            else
              (user.enabled.yes ?? env.clas.api.clas.isTeacherOf(holder.id, user.id)) flatMap {
                if _ then proceed(user) else notFound
              }
        }
    }

  private def TutorPage(
      username: UserStr
  )(f: Context ?=> UserModel => TutorFullReport.Available => Fu[Result]) =
    TutorPageAvailability(username) { ctx ?=> user => availability =>
      availability match
        case TutorFullReport.InsufficientGames =>
          BadRequest(views.html.tutor.empty.insufficientGames(user)).toFuccess
        case TutorFullReport.Empty(in: TutorQueue.InQueue) =>
          env.tutor.queue.waitingGames(user) map { waitGames =>
            Accepted(views.html.tutor.empty.queued(in, user, waitGames))
          }
        case TutorFullReport.Empty(_)             => Accepted(views.html.tutor.empty.start(user)).toFuccess
        case available: TutorFullReport.Available => f(user)(available)
    }

  private def TutorPerfPage(username: UserStr, perf: Perf.Key)(
      f: Context ?=> UserModel => TutorFullReport.Available => TutorPerfReport => Fu[Result]
  ) =
    TutorPage(username) { ctx ?=> me => availability =>
      PerfType(perf).fold(redirHome(me)): perf =>
        availability match
          case full @ TutorFullReport.Available(report, _) =>
            report(perf).fold(redirHome(me)):
              f(me)(full)
    }

  private def redirHome(user: UserModel) =
    Redirect(routes.Tutor.user(user.username)).toFuccess
