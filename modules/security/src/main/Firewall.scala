package lila.security

import lila.common.PimpedJson._
import lila.common.LilaCookie
import lila.db.api._
import tube.firewallTube

import scala.concurrent.duration._
import play.api.mvc.{ RequestHeader, Handler, Action, Cookies }
import play.api.mvc.Results.Redirect
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import spray.caching.{ LruCache, Cache }
import org.joda.time.DateTime
import ornicar.scalalib.Random

final class Firewall(
    cookieName: Option[String],
    enabled: Boolean,
    cachedIpsTtl: Duration) {

  def requestHandler(req: RequestHeader): Fu[Option[Handler]] =
    cookieName filter (_ ⇒ enabled) zmap { cn ⇒
      blocksIp(req.remoteAddress) map { bIp ⇒
        val bCs = blocksCookies(req.cookies, cn)
        if (bIp && !bCs) infectCookie(cn)(req).some
        else if (bCs && !bIp) { blockIp(req.remoteAddress); None }
        else None
      }
    }

  def blocks(req: RequestHeader): Fu[Boolean] = if (enabled) {
    cookieName.fold(blocksIp(req.remoteAddress)) { cn ⇒
      blocksIp(req.remoteAddress) map (_ || blocksCookies(req.cookies, cn))
    }
  }
  else fuccess(false)

  def accepts(req: RequestHeader): Fu[Boolean] = blocks(req) map (!_)

  def blockIp(ip: String): Funit = blocksIp(ip) flatMap { blocked ⇒
    if (validIp(ip) && !blocked) {
      log("Block IP: " + ip)
      $insert(Json.obj("_id" -> ip, "date" -> DateTime.now)) >> cachedIps.clear
    }
    else fuccess(log("Invalid IP block: " + ip))
  }

  private def infectCookie(name: String)(implicit req: RequestHeader) = Action {
    log("Infect cookie " + formatReq(req))
    val cookie = LilaCookie.cookie(name, Random nextString 32)
    Redirect("/") withCookies cookie
  }

  def logBlock(req: RequestHeader) {
    log("Block " + formatReq(req))
  }

  private def log(msg: Any) {
    loginfo("[%s] %s".format("firewall", msg.toString))
  }

  private def formatReq(req: RequestHeader) =
    "%s %s %s".format(req.remoteAddress, req.uri, req.headers.get("User-Agent") | "?")

  private def blocksIp(ip: String): Fu[Boolean] = ips map (_ contains ip)

  private def blocksCookies(cookies: Cookies, name: String) =
    (cookies get name).isDefined

  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipRegex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r

  private def validIp(ip: String) =
    (ipRegex matches ip) && ip != "127.0.0.1" && ip != "0.0.0.0"

  private lazy val cachedIps = new {
    private val cache: Cache[Set[String]] = LruCache(timeToLive = cachedIpsTtl)
    def apply: Fu[Set[String]] = cache.fromFuture(true)(fetch)
    def clear { cache.clear }
  }

  private def ips: Fu[Set[String]] = cachedIps.apply

  private def fetch: Fu[Set[String]] =
    $primitive($select.all, "id")(_.asOpt[String]) map (_.toSet)
}
