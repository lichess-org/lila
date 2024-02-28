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
    def enableSharedArrayBuffer(using req: RequestHeader): Result =
      val coep =
        if HTTPRequest
            .isChrome96Plus(req) || (HTTPRequest.isFirefox119Plus(req) && !HTTPRequest.isMobileBrowser(
            req
          )) || (!HTTPRequest.isFirefox119Plus(req) && HTTPRequest.isMobileBrowser(req))
        then "credentialless"
        else "require-corp"
      result.withHeaders(
        "Cross-Origin-Embedder-Policy" -> coep,
        "Cross-Origin-Opener-Policy"   -> "same-origin"
      )
    def noCache: Result = result.withHeaders(
      CACHE_CONTROL -> "no-cache, no-store, must-revalidate",
      EXPIRES       -> "0"
    )
