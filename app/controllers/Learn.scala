package controllers

import play.api.libs.json.*

import lila.app.*

final class Learn(env: Env) extends LilaController(env):

  import lila.learn.LearnHandlers.given

  def index = OpenOrScoped(_.Web.Mobile)(serveIndex)
  def indexLang = LangPage(routes.Learn.index)(serveIndex)

  private def serveIndex(using ctx: Context) = NoBot:
    pageHit
    ctx.me
      .traverse: me =>
        env.learn.api.get(me).map(Json.toJson)
      .flatMap: progress =>
        negotiate(Ok.page(views.learn(progress)), JsonOk(progress))

  def score = AuthOrScopedBody(_.Web.Mobile) { ctx ?=> me ?=>
    bindForm(lila.learn.StageProgress.form)(
      jsonFormError,
      (stage, level, s) =>
        val score = lila.learn.StageProgress.Score(s)
        for
          _ <- env.learn.api.setScore(me, stage, level, score)
          _ <- env.activity.write.learn(me, stage)
        yield jsonOkResult
    )
  }

  def reset = AuthOrScopedBody(_.Web.Mobile) { _ ?=> me ?=>
    for _ <- env.learn.api.reset(me)
    yield jsonOkResult
  }
