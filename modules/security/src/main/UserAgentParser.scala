package lila.security

import org.uaparser.scala.*

object UserAgentParser:

  private val generic = org.uaparser.scala.Parser.default

  def parse(agent: lila.common.UserAgent): Client =
    Mobile.LichessMobileUaTrim.parse(agent) match
      case Some(m) =>
        Client(UserAgent("Lichess Mobile", m.version.some), OS(m.osName, m.osVersion.some), Device(m.device))
      case None => generic.parse(agent.value)
