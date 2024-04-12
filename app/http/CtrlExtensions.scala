package lila.app
package http

import play.api.mvc.*

import lila.common.HTTPRequest

trait CtrlExtensions extends ControllerHelpers with ResponseHeaders:

  val env: Env

  extension (req: RequestHeader)
    def ipAddress = HTTPRequest.ipAddress(req)
    def referer   = HTTPRequest.referer(req)
    def sid       = lila.security.LilaCookie.sid(req)

  extension (result: Result)
    def toFuccess                         = Future.successful(result)
    def flashSuccess(msg: String): Result = result.flashing("success" -> msg)
    def flashSuccess: Result              = flashSuccess("")
    def flashFailure(msg: String): Result = result.flashing("failure" -> msg)
    def flashFailure: Result              = flashFailure("")
    def withCanonical(url: String): Result =
      result.withHeaders(LINK -> s"<${env.net.baseUrl}${url}>; rel=\"canonical\"")
    def withCanonical(url: Call): Result = withCanonical(url.url)
    def enforceCrossSiteIsolation(using req: RequestHeader): Result =
      result.withHeaders(embedderPolicy.forReq(req)*)
    def noCache: Result = result.withHeaders(
      CACHE_CONTROL -> "no-cache, no-store, must-revalidate",
      EXPIRES       -> "0"
    )
