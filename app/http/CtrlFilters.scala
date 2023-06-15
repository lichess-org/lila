package lila.app
package http

import play.api.http.*
import play.api.mvc.*
import play.api.libs.json.JsNumber

import lila.api.context.*
import lila.security.{ Permission, Granter }
import lila.user.User
import lila.common.HTTPRequest
import lila.common.config

trait CtrlFilters extends ControllerHelpers with ResponseBuilder with CtrlConversions:

  def isGranted(permission: Permission.Selector, user: User): Boolean =
    Granter(permission(Permission))(user)

  def isGranted(permission: Permission.Selector)(using AnyContext): Boolean =
    isGranted(permission(Permission))

  def isGranted(permission: Permission)(using ctx: AnyContext): Boolean =
    ctx.me so Granter(permission)

  def NoCurrentGame(a: => Fu[Result])(using ctx: WebContext)(using Executor): Fu[Result] =
    ctx.me.so(env.preloader.currentGameMyTurn) flatMap {
      _.fold(a): current =>
        negotiate(
          html = keyPages.home(Results.Forbidden),
          api = _ => currentGameJsonError(current)
        )
    }
  def NoCurrentGame(me: Option[User])(a: => Fu[Result])(using Executor): Fu[Result] = me
    .so(env.preloader.currentGameMyTurn)
    .flatMap:
      _.fold(a)(currentGameJsonError)

  private def currentGameJsonError(current: lila.app.mashup.Preload.CurrentGame) = fuccess:
    Forbidden(
      jsonError:
        s"You are already playing ${current.opponent}"
    ) as JSON

  def NoPlaybanOrCurrent(a: => Fu[Result])(using WebContext, Executor): Fu[Result] =
    NoPlayban(NoCurrentGame(a))
  def NoPlaybanOrCurrent(me: Option[User])(a: => Fu[Result])(using Executor): Fu[Result] =
    NoPlayban(me.map(_.id))(NoCurrentGame(me)(a))

  def IfGranted(perm: Permission.Selector)(f: => Fu[Result])(using ctx: AnyContext): Fu[Result] =
    if isGranted(perm) then f
    else
      ctx match
        case web: WebContext => authorizationFailed(using web)
        case _               => authorizationFailed(ctx.req)

  def IfGranted(perm: Permission.Selector, req: RequestHeader, me: User)(f: => Fu[Result]): Fu[Result] =
    if isGranted(perm, me) then f else authorizationFailed(req)

  def Firewall[A <: Result](a: => Fu[A])(using ctx: WebContext): Fu[Result] =
    if env.security.firewall.accepts(ctx.req) then a
    else keyPages.blacklisted

  def NoTor(res: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    if env.security.tor.isExitNode(ctx.ip)
    then Unauthorized(views.html.auth.bits.tor())
    else res

  def NoEngine[A <: Result](a: => Fu[A])(using ctx: WebContext): Fu[Result] =
    if ctx.me.exists(_.marks.engine)
    then Forbidden(views.html.site.message.noEngine)
    else a

  def NoBooster[A <: Result](a: => Fu[A])(using ctx: WebContext): Fu[Result] =
    if ctx.me.exists(_.marks.boost)
    then Forbidden(views.html.site.message.noBooster)
    else a

  def NoLame[A <: Result](a: => Fu[A])(using WebContext): Fu[Result] =
    NoEngine(NoBooster(a))

  def NoBot[A <: Result](a: => Fu[A])(using ctx: AnyContext): Fu[Result] =
    if ctx.isBot then
      ctx match
        case web: WebContext => Forbidden(views.html.site.message.noBot(using web))
        case _               => Forbidden(jsonError("no bots allowed"))
    else a

  def NoLameOrBot[A <: Result](a: => Fu[A])(using WebContext): Fu[Result] =
    NoLame(NoBot(a))

  def NoLameOrBot[A <: Result](me: User)(a: => Fu[A]): Fu[Result] =
    if me.isBot then notForBotAccounts
    else if me.lame then Forbidden
    else a

  def NoShadowban[A <: Result](a: => Fu[A])(using ctx: WebContext): Fu[Result] =
    if (ctx.me.exists(_.marks.troll)) notFound else a

  def NoPlayban(a: => Fu[Result])(using ctx: WebContext)(using Executor): Fu[Result] =
    ctx.userId
      .so(env.playban.api.currentBan)
      .flatMap:
        _.fold(a): ban =>
          negotiate(
            html = keyPages.home(Results.Forbidden),
            api = _ => playbanJsonError(ban)
          )

  def NoPlayban(userId: Option[UserId])(a: => Fu[Result])(using Executor): Fu[Result] = userId
    .so(env.playban.api.currentBan)
    .flatMap:
      _.fold(a)(playbanJsonError)

  import env.security.csrfRequestHandler.check as csrfCheck
  val csrfForbiddenResult = Forbidden("Cross origin request forbidden")

  def CSRF(req: RequestHeader)(f: => Fu[Result]): Fu[Result] =
    if csrfCheck(req) then f else csrfForbiddenResult

  def XhrOnly(res: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then res else notFound

  def XhrOrRedirectHome(res: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then res
    else Redirect(controllers.routes.Lobby.home)

  def Reasonable(
      page: Int,
      max: config.Max = config.Max(40),
      errorPage: => Fu[Result] = BadRequest("resource too old")
  )(result: => Fu[Result]): Fu[Result] =
    if page < max.value && page > 0 then result else errorPage

  def NotForKids(f: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    if ctx.kid then notFound else f

  def NoCrawlers(result: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    if HTTPRequest.isCrawler(ctx.req).yes then notFound else result

  def NotManaged(result: => Fu[Result])(using ctx: WebContext)(using Executor): Fu[Result] =
    ctx.me.so(env.clas.api.student.isManaged) flatMap {
      if _ then notFound else result
    }
