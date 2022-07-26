package lila.common

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import play.api.routing.Router

object HTTPRequest {

  def isXhr(req: RequestHeader): Boolean =
    req.headers get "X-Requested-With" contains "XMLHttpRequest"

  def isSynchronousHttp(req: RequestHeader) = !isXhr(req)

  def isEventSource(req: RequestHeader): Boolean =
    req.headers get "Accept" contains "text/event-stream"

  def isSafe(req: RequestHeader)   = req.method == "GET" || req.method == "HEAD" || req.method == "OPTIONS"
  def isUnsafe(req: RequestHeader) = !isSafe(req)

  def isRedirectable(req: RequestHeader) = isSynchronousHttp(req) && isSafe(req)

  def isProgrammatic(req: RequestHeader) =
    !isSynchronousHttp(req) || isFishnet(req) || isApi(req) || req.headers
      .get(HeaderNames.ACCEPT)
      .exists(_ startsWith "application/vnd.lichess.v")

  private val appOrigins = List(
    "capacitor://localhost", // ios
    "ionic://localhost",     // ios
    "http://localhost"       // android/dev/flutter
  )

  def appOrigin(req: RequestHeader): Option[String] =
    origin(req) filter { reqOrigin =>
      appOrigins exists { appOrigin =>
        reqOrigin == appOrigin || reqOrigin.startsWith(s"$appOrigin:")
      }
    }

  def isApi(req: RequestHeader)      = req.path startsWith "/api/"
  def isApiOrApp(req: RequestHeader) = isApi(req) || appOrigin(req).isDefined

  def isAssets(req: RequestHeader) = req.path startsWith "/assets/"

  def userAgent(req: RequestHeader): Option[String] = req.headers get HeaderNames.USER_AGENT

  val isAndroid = UaMatcher("""(?i)android.+mobile""")
  val isMobile  = UaMatcher("""(?i)iphone|ipad|ipod|android.+mobile""")

  private def uaContains(req: RequestHeader, str: String) = userAgent(req).exists(_ contains str)
  def isChrome(req: RequestHeader)                        = uaContains(req, "Chrome/")
  val isChrome96OrMore                                    = UaMatcher("""Chrome/(?:\d{3,}|9[6-9])""")

  def origin(req: RequestHeader): Option[String] = req.headers get HeaderNames.ORIGIN

  def referer(req: RequestHeader): Option[String] = req.headers get HeaderNames.REFERER

  def ipAddress(req: RequestHeader) =
    IpAddress.unchecked {
      req.remoteAddress.split(", ").lastOption | req.remoteAddress // trusted
    }

  def sid(req: RequestHeader): Option[String] = req.session get LilaCookie.sessionId

  val isCrawler = UaMatcher {
    """(?i)googlebot|googlebot-mobile|googlebot-image|mediapartners-google|bingbot|slurp|java|wget|curl|python-requests|commons-httpclient|python-urllib|libwww|httpunit|nutch|phpcrawl|msnbot|adidxbot|blekkobot|teoma|ia_archiver|gingercrawler|webmon|httrack|webcrawler|fast-webcrawler|fastenterprisecrawler|convera|biglotron|grub\.org|usinenouvellecrawler|antibot|netresearchserver|speedy|fluffy|jyxobot|bibnum\.bnf|findlink|exabot|gigabot|msrbot|seekbot|ngbot|panscient|yacybot|aisearchbot|ioi|ips-agent|tagoobot|mj12bot|dotbot|woriobot|yanga|buzzbot|mlbot|purebot|lingueebot|yandex\.com/bots|""" +
      """voyager|cyberpatrol|voilabot|baiduspider|citeseerxbot|spbot|twengabot|postrank|turnitinbot|scribdbot|page2rss|sitebot|linkdex|ezooms|dotbot|mail\.ru|discobot|zombie\.js|heritrix|findthatfile|europarchive\.org|nerdbynature\.bot|sistrixcrawler|ahrefsbot|aboundex|domaincrawler|wbsearchbot|summify|ccbot|edisterbot|seznambot|ec2linkfinder|gslfbot|aihitbot|intelium_bot|yeti|retrevopageanalyzer|lb-spider|sogou|lssbot|careerbot|wotbox|wocbot|ichiro|duckduckbot|lssrocketcrawler|drupact|webcompanycrawler|acoonbot|openindexspider|gnamgnamspider|web-archive-net\.com\.bot|backlinkcrawler|""" +
      """coccoc|integromedb|contentcrawlerspider|toplistbot|seokicks-robot|it2media-domain-crawler|ip-web-crawler\.com|siteexplorer\.info|elisabot|proximic|changedetection|blexbot|arabot|wesee:search|niki-bot|crystalsemanticsbot|rogerbot|360spider|psbot|interfaxscanbot|lipperheyseoservice|ccmetadatascaper|g00g1e\.net|grapeshotcrawler|urlappendbot|brainobot|fr-crawler|binlar|simplecrawler|simplecrawler|livelapbot|twitterbot|cxensebot|smtbot|facebookexternalhit|daumoa|sputnikimagebot|visionutils|yisouspider|parsijoobot|mediatoolkit\.com|semrushbot"""
  }

