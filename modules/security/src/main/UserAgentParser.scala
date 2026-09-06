package lila.security

import org.uaparser.scala.*
import scalalib.net.UserAgent as UA

import play.api.mvc.RequestHeader
import lila.common.HTTPRequest

object UserAgentParser:

  private val generic = org.uaparser.scala.Parser.default

  def parseSlowly(agent: UA): Client =
    lichessBot(agent.value).orElse(lichessMobile(agent)).getOrElse(generic.parse(agent.value))

  private def lichessBot(agent: String): Option[Client] =
    agent
      .startsWith("lichess-bot/")
      .option(
        Client(
          UserAgent(agent.take(11), agent.drop(12).takeWhile(' ' !=).some),
          OS("Other"),
          Device("Computer")
        )
      )

  private def lichessMobile(agent: UA): Option[Client] =
    Mobile.LichessMobileUaTrim
      .parse(agent)
      .map: m =>
        Client(
          UserAgent("Lichess Mobile", m.version.some),
          OS(m.osName, m.osVersion.some),
          Device(m.device)
        )

  object trust:

    def isSuspicious(req: RequestHeader): Boolean = isSuspicious(HTTPRequest.userAgent(req))

    def isSuspicious(ua: UA): Boolean =
      !isLichobile(ua) && !isLM(ua) && {
        val str = ua.value.take(200).toLowerCase
        str.lengthIs < 30 || isMacOsEdge(str) || !popularBrowser(str)
      }

    private type UaSection = String

    private def popularBrowser(ua: String) =
      val sections = ua.split(' ')
      sections.exists: s =>
        isRecentChrome(s) || isLastWindows8Chrome(s) || isRecentFirefox(s) || isRecentSafari(s)

    // based on https://caniuse.com/usage-table
    private val isRecentChrome = isRecentBrowser("chrome", 125) // also covers Edge and Opera
    private val isRecentFirefox = isRecentBrowser("firefox", 128)
    private val isRecentSafari = isRecentBrowser("safari", 604) // most safaris also have a chrome/ section

    private def isRecentBrowser(name: String, minVersion: Int): UaSection => Boolean =
      val slashed = name + "/"
      val prefixLength = slashed.length
      (s: String) =>
        s.startsWith(slashed) &&
          s.drop(prefixLength).takeWhile(_ != '.').toIntOption.exists(_ >= minVersion)

    private def isLastWindows8Chrome(s: String) = s.startsWith("chrome/109.")

    private def isMacOsEdge(ua: String) = ua.contains("macintosh") && ua.contains("edg/")

    private def isLichobile(ua: UA) = ua.value.contains("Lichobile/")

    private def isLM(ua: UA) =
      // stored UA in oauth security document compresses `Lichess Mobile/` into `LM/`
      ua.value.startsWith("LM/") || HTTPRequest.isLichessMobile(ua)

  // way too long since the last security update
  def isDangerousDevice(using req: RequestHeader): Option[String] =
    isDangerous(HTTPRequest.userAgent(req))

  def isDangerous(ua: UA): Option[String] =
    val client = parseSlowly(ua)
    isDangerousIOS(client) orElse
      isDangerousSafari(client) orElse
      isDangerousChrome(client) orElse
      isDangerousFirefox(client)

  def isDangerousIOS(c: Client): Option[String] = {
    c.os.family == "iOS" && c.os.major.flatMap(_.toIntOption).exists(_.toInt < 12)
  }.option(showOs(c))
  def isDangerousSafari(c: Client) = {
    c.userAgent.family == "Safari" && c.userAgent.major.flatMap(_.toIntOption).exists(_.toInt < 14)
  }.option(showBrowser(c))
  def isDangerousChrome(c: Client) = {
    c.userAgent.family == "Chrome" && c.userAgent.major.flatMap(_.toIntOption).exists(_.toInt < 100)
  }.option(showBrowser(c))
  def isDangerousFirefox(c: Client) = {
    c.userAgent.family == "Firefox" && c.userAgent.major.flatMap(_.toIntOption).exists(_.toInt < 91)
  }.option(showBrowser(c))

  private def showOs(c: Client) = s"${c.os.family} ${~c.os.major}"
  private def showBrowser(c: Client) = s"${c.userAgent.family} ${~c.userAgent.major}"
