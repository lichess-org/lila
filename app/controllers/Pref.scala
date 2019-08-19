package controllers

import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.common.LidraughtsCookie
import views._

object Pref extends LidraughtsController {

  private def api = Env.pref.api
  private def forms = lidraughts.pref.DataForm

  def apiGet = Scoped(_.Preference.Read) { _ => me =>
    Env.pref.api.getPref(me) map { prefs =>
      JsonOk {
        import play.api.libs.json._
        import lidraughts.pref.JsonView._
        Json.obj("prefs" -> prefs)
      }
    }
  }

  def form(categSlug: String) = Auth { implicit ctx => me =>
    lidraughts.pref.PrefCateg(categSlug) match {
      case None => notFound
      case Some(categ) =>
        Ok(html.account.pref(me, forms prefOf ctx.pref, categ)).fuccess
    }
  }

  def formApply = AuthBody { implicit ctx => me =>
    def onSuccess(data: lidraughts.pref.DataForm.PrefData) = api.setPref(data(ctx.pref)) inject Ok("saved")
    implicit val req = ctx.body
    forms.pref.bindFromRequest.fold(
      err => forms.pref.bindFromRequest(lidraughts.pref.FormCompatLayer(ctx.pref, ctx.body)).fold(
        err => BadRequest(err.toString).fuccess,
        onSuccess
      ),
      onSuccess
    )
  }

  def set(name: String) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    if (name == "zoom") {
      Ok.withCookies(LidraughtsCookie.session("zoom2", (getInt("v") | 185).toString)).fuccess
    } else {
      implicit val req = ctx.body
      (setters get name) ?? {
        case (form, fn) => FormResult(form) { v =>
          fn(v, ctx) map { cookie => Ok(()).withCookies(cookie) }
        }
      }
    }
  }

  def verifyTitle = AuthBody { implicit ctx => me =>
    import play.api.data._, Forms._
    implicit val req = ctx.body
    Form(single("v" -> boolean)).bindFromRequest.fold(
      _ => fuccess(Redirect(routes.User.show(me.username))),
      v => api.saveTag(me, _.verifyTitle, if (v) "1" else "0") inject Redirect {
        if (v) routes.Page.master else routes.User.show(me.username)
      }
    )
  }

  private lazy val setters = Map(
    "theme" -> (forms.theme -> save("theme") _),
    "pieceSet" -> (forms.pieceSet -> save("pieceSet") _),
    "soundSet" -> (forms.soundSet -> save("soundSet") _),
    "bg" -> (forms.bg -> save("bg") _),
    "bgImg" -> (forms.bgImg -> save("bgImg") _),
    "zen" -> (forms.zen -> save("zen") _),
    "puzzleVariant" -> (forms.puzzleVariant -> save("puzzleVariant") _)
  )

  def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me ?? {
      api.setPrefString(_, name, value)
    } inject LidraughtsCookie.session(name, value)(ctx.req)
}
