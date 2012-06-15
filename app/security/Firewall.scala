package lila
package security

import memo.MonoMemo
import http.LilaCookie
import controllers.routes

import play.api.mvc.{ RequestHeader, Handler, Action, Cookies }
import play.api.mvc.Results.Redirect
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._
import java.util.Date
import ornicar.scalalib.OrnicarRandom

final class Firewall(
    collection: MongoCollection,
    cacheTtl: Int,
    blockCookieName: String,
    enabled: Boolean) {

  def requestHandler(implicit req: RequestHeader): Option[Handler] = if (enabled) {
    val bIp = blocksIp(req.remoteAddress)
    val bCs = blocksCookies(req.cookies)
    if (bIp && !bCs) infectCookie.some
    else if (bCs && !bIp) { blockIp(req.remoteAddress); none }
    else none
  }
  else none

  def blocks(req: RequestHeader): Boolean =
    enabled && (blocksIp(req.remoteAddress) || blocksCookies(req.cookies))

  def accepts(req: RequestHeader): Boolean = !blocks(req)

  def blockIp(ip: String) {
    if (validIp(ip)) {
      if (!blocksIp(ip)) {
        log("Block IP: " + ip)
        collection += DBObject("_id" -> ip, "date" -> new Date)
        ips.refresh
      }
    }
    else log("Invalid IP block: " + ip)
  }

  def infectCookie(implicit req: RequestHeader) = Action {
    log("Infect cookie for IP: " + req.remoteAddress)
    val cookie = LilaCookie.cookie(blockCookieName, OrnicarRandom nextAsciiString 32)
    Redirect(routes.Lobby.home()) withCookies cookie
  }

  private def log(msg: Any) {
    println("[%s] %s".format("firewall", msg.toString))
  }

  private def blocksIp(ip: String) = ips.apply contains ip

  private def blocksCookies(cookies: Cookies) = (cookies get blockCookieName).isDefined

  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipRegex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r

  private def validIp(ip: String) =
    (ipRegex matches ip) && ip != "127.0.0.1" && ip != "0.0.0.0"

  private val ips = new MonoMemo(cacheTtl, fetch)

  private def fetch: IO[Set[String]] = io {
    collection.find() map { obj ⇒
      obj.getAs[String]("_id")
    }
  } map (_.flatten.toSet)
}
