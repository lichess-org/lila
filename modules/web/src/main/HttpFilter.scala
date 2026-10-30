package lila.web

import org.apache.pekko.stream.Materializer
import play.api.http.Status.*
import play.api.mvc.*

import lila.common.{ ClientName, HTTPRequest }
import lila.core.config.NetConfig
import lila.core.net.LichessMobileUa

final class HttpFilter(
    net: NetConfig,
    parseMobileUa: RequestHeader => Option[LichessMobileUa]
)(using val mat: Materializer, scheduler: Scheduler)(using Executor)
    extends Filter
    with ResponseHeaders:

  private def logger = lila.log("http")

  def apply(handle: RequestHeader => Fu[Result])(req: RequestHeader): Fu[Result] =
    if HTTPRequest.isAssets(req) then serveAssets(handle(req))
    else
      val startTime = nowMillis
      redirectWrongDomain(req)
        .map(fuccess)
        .getOrElse:
          val lilaReq = toLilaReq(using req)
          WarmupFilter(lilaReq).getOrElse:
            handle(lilaReq).map: result =>
              monitoring(lilaReq, startTime):
                addContextualResponseHeaders(lilaReq):
                  addEmbedderPolicyHeaders(lilaReq):
                    result

  private def toLilaReq(using req: RequestHeader) =
    val clientName =
      import ClientName.*
      if HTTPRequest.isXhr(req) then if HTTPRequest.isLichobile then lichobile else xhr
      else if HTTPRequest.isLichessMobile then mobile
      else if req.headers.get("X-Crawler").contains("1") then crawler
      else if crawlerMatcher(req) then crawler
      else if req.path.startsWith("/fishnet/") then fishnet
      else browser
    req.addAttr(ClientName.reqAttr, clientName)

  private object WarmupFilter:
    private var justBooted = true
    scheduler.scheduleOnce(20.seconds):
      justBooted = false
    private val response = fuccess(Results.ServiceUnavailable("Please retry in a few seconds"))
    private val actions = Set(
      "Account.info",
      "User.redirect",
      "Api.crosstable",
      "Main.playerCount",
      "Tournament.featured",
      "Game.apiExportByUser",
      "Push.mobileRegister",
      "Api.cloudEval",
      "User.autocomplete"
    )
    def apply(req: RequestHeader): Option[Fu[Result]] =
      val reject = justBooted && {
        actions(HTTPRequest.actionName(req)) ||
        req.path.startsWith("/api/bot/") ||
        !ClientName(req).isHuman
      }
      Option.when(reject)(response)

  private val crawlerMatcher = HTTPRequest.UaMatcher:
    // spiders/crawlers
    """Qwantbot|Googlebot|GoogleOther|AdsBot|Google-Read-Aloud|bingbot|BingPreview|facebookexternalhit|meta-externalagent|SemrushBot|AhrefsBot|PetalBot|Applebot|YandexBot|YandexAdNet|YandexImages|YandexRenderResourcesBot|Twitterbot|Bluesky|Baiduspider|Amazonbot|Bytespider|yacybot|ImagesiftBot|ChatGLM-Spider|YisouSpider|Yeti/|DataForSeoBot|ChatGPT|openai\.com|anthropic\.com|anthropic\.ai|TikTokSpider|MJ12bot|SeznamBot|Mwmbl|DotBot|IABot|rednote-websearch-bot|kagi-fetcher|kagibot|Bravebot|SofyaBot|SearchBot|AdsBot|GPTBot|Diffbot|CCBot|ClaudeBot|Claude-Web|Google-Extended|Google-CloudVertexBot|omgili|cohere-ai|MistralAI-Training|Awario|QualifiedBot|Cotoyogi|ICC-Crawler|atlassian-bot|FishBot|BorderxBot|NavuBot|WARDBot|magpie-crawler|KimiBot|CitibotSiteCrawler""" +
      // apps and servers that load previews
      """|Discordbot|WhatsApp""" +
      // http libs
      """|HeadlessChrome|okhttp|axios|undici|wget|curl|python-requests|aiohttp|commons-httpclient|python-urllib|python-httpx|Nessus|imroc/req"""

  private def monitoring(req: RequestHeader, startTime: Long)(result: Result) =
    val actionName = HTTPRequest.actionName(req)
    val reqTime = nowMillis - startTime
    val statusCode = result.header.status
    val mobile = parseMobileUa(req)
    val client = ClientName(req)
    lila.mon.http.count(actionName, client.name, req.method, statusCode).increment()
    lila.mon.http.time(actionName).record(reqTime)
    if net.logRequests then logger.info(s"$statusCode $client $req $actionName ${reqTime}ms")
    mobile.foreach(lila.mon.http.mobileCount(actionName, _))
    HttpFilter
      .apiAgent(req, client)
      .foreach: agent =>
        lila.mon.http.apiAgentCount(actionName, agent).increment()
    result

  private def serveAssets(res: Fu[Result]) =
    res.dmap:
      _.withHeaders(assetsHeaders*)

  private def redirectWrongDomain(req: RequestHeader): Option[Result] = {
    req.host != net.domain.value &&
    HTTPRequest.isRedirectable(using req) &&
    !HTTPRequest.isProgrammatic(req) &&
    // asset request, don't redirect
    !HTTPRequest.hasFileExtension(req)
  }.option(Results.MovedPermanently(s"http${if req.secure then "s" else ""}://${net.domain}${req.uri}"))

  private def addContextualResponseHeaders(req: RequestHeader)(result: Result) =
    if HTTPRequest.isApiOrApp(req)
    then result.withHeaders(headersForApiOrApp(using req)*)
    else if result.header.status == OK
    then result.withHeaders(permissionsPolicyHeader)
    else result

  private def addEmbedderPolicyHeaders(req: RequestHeader)(result: Result) =
    if result.header.status != NO_CONTENT
      && !crossOriginPolicy.isSet(result)
      && crossOriginPolicy.supportsCredentiallessIFrames(req)
    then result.withHeaders(crossOriginPolicy.credentialless*)
    else result

private object HttpFilter:

  import scalalib.net.UserAgent

  def apiAgent(req: RequestHeader, client: ClientName): Option[String] =
    val isApiAgent = !client.isMobile && !client.isLichobile && {
      HTTPRequest.isApi(req) || req.path.endsWith(".pgn")
    }
    isApiAgent.option(apiAgent(HTTPRequest.userAgent(req)))

  def apiAgent(ua: UserAgent): String = ua.value
    .dropWhile(!_.isLetterOrDigit)
    .takeWhile: c =>
      c != '/' && c != '(' && c != '@'
    .filterNot(_.isDigit)
    .trim
    .toLowerCase
    .nonEmptyOption | "-"

  def isProgrammatic(req: RequestHeader) =
    HTTPRequest.isXhr(req) ||
      ClientName(req).isFishnet ||
      HTTPRequest.isApi(req) ||
      isPrometheus(req) ||
      HTTPRequest.accepts(req).exists(_.startsWith("application/vnd.lichess.v")) ||
      req.path.startsWith("/prometheus-metrics/")
