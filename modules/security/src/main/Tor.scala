package lila.security

import lila.common.IpAddress

import play.api.libs.ws.WSClient

final class Tor(ws: WSClient, config: SecurityConfig.Tor) {

  private var ips = Set.empty[IpAddress]

  private[security] def refresh(withIps: Iterable[IpAddress] => Funit): Unit = {
    ws.url(config.providerUrl).get() map { res =>
      ips = res.body.linesIterator.filterNot(_ startsWith "#").map(IpAddress.apply).toSet
      withIps(ips)
      lila.mon.security.tor.node(ips.size)
    }
  }

  def isExitNode(ip: IpAddress) = ips contains ip
}
