package lila.app
package http

import play.api.libs.json.*
import play.api.http.*
import play.api.mvc.*

import lila.api.context.*
import lila.common.{ HTTPRequest, ApiVersion }

trait ResponseBuilder(using Executor)
    extends ControllerHelpers
    with ResponseWriter
    with CtrlExtensions
    with CtrlConversions:

  val keyPages = KeyPages(env)
  export keyPages.{ notFound as renderNotFound }

  given Conversion[scalatags.Text.Frag, Result]     = Ok(_)
  given Conversion[Result, Fu[Result]]              = fuccess(_)
  given Conversion[scalatags.Text.Frag, Fu[Result]] = html => fuccess(Ok(html))
  given (using RequestHeader): Conversion[Funit, Fu[Result]] =
    _ => negotiate(fuccess(Ok("ok")), _ => fuccess(jsonOkResult))
  given alleycats.Zero[Result] = alleycats.Zero(Results.NotFound)

  val rateLimitedMsg             = "Too many requests. Try again later."
  val rateLimited                = Results.TooManyRequests(rateLimitedMsg)
  val rateLimitedJson            = Results.TooManyRequests(jsonError(rateLimitedMsg))
  val rateLimitedJsonFu          = rateLimitedJson.toFuccess
  val rateLimitedFu              = rateLimited.toFuccess
  def rateLimitedFu(msg: String) = Results.TooManyRequests(jsonError(msg)).toFuccess

  val jsonOkBody   = Json.obj("ok" -> true)
  val jsonOkResult = JsonOk(jsonOkBody)

  def JsonOk(body: JsValue): Result             = Ok(body) as JSON
  def JsonOk[A: Writes](body: A): Result        = Ok(Json toJson body) as JSON
  def JsonOk[A: Writes](fua: Fu[A]): Fu[Result] = fua dmap { JsonOk(_) }
  def JsonOptionOk[A: Writes](fua: Fu[Option[A]]) =
    fua.flatMap:
      _.fold(notFoundJson())(a => fuccess(JsonOk(a)))
  def JsonStrOk(str: JsonStr): Result       = Ok(str) as JSON
  def JsonBadRequest(body: JsValue): Result = BadRequest(body) as JSON

  def negotiate(html: => Fu[Result], api: ApiVersion => Fu[Result])(using
      req: RequestHeader
  ): Fu[Result] =
    lila.api.Mobile.Api
      .requestVersion(req)
      .fold(html): v =>
        api(v).dmap(_ as JSON)
      .dmap(_.withHeaders("Vary" -> "Accept"))

  def jsonError[A: Writes](err: A): JsObject = Json.obj("error" -> err)

  def notFoundJsonSync(msg: String = "Not found"): Result = NotFound(jsonError(msg)) as JSON

  def notFoundJson(msg: String = "Not found"): Fu[Result] = fuccess(notFoundJsonSync(msg))

  def notForBotAccounts = JsonBadRequest(jsonError("This API endpoint is not for Bot accounts."))

  def ridiculousBackwardCompatibleJsonError(err: JsObject): JsObject =
    err ++ Json.obj("error" -> err)

  def notFound(using ctx: WebContext): Fu[Result] =
    negotiate(
      html =
        if HTTPRequest.isSynchronousHttp(ctx.req)
        then fuccess(renderNotFound)
        else fuccess(Results.NotFound("Resource not found")),
      api = _ => notFoundJson("Resource not found")
    )

  def authenticationFailed(using ctx: WebContext): Fu[Result] =
    negotiate(
      html = fuccess:
        Redirect(
          if HTTPRequest.isClosedLoginPath(ctx.req)
          then controllers.routes.Auth.login
          else controllers.routes.Auth.signup
        ) withCookies env.lilaCookie.session(env.security.api.AccessUri, ctx.req.uri)
      ,
      api = _ =>
        env.lilaCookie
          .ensure(ctx.req):
            Unauthorized(jsonError("Login required"))
    )

  private val forbiddenJsonResult = Forbidden(jsonError("Authorization failed"))

  def authorizationFailed(using ctx: WebContext): Fu[Result] =
    negotiate(
      html =
        if HTTPRequest.isSynchronousHttp(ctx.req)
        then Forbidden(views.html.site.message.authFailed)
        else Results.Forbidden("Authorization failed"),
      api = _ => fuccess(forbiddenJsonResult)
    )
  def authorizationFailed(req: RequestHeader): Fu[Result] =
    negotiate(
      html = fuccess(Results.Forbidden("Authorization failed")),
      api = _ => fuccess(forbiddenJsonResult)
    )(using req)

  def playbanJsonError(ban: lila.playban.TempBan) = fuccess:
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
