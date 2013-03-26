package lila.bookmark

import lila.common.PimpedJson._
import lila.db.Types.ReactiveColl
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
      case e: DatabaseException ⇒ remove(gameId, userId) inject false
    }

  //   def userIdsByGameId(gameId: String): IO[List[String]] = io {
  //     (collection find gameIdQuery(gameId) sort sortQuery(1) map { obj ⇒
  //       obj.getAs[String]("u")
  //     }).flatten.toList
  //   }

  //   def gameIdsByUserId(userId: String): IO[Set[String]] = io {
  //     (collection find userIdQuery(userId) map { obj ⇒
  //       obj.getAs[String]("g")
  //     }).flatten.toSet
  //   }

  //   def removeByGameId(gameId: String): IO[Unit] = io {
  //     collection remove gameIdQuery(gameId)
  //   }

  //   def removeByGameIds(gameIds: List[String]): IO[Unit] = io {
  //     collection remove ("g" $in gameIds)
  //   }

  //   def idQuery(gameId: String, userId: String) = DBObject("_id" -> (gameId + userId))
  //   def gameIdQuery(gameId: String) = DBObject("g" -> gameId)
  //   def userIdQuery(userId: String) = DBObject("u" -> userId)
  //   def sortQuery(order: Int = -1) = DBObject("d" -> order)

  private def add(gameId: String, userId: String, date: DateTime) =
    coll.insert(Json.obj(
      "_id" -> makeId(gameId, userId),
      "g" -> gameId,
      "u" -> userId,
      "d" -> date))

  def makeId(gameId: String, userId: String) = gameId + userId

  def remove(gameId: String, userId: String) = remove(select(makeId(gameId, userId)))
  def remove(selector: JsObject) = (coll remove selector).void

  private def query(selector: JsObject) = coll.genericQueryBuilder query selector
}
