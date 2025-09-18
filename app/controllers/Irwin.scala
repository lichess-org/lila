package controllers

import lila.app.*

final class Irwin(env: Env) extends LilaController(env):

  import lila.irwin.JSONHandlers.given

  def dashboard = Secure(_.MarkEngine) { ctx ?=> _ ?=>
    Ok.async:
      env.irwin.irwinApi.dashboard.map:
        views.irwin.dashboard
  }

  def saveReport = ScopedBody(parse.json)(Nil) { ctx ?=> me ?=>
    IfGranted(_.Admin):
      ctx.body.body
        .validate[lila.irwin.IrwinReport]
        .fold(
          err => BadRequest(err.toString).toFuccess,
          report => env.irwin.irwinApi.reports.insert(report).inject(Ok)
        )
        .map(_.as(TEXT))
  }

  def eventStream = Scoped() { _ ?=> me ?=>
    IfGranted(_.Admin):
      Ok.chunked(env.irwin.irwinStream()).noProxyBuffer
  }

  def kaladin = Secure(_.MarkEngine) { ctx ?=> _ ?=>
    Ok.async:
      env.irwin.kaladinApi.dashboard.map:
        views.irwin.kaladin.dashboard
  }
