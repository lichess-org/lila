package controllers

import lila.app._
import lila.user.Context
import lila.common.LilaCookie
import lila.lobby.{ Hook, HookRepo }
import lila.tournament.TournamentRepo
import views._

import play.api.mvc._
import play.api.libs.json.JsValue
import play.api.libs.json.Json

object Lobby extends LilaController with Results {

  def home = Open { implicit ctx ⇒
    renderHome(none, Ok).map(_.withHeaders(
      CACHE_CONTROL -> "no-cache", PRAGMA -> "no-cache"
    ))
  }

  def handleNotFound(req: RequestHeader): Fu[Result] =
    reqToCtx(req) flatMap { ctx ⇒ handleNotFound(ctx) }

  def handleNotFound(implicit ctx: Context): Fu[Result] =
    renderHome(none, NotFound)

  private def renderHome[A](myHook: Option[Hook], status: Status)(implicit ctx: Context): Fu[Result] =
    Env.current.preloader(
      myHook = myHook,
      timeline = Env.timeline.recent,
      posts = Env.forum.recent(ctx.me, Env.team.cached.teamIds.apply),
      tours = TournamentRepo.created,
      filter = Env.setup.filter
    ).map(_.fold(Redirect(_), {
        case (preload, entries, posts, tours, featured) ⇒ status(html.lobby.home(
          Json stringify preload, myHook, entries, posts, tours, featured)) |> { response ⇒
          ctx.req.session.data.contains(LilaCookie.sessionId).fold(
            response,
            response withCookies LilaCookie.makeSessionId(ctx.req)
          )
        }
      }))

  def socket = Socket[JsValue] { implicit ctx ⇒
    get("sri") zmap { uid ⇒
      Env.lobby.socketHandler(uid = uid, user = ctx.me, hook = get("hook"))
    }
  }

  def hook(ownerId: String) = Open { implicit ctx ⇒
    HookRepo.ownedHook(ownerId) flatMap {
      _.fold(Redirect(routes.Lobby.home).fuccess) { hook ⇒
        renderHome(hook.some, Ok)
      }
    }
  }

  def join(hookId: String) = Open { implicit ctx ⇒
    val myHookId = get("cancel")
    Env.setup.hookJoiner(hookId, myHookId)(ctx.me) map { result ⇒
      Redirect {
        result.fold(
          _ ⇒ myHookId.fold(routes.Lobby.home)(routes.Lobby.hook(_)),
          pov ⇒ routes.Round.player(pov.fullId))
      }
    }
  }

  def cancel(ownerId: String) = Open { implicit ctx ⇒
    HookRepo ownedHook ownerId flatMap {
      _ zmap Env.lobby.fisherman.delete inject Redirect(routes.Lobby.home)
    }
  }

  def log = Open { implicit ctx ⇒
    Env.lobby.messenger.recent(ctx.troll, 200) map { html.lobby.log(_) }
  }
}
