package controllers

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.LilaCookie
import lila.tournament.TournamentRepo
import views._

object Lobby extends LilaController {

  def home = Open { implicit ctx =>
    ctx.me match {
      case Some(u) if u.artificial => fuccess {
        views.html.auth.artificialPassword(u, Env.security.forms.newPassword)
      }
      case _ => renderHome(Results.Ok).map(_.withHeaders(
        CACHE_CONTROL -> "no-cache", PRAGMA -> "no-cache"
      ))
    }
  }

  def handleStatus(req: RequestHeader, status: Results.Status): Fu[Result] =
    reqToCtx(req) flatMap { ctx => renderHome(status)(ctx) }

  def renderHome(status: Results.Status)(implicit ctx: Context): Fu[Result] =
    Env.current.preloader(
      posts = Env.forum.recent(ctx.me, Env.team.cached.teamIds),
      tours = Env.tournament promotable true,
      filter = Env.setup.filter
    ).map(_.fold(Redirect(_), {
        case (preload, entries, posts, tours, featured, lead, tWinners, puzzle, playing, pools, streams) =>
          val response = status(html.lobby.home(
            Json stringify preload, entries, posts, tours, featured, lead, tWinners,
            puzzle, playing, streams, Env.blog.lastPostCache.apply, pools
          ))
          // the session cookie is required for anon lobby filter storage
          ctx.req.session.data.contains(LilaCookie.sessionId).fold(
            response,
            response withCookies LilaCookie.makeSessionId(ctx.req)
          )
      }))

  def socket = Socket[JsValue] { implicit ctx =>
    get("sri") ?? { uid =>
      Env.lobby.socketHandler(uid = uid, user = ctx.me)
    }
  }

  def timeline = Auth { implicit ctx =>
    me =>
      Env.timeline.getter.userEntries(me.id) map { html.timeline.entries(_) }
  }

  def timelineMore = Auth { implicit ctx =>
    me =>
      Env.timeline.getter.moreUserEntries(me.id) map { html.timeline.more(_) }
  }
}
