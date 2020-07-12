package controllers

import play.api.mvc._
import play.api.libs.json.Json
import scalatags.Text.all.Frag

import lila.api.Context
import lila.app._
import lila.memo.CacheApi._
import views._

final class KeyPages(env: Env)(implicit ec: scala.concurrent.ExecutionContext) {

  def home(status: Results.Status)(implicit ctx: Context): Fu[Result] =
    homeHtml
      .dmap { html =>
        env.lilaCookie.ensure(ctx.req)(status(html))
      }

  def homeHtml(implicit ctx: Context): Fu[Frag] =
    env
      .preloader(
        posts = env.forum.recent(ctx.me, env.team.cached.teamIdsList).nevermind,
        tours = env.tournament.cached.onHomepage.getUnit.nevermind,
        events = env.event.api.promoteTo(ctx.req).nevermind,
        simuls = env.simul.allCreatedFeaturable.get {}.nevermind,
        streamerSpots = env.streamer.homepageMaxSetting.get()
      )
      .mon(_.lobby segment "preloader.total")
      .map { h =>
        lila.mon.chronoSync(_.lobby segment "renderSync") {
          html.lobby.home(h)
        }
      }

  def notFound(ctx: Context): Result = {
    Results.NotFound(html.base.notFound()(ctx))
  }

  def blacklisted(implicit ctx: Context): Result =
    if (lila.api.Mobile.Api requested ctx.req) Results.Unauthorized(Json.obj("error" -> blacklistMessage))
    else Results.Unauthorized(blacklistMessage)

  private val blacklistMessage =
    "Sorry, your IP address has been used to violate the ToS, and is now blacklisted."
}
