package lila.security

import scala.concurrent.Future

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson.BSONDocument

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Types.Coll
import lila.user.{ User, UserRepo }
import tube.storeTube

object Store {

  type IP = String

  private[security] def save(sessionId: String, userId: String, req: RequestHeader): Funit =
    $insert(Json.obj(
      "_id" -> sessionId,
      "user" -> userId,
      "ip" -> ip(req),
      "ua" -> ua(req),
      "date" -> $date(DateTime.now),
      "up" -> true))

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

  private def ip(req: RequestHeader) = req.remoteAddress

  private def ua(req: RequestHeader) = req.headers.get("User-Agent") | "?"
}
