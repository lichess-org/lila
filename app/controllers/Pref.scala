package controllers

import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.notify.NotificationPref

final class Pref(env: Env) extends LilaController(env):

  private def api   = env.pref.api
  private def forms = lila.pref.PrefForm

  def apiGet =
    Scoped(_.Preference.Read) { _ => me =>
      env.pref.api.getPref(me) map { prefs =>
        JsonOk {
          import play.api.libs.json.*
          import lila.pref.JsonView.given
          Json.obj("prefs" -> prefs).add("language" -> me.lang)
        }
      }
    }

  private val redirects = Map(
    "game-display" -> "display",
    "site"         -> "privacy"
  )

  def form(categSlug: String) =
    redirects get categSlug match
      case Some(redir) => Action(Redirect(routes.Pref.form(redir)))
      case None =>
        Auth { implicit ctx => me =>
          lila.pref.PrefCateg(categSlug) match
            case None if categSlug == "notification" =>
              env.notifyM.api.prefs.form(me) map { form =>
                Ok(html.account.notification(form))
              }
            case None        => notFound
            case Some(categ) => Ok(html.account.pref(me, forms prefOf ctx.pref, categ)).toFuccess
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
                err => BadRequest(err.toString).toFuccess,
                onSuccess
              ),
          onSuccess
        )
    }

  def notifyFormApply =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      NotificationPref.form.form
        .bindFromRequest()
        .fold(
          err => BadRequest(err.toString).toFuccess,
          data => env.notifyM.api.prefs.set(me, data) inject Ok("saved")
        )
    }

  def set(name: String) =
    OpenBody { implicit ctx =>
      if (name == "zoom") Ok.withCookies(env.lilaCookie.cookie("zoom", (getInt("v") | 85).toString)).toFuccess
      else if (name == "agreement")
        ctx.me ?? api.agree inject {
          if (HTTPRequest.isXhr(ctx.req)) NoContent else Redirect(routes.Lobby.home)
        }
      else
        given play.api.mvc.Request[?] = ctx.body
        (setters get name) ?? { case (form, fn) =>
          FormResult(form) { v =>
            fn(v, ctx) map { cookie =>
              Ok(()).withCookies(cookie)
            }
          }
        }
    }

  private lazy val setters = Map(
    "theme"      -> (forms.theme      -> save("theme")),
    "pieceSet"   -> (forms.pieceSet   -> save("pieceSet")),
    "theme3d"    -> (forms.theme3d    -> save("theme3d")),
    "pieceSet3d" -> (forms.pieceSet3d -> save("pieceSet3d")),
    "soundSet"   -> (forms.soundSet   -> save("soundSet")),
    "bg"         -> (forms.bg         -> save("bg")),
    "bgImg"      -> (forms.bgImg      -> save("bgImg")),
    "is3d"       -> (forms.is3d       -> save("is3d")),
    "zen"        -> (forms.zen        -> save("zen"))
  )

  private def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me ?? {
      api.setPrefString(_, name, value)
    } inject env.lilaCookie.session(name, value)(using ctx.req)
