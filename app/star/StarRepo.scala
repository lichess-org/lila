package lila
package star

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import com.mongodb.MongoException.DuplicateKey

import scalaz.effects._
import org.joda.time.DateTime

// db.star.ensureIndex({g:1})
// db.star.ensureIndex({u:1})
// db.star.ensureIndex({d: -1})
final class StarRepo(val collection: MongoCollection) {

  private[star] def toggle(gameId: String, userId: String): IO[Boolean] = io {
    try {
      add(gameId, userId, DateTime.now)
      true
    }
    catch {
      case e: DuplicateKey ⇒ {
        remove(gameId, userId)
        false
      }
    }
  }

  def exists(gameId: String, userId: String) = io {
    (collection count idQuery(gameId, userId)) > 0
  }

  def countByUserId(userId: String) = io {
    collection count userIdQuery(userId) toInt
  }

  def userIdsByGameId(gameId: String): IO[List[String]] = io {
    (collection find gameIdQuery(gameId) sort sortQuery(1) map { obj ⇒
      obj.getAs[String]("u")
    }).toList.flatten
  }

  def removeByGameId(gameId: String): IO[Unit] = io {
    collection remove gameIdQuery(gameId)
  }

  def removeByGameIds(gameIds: List[String]): IO[Unit] = io {
    collection remove ("g" $in gameIds)
  }

  def idQuery(gameId: String, userId: String) = DBObject("_id" -> (gameId + userId))
  def gameIdQuery(gameId: String) = DBObject("g" -> gameId)
  def userIdQuery(userId: String) = DBObject("u" -> userId)
  def sortQuery(order: Int = -1) = DBObject("d" -> order)

  private def add(
    gameId: String,
    userId: String,
    date: DateTime) {
    collection.insert(
      DBObject(
        "_id" -> (gameId + userId),
        "g" -> gameId,
        "u" -> userId,
        "d" -> date),
      writeConcern = WriteConcern.Safe
    )
  }

  private def remove(gameId: String, userId: String) {
    collection remove idQuery(gameId, userId)
  }
}
