package lila.common

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

object HTTPRequest {

  def isXhr(req: RequestHeader): Boolean =
    (req.headers get "X-Requested-With") contains "XMLHttpRequest"

  def isSocket(req: RequestHeader): Boolean =
    (req.headers get HeaderNames.UPGRADE).exists(_.toLowerCase == "websocket")

  def isSynchronousHttp(req: RequestHeader) = !isXhr(req) && !isSocket(req)

  def isSafe(req: RequestHeader) = req.method == "GET" || req.method == "HEAD" || req.method == "OPTIONS"
  def isUnsafe(req: RequestHeader) = !isSafe(req)

  def isRedirectable(req: RequestHeader) = isSynchronousHttp(req) && isSafe(req)

  def userAgent(req: RequestHeader): Option[String] = req.headers get HeaderNames.USER_AGENT

  val isAndroid = UaMatcher("""(?i)android.+mobile""")
  val isIOS = UaMatcher("""(?i)iphone|ipad|ipod""")
  val isMobile = UaMatcher("""(?i)iphone|ipad|ipod|android.+mobile""")

  private def uaContains(req: RequestHeader, str: String) = userAgent(req).exists(_ contains str)
  def isTrident(req: RequestHeader) = uaContains(req, "Trident/")
  def isChrome(req: RequestHeader) = uaContains(req, "Chrome/")

  def origin(req: RequestHeader): Option[String] = req.headers get HeaderNames.ORIGIN

  def referer(req: RequestHeader): Option[String] = req.headers get HeaderNames.REFERER

  def lastRemoteAddress(req: RequestHeader) = IpAddress {
    req.remoteAddress.split(", ").lastOption | req.remoteAddress
  }

  def sid(req: RequestHeader): Option[String] = req.session get LilaCookie.sessionId

  val isBot = UaMatcher {
    ("""(?i)googlebot|googlebot-mobile|googlebot-image|mediapartners-google|bingbot|slurp|java|wget|curl|commons-httpclient|python-urllib|libwww|httpunit|nutch|phpcrawl|msnbot|adidxbot|blekkobot|teoma|ia_archiver|gingercrawler|webmon|httrack|webcrawler|fast-webcrawler|fastenterprisecrawler|convera|biglotron|grub\.org|usinenouvellecrawler|antibot|netresearchserver|speedy|fluffy|jyxobot|bibnum\.bnf|findlink|exabot|gigabot|msrbot|seekbot|ngbot|panscient|yacybot|aisearchbot|ioi|ips-agent|tagoobot|mj12bot|dotbot|woriobot|yanga|buzzbot|mlbot|purebot|lingueebot|yandex\.com/bots|""" +
      """voyager|cyberpatrol|voilabot|baiduspider|citeseerxbot|spbot|twengabot|postrank|turnitinbot|scribdbot|page2rss|sitebot|linkdex|ezooms|dotbot|mail\.ru|discobot|zombie\.js|heritrix|findthatfile|europarchive\.org|nerdbynature\.bot|sistrixcrawler|ahrefsbot|aboundex|domaincrawler|wbsearchbot|summify|ccbot|edisterbot|seznambot|ec2linkfinder|gslfbot|aihitbot|intelium_bot|yeti|retrevopageanalyzer|lb-spider|sogou|lssbot|careerbot|wotbox|wocbot|ichiro|duckduckbot|lssrocketcrawler|drupact|webcompanycrawler|acoonbot|openindexspider|gnamgnamspider|web-archive-net\.com\.bot|backlinkcrawler|""" +
      """coccoc|integromedb|contentcrawlerspider|toplistbot|seokicks-robot|it2media-domain-crawler|ip-web-crawler\.com|siteexplorer\.info|elisabot|proximic|changedetection|blexbot|arabot|wesee:search|niki-bot|crystalsemanticsbot|rogerbot|360spider|psbot|interfaxscanbot|lipperheyseoservice|ccmetadatascaper|g00g1e\.net|grapeshotcrawler|urlappendbot|brainobot|fr-crawler|binlar|simplecrawler|simplecrawler|livelapbot|twitterbot|cxensebot|smtbot|facebookexternalhit|daumoa|sputnikimagebot|visionutils|yisouspider|parsijoobot|mediatoolkit\.com|semrushbot""")
  }

  case class UaMatcher(regex: String) {
    val pattern = regex.r.pattern

    def apply(req: RequestHeader): Boolean =
      userAgent(req) ?? { ua => pattern.matcher(ua).find }
  }

  def isHuman(req: RequestHeader) = !isBot(req)

  def isFacebookOrTwitterBot(req: RequestHeader) = userAgent(req) ?? { ua =>
    ua.contains("facebookexternalhit/") || ua.contains("twitterbot/")
  }

  private val fileExtensionPattern = """.+\.[a-z0-9]{2,4}$""".r.pattern

  def hasFileExtension(req: RequestHeader) =
    fileExtensionPattern.matcher(req.path).matches

  def weirdUA(req: RequestHeader) = userAgent(req).fold(true)(_.size < 30)

  def print(req: RequestHeader) = s"${printReq(req)} ${printClient(req)}"

  def printReq(req: RequestHeader) = s"${req.method} ${req.domain}${req.uri}"

  def printClient(req: RequestHeader) = s"${lastRemoteAddress(req)} origin:${~origin(req)} referer:${~referer(req)} ua:${~userAgent(req)}"

  def isOAuth(req: RequestHeader) = req.headers.toMap.contains(HeaderNames.AUTHORIZATION)

  def isHttp10(req: RequestHeader) = req.version == "HTTP/1.0"
}
