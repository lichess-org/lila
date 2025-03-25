package lila.common

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import play.api.routing.Router

import scala.util.matching.Regex

import lila.common.Form.trueish
import lila.core.net.*

object HTTPRequest:

  def isXhr(req: RequestHeader): Boolean =
    req.headers.get("X-Requested-With").contains("XMLHttpRequest")

  def isSynchronousHttp(req: RequestHeader) = !isXhr(req)

  def isSafe(req: RequestHeader)   = req.method == "GET" || req.method == "HEAD" || req.method == "OPTIONS"
  def isUnsafe(req: RequestHeader) = !isSafe(req)

  def isRedirectable(req: RequestHeader) = isSynchronousHttp(req) && isSafe(req) && !isLichessMobile(req)

  def isXhrFromEmbed(req: RequestHeader) =
    isXhr(req) && referer(req).exists(_.contains(s"${req.host}/embed/"))

  private val appOrigins = List(
    "capacitor://localhost", // ios
    "ionic://localhost",     // ios
    "http://localhost"       // android/dev/flutter
  )

  def appOrigin(req: RequestHeader): Option[String] =
    origin(req).filter: reqOrigin =>
      appOrigins.exists: appOrigin =>
        reqOrigin == appOrigin || reqOrigin.startsWith(s"$appOrigin:")

  def isApi(req: RequestHeader)      = req.path.startsWith("/api/")
  def isApiOrApp(req: RequestHeader) = isApi(req) || appOrigin(req).isDefined

  def isAssets(req: RequestHeader) = req.path.startsWith("/assets/")

  def userAgent(req: RequestHeader): Option[UserAgent] = UserAgent.from:
    req.headers.get(HeaderNames.USER_AGENT)

  val isChrome96Plus                               = UaMatcher("""Chrome/(?:\d{3,}|9[6-9])""")
  val isChrome113Plus                              = UaMatcher("""Chrome/(?:11[3-9]|1[2-9]\d)""")
  val isFirefox119Plus                             = UaMatcher("""Firefox/(?:119|1[2-9]\d)""")
  val isMobileBrowser                              = UaMatcher("""(?i)iphone|ipad|ipod|android.+mobile""")
  def isLichessMobile(ua: UserAgent): Boolean      = ua.value.startsWith("Lichess Mobile/")
  def isLichessMobile(req: RequestHeader): Boolean = userAgent(req).exists(isLichessMobile)
  def isLichobile(req: RequestHeader)              = userAgent(req).exists(_.value.contains("Lichobile/"))
  def isLichobileDev(req: RequestHeader) = // lichobile in a browser can't set its user-agent
    isLichobile(req) || (appOrigin(req).isDefined && !isLichessMobile(req))
  def isAndroid                     = UaMatcher("Android")
  def isLitools(req: RequestHeader) = userAgent(req).has(UserAgent("litools"))

  def origin(req: RequestHeader): Option[String]  = req.headers.get(HeaderNames.ORIGIN)
  def referer(req: RequestHeader): Option[String] = req.headers.get(HeaderNames.REFERER)

  def ipAddress(req: RequestHeader) =
    IpAddress.unchecked:
      // chain of trusted proxies, strip scope id
      req.remoteAddress.split(", ").last.split("%").head

  def isCrawler(req: RequestHeader) = Crawler(crawlerMatcher(req))

  private val crawlerMatcher = UaMatcher:
    // spiders/crawlers
    """Googlebot|GoogleOther|AdsBot|Google-Read-Aloud|bingbot|BingPreview|facebookexternalhit|meta-externalagent|SemrushBot|AhrefsBot|PetalBot|Applebot|YandexBot|YandexAdNet|YandexImages|Twitterbot|Baiduspider|Amazonbot|Bytespider|yacybot|ImagesiftBot|ChatGLM-Spider|YisouSpider|Yeti/|DataForSeoBot""" +
      // apps and servers that load previews
      """|Discordbot|WhatsApp""" +
      // http libs
      """|HeadlessChrome|okhttp|axios|undici|wget|curl|python-requests|aiohttp|commons-httpclient|python-urllib|python-httpx|Nessus|imroc/req"""

  def isImagePreviewCrawler(req: RequestHeader) = Crawler(imagePreviewCrawlerMatcher(req))

  private val imagePreviewCrawlerMatcher = UaMatcher:
    """BingPreview|Discordbot|WhatsApp"""

  final class UaMatcher(rStr: String):
    private val pattern                    = rStr.r.pattern
    def apply(req: RequestHeader): Boolean = userAgent(req).exists(ua => pattern.matcher(ua.value).find)

  def uaMatches(req: RequestHeader, regex: Regex): Boolean =
    userAgent(req).exists(ua => regex.find(ua.value))

  def isFishnet(req: RequestHeader) = req.path.startsWith("/fishnet/")

  def isHuman(req: RequestHeader) = isCrawler(req).no && !isFishnet(req)

  private val fileExtensionRegex = """\.(?<!^\.)[a-zA-Z0-9]{2,4}$""".r

  def hasFileExtension(req: RequestHeader) = fileExtensionRegex.find(req.path)

  def print(req: RequestHeader) = s"${printReq(req)} ${printClient(req)}"

  def printReq(req: RequestHeader) = s"${req.method} ${req.domain}${req.uri}"

  def printClient(req: RequestHeader) =
    s"${ipAddress(req)} origin:${~origin(req)} referer:${~referer(req)} ua:${userAgent(req).so(_.value)}"

  def bearer(req: RequestHeader): Option[Bearer] = for
    authorization <- req.headers.get(HeaderNames.AUTHORIZATION)
    prefix = "Bearer "
    if authorization.startsWith(prefix)
  yield Bearer(authorization.stripPrefix(prefix))

  def isOAuth(req: RequestHeader) = bearer(req).isDefined

  private val webXhrAccepts                       = "application/web.lichess+json"
  def startsWithLichobileAccepts(a: String)       = a.startsWith("application/vnd.lichess.v")
  def accepts(req: RequestHeader): Option[String] = req.headers.get(HeaderNames.ACCEPT)
  def acceptsNdJson(req: RequestHeader)           = accepts(req) contains "application/x-ndjson"
  def acceptsJson(req: RequestHeader) = accepts(req).exists: a =>
    a == webXhrAccepts || a.startsWith("application/json") || startsWithLichobileAccepts(a)
  def acceptsCsv(req: RequestHeader)             = accepts(req) contains "text/csv"
  def isEventSource(req: RequestHeader): Boolean = accepts(req) contains "text/event-stream"
  def isProgrammatic(req: RequestHeader) =
    !isSynchronousHttp(req) || isFishnet(req) || isApi(req) ||
      accepts(req).exists(startsWithLichobileAccepts)

  def actionName(req: RequestHeader): String =
    req.attrs.get(Router.Attrs.ActionName).getOrElse("NoHandler")

  private val LichobileVersionHeaderPattern = """application/vnd\.lichess\.v(\d++)\+json""".r

  def apiVersion(req: RequestHeader): Option[ApiVersion] =
    accepts(req).flatMap:
      case LichobileVersionHeaderPattern(v) => ApiVersion.from(v.toIntOption)
      case _                                => none

  private def isDataDump(req: RequestHeader) = req.path == "/account/personal-data"
  private def isAppeal(req: RequestHeader)   = req.path.startsWith("/appeal")
  private def isGameExport(req: RequestHeader) =
    "^/@/[\\w-]{2,30}/download$".r.matches(req.path) ||
      "^/(api/games/user|games/export)/[\\w-]{2,30}($|/.+)".r.matches(req.path)
  private def isStudyExport(req: RequestHeader) = "^/study/by/[\\w-]{2,30}/export.pgn$".r.matches(req.path)
  private def isAccountClose(req: RequestHeader) =
    req.path == "/account/close" || req.path == "/account/delete"

  def isClosedLoginPath(req: RequestHeader) =
    isDataDump(req) || isAppeal(req) || isStudyExport(req) || isGameExport(req) || isAccountClose(req)

  def clientName(req: RequestHeader) =
    // lichobile sends XHR headers
    if isXhr(req) then apiVersion(req).fold("xhr")(v => s"lichobile/$v")
    else if isLichessMobile(req) then "mobile"
    else if isCrawler(req).yes then "crawler"
    else "browser"

  def queryStringGet(req: RequestHeader, name: String): Option[String] =
    req.queryString.get(name).flatMap(_.headOption).filter(_.nonEmpty)

  def looksLikeLichessBot(req: RequestHeader) =
    userAgent(req).exists: ua =>
      ua.value.startsWith("lichess-bot/") || ua.value.startsWith("maia-bot/")

  // this header is set by our nginx config, based on the nginx whitelist file.
  def nginxWhitelist(req: RequestHeader) =
    req.headers.get("X-Ip-Tier").flatMap(_.toIntOption).exists(_ > 1)

  def isKid(req: RequestHeader) =
    req.headers.get("X-Lichess-KidMode").exists(trueish)
