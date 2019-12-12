package controllers

import lila.api.Context
import lila.app._
import lila.user.UserRepo

import play.api.libs.json._
import play.api.mvc._

object Irwin extends LilaController {

  import lila.irwin.JSONHandlers.reportReader

  def dashboard = Secure(_.SeeReport) { implicit ctx => me =>
    Env.irwin.api.dashboard map { d =>
      Ok(views.html.irwin.dashboard(d))
    }
  }

  def saveReport = ScopedBody(parse.json)(Nil) { req => me =>
    isGranted(_.Admin, me) ?? {
      req.body.validate[lila.irwin.IrwinReport].fold(
        err => fuccess(BadRequest(err.toString)),
        report => Env.irwin.api.reports.insert(report) inject Ok
      ) map (_ as TEXT)
    }
  }

  def eventStream = Scoped() { _ => me =>
    isGranted(_.Admin, me) ?? {
      noProxyBuffer(Ok.chunked(Env.irwin.stream.enumerator)).fuccess
    }
  }
}
