package controllers

import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.common.LidraughtsCookie
import views._

object Pref extends LidraughtsController {

  private def api = Env.pref.api
  private def forms = lidraughts.pref.DataForm

  def form(categSlug: String) = Auth { implicit ctx => me =>
    lidraughts.pref.PrefCateg(categSlug) match {
      case None => notFound
      case Some(categ) =>
        Ok(html.account.pref(me, forms prefOf ctx.pref, categ)).fuccess
    }
  }

  def formApply = AuthBody { implicit ctx => me =>
    def onSuccess(data: lidraughts.pref.DataForm.PrefData) =
      api.setPref(data(ctx.pref), notifyChange = true) inject Ok("saved")
    implicit val req = ctx.body
    forms.pref.bindFromRequest.fold(
      err => forms.pref.bindFromRequest(lidraughts.pref.FormCompatLayer(ctx.body)).fold(
        err => BadRequest(err.toString).fuccess,
        onSuccess
      ),
      onSuccess
    )
  }

  def setZoom = Action { implicit req =>
    val zoom = getInt("v", req) | 100
    Ok(()).withCookies(LidraughtsCookie.session("zoom", zoom.toString))
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
    "soundSet" -> (forms.soundSet -> save("soundSet") _),
    "bg" -> (forms.bg -> save("bg") _),
    "bgImg" -> (forms.bgImg -> save("bgImg") _),
    "zen" -> (forms.zen -> save("zen") _)
  )

  private def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me ?? {
      api.setPrefString(_, name, value, notifyChange = false)
    } inject LidraughtsCookie.session(name, value)(ctx.req)
}
