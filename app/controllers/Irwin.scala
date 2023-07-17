package controllers

import lila.app.{ given, * }

final class Irwin(env: Env) extends LilaController(env):

  import lila.irwin.JSONHandlers.given

  def dashboard = Secure(_.MarkEngine) { ctx ?=> _ ?=>
    Ok.pageAsync:
      env.irwin.irwinApi.dashboard.map:
        views.html.irwin.dashboard
  }

  def saveReport = ScopedBody(parse.json)(Nil) { ctx ?=> me ?=>
    IfGranted(_.Admin):
      ctx.body.body
        .validate[lila.irwin.IrwinReport]
        .fold(
          err => BadRequest(err.toString).toFuccess,
          report => env.irwin.irwinApi.reports.insert(report) inject Ok
        ) map (_ as TEXT)
  }

  def eventStream = Scoped() { _ ?=> me ?=>
    IfGranted(_.Admin):
      noProxyBuffer(Ok.chunked(env.irwin.irwinStream()))
  }

  def kaladin = Secure(_.MarkEngine) { ctx ?=> _ ?=>
    Ok.pageAsync:
      env.irwin.kaladinApi.dashboard.map:
        views.html.kaladin.dashboard
  }
