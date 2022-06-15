package controllers

import play.api.mvc._
import views._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest

final class Pref(env: Env) extends LilaController(env) {

  private def api   = env.pref.api
  private def forms = lila.pref.PrefForm

  def apiGet =
    Scoped(_.Preference.Read) { _ => me =>
      env.pref.api.getPref(me) map { prefs =>
        JsonOk {
          import play.api.libs.json._
          import lila.pref.JsonView._
          Json.obj("prefs" -> prefs).add("language" -> me.lang)
        }
      }
    }

  private val redirects = Map(
    "game-display" -> "display",
    "site"         -> "privacy"
  )

  def form(categSlug: String) =
    redirects get categSlug match {
      case Some(redir) => Action(Redirect(routes.Pref.form(redir)))
      case None =>
        Auth { implicit ctx => me =>
          lila.pref.PrefCateg(categSlug) match {
            case None => notFound
            case Some(categ) =>
              Ok(html.account.pref(me, forms prefOf ctx.pref, categ)).fuccess
          }
        }
    }

  def formApply =
    AuthBody { implicit ctx => _ =>
      def onSuccess(data: lila.pref.PrefForm.PrefData) = api.setPref(data(ctx.pref)) inject Ok("saved")
      implicit val req                                 = ctx.body
      forms.pref
        .bindFromRequest()
        .fold(
          _ =>
            forms.pref
              .bindFromRequest(lila.pref.FormCompatLayer(ctx.pref, ctx.body))
              .fold(
                err => BadRequest(err.toString).fuccess,
                onSuccess
              ),
          onSuccess
        )
    }

  def set(name: String) =
    OpenBody { implicit ctx =>
      if (name == "zoom") {
        Ok.withCookies(env.lilaCookie.cookie("zoom", (getInt("v") | 85).toString)).fuccess
      } else if (name == "agreement") {
        ctx.me ?? api.agree inject {
          if (HTTPRequest.isXhr(ctx.req)) NoContent else Redirect(routes.Lobby.home)
        }
      } else {
        implicit val req = ctx.body
        (setters get name) ?? { case (form, fn) =>
          FormResult(form) { v =>
            fn(v, ctx) map { cookie =>
              Ok(()).withCookies(cookie)
            }
          }
        }
      }
    }

  private lazy val setters = Map(
    "theme"      -> (forms.theme      -> save("theme") _),
    "pieceSet"   -> (forms.pieceSet   -> save("pieceSet") _),
    "theme3d"    -> (forms.theme3d    -> save("theme3d") _),
    "pieceSet3d" -> (forms.pieceSet3d -> save("pieceSet3d") _),
    "soundSet"   -> (forms.soundSet   -> save("soundSet") _),
    "bg"         -> (forms.bg         -> save("bg") _),
    "bgImg"      -> (forms.bgImg      -> save("bgImg") _),
    "is3d"       -> (forms.is3d       -> save("is3d") _),
    "zen"        -> (forms.zen        -> save("zen") _)
  )

  private def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me ?? {
      api.setPrefString(_, name, value)
    } inject env.lilaCookie.session(name, value)(ctx.req)
}
