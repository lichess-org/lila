package controllers

import lila.api.Context
import lila.app._
import lila.user.UserRepo

import play.api.libs.json._
import play.api.mvc._

object Irwin extends LilaController {

  import lila.irwin.JSONHandlers.reportReader

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

  import scala.concurrent.duration._
  private lazy val irwinProcessedUserIds = new lila.memo.ExpireSetMemo(ttl = 30 minutes)

  def getRequest = Open { implicit ctx =>
    ModExternalBot {
      Env.irwin.api.requests.getAndStart map {
        case None => NotFound
        case Some(req) => Ok(req.id)
      }
    }
  }

  def assessment(username: String) = Open { implicit ctx =>
    ModExternalBot {
      OptionFuResult(UserRepo named username) { user =>
        Env.mod.jsonView(user) flatMap {
          case None => NotFound.fuccess
          case Some(data) => Env.mod.userHistory(user) map { history =>
            Ok(data + ("history" -> history))
          }
        } map (_ as JSON)
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

  private def ModExternalBot(f: => Fu[Result])(implicit ctx: Context) =
    if (!get("api_key").contains(Env.mod.ApiKey)) fuccess(NotFound)
    else f
}
