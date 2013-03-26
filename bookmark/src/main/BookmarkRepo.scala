package lila.bookmark

import lila.db.Implicits._
import lila.db.DbApi

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.core.errors._

import play.modules.reactivemongo.Implicits.{ JsObjectWriter ⇒ _, _ }
import lila.db.PlayReactiveMongoPatch._

import org.joda.time.DateTime
import scala.concurrent.Future

case class Bookmark(
  game: lila.game.Game,
  user: lila.user.User,
  date: org.joda.time.DateTime)

// db.bookmark.ensureIndex({g:1})
// db.bookmark.ensureIndex({u:1})
// db.bookmark.ensureIndex({d: -1})
private[bookmark] final class BookmarkRepo(coll: ReactiveColl) extends DbApi {

  def toggle(gameId: String, userId: String): Fu[Boolean] =
    add(gameId, userId, DateTime.now) inject true recoverWith {
      case e: DBError ⇒ (remove(gameId, userId) >> println(e)) inject false
    }

  def userIdsByGameId(gameId: String): Fu[List[String]] =
    primitive(Json.obj("g" -> gameId), "u")(_.asOpt[String])

  def gameIdsByUserId(userId: String): Fu[List[String]] =
    primitive(Json.obj("u" -> userId), "g")(_.asOpt[String])

  def removeByGameId(gameId: String): Funit =
    coll remove Json.obj("g" -> gameId) void

  def removeByGameIds(gameIds: List[String]): Funit =
    coll remove Json.obj("g" -> $in(gameIds)) void

  private def add(gameId: String, userId: String, date: DateTime) =
    coll.insert(Json.obj(
      "_id" -> makeId(gameId, userId),
      "g" -> gameId,
      "u" -> userId,
      "d" -> date))

  def makeId(gameId: String, userId: String) = gameId + userId

  def remove(gameId: String, userId: String): Funit = remove(select(makeId(gameId, userId)))
  def remove(selector: JsObject): Funit = (coll remove selector).void

  protected implicit val builder = coll.genericQueryBuilder
}
