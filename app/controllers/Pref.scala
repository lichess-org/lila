package controllers

import play.api.mvc.*
import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.notify.NotificationPref
import lila.common.Json.given

final class Pref(env: Env) extends LilaController(env):

  private def api   = env.pref.api
  private def forms = lila.pref.PrefForm

  def apiGet = Scoped(_.Preference.Read, _.Web.Mobile) { _ ?=> me ?=>
    env.pref.api.get(me).map { prefs =>
      JsonOk:
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
    redirects.get(categSlug) match
      case Some(redir) => Action(Redirect(routes.Pref.form(redir)))
      case None =>
        Auth { ctx ?=> me ?=>
          lila.pref.PrefCateg(categSlug) match
            case None if categSlug == "notification" =>
              Ok.async:
                env.notifyM.api.prefs
                  .form(me)
                  .map:
                    views.account.pref.notification(_)
            case None        => notFound
            case Some(categ) => Ok.page(views.account.pref(me, forms.prefOf(ctx.pref), categ))
        }

  def formApply = AuthBody { ctx ?=> me ?=>
    def onSuccess(data: lila.pref.PrefForm.PrefData) =
      api.setPref(me, data(ctx.pref)).inject(Ok("saved"))
    val form = forms.pref(lichobile = HTTPRequest.isLichobile(req))
    bindForm(form)(
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
    bindForm(NotificationPref.form.form)(
      err => BadRequest(err.toString).toFuccess,
      data => env.notifyM.api.prefs.set(me, data).inject(Ok("saved"))
    )
  }

  def set(name: String) = OpenBody:
    if name == "zoom"
    then Ok.withCookies(env.security.lilaCookie.cookie("zoom", (getInt("v") | 85).toString))
    else if name == "agreement" then
      ctx.me
        .so(api.agree(_))
        .inject:
          if HTTPRequest.isXhr(ctx.req) then NoContent else Redirect(routes.Lobby.home)
    else
      lila.pref.PrefSingleChange.changes
        .get(name)
        .so: change =>
          bindForm(change.form)(
            form => fuccess(BadRequest(form.errors.flatMap(_.messages).mkString("\n"))),
            v =>
              ctx.me
                .so(api.setPref(_, change.update(v)))
                .inject(env.security.lilaCookie.session(name, v.toString))
                .map: cookie =>
                  Ok(()).withCookies(cookie)
          )

  def apiSet(name: String) = ScopedBody(_.Web.Mobile) { ctx ?=> me ?=>
    lila.pref.PrefSingleChange.changes
      .get(name)
      .so: change =>
        bindForm(change.form)(
          jsonFormError,
          v => api.setPref(me, change.update(v)).inject(NoContent)
        )
  }
