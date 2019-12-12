package controllers

import play.api.libs.json.JsValue
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.relay.{ Relay => RelayModel }
import views._

object Relay extends LilaController {

  private val env = Env.relay

  def index(page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      for {
        fresh <- (page == 1).??(env.api.fresh(ctx.me) map some)
        pager <- env.pager.finished(ctx.me, page)
      } yield Ok(html.relay.index(fresh, pager, routes.Relay.index()))
    }
  }

  def form = Auth { implicit ctx => me =>
    NoLame {
      Ok(html.relay.form.create(env.forms.create)).fuccess
    }
  }

  def create = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.forms.create.bindFromRequest.fold(
      err => BadRequest(html.relay.form.create(err)).fuccess,
      setup => env.api.create(setup, me) map { relay =>
        Redirect(showRoute(relay))
      }
    )
  }

  def edit(slug: String, id: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.api.byIdAndContributor(id, me)) { relay =>
      Ok(html.relay.form.edit(relay, env.forms.edit(relay))).fuccess
    }
  }

  def update(slug: String, id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(env.api.byIdAndContributor(id, me)) { relay =>
      implicit val req = ctx.body
      env.forms.edit(relay).bindFromRequest.fold(
        err => BadRequest(html.relay.form.edit(relay, err)).fuccess,
        data => env.api.update(relay) { data.update(_, me) } map { r =>
          Redirect(showRoute(r))
        }
      )
    }
  }

  def reset(slug: String, id: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.api.byIdAndContributor(id, me)) { relay =>
      env.api.reset(relay, me) inject Redirect(showRoute(relay))
    }
  }

  def show(slug: String, id: String) = Open { implicit ctx =>
    pageHit
    WithRelay(slug, id) { relay =>
      val sc =
        if (relay.sync.ongoing) Env.study.chapterRepo relaysAndTagsByStudyId relay.studyId flatMap { chapters =>
          chapters.find(_.looksAlive) orElse chapters.headOption match {
            case Some(chapter) => Env.study.api.byIdWithChapter(relay.studyId, chapter.id)
            case None => Env.study.api byIdWithChapter relay.studyId
          }
        }
        else Env.study.api byIdWithChapter relay.studyId
      sc flatMap { _ ?? { doShow(relay, _) } }
    }
  }

  def chapter(slug: String, id: String, chapterId: String) = Open { implicit ctx =>
    WithRelay(slug, id) { relay =>
      Env.study.api.byIdWithChapter(relay.studyId, chapterId) flatMap {
        _ ?? { doShow(relay, _) }
      }
    }
  }

  private def WithRelay(slug: String, id: String)(f: RelayModel => Fu[Result])(implicit ctx: Context): Fu[Result] =
    OptionFuResult(env.api byId id) { relay =>
      if (relay.slug != slug) Redirect(showRoute(relay)).fuccess
      else f(relay)
    }

  private def doShow(relay: RelayModel, oldSc: lila.study.Study.WithChapter)(implicit ctx: Context): Fu[Result] =
    Study.CanViewResult(oldSc.study) {
      for {
        (sc, studyData) <- Study.getJsonData(oldSc)
        data = env.jsonView.makeData(relay, studyData)
        chat <- Study.chatOf(sc.study)
        sVersion <- Env.study.version(sc.study.id)
        streams <- Study.streamsOf(sc.study)
      } yield Ok(html.relay.show(relay, sc.study, data, chat, sVersion, streams))
    }

  private def showRoute(r: RelayModel) = routes.Relay.show(r.slug, r.id.value)

  private implicit def makeRelayId(id: String): RelayModel.Id = RelayModel.Id(id)
  private implicit def makeChapterId(id: String): lila.study.Chapter.Id = lila.study.Chapter.Id(id)
}
