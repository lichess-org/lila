package lila.security

import scala.concurrent.Future

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson.BSONDocument

import lila.db.api._
import lila.db.Types.Coll
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.user.{ User, UserRepo }
import tube.storeTube

object Store {

  private[security] def save(sessionId: String, userId: String, req: RequestHeader, apiVersion: Option[Int]): Funit =
    storeTube.coll.insert(BSONDocument(
      "_id" -> sessionId,
      "user" -> userId,
      "ip" -> req.remoteAddress,
      "ua" -> lila.common.HTTPRequest.userAgent(req),
      "date" -> DateTime.now,
      "up" -> true,
      "api" -> apiVersion)).void

  def userId(sessionId: String): Fu[Option[String]] =
    storeTube.coll.find(
      BSONDocument("_id" -> sessionId, "up" -> true),
      BSONDocument("user" -> true)
    ).one[BSONDocument] map { _ flatMap (_.getAs[String]("user")) }

  def delete(sessionId: String): Funit =
    $update($select(sessionId), $set("up" -> false))

  // useful when closing an account,
  // we want to logout too
  def disconnect(userId: String): Funit = $update(
    Json.obj("user" -> userId),
    $set("up" -> false),
    upsert = false,
    multi = true)
}
