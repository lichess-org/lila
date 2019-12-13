package lila.app
package http

import akka.stream.Materializer
import play.api.mvc._

import lila.common.HTTPRequest

final class HttpFilter(env: Env)(implicit val mat: Materializer) extends Filter {

  private val httpMon = lila.mon.http
  private val net     = env.net
  private val logger  = lila.log("http")

  def apply(nextFilter: RequestHeader => Fu[Result])(req: RequestHeader): Fu[Result] =
    if (HTTPRequest isAssets req) nextFilter(req)
    else {
      val startTime = nowMillis
      redirectWrongDomain(req) map fuccess getOrElse {
        nextFilter(req) dmap addApiResponseHeaders(req) dmap { result =>
          monitoring(req, startTime, result)
          result
        }
      }
    }

  private def monitoring(req: RequestHeader, startTime: Long, result: Result) = {
    val actionName = HTTPRequest actionName req
    val reqTime    = nowMillis - startTime
    val statusCode = result.header.status
    if (env.isDev) logger.info(s"$statusCode $req $actionName ${reqTime}ms")
    else {
      val client = HTTPRequest clientName req
      httpMon.time(actionName, client, req.method, statusCode).record(reqTime)
    }
  }

  private def redirectWrongDomain(req: RequestHeader): Option[Result] =
    (
      req.host != net.domain.value &&
        HTTPRequest.isRedirectable(req) &&
        !HTTPRequest.isProgrammatic(req) &&
        // asset request going through the CDN, don't redirect
        !(req.host == net.assetDomain.value && HTTPRequest.hasFileExtension(req))
    ) option Results.MovedPermanently(s"http${if (req.secure) "s" else ""}://${net.domain}${req.uri}")

  private def addApiResponseHeaders(req: RequestHeader)(result: Result) =
    if (HTTPRequest.isApiOrApp(req))
      result.withHeaders(ResponseHeaders.headersForApiOrApp(req): _*)
    else
      result
}
