package lila.app
package http

import play.api.mvc.*
import play.api.libs.json.Json
import scalatags.Text.all.Frag

import lila.app.{ *, given }
import lila.memo.CacheApi.*
import views.*

final class KeyPages(env: Env)(using Executor) extends ResponseWriter with ResponseBuilder:

  def home(status: Results.Status)(using ctx: WebContext): Fu[Result] =
    homeHtml
      .map: html =>
        env.lilaCookie.ensure(ctx.req)(status(html))

  def homeHtml(using ctx: WebContext): Fu[Frag] =
    env
      .preloader(
        tours = env.tournament.cached.onHomepage.getUnit.recoverDefault,
        swiss = env.swiss.feature.onHomepage.getUnit.getIfPresent,
        events = env.event.api.promoteTo(ctx.req).recoverDefault,
        simuls = env.simul.allCreatedFeaturable.get {}.recoverDefault,
        streamerSpots = env.streamer.homepageMaxSetting.get()
      )
      .mon(_.lobby segment "preloader.total")
      .flatMap: h =>
        page(_ ?=> html.lobby.home(h)).mon(_.lobby segment "render")

  def notFound(using WebContext): Fu[Result] =
    NotFound.page(html.base.notFound())

  def blacklisted(using ctx: AnyContext): Fu[Result] =
    if lila.api.Mobile.Api requested ctx.req then
      Results
        .Unauthorized:
          Json.obj:
            "error" -> html.site.message.blacklistedMessage
        .toFuccess
    else Unauthorized.page(html.site.message.blacklistedFrag)
