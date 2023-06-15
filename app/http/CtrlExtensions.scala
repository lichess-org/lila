package lila.app
package http

import play.api.mvc.*
import lila.common.HTTPRequest

trait CtrlExtensions extends ControllerHelpers:

  val env: Env

  extension (req: RequestHeader)
    def ipAddress = HTTPRequest.ipAddress(req)
    def referer   = HTTPRequest.referer(req)
    def sid       = HTTPRequest.sid(req)

  extension (result: Result)
    def toFuccess                         = Future successful result
    def flashSuccess(msg: String): Result = result.flashing("success" -> msg)
    def flashSuccess: Result              = flashSuccess("")
    def flashFailure(msg: String): Result = result.flashing("failure" -> msg)
    def flashFailure: Result              = flashFailure("")
    def withCanonical(url: String): Result =
      result.withHeaders(LINK -> s"<${env.net.baseUrl}${url}>; rel=\"canonical\"")
    def withCanonical(url: Call): Result = withCanonical(url.url)
    def enableSharedArrayBuffer(using req: RequestHeader): Result = {
      if HTTPRequest.isChrome96Plus(req) then
        result.withHeaders("Cross-Origin-Embedder-Policy" -> "credentialless")
      else if HTTPRequest.isFirefox114Plus(req) && env.firefoxOriginTrial.get().nonEmpty then
        result.withHeaders(
          "Origin-Trial"                 -> env.firefoxOriginTrial.get(),
          "Cross-Origin-Embedder-Policy" -> "credentialless"
        )
      else result.withHeaders("Cross-Origin-Embedder-Policy" -> "require-corp")
    }.withHeaders("Cross-Origin-Opener-Policy" -> "same-origin")
    def noCache: Result = result.withHeaders(
      CACHE_CONTROL -> "no-cache, no-store, must-revalidate",
      EXPIRES       -> "0"
    )
