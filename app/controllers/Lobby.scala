package controllers

import play.api.libs.json._
import play.api.mvc._
import play.twirl.api.Html
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
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
      api = _ => fuccess(Ok(lobbyJson))
    )
  }

  def handleStatus(req: RequestHeader, status: Results.Status): Fu[Result] = {
    reqToCtx(req) flatMap { ctx => renderHome(status)(ctx) }
  }

  def renderHome(status: Results.Status)(implicit ctx: Context): Fu[Result] = {
    HomeCache(ctx) map { status(_) } map ensureSessionId(ctx.req)
  }.mon(_.http.response.home)

  def seeks = Open { implicit ctx =>
    negotiate(
      html = fuccess(NotFound),
      api = _ => ctx.me.fold(Env.lobby.seekApi.forAnon)(Env.lobby.seekApi.forUser) map { seeks =>
        Ok(JsArray(seeks.map(_.render)))
      }
    )
  }

  private val MessageLimitPerIP = new lila.memo.RateLimit(
    credits = 40,
    duration = 10 seconds,
    name = "lobby socket message per IP",
    key = "lobby_socket.message.ip")

  def socket(apiVersion: Int) = SocketOptionLimited[JsValue](MessageLimitPerIP, "lobby") { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      Env.lobby.socketHandler(uid, user = ctx.me, mobile = getBool("mobile")) map some
    }
  }

  def timeline = Auth { implicit ctx => me =>
    Env.timeline.entryRepo.userEntries(me.id) map { html.timeline.entries(_) }
  }

  private object HomeCache {

    private case class RequestKey(
      uri: String,
      headers: Headers)

    private val cache = Env.memo.asyncCache.multi[RequestKey, Html](
      name = "lobby.homeCache",
      f = renderRequestKey,
      expireAfter = _.ExpireAfterWrite(1 second))

    private def renderCtx(implicit ctx: Context): Fu[Html] = Env.current.preloader(
      posts = Env.forum.recent(ctx.me, Env.team.cached.teamIdsList).nevermind,
      tours = Env.tournament.cached.promotable.get.nevermind,
      events = Env.event.api.promotable.get.nevermind,
      simuls = Env.simul.allCreatedFeaturable.get.nevermind
    ) map (html.lobby.home.apply _).tupled

    private def renderRequestKey(r: RequestKey): Fu[Html] = renderCtx {
      lila.mon.lobby.cache.miss()
      val req = new RequestHeader {
        def id = 1000l
        def tags = Map.empty
        def uri = r.uri
        def path = "/"
        def method = "GET"
        def version = "1.1"
        def queryString = Map.empty
        def headers = r.headers
        def remoteAddress = "0.0.0.0"
        def secure = true
      }
      new lila.api.HeaderContext(
        headerContext = new lila.user.HeaderUserContext(req, none),
        data = lila.api.PageData default Env.api.assetVersion.get)
    }

    def apply(ctx: Context) =
      if (ctx.isAuth) {
        lila.mon.lobby.cache.user()
        renderCtx(ctx)
      }
      else {
        lila.mon.lobby.cache.anon()
        cache get RequestKey(
          uri = ctx.req.uri,
          headers = new Headers(
          List(HOST -> ctx.req.host) :::
            ctx.req.headers.get(COOKIE).?? { cookie =>
              List(COOKIE -> cookie)
            }
        ))
      }
  }
}
