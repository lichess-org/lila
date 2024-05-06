package controllers

import play.api.libs.json.*

import lila.app.*

final class Learn(env: Env) extends LilaController(env):

  import lila.learn.JSONHandlers.given

  def index     = Open(serveIndex)
  def indexLang = LangPage(routes.Learn.index)(serveIndex)

  private def serveIndex(using ctx: Context) = NoBot:
    pageHit
    ctx.me
      .soFu: me =>
        env.learn.api.get(me).map(Json.toJson)
      .flatMap: progress =>
        Ok.page(views.learn(progress))

  def score = AuthBody { ctx ?=> me ?=>
    bindForm(lila.learn.StageProgress.form)(
      _ => BadRequest,
      (stage, level, s) =>
        val score = lila.learn.StageProgress.Score(s)
        (env.learn.api.setScore(me, stage, level, score) >>
          env.activity.write.learn(me, stage)).inject(Ok(Json.obj("ok" -> true)))
    )
  }

  def reset = AuthBody { _ ?=> me ?=>
    env.learn.api.reset(me).inject(Ok(Json.obj("ok" -> true)))
  }
