package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.app._
import views._

final class Lobby(
    env: Env
) extends LilaController(env) {

  private lazy val lobbyJson = Json.obj(
    "lobby" -> Json.obj(
      "version" -> 0
    ),
    "assets" -> Json.obj(
      "domain" -> env.net.assetDomain.value
    )
  )

  def home =
    Open { implicit ctx =>
      pageHit
      negotiate(
        html = env.pageCache { () =>
          keyPages.homeHtml.dmap { html =>
            Ok(html).noCache
          }
        } dmap env.lilaCookie.ensure(ctx.req),
        api = _ => fuccess(Ok(lobbyJson))
      )
    }

  def handleStatus(req: RequestHeader, status: Results.Status): Fu[Result] =
    reqToCtx(req) flatMap { ctx =>
      keyPages.home(status)(ctx)
    }

  def seeks =
    Open { implicit ctx =>
      negotiate(
        html = fuccess(NotFound),
        api = _ =>
          ctx.me.fold(env.lobby.seekApi.forAnon)(env.lobby.seekApi.forUser) map { seeks =>
            Ok(JsArray(seeks.map(_.render))).withHeaders(CACHE_CONTROL -> s"max-age=10")
          }
      )
    }

  def timeline =
    Auth { implicit ctx => me =>
      env.timeline.entryApi.userEntries(me.id) map { entries =>
        Ok(html.timeline.entries(entries)).withHeaders(CACHE_CONTROL -> s"max-age=20")
      }
    }
}
