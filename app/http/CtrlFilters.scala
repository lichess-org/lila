package lila.app
package http

import play.api.http.*
import play.api.mvc.*
import play.api.libs.json.JsNumber

import lila.security.{ Permission, Granter }

import lila.common.HTTPRequest
import lila.common.config

trait CtrlFilters extends ControllerHelpers with ResponseBuilder with CtrlConversions:

  def isGranted(permission: Permission.Selector)(using Me): Boolean =
    Granter(permission(Permission))

  def isGrantedOpt(permission: Permission.Selector)(using Option[Me]): Boolean =
    isGranted(permission(Permission))

  def isGranted(permission: Permission)(using me: Option[Me]): Boolean =
    me.exists(Granter(permission)(using _))

  def NoCurrentGame(a: => Fu[Result])(using ctx: Context)(using Executor): Fu[Result] =
    ctx.me
      .soUse(env.preloader.currentGameMyTurn)
      .flatMap:
        _.fold(a): current =>
          negotiate(keyPages.home(Results.Forbidden), currentGameJsonError(current))

  private def currentGameJsonError(current: lila.app.mashup.Preload.CurrentGame) = fuccess:
    Forbidden(
      jsonError:
        s"You are already playing ${current.opponent}"
    ) as JSON

  def NoPlaybanOrCurrent(a: => Fu[Result])(using Context, Executor): Fu[Result] =
    NoPlayban(NoCurrentGame(a))

  def IfGranted(perm: Permission.Selector)(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if isGrantedOpt(perm) then f
    else negotiate(authorizationFailed, authorizationFailed)

  def Firewall[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] =
    if env.security.firewall.accepts(ctx.req) then a
    else keyPages.blacklisted

  def NoTor(res: => Fu[Result])(using ctx: Context)(using Executor): Fu[Result] =
    env.security.ipTrust
      .isPubOrTor(ctx.ip)
      .flatMap:
        if _ then Unauthorized.page(views.html.auth.bits.tor())
        else res

  def NoEngine[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] =
    if ctx.me.exists(_.marks.engine)
    then Forbidden.page(views.html.site.message.noEngine)
    else a

  def NoBooster[A <: Result](a: => Fu[A])(using ctx: Context): Fu[Result] =
    if ctx.me.exists(_.marks.boost)
    then Forbidden.page(views.html.site.message.noBooster)
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

  def NoPlayban(a: => Fu[Result])(using ctx: Context)(using Executor): Fu[Result] =
    ctx.userId
      .so(env.playban.api.currentBan)
      .flatMap:
        _.fold(a): ban =>
          negotiate(keyPages.home(Results.Forbidden), playbanJsonError(ban))

  private val csrfForbiddenResult = Forbidden("Cross origin request forbidden")

  def CSRF(f: => Fu[Result])(using req: RequestHeader): Fu[Result] =
    if env.security.csrfRequestHandler.check(req) then f else csrfForbiddenResult

  def XhrOnly(res: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then res else notFound

  def XhrOrRedirectHome(res: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then res
    else Redirect(controllers.routes.Lobby.home)

  def Reasonable(
      page: Int,
      max: config.Max = config.Max(40),
      errorPage: => Fu[Result] = BadRequest("resource too old")
  )(result: => Fu[Result]): Fu[Result] =
    if page < max.value && page > 0 then result else errorPage

  def NotForKids(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    if ctx.kid.no then f else notFound

  def NoCrawlers(result: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.isCrawler(ctx.req).yes then notFound else result

  def NotManaged(result: => Fu[Result])(using ctx: Context)(using Executor): Fu[Result] =
    ctx.me.so(env.clas.api.student.isManaged(_)) flatMap {
      if _ then notFound else result
    }
