package controllers

import lila.app.{ given, * }

final class Irwin(env: Env) extends LilaController(env):

  import lila.irwin.JSONHandlers.given

  def dashboard = Secure(_.MarkEngine) { ctx ?=> _ =>
    env.irwin.irwinApi.dashboard map { d =>
      Ok(views.html.irwin.dashboard(d))
    }
  }

  def saveReport = ScopedBody(parse.json)(Nil) { ctx ?=> me =>
    IfGranted(_.Admin):
      ctx.body.body
        .validate[lila.irwin.IrwinReport]
        .fold(
          err => fuccess(BadRequest(err.toString)),
          report => env.irwin.irwinApi.reports.insert(report) inject Ok
        ) map (_ as TEXT)
  }

  def eventStream = Scoped() { _ ?=> me =>
    IfGranted(_.Admin):
      noProxyBuffer(Ok.chunked(env.irwin.irwinStream())).toFuccess
  }

  def kaladin = Secure(_.MarkEngine) { ctx ?=> _ =>
    env.irwin.kaladinApi.dashboard map { d =>
      Ok(views.html.kaladin.dashboard(d))
    }
  }
