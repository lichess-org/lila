package lila.common

import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

object HTTPRequest {

  def isXhr(req: RequestHeader): Boolean =
    (req.headers get "X-Requested-With") == Some("XMLHttpRequest")

  def isSocket(req: RequestHeader): Boolean =
    (req.headers get HeaderNames.UPGRADE) == Some("websocket")

  def isSynchronousHttp(req: RequestHeader) = !isXhr(req) && !isSocket(req)

  def isSafe(req: RequestHeader) = req.method == "GET"

  def isRedirectable(req: RequestHeader) = isSynchronousHttp(req) && isSafe(req)

  def fullUrl(req: RequestHeader): String = "http://" + req.host + req.uri

  def userAgent(req: RequestHeader): Option[String] = req.headers get HeaderNames.USER_AGENT

  def sid(req: RequestHeader): Option[String] = req.session get "sid"

  private val isBotPattern = {
    """(?i).*(googlebot|googlebot-mobile|googlebot-image|mediapartners-google|bingbot|slurp|java|wget|curl|commons-httpclient|python-urllib|libwww|httpunit|nutch|phpcrawl|msnbot|adidxbot|blekkobot|teoma|ia_archiver|gingercrawler|webmon|httrack|webcrawler|fast-webcrawler|fastenterprisecrawler|convera|biglotron|grub.org|usinenouvellecrawler|antibot|netresearchserver|speedy|fluffy|jyxobot|bibnum.bnf|findlink|exabot|gigabot|msrbot|seekbot|ngbot|panscient|yacybot|aisearchbot|ioi|ips-agent|tagoobot|mj12bot|dotbot|woriobot|yanga|buzzbot|mlbot|yandex|purebot|lingueebot|""" +
      """voyager|cyberpatrol|voilabot|baiduspider|citeseerxbot|spbot|twengabot|postrank|turnitinbot|scribdbot|page2rss|sitebot|linkdex|ezooms|dotbot|mail.ru|discobot|heritrix|findthatfile|europarchive.org|nerdbynature.bot|sistrixcrawler|ahrefsbot|aboundex|domaincrawler|wbsearchbot|summify|ccbot|edisterbot|seznambot|ec2linkfinder|gslfbot|aihitbot|intelium_bot|yeti|retrevopageanalyzer|lb-spider|sogou|lssbot|careerbot|wotbox|wocbot|ichiro|duckduckbot|lssrocketcrawler|drupact|webcompanycrawler|acoonbot|openindexspider|gnamgnamspider|web-archive-net.com.bot|backlinkcrawler|""" +
      """coccoc|integromedb|contentcrawlerspider|toplistbot|seokicks-robot|it2media-domain-crawler|ip-web-crawler.com|siteexplorer.info|elisabot|proximic|changedetection|blexbot|arabot|wesee:search|niki-bot|crystalsemanticsbot|rogerbot|360spider|psbot|interfaxscanbot|lipperheyseoservice|ccmetadatascaper|g00g1e.net|grapeshotcrawler|urlappendbot|brainobot|fr-crawler|binlar|simplecrawler|simplecrawler|livelapbot|twitterbot|cxensebot|smtbot|facebookexternalhit).*"""
  }.r.pattern

  def isBot(req: RequestHeader): Boolean = userAgent(req) ?? { ua =>
    isBotPattern.matcher(ua).matches
  }

  def isFacebookBot(req: RequestHeader) = userAgent(req) ?? (_ contains "facebookexternalhit")
}
