package lila.security

import scala.concurrent.duration._

import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._
import play.api.mvc.Results.Redirect
import play.api.mvc.{ RequestHeader, Handler, Action, Cookies }
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import spray.caching.{ LruCache, Cache }

import lila.common.LilaCookie
import lila.common.PimpedJson._
import lila.db.api._
import tube.firewallTube

final class Firewall(
    cookieName: Option[String],
    enabled: Boolean,
    cachedIpsTtl: Duration) {

  // def requestHandler(req: RequestHeader): Fu[Option[Handler]] =
  //   cookieName.filter(_ => enabled) ?? { cn =>
  //     blocksIp(req.remoteAddress) map { bIp =>
  //       val bCs = blocksCookies(req.cookies, cn)
  //       if (bIp && !bCs) infectCookie(cn)(req).some
  //       else if (bCs && !bIp) { blockIp(req.remoteAddress); None }
  //       else None
  //     }
  //   }

  def blocks(req: RequestHeader): Fu[Boolean] = if (enabled) {
    cookieName.fold(blocksIp(req.remoteAddress)) { cn =>
      blocksIp(req.remoteAddress) map (_ || blocksCookies(req.cookies, cn))
    }
  }
  else fuccess(false)

  def accepts(req: RequestHeader): Fu[Boolean] = blocks(req) map (!_)

  def blockIp(ip: String): Funit = validIp(ip) ?? {
    log("Block IP: " + ip)
    $update(Json.obj("_id" -> ip), Json.obj("_id" -> ip, "date" -> $date(DateTime.now)), upsert = true) >>- refresh
  }

  def unblockIp(ip: String): Funit = validIp(ip) ?? {
    log("Unblock IP: " + ip)
    $remove($select(ip)) >>- refresh
  }

  private def infectCookie(name: String)(implicit req: RequestHeader) = Action {
    log("Infect cookie " + formatReq(req))
    val cookie = LilaCookie.cookie(name, Random nextStringUppercase 32)
    Redirect("/") withCookies cookie
  }

  def logBlock(req: RequestHeader) {
    log("Block " + formatReq(req))
  }

  def blocksIp(ip: String): Fu[Boolean] = ips.apply map (_ contains ip)

  private def refresh {
    log("refresh IPs")
    ips.clear
  }

  private def log(msg: Any) {
    loginfo("[%s] %s".format("firewall", msg.toString))
  }

  private def formatReq(req: RequestHeader) =
    "%s %s %s".format(req.remoteAddress, req.uri, req.headers.get("User-Agent") | "?")

  private def blocksCookies(cookies: Cookies, name: String) =
    (cookies get name).isDefined

  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipRegex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r

  private def validIp(ip: String) =
    (ipRegex matches ip) && ip != "127.0.0.1" && ip != "0.0.0.0"

  private lazy val ips = new {
    private val cache: Cache[Set[String]] = LruCache(timeToLive = cachedIpsTtl)
    def apply: Fu[Set[String]] = cache(true)(fetch)
    def clear { cache.clear }
  }

  private def fetch: Fu[Set[String]] =
    $primitive($select.all, "_id")(_.asOpt[String]) map (_.toSet)
}
