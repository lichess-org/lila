package lila.security

import play.api.mvc.RequestHeader
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*

import lila.core.net.IpAddress
import lila.db.dsl.{ *, given }

final class Firewall(
    coll: Coll,
    scheduler: Scheduler,
    ws: StandaloneWSClient
)(using Executor):

  private var current: Set[String]    = Set.empty
  private var proxies: Set[IpAddress] = Set.empty

  scheduler.scheduleOnce(49.seconds):
    loadFromDb

  scheduler.scheduleWithFixedDelay(47.seconds, 2.hours): () =>
    blockProxyScrapeIps()

  def blocksIp(ip: IpAddress): Boolean = current.contains(ip.value) || proxies.contains(ip)

  def blocks(req: RequestHeader): Boolean =
    val v = blocksIp(lila.common.HTTPRequest.ipAddress(req))
    if v then lila.mon.security.firewall.block.increment()
    v

  def accepts(req: RequestHeader): Boolean = !blocks(req)

  def blockIps(ips: Iterable[IpAddress]): Funit = ips.nonEmpty.so:
    ips.toList.sequentiallyVoid { ip =>
      coll.update
        .one(
          $id(ip),
          $doc("_id" -> ip, "date" -> nowInstant),
          upsert = true
        )
        .void
    } >> loadFromDb

  def unblockIps(ips: Iterable[IpAddress]): Funit = ips.nonEmpty.so:
    for _ <- coll.delete.one($inIds(ips)) yield loadFromDb

  private def loadFromDb: Funit =
    coll.distinctEasy[String, Set]("_id", $empty).map { ips =>
      current = ips
      lila.mon.security.firewall.ip.update(ips.size)
    }

  private def blockProxyScrapeIps() =
    ws.url(
      "https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=3000&country=all&ssl=all&anonymity=all"
    ).get()
      .map: res =>
        if res.status == 200 then
          val ips =
            res
              .body[String]
              .linesIterator
              .map(_.takeWhile(':' != _))
              .flatMap(IpAddress.from)
              .toSet
              .take(5000)
          logger.info(s"Firewall: blocking ${ips.size} IPs from proxyscrape")
          proxies = ips
        else logger.warn(s"Firewall: failed to fetch proxies from proxyscrape: ${res.status}")
