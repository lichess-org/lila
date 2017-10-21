package lila.security

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.mvc.{ RequestHeader, Cookies }
import reactivemongo.api.commands.GetLastError
import reactivemongo.api.ReadPreference

import lila.common.IpAddress
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final class Firewall(
    coll: Coll,
    cookieName: Option[String],
    enabled: Boolean,
    asyncCache: lila.memo.AsyncCache.Builder,
    cachedIpsTtl: FiniteDuration
) {

  private val ipsCache = asyncCache.single[Set[String]](
    name = "firewall.ips",
    f = coll.distinctWithReadPreference[String, Set]("_id", none, ReadPreference.secondaryPreferred).addEffect { ips =>
      lila.mon.security.firewall.ip(ips.size)
    },
    expireAfter = _.ExpireAfterWrite(cachedIpsTtl)
  )

  private def ipOf(req: RequestHeader) =
    lila.common.HTTPRequest lastRemoteAddress req

  def blocks(req: RequestHeader): Fu[Boolean] = if (enabled) {
    cookieName.fold(blocksIp(ipOf(req))) { cn =>
      blocksIp(ipOf(req)) map (_ || blocksCookies(req.cookies, cn))
    } addEffect { v =>
      if (v) lila.mon.security.firewall.block()
    }
  } else fuccess(false)

  def accepts(req: RequestHeader): Fu[Boolean] = blocks(req) map (!_)

  // since we'll read from secondary right after writing
  private val writeConcern = GetLastError.ReplicaAcknowledged(
    n = 2,
    timeout = 5000,
    journaled = false
  )

  def blockIp(ip: IpAddress): Funit = validIp(ip) ?? {
    coll.update(
      $id(ip),
      $doc("_id" -> ip, "date" -> DateTime.now),
      upsert = true,
      writeConcern = writeConcern
    ).void >>- ipsCache.refresh
    funit // do not wait for the replica aknowledgement, return right away
  }

  def unblockIps(ips: Iterable[IpAddress]): Funit = {
    coll.remove(
      $inIds(ips.filter(validIp)),
      writeConcern = writeConcern
    ).void >>- ipsCache.refresh
    funit // do not wait for the replica aknowledgement, return right away
  }

  def blocksIp(ip: IpAddress): Fu[Boolean] = ipsCache.get.dmap(_ contains ip.value)

  private def blocksCookies(cookies: Cookies, name: String) =
    (cookies get name).isDefined

  private def validIp(ip: IpAddress) =
    (IpAddress.isv4(ip) && ip.value != "127.0.0.1" && ip.value != "0.0.0.0") ||
      (IpAddress.isv6(ip) && ip.value != "0:0:0:0:0:0:0:1" && ip.value != "0:0:0:0:0:0:0:0")
}
