package lila.bookmark

import lila.db.Implicits._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import play.modules.reactivemongo.Implicits._
import reactivemongo.core.commands.Count

import org.joda.time.DateTime
import scala.concurrent.Future

case class Bookmark(
  game: lila.game.Game,
  user: lila.user.User,
  date: org.joda.time.DateTime)

// db.bookmark.ensureIndex({g:1})
// db.bookmark.ensureIndex({u:1})
// db.bookmark.ensureIndex({d: -1})
private[bookmark] final class BookmarkRepo(implicit val coll: ReactiveColl) extends lila.db.api.Full {

  def toggle(gameId: String, userId: String): Fu[Boolean] =
    exists(selectId(gameId, userId)) flatMap { e ⇒
      e.fold(
        remove(gameId, userId),
        add(gameId, userId, DateTime.now)
      ) inject !e
    }

  def userIdsByGameId(gameId: String): Fu[List[String]] =
    primitive(Json.obj("g" -> gameId), "u")(_.asOpt[String])

  def gameIdsByUserId(userId: String): Fu[List[String]] =
    primitive(userIdQuery(userId), "g")(_.asOpt[String])

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

  def userIdQuery(userId: String) = Json.obj("u" -> userId)
  def makeId(gameId: String, userId: String) = gameId + userId
  def selectId(gameId: String, userId: String) = select(makeId(gameId, userId))

  def remove(gameId: String, userId: String): Funit = remove(selectId(gameId, userId))
  def remove(selector: JsObject): Funit = (coll remove selector).void
}
