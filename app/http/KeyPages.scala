package lila.app
package http

import play.api.mvc.*
import lila.app.{ *, given }
import lila.memo.CacheApi.*

final class KeyPages(val env: Env)(using Executor)
    extends lila.web.ResponseWriter
    with RequestContext
    with CtrlPage
    with lila.web.CtrlErrors
    with ControllerHelpers:

  def home(status: Results.Status)(using ctx: Context): Fu[Result] =
    homeHtml.map: html =>
      env.security.lilaCookie.ensure(ctx.req)(status(html))

  def homeHtml(using ctx: Context): Fu[lila.ui.RenderedPage] =
    env
      .preloader(
        tours = ctx.userId
          .so(env.team.cached.teamIdsList)
          .flatMap(env.tournament.featuring.homepage.get)
          .recoverDefault,
        swiss = env.swiss.feature.onHomepage.getUnit.getIfPresent,
        events = env.event.api.promoteTo(ctx.acceptLanguages).recoverDefault,
        simuls = env.simul.allCreatedFeaturable.get {}.recoverDefault,
        streamerSpots = env.streamer.homepageMaxSetting.get()
      )
      .mon(_.lobby.segment("preloader.total"))
      .flatMap: h =>
        ctx.me.filter(_.hasTitle).foreach(env.msg.twoFactorReminder(_))
        ctx.me.filterNot(_.hasEmail).foreach(env.msg.emailReminder(_))
        renderPage:
          lila.mon.chronoSync(_.lobby.segment("renderSync")):
            views.lobby.home(h)

  def notFound(msg: Option[String])(using Context): Fu[Result] =
    NotFound.page(views.base.notFound(msg))

  def notFoundEmbed(msg: Option[String])(using EmbedContext): Result =
    NotFound.snip(views.base.notFoundEmbed(msg))

  def blacklisted(using ctx: Context): Result =
    if lila.security.Mobile.Api.requested(ctx.req)
    then Unauthorized(jsonError(views.site.message.blacklistedMessage))
    else Unauthorized(views.site.message.blacklistedSnippet)
