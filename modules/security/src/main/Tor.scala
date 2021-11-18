package lila.security

import lila.common.IpAddress

import play.api.libs.ws.WSClient

final class Tor(ws: WSClient, config: SecurityConfig.Tor)(implicit ec: scala.concurrent.ExecutionContext) {

  private var ips = Set.empty[IpAddress]

  private[security] def refresh: Fu[Set[IpAddress]] =
    ws.url(config.providerUrl).get() map { res =>
      ips = res.body.linesIterator.filterNot(_ startsWith "#").map(IpAddress.apply).toSet
      lila.mon.security.torNodes.update(ips.size)
      ips
    }

  def isExitNode(ip: IpAddress) = ips contains ip
}
