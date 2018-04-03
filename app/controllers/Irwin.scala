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

  def saveReport = OpenBody(parse.json) { implicit ctx =>
    ModExternalBot {
      UserRepo.irwin.flatten("Missing irwin user") flatMap { irwin =>
        ctx.body.body.validate[lila.irwin.IrwinReport].fold(
          err => fuccess(BadRequest(err.toString)),
          report => Env.irwin.api.reports.insert(report) inject Ok
        )
      }
    }
  }

  def assessment(username: String) = Open { implicit ctx =>
    ModExternalBot {
      OptionFuResult(UserRepo named username) { user =>
        lila.mon.mod.irwin.assessment.count()
        (Env.mod.assessApi.refreshAssessByUsername(user.id) >>
          Env.mod.jsonView(user) map {
            _.fold[Result](NotFound) { obj => Ok(obj) as JSON }
          }).mon(_.mod.irwin.assessment.time)
      }
    }
  }

  def usersMarkAndCurrentReport(idsStr: String) = Open { implicit ctx =>
    ModExternalBot {
      val ids = idsStr.split(',').toList map lila.user.User.normalize
      for {
        engineIds <- UserRepo filterByEngine ids
        reportedIds <- Env.report.api.currentlyReportedForCheat
      } yield Ok(Json.toJson(ids.map { id =>
        id -> Json.obj("engine" -> engineIds(id), "report" -> reportedIds(id))
      }.toMap)) as JSON
    }
  }

  def eventStream = Open { implicit ctx =>
    ModExternalBot {
      RequireHttp11 {
        Ok.chunked(Env.irwin.stream.enumerator).fuccess
      }
    }
  }

  private def ModExternalBot(f: => Fu[Result])(implicit ctx: Context) =
    if (get("api_key") contains Env.mod.ApiKey) f
    else fuccess(NotFound)
}
