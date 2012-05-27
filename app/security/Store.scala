package lila
package security

import user.User

import play.api.mvc.RequestHeader
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

final class Store(collection: MongoCollection) {

  def save(sessionId: String, username: String, req: RequestHeader) {
    collection += DBObject(
      "_id" -> sessionId,
      "user" -> normalize(username),
      "ip" -> ip(req),
      "ua" -> ua(req),
      "date" -> DateTime.now)
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

  // useful when closing an account,
  // we want to logout too
  def deleteUsername(username: String) {
    collection.remove(DBObject("user" -> normalize(username)))
  }

  case class Info(
    user: String,
    ip: String,
    ua: String,
    date: DateTime)

  def userInfo(username: String): Option[Info] = for {
    obj ← collection.findOne(DBObject("user" -> normalize(username)))
    ip ← obj.getAs[String]("ip")
    ua ← obj.getAs[String]("ua")
    date ← obj.getAs[DateTime]("date")
  } yield Info(username, ip, ua, date)

  // nginx: proxy_set_header X-Real-IP $remote_addr;
  private def ip(req: RequestHeader) = req.headers.get("X-Real-IP") | "0.0.0.0"

  private def ua(req: RequestHeader) = req.headers.get("User-Agent") | "?"

  private def normalize(username: String) = username.toLowerCase
}
