package lila.security

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.net.IpAddress

final private class Tor(ws: StandaloneWSClient, config: SecurityConfig.Tor)(using Executor)(using
    scheduler: Scheduler
):

  def isExitNode(ip: IpAddress) = ips contains ip

  private var ips = Set.empty[IpAddress]

  private def refresh: Funit =
    ws.url(config.providerUrl).get().map { res =>
      ips = res.body[String].linesIterator.filterNot(_.startsWith("#")).flatMap(IpAddress.from).toSet
      lila.mon.security.torNodes.update(ips.size)
    }

  if config.enabled then
    scheduler.scheduleWithFixedDelay(44.seconds, config.refreshDelay): () =>
      refresh
