package lila.security

import play.api.libs.ws.StandaloneWSClient

import lila.common.IpAddress

final class Tor(ws: StandaloneWSClient, config: SecurityConfig.Tor)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private var ips = Set.empty[IpAddress]

  private[security] def refresh: Fu[Set[IpAddress]] =
    ws.url(config.providerUrl).get() map { res =>
      ips = res.body.linesIterator.filterNot(_ startsWith "#").flatMap(IpAddress.from).toSet
      lila.mon.security.torNodes.update(ips.size)
      ips
    }

  def isExitNode(ip: IpAddress) = ips contains ip
}
