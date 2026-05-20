package controllers

import play.api.data.Form
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.LilaOpeningFamily
import lila.rating.PerfType
import lila.tutor.{ TutorFullReport, TutorPerfReport, TutorConfig, TutorAvailability }

final class Tutor(env: Env) extends LilaController(env):

  def home = Auth { _ ?=> me ?=>
    Redirect(routes.Tutor.user(me.username))
  }

  def user(username: UserStr) = Auth { _ ?=> _ ?=>
    WithUser(username): user =>
      renderUser(user, TutorConfig.form.default)
  }

  private def renderUser(user: UserModel, form: Form[?])(using Context) =
    for
      withPerfs <- env.user.api.withPerfs(user)
      av <- env.tutor.api.availability(withPerfs)
      res <- av match
        case TutorAvailability.InsufficientGames =>
          Ok.page(views.tutor.home.insufficientGames(user.id))
        case TutorAvailability.Available(home) =>
          home.previews.headOption.ifTrue(getBool("waiting") && home.awaiting.isEmpty) match
            case Some(done) => Redirect(done.config.url.root).toFuccess
            case None => Ok.page(views.tutor.home(home, form, env.tutor.limit.status(user.id)))
    yield res

  def report(username: UserStr, range: String) = TutorReport(username, range) { _ ?=> full =>
    Ok.page(views.tutor.report(full))
  }

  def perf(username: UserStr, range: String, perf: PerfKey) = TutorPerfPage(username, range, perf) {
    _ ?=> full => perf =>
      Ok.page(views.tutor.perf(full, perf))
  }

  def angle(username: UserStr, range: String, perf: PerfKey, angle: String) =
    TutorPerfPage(username, range, perf) { _ ?=> full => perf =>
      angle match
        case "skills" => Ok.page(views.tutor.perf.skills(full, perf))
        case "phases" => Ok.page(views.tutor.perf.phases(full, perf))
        case "time" => Ok.page(views.tutor.perf.time(full, perf))
        case "pieces" => Ok.page(views.tutor.perf.pieces(full, perf))
        case "opening" => Ok.page(views.tutor.openingUi.openings(full, perf))
        case _ => notFound
    }

  def opening(username: UserStr, range: String, perf: PerfKey, color: Color, opName: String) =
    TutorPerfPage(username, range, perf) { _ ?=> full => perf =>
      LilaOpeningFamily
        .find(opName)
        .flatMap(perf.openings(color).find)
        .fold(Redirect(full.url.angle(perf.perf, "opening")).toFuccess): family =>
          Ok.page(views.tutor.openingUi.opening(full, perf, family, color))
    }

  def compute(username: UserStr) = AuthBody { _ ?=> _ ?=>
    WithUser(username): user =>
      bindForm(TutorConfig.form.dates)(
        err => renderUser(user, err),
        dates =>
          val config = dates.config(user.id)
          env.tutor.api
            .get(config)
            .flatMap:
              case Some(report) => Redirect(report.url.root).toFuccess
              case _ if env.tutor.limit.zero(user.id)(true) =>
                for _ <- env.tutor.queue.enqueue(config)
                yield Redirect(routes.Tutor.user(user.username))
              case _ => Redirect(routes.Tutor.user(user.username)).toFuccess
      )
  }

  def delete(username: UserStr, range: String) = TutorReport(username, range) { _ ?=> full =>
    for _ <- env.tutor.api.delete(full.config)
    yield Redirect(routes.Tutor.user(username))
  }

  private def WithUser(username: UserStr)(f: UserModel => Fu[Result])(using
      me: Me
  )(using Context): Fu[Result] =
    val user: Fu[Option[UserModel]] =
      if me.is(username) then fuccess(me.some)
      else
        env.user.api
          .byId(username.id)
          .flatMapz: user =>
            for canSee <- fuccess(isGranted(_.SeeInsight)) >>|
                user.enabled.yes.so(env.clas.api.clas.isTeacherOf(me, user.id))
            yield Option.when(canSee)(user)
    Found(user)(f)

  private def TutorReport(username: UserStr, range: String)(
      f: Context ?=> TutorFullReport => Fu[Result]
  ): EssentialAction =
    Auth { _ ?=> _ ?=>
      WithUser(username): _ =>
        TutorConfig
          .parse(username.id, range)
          .so(env.tutor.api.get)
          .flatMap:
            case None => Redirect(routes.Tutor.user(username)).toFuccess
            case Some(full) => f(full)
    }

  private def TutorPerfPage(username: UserStr, range: String, perf: PerfKey)(
      f: Context ?=> TutorFullReport => TutorPerfReport => Fu[Result]
  ): EssentialAction =
    TutorReport(username, range) { _ ?=> full =>
      full(perf).fold(Redirect(full.url.root).toFuccess)(f(full))
    }
