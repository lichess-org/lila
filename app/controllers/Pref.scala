package controllers

import play.api.data.Form
import play.api.mvc._, Results._

import lila.api.Context
import lila.app._
import lila.common.LilaCookie
import views._

object Pref extends LilaController {

  private def api = Env.pref.api
  private def forms = Env.pref.forms

  def form = Auth { implicit ctx =>
    me =>
      forms prefOf me map { form =>
        Ok(html.account.pref(me, form))
      }
  }

  def formApply = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      FormFuResult(forms.pref) { err =>
        fuccess(html.account.pref(me, err))
      } { data =>
        api getPref me flatMap { pref =>
          api.setPref(data(pref))
        } inject Ok("saved")
      }
  }

  def set(name: String) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    (setters get name) ?? {
      case (form, fn) => FormResult(form) { v =>
        fn(v, ctx) map { Ok(()) withCookies _ }
      }
    }
  }

  def saveTag(name: String, value: String) = Auth { implicit ctx =>
    me =>
      api.saveTag(me, name, value)
  }

  private lazy val setters = Map(
    "theme" -> (forms.theme -> save("theme") _),
    "pieceSet" -> (forms.pieceSet -> save("pieceSet") _),
    "bg" -> (forms.bg -> save("bg") _))

  private def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me ?? { api.setPrefString(_, name, value) } inject LilaCookie.session(name, value)(ctx.req)
}
