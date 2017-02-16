package lila.security

import scala.concurrent.duration._

import java.net.InetAddress
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.mvc.Results.Redirect
import play.api.mvc.{ RequestHeader, Action, Cookies }

import lila.common.{ IpAddress, LilaCookie }
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final class Firewall(
    coll: Coll,
    cookieName: Option[String],
    enabled: Boolean,
    asyncCache: lila.memo.AsyncCache.Builder,
    cachedIpsTtl: FiniteDuration
) {

  private def ipOf(req: RequestHeader) =
    lila.common.HTTPRequest lastRemoteAddress req

  def blocks(req: RequestHeader): Fu[Boolean] = if (enabled) {
    cookieName.fold(blocksIp(ipOf(req))) { cn =>
      blocksIp(ipOf(req)) map (_ || blocksCookies(req.cookies, cn))
    } addEffect { v =>
      if (v) lila.mon.security.firewall.block()
    }
  }
  else fuccess(false)

  def accepts(req: RequestHeader): Fu[Boolean] = blocks(req) map (!_)

  def blockIp(ip: IpAddress): Funit = validIp(ip) ?? {
    coll.update(
      $id(ip),
      $doc("_id" -> ip, "date" -> DateTime.now),
      upsert = true
    ).void >>- refresh
  }

  def unblockIps(ips: Iterable[IpAddress]): Funit =
    coll.remove($inIds(ips.filter(validIp).map(_.value))).void >>- refresh

  def blocksIp(ip: IpAddress): Fu[Boolean] = ips contains ip.value

  private def refresh {
    ips.clear
  }

  private def blocksCookies(cookies: Cookies, name: String) =
    (cookies get name).isDefined

  private def validIp(ip: IpAddress) =
    (IpAddress.isv4(ip) && ip.value != "127.0.0.1" && ip.value != "0.0.0.0") ||
      (IpAddress.isv6(ip) && ip.value != "0:0:0:0:0:0:0:1" && ip.value != "0:0:0:0:0:0:0:0")

  private type BinIP = Vector[Byte]

  private lazy val ips = new {
    private val cache = asyncCache.single(
      name = "firewall.ips",
      f = fetch,
      expireAfter = _.ExpireAfterWrite(cachedIpsTtl)
    )
    private def strToIp(ip: String): Option[BinIP] = scala.util.Try {
      InetAddress.getByName(ip).getAddress.toVector
    }.toOption
    def apply: Fu[Set[BinIP]] = cache.get
    def clear = cache.refresh
    def contains(str: String) = strToIp(str) ?? { ip => apply.dmap(_ contains ip) }
    private def fetch: Fu[Set[BinIP]] =
      coll.distinct[String, Set]("_id").map(_.flatMap(strToIp)).addEffect { ips =>
        lila.mon.security.firewall.ip(ips.size)
      }
  }
}
