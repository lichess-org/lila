package controllers

import play.api.mvc._
import play.twirl.api.Html
import views._

import lila.app._
import lila.security.Granter
import lila.user.{ User => UserModel, UserRepo }

object Report extends LilaController {

  private def forms = Env.report.forms
  private def api = Env.report.api

  def list = Secure(_.SeeReport) { implicit ctx => _ =>
    api unprocessedAndRecent 50 map { reports =>
      html.report.list(reports)
    }
  }

  def process(id: String) = Secure(_.SeeReport) { implicit ctx => me =>
    api.process(id, me) inject Redirect(routes.Report.list)
  }

  def form = Auth { implicit ctx => implicit me =>
    NotForKids {
      get("username") ?? UserRepo.named flatMap { user =>
        forms.createWithCaptcha map {
          case (form, captcha) => Ok(html.report.form(form, user, captcha))
        }
      }
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    implicit val req = ctx.body
    forms.create.bindFromRequest.fold(
      err => get("username") ?? UserRepo.named flatMap { user =>
        forms.anyCaptcha map { captcha =>
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

  def next = Open { implicit ctx =>
    Mod.ModExternalBot {
      api unprocessedAndRecent 50 map { all =>
        all.find { r =>
          r.report.isCheat && r.report.unprocessed && !r.hasClarkeyBotNote
        } match {
          case None    => NotFound
          case Some(r) => Ok(r.user.id)
        }
      }
    }
  }
}
