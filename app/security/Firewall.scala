package lila
package security

import memo.MonoMemo

import play.api.mvc.{ Action, RequestHeader, Handler }
import play.api.mvc.Results.BadRequest
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class Firewall(
    collection: MongoCollection,
    cacheTtl: Int) {

  def requestHandler(req: RequestHeader): Option[Handler] = 
    req.headers.get("X-Real-IP").fold(blocks, false) option {
      Action {
        println("IP block " + req.headers.get("X-Real-IP"))
        BadRequest("Your IP has been blocked due to abuse.")
      } 
    }

  def blocks(ip: String) = ips.apply contains ip

  def accepts(ip: String) = !blocks(ip)

  private val ips = new MonoMemo(cacheTtl, fetch)

  private def fetch: IO[Set[String]] = io {
    collection.find() map { obj ⇒
      obj.getAs[String]("_id")
    } 
  } map (_.flatten.toSet)
}
