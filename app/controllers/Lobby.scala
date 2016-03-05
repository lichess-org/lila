package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }
import views._

object Lobby extends LilaController {

  def home = Open { implicit ctx =>
    negotiate(
      html = renderHome(Results.Ok).map(NoCache),
      api = _ => fuccess {
        Ok(Json.obj(
          "lobby" -> Json.obj(
            "version" -> Env.lobby.history.version)
        ))
      }
    )
  }

  def handleStatus(req: RequestHeader, status: Results.Status): Fu[Result] =
    reqToCtx(req) flatMap { ctx => renderHome(status)(ctx) }

  def renderHome(status: Results.Status)(implicit ctx: Context): Fu[Result] =
    Env.current.preloader(
      posts = Env.forum.recent(ctx.me, Env.team.cached.teamIds),
      tours = Env.tournament.cached promotable true,
      simuls = Env.simul allCreatedFeaturable true
    ) map (html.lobby.home.apply _).tupled map { status(_) } map ensureSessionId(ctx.req)

  def seeks = Open { implicit ctx =>
    negotiate(
      html = fuccess(NotFound),
      api = _ => ctx.me.fold(Env.lobby.seekApi.forAnon)(Env.lobby.seekApi.forUser) map { seeks =>
        Ok(JsArray(seeks.map(_.render)))
      }
    )
  }

  def socket(apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    get("sri") ?? { uid =>
      Env.lobby.socketHandler(uid = uid, ip = ctx.req.remoteAddress, user = ctx.me) map some
    }
  }

  def timeline = Auth { implicit ctx =>
    me =>
      Env.timeline.entryRepo.userEntries(me.id) map { html.timeline.entries(_) }
  }
}
