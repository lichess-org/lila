package controllers

import play.api.data.Form
import play.api.mvc._, Results._

import lila.app._
import lila.common.LilaCookie
import lila.user.Context
import views._

object Pref extends LilaController {

  private def api = Env.pref.api
  private def forms = Env.pref.forms

  def set(name: String) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    (setters get name) ?? {
      case (form, fn) ⇒ FormResult(form) { v ⇒
        fn(v, ctx) map { Ok() withCookies _ }
      }
    }
  }

  private lazy val setters = Map(
    "theme" -> (forms.theme -> save("theme") _),
    "bg" -> (forms.bg -> save("bg") _))

  private def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me ?? { api.setPrefString(_, name, value) } inject LilaCookie.session(name, value)(ctx.req)
}
