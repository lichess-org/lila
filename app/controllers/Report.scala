package controllers

import views._

import lila.api.Context
import lila.app._
import lila.report.Reason
import lila.user.UserRepo

object Report extends LilaController {

  private def env = Env.report
  private def api = env.api

  def list = Secure(_.SeeReport) { implicit ctx => me =>
    renderList(env.modFilters.get(me).fold("all")(_.key))
  }

  def listWithFilter(reason: String) = Secure(_.SeeReport) { implicit ctx => me =>
    env.modFilters.set(me, Reason(reason))
    renderList(reason)
  }

  private def renderList(reason: String)(implicit ctx: Context) = for {
    reports <- api.unprocessedAndRecentWithFilter(50, Reason(reason))
    counts <- api.countUnprocesssedByReasons
    _ <- Env.user.lightUserApi preloadMany reports.flatMap(_.userIds)
  } yield Ok(html.report.list(reports, reason, counts))

  def process(id: String) = Secure(_.SeeReport) { implicit ctx => me =>
    api.process(id, me) inject Redirect(routes.Report.list)
  }

  def form = Auth { implicit ctx => implicit me =>
    NotForKids {
      get("username") ?? UserRepo.named flatMap { user =>
        env.forms.createWithCaptcha map {
          case (form, captcha) => Ok(html.report.form(form, user, captcha))
        }
      }
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    implicit val req = ctx.body
    env.forms.create.bindFromRequest.fold(
      err => get("username") ?? UserRepo.named flatMap { user =>
        env.forms.anyCaptcha map { captcha =>
          BadRequest(html.report.form(err, user, captcha))
        }
      },
      data => api.create(data, me) map { report =>
        Redirect(routes.Report.thanks(data.user.username))
      })
  }

  def thanks(reported: String) = Auth { implicit ctx => me =>
    Env.relation.api.fetchBlocks(me.id, reported) map { blocked =>
      html.report.thanks(reported, blocked)
    }
  }

  import scala.concurrent.duration._
  private lazy val irwinProcessedUserIds = new lila.memo.ExpireSetMemo(ttl = 30 minutes)

  def irwinBotNext = Open { implicit ctx =>
    Mod.ModExternalBot {
      api.unprocessedAndRecentWithFilter(100, Reason.Cheat.some) map { all =>
        all.find { r =>
          r.report.unprocessed && !r.hasIrwinNote && !irwinProcessedUserIds.get(r.user.id)
        } match {
          case None => NotFound
          case Some(r) =>
            irwinProcessedUserIds put r.user.id
            Ok(r.user.id)
        }
      }
    }
  }
}
