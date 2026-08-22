package lila.web

import play.api.mvc.*

import lila.common.{ HTTPRequest, ClientName }
import lila.core.config.BaseUrl

trait CtrlExtensions extends play.api.mvc.ControllerHelpers with ResponseHeaders:

  def baseUrl: BaseUrl

  extension (req: RequestHeader)
    def ipAddress = HTTPRequest.ipAddress(req)
    def client = ClientName(req)
    def sid = lila.core.security.LilaCookie.sid(req)

  extension (result: Result)
    def toFuccess = Future.successful(result)
    def flashSuccess(msg: String): Result = result.flashing("success" -> msg)
    def flashSuccess: Result = flashSuccess("")
    def flashFailure(msg: String): Result = result.flashing("failure" -> msg)
    def flashFailure: Result = flashFailure("")
    def withCanonical(url: String): Result =
      result.withHeaders(LINK -> s"<${baseUrl}${url}>; rel=\"canonical\"")
    def withCanonical(url: Call): Result = withCanonical(url.url)
    def enforceCrossSiteIsolation(using req: RequestHeader): Result =
      result.withHeaders(crossOriginPolicy.forReq(req)*)
    def noCache: Result = result.withHeaders(
      CACHE_CONTROL -> "no-cache, no-store, must-revalidate",
      EXPIRES -> "0"
    )
    def headerCacheSeconds(seconds: Int) = result.withHeaders(CACHE_CONTROL -> s"max-age=$seconds")
    def hasPersonalData = result.noCache
    def noProxyBuffer = result.withHeaders("X-Accel-Buffering" -> "no")
    def withServiceWorker(using RequestHeader) =
      result.enforceCrossSiteIsolation.withHeaders("Service-Worker-Allowed" -> "/")
    def asAttachment(name: String) =
      val cleanName = lila.common.String.fullCleanUp(name)
      result.withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=$cleanName")
    def asAttachmentStream(name: String) = result.noProxyBuffer.asAttachment(name)
