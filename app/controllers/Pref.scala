package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import views._

final class Pref(env: Env) extends LilaController(env) {

  private def api   = env.pref.api
  private def forms = lila.pref.DataForm

  def apiGet =
    Scoped(_.Preference.Read) { _ => me =>
      env.pref.api.getPref(me) map { prefs =>
        JsonOk {
          import play.api.libs.json._
          import lila.pref.JsonView._
          Json.obj("prefs" -> prefs)
        }
      }
    }

  def form(categSlug: String) =
    Auth { implicit ctx => me =>
      lila.pref.PrefCateg(categSlug) match {
        case None => notFound
        case Some(categ) =>
          Ok(html.account.pref(me, forms prefOf ctx.pref, categ)).fuccess
      }
    }

  def formApply =
    AuthBody { implicit ctx => _ =>
      implicit val req = ctx.body
      forms.pref
        .bindFromRequest()
        .fold(
          err => BadRequest(err.toString).fuccess,
          data => api.setPref(data(ctx.pref)) inject Ok("saved")
        )
    }

  def set(name: String) =
    OpenBody { implicit ctx =>
      if (name == "zoom") {
        Ok.withCookies(env.lilaCookie.session("zoom2", (getInt("v") | 185).toString)).fuccess
      } else if (name == "customTheme") {
        implicit val req = ctx.body
        FormResult(forms.customTheme) { v =>
          saveCustomTheme(v, ctx) map { cookie =>
            Ok(()).withCookies(cookie)
          }
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

  def verifyTitle =
    AuthBody { implicit ctx => me =>
      import play.api.data._, Forms._
      implicit val req = ctx.body
      Form(single("v" -> boolean))
        .bindFromRequest()
        .fold(
          _ => fuccess(Redirect(routes.User.show(me.username))),
          v =>
            api.saveTag(me, _.verifyTitle, if (v) "1" else "0") inject Redirect {
              if (v) routes.Main.contact else routes.User.show(me.username)
            }
        )
    }

  private lazy val setters = Map(
    "theme"       -> (forms.theme       -> save("theme") _),
    "pieceSet"    -> (forms.pieceSet    -> save("pieceSet") _),
    "chuPieceSet" -> (forms.chuPieceSet -> save("chuPieceSet") _),
    "kyoPieceSet" -> (forms.kyoPieceSet -> save("kyoPieceSet") _),
    "soundSet"    -> (forms.soundSet    -> save("soundSet") _),
    "bg"          -> (forms.bg          -> save("bg") _),
    "thickGrid"   -> (forms.thickGrid   -> save("thickGrid") _),
    "bgImg"       -> (forms.bgImg       -> save("bgImg") _),
    "zen"         -> (forms.zen         -> save("zen") _),
    "notation"    -> (forms.notation    -> save("notation") _)
  )

  private def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me ?? {
      api.setPrefString(_, name, value)
    } inject env.lilaCookie.session(name, value)(ctx.req)

  private def saveCustomTheme(ct: lila.pref.CustomTheme, ctx: Context): Fu[Cookie] =
    ctx.me ?? {
      api.setPref(_, p => p.copy(customTheme = ct.some))
    } inject env.lilaCookie.session(
      List(
        ("boardColor" -> ct.boardColor),
        ("boardImg"   -> ct.boardImg),
        ("gridColor"  -> ct.gridColor),
        ("gridWidth"  -> ct.gridWidth.toString),
        ("handsColor" -> ct.handsColor),
        ("handsImg"   -> ct.handsImg)
      )
    )(ctx.req)
}
