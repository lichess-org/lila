package lila.security

import scala.concurrent.duration._

import java.net.InetAddress
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._
import play.api.mvc.Results.Redirect
import play.api.mvc.{ RequestHeader, Handler, Action, Cookies }
import spray.caching.{ LruCache, Cache }

import lila.common.LilaCookie
import lila.common.PimpedJson._
import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler

final class Firewall(
    coll: Coll,
    cookieName: Option[String],
    enabled: Boolean,
    cachedIpsTtl: Duration) {

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

  def blockIp(ip: String): Funit = validIp(ip) ?? {
    coll.update($id(ip), $doc("_id" -> ip, "date" -> DateTime.now), upsert = true).void >>- refresh
  }

  def unblockIps(ips: Iterable[String]): Funit =
    coll.remove($inIds(ips filter validIp)).void >>- refresh

  private def infectCookie(name: String)(implicit req: RequestHeader) = Action {
    logger.info("Infect cookie " + formatReq(req))
    val cookie = LilaCookie.cookie(name, Random nextStringUppercase 32)
    Redirect("/") withCookies cookie
  }

  def blocksIp(ip: String): Fu[Boolean] = ips contains ip

  private def refresh {
    ips.clear
  }

  private def formatReq(req: RequestHeader) =
    "%s %s %s".format(ipOf(req), req.uri, req.headers.get("User-Agent") | "?")

  private def blocksCookies(cookies: Cookies, name: String) =
    (cookies get name).isDefined

  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipRegex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r

  private def validIp(ip: String) =
    (ipRegex matches ip) && ip != "127.0.0.1" && ip != "0.0.0.0"

  private type IP = Vector[Byte]

  private lazy val ips = new {
    private val cache: Cache[Set[IP]] = LruCache(timeToLive = cachedIpsTtl)
    private def strToIp(ip: String) = InetAddress.getByName(ip).getAddress.toVector
    def apply: Fu[Set[IP]] = cache(true)(fetch)
    def clear { cache.clear }
    def contains(ip: String) = apply map (_ contains strToIp(ip))
    def fetch: Fu[Set[IP]] =
      coll.distinct("_id") map { res =>
        lila.db.BSON.asStringSet(res) map strToIp
      } addEffect { ips =>
        lila.mon.security.firewall.ip(ips.size)
      }
  }
}
