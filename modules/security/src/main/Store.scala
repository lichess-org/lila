package lila.security

import scala.concurrent.Future

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import reactivemongo.bson.BSONDocument

import lila.common.String.{ hex2bytes, bytes2hex }
import lila.db.api._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.user.{ User, UserRepo }
import tube.storeColl

object Store {

  private[security] def save(
    sessionId: String,
    userId: String,
    req: RequestHeader,
    apiVersion: Option[Int],
    isTor: Boolean): Funit =
    storeColl.insert(BSONDocument(
      "_id" -> sessionId,
      "user" -> userId,
      "ip" -> req.remoteAddress,
      "ua" -> lila.common.HTTPRequest.userAgent(req).|("?"),
      "date" -> DateTime.now,
      "up" -> true,
      "api" -> apiVersion,
      "tor" -> isTor.option(true)
    )).void

  def userId(sessionId: String): Fu[Option[String]] =
    storeColl.find(
      BSONDocument("_id" -> sessionId, "up" -> true),
      BSONDocument("user" -> true)
    ).one[BSONDocument] map { _ flatMap (_.getAs[String]("user")) }

  def delete(sessionId: String): Funit =
    storeColl.update(
      BSONDocument("_id" -> sessionId),
      BSONDocument("$set" -> BSONDocument("up" -> false))).void

  // useful when closing an account,
  // we want to logout too
  def disconnect(userId: String): Funit = storeColl.update(
    BSONDocument("user" -> userId),
    BSONDocument("$set" -> BSONDocument("up" -> false)),
    multi = true).void

  def setFingerprint(id: String, fingerprint: String) = storeColl.update(
    BSONDocument("_id" -> id),
    BSONDocument("$set" -> BSONDocument("fp" -> hex2bytes(fingerprint)))).void

  case class Info(ip: String, ua: String, tor: Option[Boolean], fp: Option[String]) {
    def isTorExitNode = ~tor
  }
  import reactivemongo.bson.Macros
  private implicit val InfoBSONHandler = Macros.handler[Info]

  def findInfoByUser(userId: String): Fu[List[Info]] =
    storeColl.find(
      BSONDocument("user" -> userId),
      BSONDocument("_id" -> false, "ip" -> true, "ua" -> true, "tor" -> true, "fp" -> true)
    ).cursor[Info]().collect[List]()
}
