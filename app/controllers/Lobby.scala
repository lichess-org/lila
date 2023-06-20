package controllers

import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.app.{ *, given }

import lila.common.Json.given

final class Lobby(env: Env) extends LilaController(env):

  private lazy val lobbyJson = Json.obj(
    "lobby" -> Json.obj(
      "version" -> 0,
      "pools"   -> lila.pool.PoolList.json
    ),
    "assets" -> Json.obj(
      "domain" -> env.net.assetDomain
    )
  )

  def home = Open:
    pageHit
    negotiate(
      html = serveHtmlHome,
      api = _ =>
        val expiration = 60 * 60 * 24 * 7 // set to one hour, one week before changing the pool config
        Ok(lobbyJson).withHeaders(CACHE_CONTROL -> s"max-age=$expiration")
    )

  private def serveHtmlHome(using ctx: WebContext) =
    env.pageCache { () =>
      keyPages.homeHtml.map: html =>
        Ok(html).withCanonical("").noCache
    } map env.lilaCookie.ensure(ctx.req)

  def homeLang(lang: String) =
    staticRedirect(lang).map(Action.async(_)) getOrElse
      LangPage("/")(serveHtmlHome)(lang)

  def handleStatus(status: Results.Status)(using RequestHeader): Fu[Result] =
    webContext.flatMap: ctx =>
      keyPages.home(status)(using ctx)

  def seeks = Open:
    negotiate(
      html = NotFound,
      api = _ =>
        ctx.me.fold(env.lobby.seekApi.forAnon)(env.lobby.seekApi.forUser(_)) map { seeks =>
          Ok(JsArray(seeks.map(_.render))).withHeaders(CACHE_CONTROL -> s"max-age=10")
        }
    )

  def timeline = Auth { _ ?=> me ?=>
    env.timeline.entryApi
      .userEntries(me)
      .map: entries =>
        Ok(html.timeline.entries(entries)).withHeaders(CACHE_CONTROL -> s"max-age=20")
  }
