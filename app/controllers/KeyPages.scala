package controllers

import play.api.mvc._
import scalatags.Text.all.Frag

import lila.api.Context
import lila.app._
import views._

private final class KeyPages(env: Env) {

  def home(status: Results.Status)(implicit ctx: Context): Fu[Result] =
    env.preloader(
      posts = env.forum.recent(ctx.me, env.team.cached.teamIdsList).nevermind,
      tours = env.tournament.cached.promotable.get.nevermind,
      events = env.event.api.promoteTo(ctx.req).nevermind,
      simuls = env.simul.allCreatedFeaturable.get.nevermind
    ).map(h => html.lobby.home(h)).dmap { (html: Frag) =>
      env.lilaCookie.ensure(ctx.req)(status(html))
    }.mon(_.http.response.home)

  def notFound(ctx: Context): Result = {
    lila.mon.http.response.code404()
    Results.NotFound(html.base.notFound()(ctx))
  }
}
