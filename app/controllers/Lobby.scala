package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.IpAddress
import views._

object Lobby extends LilaController {

  private val lobbyJson = Json.obj(
    "lobby" -> Json.obj(
      "version" -> 0,
      "pools" -> Env.api.lobbyApi.poolsJson
    )
  )

  def home = Open { implicit ctx =>
    negotiate(
      html = renderHome(Results.Ok).map(NoCache),
      api = _ => fuccess {
        val expiration = 60 * 60 * 24 * 7 // set to one hour, one week before changing the pool config
        Ok(lobbyJson).withHeaders(CACHE_CONTROL -> s"max-age=$expiration")
      }
    )
  }

  def handleStatus(req: RequestHeader, status: Results.Status): Fu[Result] =
    reqToCtx(req) flatMap { ctx => renderHome(status)(ctx) }

  def renderHome(status: Results.Status)(implicit ctx: Context): Fu[Result] = {
    pageHit
    Env.current.preloader(
      posts = Env.forum.recent(ctx.me, Env.team.cached.teamIdsList).nevermind,
      tours = Env.tournament.cached.promotable.get.nevermind,
      events = Env.event.api.promoteTo(ctx.req).nevermind,
      simuls = Env.simul.allCreatedFeaturable.get.nevermind
    ) map (html.lobby.home.apply _).tupled dmap { html =>
      ensureSessionId(ctx.req)(status(html))
    }
  }.mon(_.http.response.home)

  def seeks = Open { implicit ctx =>
    negotiate(
      html = fuccess(NotFound),
      api = _ => ctx.me.fold(Env.lobby.seekApi.forAnon)(Env.lobby.seekApi.forUser) map { seeks =>
        Ok(JsArray(seeks.map(_.render)))
      }
    )
  }

  private val MessageLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 40,
    duration = 10 seconds,
    name = "lobby socket message per IP",
    key = "lobby_socket.message.ip",
    enforce = Env.api.Net.RateLimit
  )

  def socket(apiVersion: Int) = SocketOptionLimited[JsValue](MessageLimitPerIP, "lobby") { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      Env.lobby.socketHandler(uid, user = ctx.me, mobile = getBool("mobile"), apiVersion) map some
    }
  }

  def timeline = Auth { implicit ctx => me =>
    Env.timeline.entryApi.userEntries(me.id) map { html.timeline.entries(_) }
  }
}
