package lila.app
package http
import alleycats.Zero
import play.api.http.*
import play.api.libs.json.*
import play.api.mvc.*

import lila.common.HTTPRequest
import lila.core.net.ApiVersion
import lila.ui.{ Page, Snippet }

trait ResponseBuilder(using Executor)
    extends lila.web.ResponseBuilder
    with lila.web.CtrlExtensions
    with RequestContext
    with CtrlPage:

  val keyPages = KeyPages(env)
  export env.net.baseUrl

  given (using Context): Zero[Fu[Result]] = Zero(notFound)

  def Found[A](a: Fu[Option[A]])(f: A => Fu[Result])(using Context): Fu[Result] =
    a.flatMap(_.fold(notFound)(f))

  def FoundEmbed[A](a: Fu[Option[A]])(f: A => Fu[Result])(using EmbedContext): Fu[Result] =
    a.flatMap(_.fold(notFoundEmbed)(f))

  def Found[A](a: Option[A])(f: A => Fu[Result])(using Context): Fu[Result] =
    a.fold(notFound)(f)

  def FoundOk[A, B: Writeable](fua: Fu[Option[A]])(op: A => Fu[B])(using Context): Fu[Result] =
    Found(fua): a =>
      op(a).dmap(Ok(_))

  def FoundPage[A](fua: Fu[Option[A]])(op: A => Fu[Page])(using Context): Fu[Result] =
    Found(fua): a =>
      Ok.async(op(a))

  def FoundSnip[A](fua: Fu[Option[A]])(op: A => Fu[Snippet])(using Context): Fu[Result] =
    Found(fua): a =>
      Ok.snipAsync(op(a).map(_.frag))

  extension [A](fua: Fu[Option[A]])
    def orNotFound(f: A => Fu[Result])(using Context): Fu[Result] =
      fua.flatMap { _.fold(notFound)(f) }
  extension [A](fua: Fu[Boolean])
    def elseNotFound(f: => Fu[Result])(using Context): Fu[Result] =
      fua.flatMap { if _ then f else notFound }

  def rateLimited(using Context): Fu[Result] = rateLimited(rateLimitedMsg)
  def rateLimited(msg: String = rateLimitedMsg)(using ctx: Context): Fu[Result] = negotiate(
    html =
      if HTTPRequest.isSynchronousHttp(ctx.req)
      then TooManyRequests.page(views.site.message.rateLimited(msg))
      else TooManyRequests(msg).toFuccess,
    json = TooManyRequests(jsonError(msg))
  )

  def negotiateApi(html: => Fu[Result], api: ApiVersion => Fu[Result])(using ctx: Context): Fu[Result] =
    lila.security.Mobile.Api
      .requestVersion(ctx.req)
      .match
        case Some(v) => api(v).dmap(_.withHeaders(VARY -> "Accept").as(JSON))
        case None    => negotiate(html, api(ApiVersion.mobile))

  def negotiate(html: => Fu[Result], json: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.acceptsJson(ctx.req) || ctx.isOAuth
    then json.dmap(_.withHeaders(VARY -> "Accept").as(JSON))
    else html.dmap(_.withHeaders(VARY -> "Accept"))

  def negotiateJson(result: => Fu[Result])(using Context): Fu[Result] =
    negotiate(
      notFound("This endpoint only returns JSON, add the header `Accept: application/json`".some),
      result
    )

  def notFound(using ctx: Context): Fu[Result] = notFound(none)
  def notFound(msg: Option[String])(using ctx: Context): Fu[Result] =
    negotiate(
      html =
        if HTTPRequest.isSynchronousHttp(ctx.req)
        then keyPages.notFound(msg)
        else msg.fold(notFoundText())(notFoundText),
      json = msg.fold(notFoundJson())(notFoundJson)
    )

  def notFoundEmbed(using EmbedContext): Fu[Result]                      = notFoundEmbed(none)
  def notFoundEmbed(msg: Option[String])(using EmbedContext): Fu[Result] = keyPages.notFoundEmbed(msg)

  def authenticationFailed(using ctx: Context): Fu[Result] =
    negotiate(
      html = Redirect(
        if HTTPRequest.isClosedLoginPath(ctx.req)
        then routes.Auth.login.url
        else
          HTTPRequest.queryStringGet(ctx.req, "login") match
            case Some(login) => s"${routes.Auth.login.url}?as=$login"
            case _           => routes.Auth.signup.url
      ).withCookies(env.security.lilaCookie.session(env.security.api.AccessUri, ctx.req.uri)),
      json = env.security.lilaCookie.ensure(ctx.req):
        Unauthorized(jsonError("Login required"))
    )

  def authorizationFailed(using ctx: Context): Fu[Result] =
    if HTTPRequest.isSynchronousHttp(ctx.req)
    then Forbidden.page(views.site.message.authFailed)
    else
      fuccess:
        render:
          case Accepts.Json() => forbiddenJson()
          case _              => forbiddenText()

  def serverError(msg: String)(using ctx: Context): Fu[Result] =
    negotiate(
      InternalServerError.page(views.site.message.serverError(msg)),
      InternalServerError(jsonError(msg))
    )

  def notForBotAccounts(using Context): Fu[Result] = negotiate(
    Forbidden.page(views.site.message.noBot),
    forbiddenJson("This API endpoint is not for Bot accounts.")
  )

  def notForLameAccounts(using Context, Me): Fu[Result] = negotiate(
    Forbidden.page(views.site.message.noLame),
    forbiddenJson("The access to this resource is restricted.")
  )

  def playbanJsonError(ban: lila.playban.TempBan): Result =
    Forbidden(
      jsonError(
        s"Banned from playing for ${ban.remainingMinutes} minutes. Reason: Too many aborts, unplayed games, or rage quits."
      ) + ("minutes" -> JsNumber(ban.remainingMinutes))
    ).as(JSON)
