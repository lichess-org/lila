package lila.security

import org.uaparser.scala.*
import lila.common.{ UserAgent as UA }

object UserAgentParser:

  private val generic = org.uaparser.scala.Parser.default

  private given Conversion[UA, String] = _.value

  def parse(agent: UA): Client =
    lichessBot(agent) orElse lichessMobile(agent) getOrElse generic.parse(agent.value)

  private def lichessBot(agent: String): Option[Client] =
    agent.startsWith("lichess-bot/") option Client(
      UserAgent(agent.take(11), agent.drop(12).takeWhile(' ' !=).some),
      OS("Other"),
      Device("Computer")
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
