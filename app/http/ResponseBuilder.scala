package lila.app
package http

import play.api.libs.json.*
import play.api.http.*
import play.api.mvc.*
import alleycats.Zero

import lila.common.{ HTTPRequest, ApiVersion }

trait ResponseBuilder(using Executor)
    extends ControllerHelpers
    with RequestContext
    with ResponseWriter
    with CtrlExtensions
    with CtrlConversions
    with CtrlPage
    with CtrlErrors:

  val keyPages = KeyPages(env)
  export scalatags.Text.Frag

  given Conversion[Result, Fu[Result]]    = fuccess(_)
  given Conversion[Frag, Fu[Frag]]        = fuccess(_)
  given (using Context): Zero[Fu[Result]] = Zero(notFound)

  def Found[A](a: Fu[Option[A]])(f: A => Fu[Result])(using Context): Fu[Result] =
    a.flatMap(_.fold(notFound)(f))

  def Found[A](a: Option[A])(f: A => Fu[Result])(using Context): Fu[Result] =
    a.fold(notFound)(f)

  def FoundOk[A, B: Writeable](fua: Fu[Option[A]])(op: A => Fu[B])(using Context): Fu[Result] =
    Found(fua): a =>
      op(a).dmap(Ok(_))

  def FoundPage[A](fua: Fu[Option[A]])(op: A => PageContext ?=> Fu[Frag])(using Context): Fu[Result] =
    Found(fua): a =>
      Ok.pageAsync(op(a))

  extension [A](fua: Fu[Option[A]])
    def orNotFound(f: A => Fu[Result])(using Context): Fu[Result] =
      fua flatMap { _.fold(notFound)(f) }
  extension [A](fua: Fu[Boolean])
    def elseNotFound(f: => Fu[Result])(using Context): Fu[Result] =
      fua flatMap { if _ then f else notFound }

  val rateLimitedMsg                         = "Too many requests. Try again later."
  val rateLimitedJson                        = TooManyRequests(jsonError(rateLimitedMsg))
  def rateLimited(using Context): Fu[Result] = rateLimited(rateLimitedMsg)
  def rateLimited(msg: String = rateLimitedMsg)(using ctx: Context): Fu[Result] = negotiate(
    html =
      if HTTPRequest.isSynchronousHttp(ctx.req)
      then TooManyRequests.page(views.html.site.message.rateLimited(msg))
      else TooManyRequests(msg).toFuccess,
    json = TooManyRequests(jsonError(msg))
  )

  val jsonOkBody   = Json.obj("ok" -> true)
  val jsonOkResult = JsonOk(jsonOkBody)

  def JsonOk(body: JsValue): Result               = Ok(body) as JSON
  def JsonOk[A: Writes](body: A): Result          = Ok(Json toJson body) as JSON
  def JsonOk[A: Writes](fua: Fu[A]): Fu[Result]   = fua.dmap(JsonOk)
  def JsonOptionOk[A: Writes](fua: Fu[Option[A]]) = fua.map(_.fold(notFoundJson())(JsonOk))
  def JsonStrOk(str: JsonStr): Result             = Ok(str) as JSON
  def JsonBadRequest(body: JsValue): Result       = BadRequest(body) as JSON
  def JsonBadRequest(msg: String): Result         = JsonBadRequest(jsonError(msg))

  def negotiateApi(html: => Fu[Result], api: ApiVersion => Fu[Result])(using ctx: Context): Fu[Result] =
    lila.security.Mobile.Api
      .requestVersion(ctx.req)
      .fold(html): v =>
        api(v).dmap(_ as JSON)
      .dmap(_.withHeaders(VARY -> "Accept"))

  def negotiate(html: => Fu[Result], json: => Fu[Result])(using ctx: Context): Fu[Result] =
    if HTTPRequest.acceptsJson(ctx.req) || ctx.isOAuth
    then json.dmap(_.withHeaders(VARY -> "Accept").as(JSON))
    else html.dmap(_.withHeaders(VARY -> "Accept"))

  def negotiateJson(result: => Fu[Result])(using Context) = negotiate(notFound, result)

  def notFound(using ctx: Context): Fu[Result] =
    negotiate(
      html =
        if HTTPRequest.isSynchronousHttp(ctx.req)
        then keyPages.notFound
        else notFoundText(),
      json = notFoundJson()
    )

  def authenticationFailed(using ctx: Context): Fu[Result] =
    negotiate(
      html = Redirect(
        if HTTPRequest.isClosedLoginPath(ctx.req)
        then controllers.routes.Auth.login
        else controllers.routes.Auth.signup
      ) withCookies env.lilaCookie.session(env.security.api.AccessUri, ctx.req.uri),
      json = env.lilaCookie.ensure(ctx.req):
        Unauthorized(jsonError("Login required"))
    )

  private val forbiddenJsonResult = Forbidden(jsonError("Authorization failed"))

  def authorizationFailed(using ctx: Context): Fu[Result] =
    if HTTPRequest.isSynchronousHttp(ctx.req)
    then Forbidden.page(views.html.site.message.authFailed)
    else
      fuccess:
        render:
          case Accepts.Json() => forbiddenJsonResult
          case _              => Results.Forbidden("Authorization failed")

  def serverError(msg: String)(using ctx: Context): Fu[Result] =
    negotiate(
      InternalServerError.page(views.html.site.message.serverError(msg)),
      InternalServerError(jsonError(msg))
    )

  def notForBotAccounts(using Context) = negotiate(
    Forbidden.page(views.html.site.message.noBot),
    Forbidden(jsonError("This API endpoint is not for Bot accounts."))
  )

  def notForLameAccounts(using Context, Me) = negotiate(
    Forbidden.page(views.html.site.message.noLame),
    Forbidden(jsonError("The access to this resource is restricted."))
  )

  def playbanJsonError(ban: lila.playban.TempBan) =
    Forbidden(
      jsonError(
        s"Banned from playing for ${ban.remainingMinutes} minutes. Reason: Too many aborts, unplayed games, or rage quits."
      ) + ("minutes" -> JsNumber(ban.remainingMinutes))
    ) as JSON

  def redirectWithQueryString(path: String)(using req: RequestHeader) =
    Redirect:
      if req.target.uriString.contains("?")
      then s"$path?${req.target.queryString}"
      else path

  val movedMap: Map[String, String] = Map(
    "swag" -> "https://shop.spreadshirt.com/lichess-org",
    "yt"   -> "https://www.youtube.com/c/LichessDotOrg",
    "dmca" -> "https://docs.google.com/forms/d/e/1FAIpQLSdRVaJ6Wk2KHcrLcY0BxM7lTwYSQHDsY2DsGwbYoLUBo3ngfQ/viewform",
    "fishnet" -> "https://github.com/lichess-org/fishnet",
    "qa"      -> "/faq",
    "help"    -> "/contact",
    "support" -> "/contact",
    "donate"  -> "/patron"
  )
  def staticRedirect(key: String): Option[Fu[Result]] =
    movedMap get key map { MovedPermanently(_) }
