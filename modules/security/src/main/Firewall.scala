package lila.security

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.mvc.{ RequestHeader, Cookies }

import lila.common.IpAddress
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final class Firewall(
    coll: Coll,
    cookieName: Option[String],
    enabled: Boolean,
    system: akka.actor.ActorSystem
) {

  private var current: Set[String] = Set.empty

  system.scheduler.scheduleOnce(10 seconds)(loadFromDb)

  def blocksIp(ip: IpAddress): Boolean = current contains ip.value

  def blocks(req: RequestHeader): Boolean = enabled && {
    val v = blocksIp {
      lila.common.HTTPRequest lastRemoteAddress req
    } || cookieName.?? { blocksCookies(req.cookies, _) }
    if (v) lila.mon.security.firewall.block()
    v
  }

  def accepts(req: RequestHeader): Boolean = !blocks(req)

  def blockIps(ips: List[IpAddress]): Funit = ips.map { ip =>
    validIp(ip) ?? {
      coll.update(
        $id(ip),
        $doc("_id" -> ip, "date" -> DateTime.now),
        upsert = true
      ).void
    }
  }.sequenceFu >> loadFromDb

  def unblockIps(ips: Iterable[IpAddress]): Funit =
    coll.remove($inIds(ips.filter(validIp))).void >>- loadFromDb

  private def loadFromDb: Funit =
    coll.distinct[String, Set]("_id", none).map { ips =>
      current = ips
      lila.mon.security.firewall.ip(ips.size)
    }

  private def blocksCookies(cookies: Cookies, name: String) =
    (cookies get name).isDefined

  private def validIp(ip: IpAddress) =
    (IpAddress.isv4(ip) && ip.value != "127.0.0.1" && ip.value != "0.0.0.0") ||
      (IpAddress.isv6(ip) && ip.value != "0:0:0:0:0:0:0:1" && ip.value != "0:0:0:0:0:0:0:0")
}
