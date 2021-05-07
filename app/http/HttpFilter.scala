package lila.app
package http

import akka.stream.Materializer
import play.api.mvc._

import lila.common.HTTPRequest

final class HttpFilter(env: Env)(implicit val mat: Materializer) extends Filter {

  private val httpMon     = lila.mon.http
  private val net         = env.net
  private val logger      = lila.log("http")
  private val logRequests = env.config.get[Boolean]("net.http.log")

  def apply(nextFilter: RequestHeader => Fu[Result])(req: RequestHeader): Fu[Result] =
    if (HTTPRequest isAssets req) nextFilter(req) dmap { result =>
      result.withHeaders(
        "Service-Worker-Allowed"       -> "/",
        "Cross-Origin-Embedder-Policy" -> "require-corp"
      )
    }
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
    val client     = HTTPRequest clientName req
    if (env.net.isProd) httpMon.time(actionName, client, req.method, statusCode).record(reqTime)
    else if (logRequests) logger.info(s"$statusCode $client $req $actionName ${reqTime}ms")
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
