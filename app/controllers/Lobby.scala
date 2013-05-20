package controllers

import lila.app._
import lila.user.Context
import lila.common.LilaCookie
import lila.tournament.TournamentRepo
import views._

import play.api.mvc._
import play.api.libs.json.JsValue
import play.api.libs.json.Json

object Lobby extends LilaController with Results {

  def home = Open { implicit ctx ⇒
    renderHome(Ok).map(_.withHeaders(
      CACHE_CONTROL -> "no-cache", PRAGMA -> "no-cache"
    ))
  }

  def handleNotFound(req: RequestHeader): Fu[Result] =
    reqToCtx(req) flatMap { ctx ⇒ handleNotFound(ctx) }

  def handleNotFound(implicit ctx: Context): Fu[Result] =
    renderHome(NotFound)

  private def renderHome[A](status: Status)(implicit ctx: Context): Fu[Result] =
    Env.current.preloader(
      timeline = Env.timeline.recent,
      posts = Env.forum.recent(ctx.me, Env.team.cached.teamIds.apply),
      tours = TournamentRepo.created,
      filter = Env.setup.filter
    ).map(_.fold(Redirect(_), {
        case (preload, entries, posts, tours, featured, requests) ⇒ status(html.lobby.home(
          Json stringify preload, entries, posts, tours, featured, requests)) |> { response ⇒
          ctx.req.session.data.contains(LilaCookie.sessionId).fold(
            response,
            response withCookies LilaCookie.makeSessionId(ctx.req)
          )
        }
      }))

  def socket = Socket[JsValue] { implicit ctx ⇒
    get("sri") ?? { uid ⇒
      Env.lobby.socketHandler(uid = uid, user = ctx.me)
    }
  }
}
