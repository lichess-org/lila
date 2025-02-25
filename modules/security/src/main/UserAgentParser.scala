package lila.security

import org.uaparser.scala.*

import lila.core.net.UserAgent as UA
import play.api.mvc.RequestHeader
import lila.common.HTTPRequest

object UserAgentParser:

  private val generic = org.uaparser.scala.Parser.default

  def parse(agent: UA): Client =
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

    def isSuspicious(req: RequestHeader): Boolean = HTTPRequest.userAgent(req).forall(isSuspicious)

    def isSuspicious(ua: UA): Boolean =
      !isLichobile(ua) && !isLM(ua) && {
        val str = ua.value.take(200).toLowerCase
        str.lengthIs < 30 || isMacOsEdge(str) || !popularBrowser(str)
      }

    private def popularBrowser(ua: String) =
      val sections = ua.split(' ')
      sections.exists: s =>
        isRecentChrome(s) || isLastWindows8Chrome(s) || isRecentFirefox(s) || isRecentSafari(s)

    // based on https://caniuse.com/usage-table
    private val isRecentChrome  = isRecentBrowser("chrome", 125) // also covers Edge and Opera
    private val isRecentFirefox = isRecentBrowser("firefox", 128)
    private val isRecentSafari  = isRecentBrowser("safari", 604) // most safaris also have a chrome/ section

    private def isRecentBrowser(name: String, minVersion: Int): String => Boolean =
      val slashed      = name + "/"
      val prefixLength = slashed.length
      (s: String) =>
        s.startsWith(slashed) &&
          s.drop(prefixLength).takeWhile(_ != '.').toIntOption.exists(_ >= minVersion)

    private def isLastWindows8Chrome(s: String) = s.startsWith("chrome/109.")

    private def isMacOsEdge(ua: String) = ua.contains("macintosh") && ua.contains("edg/")

    private def isLichobile(ua: UA) = ua.value.contains("Lichobile/")

    private def isLM(ua: UA) = ua.value.startsWith("LM/0.13.1")
