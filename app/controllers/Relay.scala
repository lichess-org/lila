package controllers

import play.api.data.Form
import play.api.mvc._
import scala.annotation.nowarn

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.relay.{ Relay => RelayModel, RelayForm }
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

  def form =
    Auth { implicit ctx => _ =>
      NoLameOrBot {
        Ok(html.relay.form.create(env.relay.forms.create)).fuccess
      }
    }

  def create =
    AuthOrScopedBody(_.Study.Write)(
      auth = implicit ctx =>
        me =>
          NoLameOrBot {
            env.relay.forms.create
              .bindFromRequest()(ctx.body, formBinding)
              .fold(
                err => BadRequest(html.relay.form.create(err)).fuccess,
                setup =>
                  env.relay.api.create(setup, me) map { relay =>
                    Redirect(showRoute(relay))
                  }
              )
          },
      scoped = req =>
        me =>
          !(me.isBot || me.lame) ??
            env.relay.forms.create
              .bindFromRequest()(req, formBinding)
              .fold(
                err => BadRequest(apiFormError(err)).fuccess,
                setup => env.relay.api.create(setup, me) map env.relay.jsonView.admin map JsonOk
              )
    )

  def edit(@nowarn("cat=unused") slug: String, id: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.relay.api.byIdAndContributor(id, me)) { relay =>
        Ok(html.relay.form.edit(relay, env.relay.forms.edit(relay))).fuccess
      }
    }

  def update(@nowarn slug: String, id: String) =
    AuthOrScopedBody(_.Study.Write)(
      auth = implicit ctx =>
        me =>
          doUpdate(id, me)(ctx.body) flatMap {
            case None => notFound
            case Some(res) =>
              res
                .fold(
                  { case (old, err) => BadRequest(html.relay.form.edit(old, err)) },
                  relay => Redirect(showRoute(relay))
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
                relay => JsonOk(env.relay.jsonView.admin(relay))
              )
          }
    )

  private def doUpdate(id: String, me: UserModel)(implicit
      req: Request[_]
  ): Fu[Option[Either[(RelayModel, Form[RelayForm.Data]), RelayModel]]] =
    env.relay.api.byIdAndContributor(id, me) flatMap {
      _ ?? { relay =>
        env.relay.forms
          .edit(relay)
          .bindFromRequest()
          .fold(
            err => fuccess(Left(relay -> err)),
            data => env.relay.api.update(relay) { data.update(_, me) } dmap Right.apply
          ) dmap some
      }
    }

  def reset(@nowarn("cat=unused") slug: String, id: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.relay.api.byIdAndContributor(id, me)) { relay =>
        env.relay.api.reset(relay, me) inject Redirect(showRoute(relay))
      }
    }

  def show(slug: String, id: String) =
    OpenOrScoped(_.Study.Read)(
      open = implicit ctx => {
        pageHit
        WithRelay(slug, id) { relay =>
          val sc =
            if (relay.sync.ongoing)
              env.study.chapterRepo relaysAndTagsByStudyId relay.studyId flatMap { chapters =>
                chapters.find(_.looksAlive) orElse chapters.headOption match {
                  case Some(chapter) => env.study.api.byIdWithChapter(relay.studyId, chapter.id)
                  case None          => env.study.api byIdWithChapter relay.studyId
                }
              }
            else env.study.api byIdWithChapter relay.studyId
          sc flatMap { _ ?? { doShow(relay, _) } }
        }
      },
      scoped = _ =>
        me =>
          env.relay.api.byIdAndContributor(id, me) map {
            case None        => NotFound(jsonError("No such broadcast"))
            case Some(relay) => JsonOk(env.relay.jsonView.admin(relay))
          }
    )

  def chapter(slug: String, id: String, chapterId: String) =
    Open { implicit ctx =>
      WithRelay(slug, id) { relay =>
        env.study.api.byIdWithChapter(relay.studyId, chapterId) flatMap {
          _ ?? { doShow(relay, _) }
        }
      }
    }

  def cloneRelay(@nowarn("cat=unused") slug: String, id: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.relay.api.byIdAndContributor(id, me)) { relay =>
        env.relay.api.cloneRelay(relay, me) map { newRelay =>
          Redirect(routes.Relay.edit(newRelay.slug, newRelay.id.value))
        }
      }
    }

  def push(@nowarn("cat=unused") slug: String, id: String) =
    ScopedBody(parse.tolerantText)(Seq(_.Study.Write)) { req => me =>
      env.relay.api.byIdAndContributor(id, me) flatMap {
        case None        => notFoundJson()
        case Some(relay) => env.relay.push(relay, req.body) inject jsonOkResult
      }
    }

  def apiIndex =
    Action.async { implicit req =>
      apiC.jsonStream {
        env.relay.api.officialStream(MaxPerSecond(20), getInt("nb", req) | 20)
      }.fuccess
    }

  private def WithRelay(slug: String, id: String)(
      f: RelayModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.relay.api byId id) { relay =>
      if (relay.slug != slug) Redirect(showRoute(relay)).fuccess
      else f(relay)
    }

  private def doShow(relay: RelayModel, oldSc: lila.study.Study.WithChapter)(implicit
      ctx: Context
  ): Fu[Result] =
    studyC.CanViewResult(oldSc.study) {
      for {
        (sc, studyData) <- studyC.getJsonData(oldSc)
        data = env.relay.jsonView.makeData(relay, studyData, ctx.userId exists sc.study.canContribute)
        chat     <- studyC.chatOf(sc.study)
        sVersion <- env.study.version(sc.study.id)
        streams  <- studyC.streamsOf(sc.study)
      } yield EnableSharedArrayBuffer(Ok(html.relay.show(relay, sc.study, data, chat, sVersion, streams)))
    }

  private def showRoute(r: RelayModel) = routes.Relay.show(r.slug, r.id.value)

  implicit private def makeRelayId(id: String): RelayModel.Id           = RelayModel.Id(id)
  implicit private def makeChapterId(id: String): lila.study.Chapter.Id = lila.study.Chapter.Id(id)
}
