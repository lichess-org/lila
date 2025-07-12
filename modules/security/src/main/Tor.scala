package lila.security

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.net.IpAddress

final private class Tor(
    ws: StandaloneWSClient,
    config: SecurityConfig.Tor,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor)(using scheduler: Scheduler):

  def isExitNode(ip: IpAddress) = ips contains ip

  private var ips = Set.empty[IpAddress]

  private def refresh: Funit =
    ws.url("https://check.torproject.org/torbulkexitlist").get().map { res =>
      ips = res.body[String].linesIterator.filterNot(_.startsWith("#")).flatMap(IpAddress.from).toSet
      mongoCache.put("security.torNodes", ips.mkString(" ")) // for lila-ws to fetch
      lila.mon.security.torNodes.update(ips.size)
    }

  if config.enabled then
    scheduler.scheduleWithFixedDelay(44.seconds, 30.minutes): () =>
      refresh
