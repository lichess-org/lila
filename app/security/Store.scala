package lila
package security

import user.User

import play.api.mvc.RequestHeader
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import java.util.Date

final class Store(collection: MongoCollection) {

  def save(sessionId: String, username: String, req: RequestHeader) {
    collection += DBObject(
      "_id" -> sessionId,
      "user" -> normalize(username),
      "ip" -> ip(req),
      "ua" -> ua(req),
      "date" -> new Date)
  }

  def getUsername(sessionId: String): Option[String] = for {
    obj ← collection.findOneByID(
      sessionId,
      DBObject("user" -> true)
    )
    v ← obj.getAs[String]("user")
  } yield v

  def delete(sessionId: String) {
    collection.remove(DBObject("_id" -> sessionId))
  }

  // nginx: proxy_set_header X-Real-IP $remote_addr;
  private def ip(req: RequestHeader) = req.headers.get("X-Real-IP") | "0.0.0.0" 

  private def ua(req: RequestHeader) = req.headers.get("User-Agent") | "?" 

  private def normalize(username: String) = username.toLowerCase
}
