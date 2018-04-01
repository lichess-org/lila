package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.LilaCookie
import views._

object Pref extends LilaController {

  private def api = Env.pref.api
  private def forms = lila.pref.DataForm

  def get = Scoped(_.Preference.Read) { _ => me =>
    Env.pref.api.getPref(me) map { prefs =>
      Ok {
        import play.api.libs.json._
        import lila.pref.JsonView._
        Json.obj("prefs" -> prefs)
      }
    }
  }

  def form(categSlug: String) = Auth { implicit ctx => me =>
    lila.pref.PrefCateg(categSlug) match {
      case None => notFound
      case Some(categ) =>
        Ok(html.account.pref(me, forms prefOf ctx.pref, categ)).fuccess
    }
  }

  def formApply = AuthBody { implicit ctx => me =>
    def onSuccess(data: lila.pref.DataForm.PrefData) =
      api.setPref(data(ctx.pref), notifyChange = true) inject Ok("saved")
    implicit val req = ctx.body
    forms.pref.bindFromRequest.fold(
      err => forms.pref.bindFromRequest(lila.pref.FormCompatLayer(ctx.body)).fold(
        err => BadRequest(err.toString).fuccess,
        onSuccess
      ),
      onSuccess
    )
  }

  def setZoom = Action { implicit req =>
    val zoom = getInt("v", req) | 100
    Ok(()).withCookies(LilaCookie.session("zoom", zoom.toString))
  }

  def set(name: String) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    (setters get name) ?? {
      case (form, fn) => FormResult(form) { v =>
        fn(v, ctx) map { cookie => Ok(()).withCookies(cookie) }
      }
    }
  }

  def saveTag(name: String, value: String) = Auth { implicit ctx => me =>
    api.saveTag(me, name, value)
  }

  private lazy val setters = Map(
    "theme" -> (forms.theme -> save("theme") _),
    "pieceSet" -> (forms.pieceSet -> save("pieceSet") _),
    "theme3d" -> (forms.theme3d -> save("theme3d") _),
    "pieceSet3d" -> (forms.pieceSet3d -> save("pieceSet3d") _),
    "soundSet" -> (forms.soundSet -> save("soundSet") _),
    "bg" -> (forms.bg -> save("bg") _),
    "bgImg" -> (forms.bgImg -> save("bgImg") _),
    "is3d" -> (forms.is3d -> save("is3d") _),
    "zen" -> (forms.zen -> save("zen") _)
  )

  private def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me ?? {
      api.setPrefString(_, name, value, notifyChange = false)
    } inject LilaCookie.session(name, value)(ctx.req)
}
