package lila.security

import play.api.libs.ws.WS
import play.api.Play.current

final class Tor(providerUrl: String) {

  private var ips = Set[String]()

  private[security] def refresh {
    WS.url(providerUrl).get() map { res =>
      ips = res.body.lines.filterNot(_ startsWith "#").toSet
    }
  }

  def isExitNode(ip: String) = ips contains ip
}
