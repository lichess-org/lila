package lila.web

import play.api.i18n.Lang
import play.api.mvc.*

import lila.common.HTTPRequest
import lila.core.config.BaseUrl
import lila.core.i18n.Translate
import lila.core.perf.UserWithPerfs
import lila.ui.Context

trait CtrlExtensions extends play.api.mvc.ControllerHelpers with ResponseHeaders:

  def baseUrl: BaseUrl

  given (using ctx: Context): Lang          = ctx.lang
  given (using ctx: Context): Translate     = ctx.translate
  given (using ctx: Context): RequestHeader = ctx.req

  given Conversion[UserWithPerfs, User] = _.user

  extension (req: RequestHeader)
    def ipAddress = HTTPRequest.ipAddress(req)
    def sid       = lila.core.security.LilaCookie.sid(req)

  extension (result: Result)
    def toFuccess                         = Future.successful(result)
    def flashSuccess(msg: String): Result = result.flashing("success" -> msg)
    def flashSuccess: Result              = flashSuccess("")
    def flashFailure(msg: String): Result = result.flashing("failure" -> msg)
    def flashFailure: Result              = flashFailure("")
    def withCanonical(url: String): Result =
      result.withHeaders(LINK -> s"<${baseUrl}${url}>; rel=\"canonical\"")
    def withCanonical(url: Call): Result = withCanonical(url.url)
    def enforceCrossSiteIsolation(using req: RequestHeader): Result =
      result.withHeaders(crossOriginPolicy.forReq(req)*)
    def noCache: Result = result.withHeaders(
      CACHE_CONTROL -> "no-cache, no-store, must-revalidate",
      EXPIRES       -> "0"
    )
