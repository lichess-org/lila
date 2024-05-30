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
      .exists(_ startsWith "application/vnd.lishogi.v")

  private val appOrigins = Set(
    "capacitor://localhost", // ios
    "ionic://localhost",     // ios
    "http://localhost",      // android
    "http://192.168.1.154",
    "http://localhost:8080", // local dev
    "http://localhost:8100", // local dev
    "file://"                // old app
  )

  def appOrigin(req: RequestHeader) = origin(req) filter appOrigins

  def isApi(req: RequestHeader)      = req.path startsWith "/api/"
  def isApiOrApp(req: RequestHeader) = isApi(req) || appOrigin(req).isDefined

  def isAssets(req: RequestHeader) = req.path startsWith "/assets/"

  def userAgent(req: RequestHeader): Option[String] = req.headers get HeaderNames.USER_AGENT

  val isAndroid = UaMatcher("""(?i)android.+mobile""")
  val isMobile  = UaMatcher("""(?i)iphone|ipad|ipod|android.+mobile""")

  private def uaContains(req: RequestHeader, str: String) = userAgent(req).exists(_ contains str)
  def isChrome(req: RequestHeader)                        = uaContains(req, "Chrome/")

  // chrome 96+, firefox 119+
  val isChrome96Plus   = UaMatcher("""Chrome/(?:\d{3,}|9[6-9])""")
  val isFirefox119Plus = UaMatcher("""Firefox/(?:119|1[2-9]\d|[2-9]\d\d)""")
  val isMobielBrowser  = UaMatcher("""(?i)iphone|ipad|ipod|android.+mobile""")
  def supportsCredentialless(req: RequestHeader) =
    isChrome96Plus(req) || (!isMobielBrowser(req) && isFirefox119Plus(req))

  def origin(req: RequestHeader): Option[String] = req.headers get HeaderNames.ORIGIN

  def referer(req: RequestHeader): Option[String] = req.headers get HeaderNames.REFERER

  def lastRemoteAddress(req: RequestHeader) =
    IpAddress {
      req.remoteAddress.split(", ").lastOption | req.remoteAddress
    }

  def sid(req: RequestHeader): Option[String] = req.session get LilaCookie.sessionId

  val userSessionIdKey = "sessionId"

  def userSessionId(req: RequestHeader): Option[String] =
    req.session.get(userSessionIdKey) orElse req.headers.get(userSessionIdKey)

  val isCrawler = UaMatcher {
    ("""(?i)googlebot|googlebot-mobile|googlebot-image|mediapartners-google|bingbot|slurp|java|wget|curl|commons-httpclient|python-urllib|libwww|httpunit|nutch|phpcrawl|msnbot|adidxbot|blekkobot|teoma|ia_archiver|gingercrawler|webmon|httrack|webcrawler|fast-webcrawler|fastenterprisecrawler|convera|biglotron|grub\.org|usinenouvellecrawler|antibot|netresearchserver|speedy|fluffy|jyxobot|bibnum\.bnf|findlink|exabot|gigabot|msrbot|seekbot|ngbot|panscient|yacybot|aisearchbot|ioi|ips-agent|tagoobot|mj12bot|dotbot|woriobot|yanga|buzzbot|mlbot|purebot|lingueebot|yandex\.com/bots|""" +
      """voyager|cyberpatrol|voilabot|baiduspider|citeseerxbot|spbot|twengabot|postrank|turnitinbot|scribdbot|page2rss|sitebot|linkdex|ezooms|dotbot|mail\.ru|discobot|zombie\.js|heritrix|findthatfile|europarchive\.org|nerdbynature\.bot|sistrixcrawler|ahrefsbot|aboundex|domaincrawler|wbsearchbot|summify|ccbot|edisterbot|seznambot|ec2linkfinder|gslfbot|aihitbot|intelium_bot|yeti|retrevopageanalyzer|lb-spider|sogou|lssbot|careerbot|wotbox|wocbot|ichiro|duckduckbot|lssrocketcrawler|drupact|webcompanycrawler|acoonbot|openindexspider|gnamgnamspider|web-archive-net\.com\.bot|backlinkcrawler|""" +
      """coccoc|integromedb|contentcrawlerspider|toplistbot|seokicks-robot|it2media-domain-crawler|ip-web-crawler\.com|siteexplorer\.info|elisabot|proximic|changedetection|blexbot|arabot|wesee:search|niki-bot|crystalsemanticsbot|rogerbot|360spider|psbot|interfaxscanbot|lipperheyseoservice|ccmetadatascaper|g00g1e\.net|grapeshotcrawler|urlappendbot|brainobot|fr-crawler|binlar|simplecrawler|simplecrawler|livelapbot|twitterbot|cxensebot|smtbot|facebookexternalhit|daumoa|sputnikimagebot|visionutils|yisouspider|parsijoobot|mediatoolkit\.com|semrushbot""")
  }

  case class UaMatcher(rStr: String) {
    private val regex = rStr.r

    def apply(req: RequestHeader): Boolean = userAgent(req) ?? { regex.find(_) }
  }

  def isFishnet(req: RequestHeader) = req.path startsWith "/shoginet/"

  def isHuman(req: RequestHeader) = !isCrawler(req) && !isFishnet(req)

  def isFacebookOrTwitterBot(req: RequestHeader) =
    userAgent(req) ?? { ua =>
      ua.contains("facebookexternalhit/") || ua.contains("twitterbot/")
    }

  private[this] val fileExtensionRegex = """\.(?<!^\.)[a-zA-Z0-9]{2,4}$""".r

  def hasFileExtension(req: RequestHeader) = fileExtensionRegex.find(req.path)

  def weirdUA(req: RequestHeader) = userAgent(req).fold(true)(_.sizeIs < 30)

  def print(req: RequestHeader) = s"${printReq(req)} ${printClient(req)}"

  def printReq(req: RequestHeader) = s"${req.method} ${req.domain}${req.uri}"

  def printClient(req: RequestHeader) =
    s"${lastRemoteAddress(req)} origin:${~origin(req)} referer:${~referer(req)} ua:${~userAgent(req)}"

  def bearer(req: RequestHeader): Option[Bearer] =
    req.headers.get(HeaderNames.AUTHORIZATION).flatMap { authorization =>
      val prefix = "Bearer "
      authorization.startsWith(prefix) option Bearer(authorization.stripPrefix(prefix))
    }

  def isOAuth(req: RequestHeader) = bearer(req).isDefined

  def acceptsNdJson(req: RequestHeader) = req.headers get HeaderNames.ACCEPT contains "application/x-ndjson"
  def acceptsJson(req: RequestHeader)   = req.headers get HeaderNames.ACCEPT contains "application/json"

  def actionName(req: RequestHeader): String =
    req.attrs.get(Router.Attrs.HandlerDef).fold("NoHandler") { handler =>
      s"${handler.controller.drop(12)}.${handler.method}"
    }

  private val ApiVersionHeaderPattern = """application/vnd\.lishogi\.v(\d++)\+json""".r

  def apiVersion(req: RequestHeader): Option[ApiVersion] = {
    req.headers.get(HeaderNames.ACCEPT) flatMap {
      case ApiVersionHeaderPattern(v) => v.toIntOption map ApiVersion.apply
      case _                          => none
    }
  }

  def clientName(req: RequestHeader) =
    if (isXhr(req)) "xhr"
    else if (isCrawler(req)) "crawler"
    else
      apiVersion(req).fold("browser") { v =>
        s"api/$v"
      }
}
