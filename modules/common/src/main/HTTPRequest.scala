package lila.common

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

object HTTPRequest {

  def isXhr(req: RequestHeader): Boolean =
    (req.headers get "X-Requested-With") contains "XMLHttpRequest"

  def isSocket(req: RequestHeader): Boolean =
    (req.headers get HeaderNames.UPGRADE) ?? (_.toLowerCase == "websocket")

  def isSynchronousHttp(req: RequestHeader) = !isXhr(req) && !isSocket(req)

  def isSafe(req: RequestHeader) = req.method == "GET"

  def isRedirectable(req: RequestHeader) = isSynchronousHttp(req) && isSafe(req)

  def fullUrl(req: RequestHeader): String = "http://" + req.host + req.uri

  def userAgent(req: RequestHeader): Option[String] = req.headers get HeaderNames.USER_AGENT

  val isAndroid = UaMatcher("""(?i).*android.+mobile.*""".r)
  val isIOS = UaMatcher("""(?i).*(iphone|ipad|ipod).*""".r)
  val isMobile = UaMatcher("""(?i).*(iphone|ipad|ipod|android.+mobile).*""".r)

  private def uaContains(req: RequestHeader, str: String) = userAgent(req).exists(_ contains str)
  def isTrident(req: RequestHeader) = uaContains(req, "Trident/")
  def isChrome(req: RequestHeader) = uaContains(req, "Chrome/")
  def isSafari(req: RequestHeader) = uaContains(req, "Safari/") && !isChrome(req)

  def referer(req: RequestHeader): Option[String] = req.headers get HeaderNames.REFERER

  def lastRemoteAddress(req: RequestHeader): String =
    req.remoteAddress.split(", ").lastOption | req.remoteAddress

  def sid(req: RequestHeader): Option[String] = req.session get LilaCookie.sessionId

  val isBot = UaMatcher {
    ("""(?i).*(googlebot|googlebot-mobile|googlebot-image|mediapartners-google|bingbot|slurp|java|wget|curl|commons-httpclient|python-urllib|libwww|httpunit|nutch|phpcrawl|msnbot|adidxbot|blekkobot|teoma|ia_archiver|gingercrawler|webmon|httrack|webcrawler|fast-webcrawler|fastenterprisecrawler|convera|biglotron|grub\.org|usinenouvellecrawler|antibot|netresearchserver|speedy|fluffy|jyxobot|bibnum\.bnf|findlink|exabot|gigabot|msrbot|seekbot|ngbot|panscient|yacybot|aisearchbot|ioi|ips-agent|tagoobot|mj12bot|dotbot|woriobot|yanga|buzzbot|mlbot|purebot|lingueebot|yandex\.com/bots|""" +
      """voyager|cyberpatrol|voilabot|baiduspider|citeseerxbot|spbot|twengabot|postrank|turnitinbot|scribdbot|page2rss|sitebot|linkdex|ezooms|dotbot|mail\.ru|discobot|zombie\.js|heritrix|findthatfile|europarchive\.org|nerdbynature\.bot|sistrixcrawler|ahrefsbot|aboundex|domaincrawler|wbsearchbot|summify|ccbot|edisterbot|seznambot|ec2linkfinder|gslfbot|aihitbot|intelium_bot|yeti|retrevopageanalyzer|lb-spider|sogou|lssbot|careerbot|wotbox|wocbot|ichiro|duckduckbot|lssrocketcrawler|drupact|webcompanycrawler|acoonbot|openindexspider|gnamgnamspider|web-archive-net\.com\.bot|backlinkcrawler|""" +
      """coccoc|integromedb|contentcrawlerspider|toplistbot|seokicks-robot|it2media-domain-crawler|ip-web-crawler\.com|siteexplorer\.info|elisabot|proximic|changedetection|blexbot|arabot|wesee:search|niki-bot|crystalsemanticsbot|rogerbot|360spider|psbot|interfaxscanbot|lipperheyseoservice|ccmetadatascaper|g00g1e\.net|grapeshotcrawler|urlappendbot|brainobot|fr-crawler|binlar|simplecrawler|simplecrawler|livelapbot|twitterbot|cxensebot|smtbot|facebookexternalhit|daumoa|sputnikimagebot|visionutils).*""").r
  }

  private def UaMatcher(regex: scala.util.matching.Regex): RequestHeader => Boolean = {
    val pattern = regex.pattern
    req => {
      userAgent(req) ?? { ua => pattern.matcher(ua).matches }
    }
  }

  def isHuman(req: RequestHeader) = !isBot(req)

  def isFacebookBot(req: RequestHeader) = userAgent(req) ?? (_ contains "facebookexternalhit")

  private val fileExtensionPattern = """.+\.[a-z0-9]{2,4}$""".r.pattern

  def hasFileExtension(req: RequestHeader) =
    fileExtensionPattern.matcher(req.path).matches
}
