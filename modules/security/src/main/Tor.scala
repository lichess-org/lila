package lila.security

import lila.common.IpAddress

import play.api.libs.ws.WS
import play.api.Play.current

final class Tor(providerUrl: String) {

  private var ips = Set[String]()

  private[security] def refresh(withIps: Iterable[IpAddress] => Funit) {
    WS.url(providerUrl).get() map { res =>
      ips = res.body.lines.filterNot(_ startsWith "#").toSet
      withIps(ips map IpAddress.apply)
      lila.mon.security.tor.node(ips.size)
    }
  }

  def isExitNode(ip: String) = ips contains ip
}
