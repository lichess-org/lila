package lila.app
package http

import akka.stream.Materializer
import play.api.mvc.*

import lila.common.HTTPRequest

final class HttpFilter(env: Env)(using val mat: Materializer)(using Executor)
    extends Filter
    with ResponseHeaders:

  private val logger      = lila.log("http")
  private val logRequests = env.config.get[Boolean]("net.http.log")

  def apply(handle: RequestHeader => Fu[Result])(req: RequestHeader): Fu[Result] =
    if HTTPRequest.isAssets(req) then serveAssets(req, handle(req))
    else
      val startTime = nowMillis
      redirectWrongDomain(req) map fuccess getOrElse {
        handle(req).map: result =>
          monitoring(req, startTime):
            addApiResponseHeaders(req):
              addCrendentialless(req):
                result
      }

  private def monitoring(req: RequestHeader, startTime: Long)(result: Result) =
    val actionName = HTTPRequest actionName req
    val reqTime    = nowMillis - startTime
    val statusCode = result.header.status
    val mobile     = lila.security.Mobile.LichessMobileUa.parse(req)
    val client     = if mobile.isDefined then "mobile" else HTTPRequest clientName req
    lila.mon.http.time(actionName, client, req.method, statusCode).record(reqTime)
    if logRequests then logger.info(s"$statusCode $client $req $actionName ${reqTime}ms")
    mobile.foreach: m =>
      lila.mon.http.mobile(actionName, m.version, m.userId.isDefined, m.osName).record(reqTime)
    result

  private def serveAssets(req: RequestHeader, res: Fu[Result]) =
    res.dmap:
      _.withHeaders(assetsHeaders*)

  private def redirectWrongDomain(req: RequestHeader): Option[Result] = {
    req.host != env.net.domain.value &&
    HTTPRequest.isRedirectable(req) &&
    !HTTPRequest.isProgrammatic(req) &&
    // asset request going through the CDN, don't redirect
    !(req.host == env.net.assetDomain.value && HTTPRequest.hasFileExtension(req))
  } option Results.MovedPermanently(s"http${if req.secure then "s" else ""}://${env.net.domain}${req.uri}")

  private def addApiResponseHeaders(req: RequestHeader)(result: Result) =
    if HTTPRequest.isApiOrApp(req)
    then result.withHeaders(headersForApiOrApp(using req)*)
    else result

  private def addCrendentialless(req: RequestHeader)(result: Result): Result =
    val actionName = HTTPRequest actionName req
    if actionName != "Plan.index" && actionName != "Plan.list" &&
      HTTPRequest.uaMatches(req, env.credentiallessUaRegex.get())
    then result.withHeaders(credentiallessHeaders*)
    else result
