package controllers

import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.notify.NotificationPref

final class Pref(env: Env) extends LilaController(env):

  private def api   = env.pref.api
  private def forms = lila.pref.PrefForm

  def apiGet = Scoped(_.Preference.Read) { _ ?=> me ?=>
    env.pref.api.get(me) map { prefs =>
      JsonOk:
        import play.api.libs.json.*
        Json
          .obj("prefs" -> lila.pref.JsonView.write(prefs, lichobileCompat = false))
          .add("language" -> me.lang)
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
        Auth { ctx ?=> me ?=>
          lila.pref.PrefCateg(categSlug) match
            case None if categSlug == "notification" =>
              Ok.pageAsync:
                env.notifyM.api.prefs.form(me) map {
                  html.account.notification(_)
                }
            case None        => notFound
            case Some(categ) => Ok.page(html.account.pref(me, forms prefOf ctx.pref, categ))
        }

  def formApply = AuthBody { ctx ?=> _ ?=>
    def onSuccess(data: lila.pref.PrefForm.PrefData) =
      api.setPref(data(ctx.pref)) inject Ok("saved")
    val form = forms.pref(lichobile = HTTPRequest.isLichobile(req))
    form
      .bindFromRequest()
      .fold(
        _ =>
          form
            .bindFromRequest(lila.pref.FormCompatLayer(ctx.pref, ctx.body))
            .fold(
              err => BadRequest(err.toString).toFuccess,
              onSuccess
            ),
        onSuccess
      )
  }

  def notifyFormApply = AuthBody { ctx ?=> me ?=>
    NotificationPref.form.form
      .bindFromRequest()
      .fold(
        err => BadRequest(err.toString).toFuccess,
        data => env.notifyM.api.prefs.set(me, data) inject Ok("saved")
      )
  }

  def set(name: String) = OpenBody:
    if name == "zoom"
    then Ok.withCookies(env.lilaCookie.cookie("zoom", (getInt("v") | 85).toString))
    else if name == "agreement" then
      ctx.me.so(api.agree(_)) inject {
        if HTTPRequest.isXhr(ctx.req) then NoContent else Redirect(routes.Lobby.home)
      }
    else
      setters
        .get(name)
        .so: (form, fn) =>
          form
            .bindFromRequest()
            .fold(
              form => fuccess(BadRequest(form.errors mkString "\n")),
              v =>
                fn(v, ctx).map: cookie =>
                  Ok(()).withCookies(cookie)
            )

  private lazy val setters = Map(
    "theme"        -> (forms.theme        -> save("theme")),
    "pieceSet"     -> (forms.pieceSet     -> save("pieceSet")),
    "theme3d"      -> (forms.theme3d      -> save("theme3d")),
    "pieceSet3d"   -> (forms.pieceSet3d   -> save("pieceSet3d")),
    "soundSet"     -> (forms.soundSet     -> save("soundSet")),
    "bg"           -> (forms.bg           -> save("bg")),
    "bgImg"        -> (forms.bgImg        -> save("bgImg")),
    "is3d"         -> (forms.is3d         -> save("is3d")),
    "zen"          -> (forms.zen          -> saveZen()),
    "voice"        -> (forms.voice        -> save("voice")),
    "keyboardMove" -> (forms.keyboardMove -> save("keyboardMove"))
  )

  private def saveZen()(value: String, ctx: Context): Fu[Cookie] =
    //Discard 0-1 bit, then add 0-1 bit with wanted value
    var newVal = ctx.pref.zenInt & ~1 | value.toInt

    save("zen")(newVal.toString, ctx)

  private def save(name: String)(value: String, ctx: Context): Fu[Cookie] =
    ctx.me so {
      api.setPrefString(_, name, value)
    } inject env.lilaCookie.session(name, value)(using ctx.req)