  case class UaMatcher(rStr: String) {
    private val regex = rStr.r

    def apply(req: RequestHeader): Boolean = userAgent(req) ?? regex.find
  }

  def isFishnet(req: RequestHeader) = req.path startsWith "/fishnet/"

  def isHuman(req: RequestHeader) = !isCrawler(req) && !isFishnet(req)

  private[this] val fileExtensionRegex = """\.(?<!^\.)[a-zA-Z0-9]{2,4}$""".r

  def hasFileExtension(req: RequestHeader) = fileExtensionRegex.find(req.path)

  def weirdUA(req: RequestHeader) = userAgent(req).fold(true)(_.lengthIs < 30)

  def print(req: RequestHeader) = s"${printReq(req)} ${printClient(req)}"

  def printReq(req: RequestHeader) = s"${req.method} ${req.domain}${req.uri}"

  def printClient(req: RequestHeader) =
    s"${ipAddress(req)} origin:${~origin(req)} referer:${~referer(req)} ua:${~userAgent(req)}"

  def bearer(req: RequestHeader): Option[Bearer] =
    req.headers.get(HeaderNames.AUTHORIZATION).flatMap { authorization =>
      val prefix = "Bearer "
      authorization.startsWith(prefix) option Bearer(authorization.stripPrefix(prefix))
    }

  def isOAuth(req: RequestHeader) = bearer(req).isDefined

  def acceptsNdJson(req: RequestHeader) = req.headers get HeaderNames.ACCEPT contains "application/x-ndjson"
  def acceptsJson(req: RequestHeader)   = req.headers get HeaderNames.ACCEPT contains "application/json"
  def acceptsCsv(req: RequestHeader)    = req.headers get HeaderNames.ACCEPT contains "text/csv"

  def actionName(req: RequestHeader): String =
    req.attrs.get(Router.Attrs.ActionName).getOrElse("NoHandler")

  private val ApiVersionHeaderPattern = """application/vnd\.lichess\.v(\d++)\+json""".r

  def apiVersion(req: RequestHeader): Option[ApiVersion] = {
    req.headers.get(HeaderNames.ACCEPT) flatMap {
      case ApiVersionHeaderPattern(v) => v.toIntOption map ApiVersion.apply
      case _                          => none
    }
  }

  private def isDataDump(req: RequestHeader) = req.path == "/account/personal-data"
  private def isAppeal(req: RequestHeader)   = req.path.startsWith("/appeal")
  private def isGameExport(req: RequestHeader) =
    "^/@/[\\w-]{2,30}/download$".r.matches(req.path) ||
      "^/(api/games/user|games/export)/[\\w-]{2,30}($|/.+)".r.matches(req.path)
  private def isStudyExport(req: RequestHeader) = "^/study/by/[\\w-]{2,30}/export.pgn$".r matches req.path

  def isClosedLoginPath(req: RequestHeader) =
    isDataDump(req) || isAppeal(req) || isStudyExport(req) || isGameExport(req)

  def clientName(req: RequestHeader) =
    // the mobile app sends XHR headers
    if (isXhr(req)) apiVersion(req).fold("xhr") { v =>
      s"mobile/$v"
    }
    else if (isCrawler(req)) "crawler"
    else "browser"
}
