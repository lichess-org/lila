package lila.app
package http

import alleycats.Zero
import play.api.http.*
import play.api.mvc.*

import lila.common.HTTPRequest
import lila.core.perm.{ Granter, Permission }
import lila.core.security.IsProxy

trait CtrlFilters(using Executor) extends ControllerHelpers with ResponseBuilder:

  export Granter.{ apply as isGranted, opt as isGrantedOpt }

  def NoCurrentGame(a: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx
      .useMe(env.preloader.currentGameMyTurn)
      .flatMap:
        _.fold(a): current =>
          negotiate(keyPages.home(Results.Forbidden), currentGameJsonError(current))

  private def currentGameJsonError(current: lila.app.mashup.Preload.CurrentGame) = fuccess:
    Forbidden(
      jsonError:
        s"You are already playing ${current.opponent}"
    ).as(JSON)

  def NoPlaybanOrCurrent(a: => Fu[Result])(using Context): Fu[Result] =
    NoPlayban(NoCurrentGame(a))

  def IfGranted(perm: Permission.Selector)(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if isGrantedOpt(perm) then f
    else negotiate(authorizationFailed, authorizationFailed)

  def Firewall(a: => Fu[Result])(using ctx: Context): Fu[Result] =
    if env.security.firewall.accepts(ctx.req) then a
    else keyPages.blacklisted

  def WithProxy[A](res: IsProxy ?=> Fu[A])(using req: RequestHeader): Fu[A] =
    env.security.ip2proxy.ofIp(req.ipAddress).flatMap(res(using _))

  def NoTor(res: => Fu[Result])(using ctx: Context): Fu[Result] =
    env.security.ipTrust
      .isPubOrTor(ctx.req)
      .flatMap:
        if _ then Unauthorized.page(views.auth.pubOrTor)
        else res

  def NoEngine[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] =
    if ctx.me.exists(_.marks.engine)
    then Forbidden.page(views.site.message.noEngine)
    else a

  def NoBooster[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] =
    if ctx.me.exists(_.marks.boost)
    then Forbidden.page(views.site.message.noBooster)
    else a

  def NoLame[A <: Result](a: => Fu[A])(using Context): Fu[Result] =
    NoEngine(NoBooster(a))

  def NoBot[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] =
    if ctx.isBot then notForBotAccounts
    else a

  def NoLameOrBotOpt[A <: Result](a: => Fu[A])(using Context): Fu[Result] =
    NoLame(NoBot(a))

  def NoLameOrBot[A <: Result](a: => Fu[A])(using me: Me)(using Context): Fu[Result] =
    if me.isBot then notForBotAccounts
    else if me.lame then notForLameAccounts
    else a

  def NoShadowban[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] =
    if ctx.me.exists(_.marks.troll) then notFound else a

  def NoPlayban(a: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.userId
      .so(env.playban.api.currentBan)
      .flatMap:
        _.fold(a): ban =>
          negotiate(keyPages.home(Results.Forbidden), playbanJsonError(ban))

  def AuthOrTrustedIp(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if ctx.isAuth then f
    else
      env.security.ip2proxy
        .ofReq(ctx.req)
        .flatMap: ip =>
          if ip.isSafeish then f
          else Redirect(routes.Auth.login)

  private val csrfForbiddenResult = Forbidden("Cross origin request forbidden")

  def CSRF(f: => Fu[Result])(using req: RequestHeader): Fu[Result] =
    if env.security.csrfRequestHandler.check(req) then f else csrfForbiddenResult

  def XhrOnly(res: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then res else notFound

  def XhrOrRedirectHome(res: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then res
    else Redirect(routes.Lobby.home)

  def Reasonable(
      page: Int,
      max: Max = Max(40),
      errorPage: => Fu[Result] = BadRequest("resource too old")
  )(result: => Fu[Result]): Fu[Result] =
    if page <= max.value && page > 0 then result else errorPage

  def NotForKids(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if ctx.kid.no then f else notFound

  def NoCrawlers(result: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isCrawler(ctx.req).yes then notFound else result

  def NoCrawlersUnlessPreview(result: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isCrawler(ctx.req).yes && HTTPRequest.isImagePreviewCrawler(ctx.req).no
    then notFound
    else result

  def NoCrawlers[A](computation: => A)(using ctx: Context, default: Zero[A]): A =
    if HTTPRequest.isCrawler(ctx.req).yes then default.zero else computation

  def NotManaged(result: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me
      .so(env.clas.api.student.isManaged(_))
      .flatMap:
        if _ then notFound else result
