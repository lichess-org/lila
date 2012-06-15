package lila
package security

import memo.MonoMemo

import play.api.mvc.{ Action, RequestHeader, Handler }
import play.api.mvc.Results.BadRequest
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._
import java.util.Date

final class Firewall(
    collection: MongoCollection,
    cacheTtl: Int,
    enabled: Boolean) {

  def requestHandler(req: RequestHeader): Option[Handler] =
    (enabled && blocks(req.remoteAddress)) option controllers.Main.blocked

  def blocks(ip: String) = ips.apply contains ip

  def accepts(ip: String) = !blocks(ip)

  def block(ip: String) {
    if (validIp(ip)) {
      if (accepts(ip)) {
        collection += DBObject("_id" -> ip, "date" -> new Date)
        ips.refresh
      }
    }
    else println("Invalid IP block: " + ip)
  }

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
