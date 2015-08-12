package lila.security

import scala.concurrent.Future

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.Macros

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
      BSONDocument("user" -> true, "_id" -> false)
    ).one[BSONDocument] map { _ flatMap (_.getAs[String]("user")) }

  case class UserIdAndFingerprint(user: String, fp: Option[String])
  private implicit val UserIdAndFingerprintBSONReader = Macros.handler[UserIdAndFingerprint]

  def userIdAndFingerprint(sessionId: String): Fu[Option[UserIdAndFingerprint]] =
    storeColl.find(
      BSONDocument("_id" -> sessionId, "up" -> true),
      BSONDocument("user" -> true, "fp" -> true, "_id" -> false)
    ).one[UserIdAndFingerprint]

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

  def setFingerprint(id: String, fingerprint: String) = {
    import java.util.Base64
    import org.apache.commons.codec.binary.Hex
    val hash = Base64.getEncoder encodeToString {
      Hex decodeHex fingerprint.toArray
    } take 8
    storeColl.update(
      BSONDocument("_id" -> id),
      BSONDocument("$set" -> BSONDocument(
        "fp" -> hash
      ))).void
  }

  case class Info(ip: String, ua: String, tor: Option[Boolean], fp: Option[String]) {
    def isTorExitNode = ~tor
    def fingerprint = fp.map(_.toString)
  }
  private implicit val InfoBSONHandler = Macros.handler[Info]

  def findInfoByUser(userId: String): Fu[List[Info]] =
    storeColl.find(
      BSONDocument("user" -> userId),
      BSONDocument("_id" -> false, "ip" -> true, "ua" -> true, "tor" -> true, "fp" -> true)
    ).cursor[Info]().collect[List]()
}
