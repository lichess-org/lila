package lila.web

import akka.stream.Materializer
import play.api.mvc.*

import lila.common.HTTPRequest
import lila.core.config.NetConfig
import lila.core.net.LichessMobileUa

final class HttpFilter(
    net: NetConfig,
    sitewideCoepCredentiallessHeader: () => Boolean,
    parseMobileUa: RequestHeader => Option[LichessMobileUa]
)(using val mat: Materializer)(using Executor)
    extends Filter
    with ResponseHeaders:

  private val logger = lila.log("http")

  def apply(handle: RequestHeader => Fu[Result])(req: RequestHeader): Fu[Result] =
    if HTTPRequest.isAssets(req) then serveAssets(req, handle(req))
    else
      val startTime = nowMillis
      redirectWrongDomain(req).map(fuccess).getOrElse {
        handle(req).map: result =>
          monitoring(req, startTime):
            addContextualResponseHeaders(req):
              addEmbedderPolicyHeaders(req):
                result
      }

  private def monitoring(req: RequestHeader, startTime: Long)(result: Result) =
    val actionName = HTTPRequest.actionName(req)
    val reqTime    = nowMillis - startTime
    val statusCode = result.header.status
    val mobile     = parseMobileUa(req)
    val client     = if mobile.isDefined then "mobile" else HTTPRequest.clientName(req)
    lila.mon.http.time(actionName, client, req.method, statusCode).record(reqTime)
    if net.logRequests then logger.info(s"$statusCode $client $req $actionName ${reqTime}ms")
    mobile.foreach: m =>
      lila.mon.http.mobile(actionName, m.version, m.userId.isDefined, m.osName).record(reqTime)
    result

  private def serveAssets(req: RequestHeader, res: Fu[Result]) =
    res.dmap:
      _.withHeaders(assetsHeaders*)

  private def redirectWrongDomain(req: RequestHeader): Option[Result] = {
    req.host != net.domain.value &&
    HTTPRequest.isRedirectable(req) &&
    !HTTPRequest.isProgrammatic(req) &&
    // asset request going through the CDN, don't redirect
    !(req.host == net.assetDomain.value && HTTPRequest.hasFileExtension(req))
  }.option(Results.MovedPermanently(s"http${if req.secure then "s" else ""}://${net.domain}${req.uri}"))

  private def addContextualResponseHeaders(req: RequestHeader)(result: Result) =
    if HTTPRequest.isApiOrApp(req)
    then result.withHeaders(headersForApiOrApp(using req)*)
    else result.withHeaders(permissionsPolicyHeader)

  private def addEmbedderPolicyHeaders(req: RequestHeader)(result: Result) =
    if !crossOriginPolicy.isSet(result)
      && crossOriginPolicy.supportsCredentiallessIFrames(req)
      && sitewideCoepCredentiallessHeader()
    then result.withHeaders(crossOriginPolicy.credentialless*)
    else result
