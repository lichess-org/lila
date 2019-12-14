package controllers

import play.api.mvc._
import scalatags.Text.all.Frag

import lila.api.Context
import lila.app._
import views._

final class KeyPages(env: Env)(implicit ec: scala.concurrent.ExecutionContext) {

  def home(status: Results.Status)(implicit ctx: Context): Fu[Result] =
    env
      .preloader(
        posts = env.forum.recent(ctx.me, env.team.cached.teamIdsList).nevermind,
        tours = env.tournament.cached.promotable.get.nevermind,
        events = env.event.api.promoteTo(ctx.req).nevermind,
        simuls = env.simul.allCreatedFeaturable.get.nevermind
      )
      .map(h => html.lobby.home(h))
      .dmap { (html: Frag) =>
        env.lilaCookie.ensure(ctx.req)(status(html))
      }

  def notFound(ctx: Context): Result = {
    Results.NotFound(html.base.notFound()(ctx))
  }
}
