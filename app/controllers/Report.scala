package controllers

import play.api.mvc._
import play.api.templates.Html
import views._

import lila.app._
import lila.security.Granter
import lila.user.{ User ⇒ UserModel, UserRepo, Context }

object Report extends LilaController {

  private def forms = Env.report.forms
  private def api = Env.report.api

  def form = Auth { implicit ctx ⇒
    implicit me ⇒
      get("username") ?? UserRepo.named flatMap { user ⇒
        forms.createWithCaptcha map {
          case (form, captcha) ⇒ Ok(html.report.form(form, user, captcha))
        }
      }
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒
      implicit val req = ctx.body
      forms.create.bindFromRequest.fold(
        err ⇒ get("username") ?? UserRepo.named flatMap { user ⇒
          forms.anyCaptcha map { captcha ⇒
            BadRequest(html.report.form(err.pp, user, captcha))
          }
        },
        data ⇒ api.create(data, me) map { thread ⇒
          Redirect(routes.Report.thanks)
        })
  }

  def thanks = Auth { implicit ctx ⇒
    implicit me ⇒
      fuccess {
        html.report.thanks()
      }
  }
}
