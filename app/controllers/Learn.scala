package controllers

import play.api.libs.json.*

import lila.app.*

final class Learn(env: Env) extends LilaController(env):

  import lila.learn.LearnHandlers.given

  def index = Open(serveIndex)
  def indexLang = LangPage(routes.Learn.index)(serveIndex)

  private def serveIndex(using ctx: Context) = NoBot:
    pageHit
    ctx.me
      .traverse: me =>
        env.learn.api.get(me).map(Json.toJson)
      .flatMap: progress =>
        Ok.page(views.learn(progress))

  def score = AuthBody { ctx ?=> me ?=>
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

  def reset = AuthBody { _ ?=> me ?=>
    for _ <- env.learn.api.reset(me)
    yield jsonOkResult
  }
