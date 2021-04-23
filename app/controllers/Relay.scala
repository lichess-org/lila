package controllers

import play.api.data.Form
import play.api.mvc._
import scala.annotation.nowarn

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.relay.{ Relay => RelayModel, RelayTour => TourModel, RelayForm }
import lila.user.{ User => UserModel }
import views._

final class Relay(
    env: Env,
    studyC: => Study,
    apiC: => Api
) extends LilaController(env) {

  def index(page: Int) =
    Open { implicit ctx =>
      Reasonable(page) {
        for {
          fresh <- (page == 1).??(env.relay.api.fresh(ctx.me) map some)
          pager <- env.relay.pager.finished(ctx.me, page)
        } yield Ok(html.relay.index(fresh, pager, routes.Relay.index()))
      }
    }

  def relayForm(tourId: String) =
    Auth { implicit ctx => me =>
      NoLameOrBot {
        WithTour(tourId) { tour =>
          (tour.owner == me.id) ?? {
            Ok(html.relay.form.relayCreate(env.relay.forms.create, tour)).fuccess
          }
        }
      }
    }

  def relayCreate(tourId: String) =
    AuthOrScopedBody(_.Study.Write)(
      auth = implicit ctx =>
        me =>
          NoLameOrBot {
            WithTour(tourId) { tour =>
              (tour.owner == me.id) ?? {
                env.relay.forms.create
                  .bindFromRequest()(ctx.body, formBinding)
                  .fold(
                    err => BadRequest(html.relay.form.relayCreate(err, tour)).fuccess,
                    setup =>
                      env.relay.api.create(setup, me, tour) map { relay =>
                        Redirect(showRoute(relay withTour tour))
                      }
                  )
              }
            }
          },
      scoped = req =>
        me =>
          env.relay.api tourById TourModel.Id(tourId) flatMap {
            _ ?? { tour =>
              !(me.isBot || me.lame) ??
                env.relay.forms.create
                  .bindFromRequest()(req, formBinding)
                  .fold(
                    err => BadRequest(apiFormError(err)).fuccess,
                    setup => env.relay.api.create(setup, me, tour) map env.relay.jsonView.admin map JsonOk
                  )
            }
          }
    )

  def relayEdit(id: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.relay.api.byIdAndContributor(id, me)) { rt =>
        Ok(html.relay.form.relayEdit(rt, env.relay.forms.edit(rt.relay))).fuccess
      }
    }

  def relayUpdate(id: String) =
    AuthOrScopedBody(_.Study.Write)(
      auth = implicit ctx =>
        me =>
          doUpdate(id, me)(ctx.body) flatMap {
            case None => notFound
            case Some(res) =>
              res
                .fold(
                  { case (old, err) => BadRequest(html.relay.form.relayEdit(old, err)) },
                  rt => Redirect(showRoute(rt))
                )
                .fuccess
          },
      scoped = req =>
        me =>
          doUpdate(id, me)(req) map {
            case None => NotFound(jsonError("No such broadcast"))
            case Some(res) =>
              res.fold(
                { case (_, err) => BadRequest(apiFormError(err)) },
                rt => JsonOk(env.relay.jsonView.admin(rt.relay))
              )
          }
    )

  private def doUpdate(id: String, me: UserModel)(implicit
      req: Request[_]
  ): Fu[Option[Either[(RelayModel.WithTour, Form[RelayForm.Data]), RelayModel.WithTour]]] =
    env.relay.api.byIdAndContributor(id, me) flatMap {
      _ ?? { rt =>
        env.relay.forms
          .edit(rt.relay)
          .bindFromRequest()
          .fold(
            err => fuccess(Left(rt -> err)),
            data =>
              env.relay.api.update(rt.relay) { data.update(_, me) }.dmap(_ withTour rt.tour) dmap Right.apply
          ) dmap some
      }
    }

  def reset(@nowarn("cat=unused") slug: String, id: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.relay.api.byIdAndContributor(id, me)) { rt =>
        env.relay.api.reset(rt.relay, me) inject Redirect(showRoute(rt))
      }
    }

  def showRelay(ts: String, rs: String, id: String) =
    OpenOrScoped(_.Study.Read)(
      open = implicit ctx => {
        pageHit
        WithRelay(ts, rs, id) { rt =>
          val sc =
            if (rt.relay.sync.ongoing)
              env.study.chapterRepo relaysAndTagsByStudyId rt.relay.studyId flatMap { chapters =>
                chapters.find(_.looksAlive) orElse chapters.headOption match {
                  case Some(chapter) => env.study.api.byIdWithChapter(rt.relay.studyId, chapter.id)
                  case None          => env.study.api byIdWithChapter rt.relay.studyId
                }
              }
            else env.study.api byIdWithChapter rt.relay.studyId
          sc flatMap { _ ?? { doShow(rt, _) } }
        }
      },
      scoped = _ =>
        me =>
          env.relay.api.byIdAndContributor(id, me) map {
            case None     => NotFound(jsonError("No such broadcast"))
            case Some(rt) => JsonOk(env.relay.jsonView.admin(rt.relay))
          }
    )

  def chapter(ts: String, rs: String, id: String, chapterId: String) =
    Open { implicit ctx =>
      WithRelay(ts, rs, id) { rt =>
        env.study.api.byIdWithChapter(rt.relay.studyId, chapterId) flatMap {
          _ ?? { doShow(rt, _) }
        }
      }
    }

  def cloneRelay(id: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.relay.api.byIdAndContributor(id, me)) { rt =>
        env.relay.api.cloneRelay(rt, me) map { newRelay =>
          Redirect(routes.Relay.relayEdit(newRelay.id.value))
        }
      }
    }

  def push(id: String) =
    ScopedBody(parse.tolerantText)(Seq(_.Study.Write)) { req => me =>
      env.relay.api.byIdAndContributor(id, me) flatMap {
        case None     => notFoundJson()
        case Some(rt) => env.relay.push(rt, req.body) inject jsonOkResult
      }
    }

  def apiIndex =
    Action.async { implicit req =>
      apiC.jsonStream {
        env.relay.api.officialStream(MaxPerSecond(20), getInt("nb", req) | 20)
      }.fuccess
    }

  private def WithRelay(ts: String, rs: String, id: String)(
      f: RelayModel.WithTour => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.relay.api byIdWithTour id) { rt =>
      if (rt.tour.slug != ts) Redirect(showRoute(rt)).fuccess
      if (rt.relay.slug != rs) Redirect(showRoute(rt)).fuccess
      else f(rt)
    }

  private def WithTour(slug: String, id: String)(
      f: TourModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.relay.api tourById TourModel.Id(id)) { tour =>
      if (tour.slug != slug) Redirect(showRoute(tour)).fuccess
      else f(tour)
    }

  private def WithTour(id: String)(
      f: TourModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.relay.api tourById TourModel.Id(id))(f)

  private def doShow(rt: RelayModel.WithTour, oldSc: lila.study.Study.WithChapter)(implicit
      ctx: Context
  ): Fu[Result] =
    studyC.CanViewResult(oldSc.study) {
      for {
        (sc, studyData) <- studyC.getJsonData(oldSc)
        data = env.relay.jsonView.makeData(rt.relay, studyData, ctx.userId exists sc.study.canContribute)
        chat     <- studyC.chatOf(sc.study)
        sVersion <- env.study.version(sc.study.id)
        streams  <- studyC.streamsOf(sc.study)
      } yield EnableSharedArrayBuffer(Ok(html.relay.show(rt, sc.study, data, chat, sVersion, streams)))
    }

  private def showRoute(rt: RelayModel.WithTour) =
    routes.Relay.showRelay(rt.tour.slug, rt.relay.slug, rt.relay.id.value)

  private def showRoute(t: TourModel) = routes.Relay.showTour(t.slug, t.id.value)

  implicit private def makeRelayId(id: String): RelayModel.Id           = RelayModel.Id(id)
  implicit private def makeChapterId(id: String): lila.study.Chapter.Id = lila.study.Chapter.Id(id)
}
