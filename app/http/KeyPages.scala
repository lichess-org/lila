package lila.app
package http

import play.api.mvc.*
import play.api.libs.json.Json
import scalatags.Text.all.Frag

import lila.app.{ *, given }
import lila.memo.CacheApi.*
import views.*

final class KeyPages(val env: Env)(using Executor)
    extends ResponseWriter
    with RequestContext
    with CtrlPage
    with ControllerHelpers:

  def home(status: Results.Status)(using ctx: Context): Fu[Result] =
    homeHtml
      .map: html =>
        env.lilaCookie.ensure(ctx.req)(status(html))

  def homeHtml(using ctx: Context): Fu[Frag] =
    env
      .preloader(
        tours = ctx.userId
          .so(env.team.cached.teamIdsList)
          .flatMap(env.tournament.featuring.homepage.get)
          .recoverDefault,
        swiss = env.swiss.feature.onHomepage.getUnit.getIfPresent,
        events = env.event.api.promoteTo(ctx.req).recoverDefault,
        simuls = env.simul.allCreatedFeaturable.get {}.recoverDefault,
        streamerSpots = env.streamer.homepageMaxSetting.get()
      )
      .mon(_.lobby segment "preloader.total")
      .flatMap: h =>
        ctx.me.filter(_.hasTitle).foreach(env.msg.twoFactorReminder(_))
        renderPage:
          lila.mon.chronoSync(_.lobby segment "renderSync"):
            html.lobby.home(h)

  def notFound(using Context): Fu[Result] =
    NotFound.page(html.base.notFound())

  def blacklisted(using ctx: Context): Fu[Result] =
    if lila.security.Mobile.Api requested ctx.req then
      fuccess:
        Results.Unauthorized:
          Json.obj:
            "error" -> html.site.message.blacklistedMessage
    else Unauthorized.page(html.site.message.blacklistedFrag)
