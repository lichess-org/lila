package lila.bookmark

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._

case class Bookmark(game: lila.game.Game, user: lila.user.User)

private[bookmark] object BookmarkRepo {

  // dirty
  private val coll = Env.current.bookmarkColl

  def toggle(gameId: String, userId: String): Fu[Boolean] =
    coll exists selectId(gameId, userId) flatMap { e =>
      e.fold(
        remove(gameId, userId),
        add(gameId, userId, DateTime.now)
      ) inject !e
    }

  def gameIdsByUserId(userId: String): Fu[Set[String]] =
    coll.distinct("g", $doc("u" -> userId).some) map lila.db.BSON.asStringSet

  def removeByGameId(gameId: String): Funit =
    coll.remove($doc("g" -> gameId)).void

  def removeByGameIds(gameIds: List[String]): Funit =
    coll.remove($doc("g" -> $in(gameIds))).void

  private def add(gameId: String, userId: String, date: DateTime): Funit =
    coll.insert($doc(
      "_id" -> makeId(gameId, userId),
      "g" -> gameId,
      "u" -> userId,
      "d" -> date)).void

  def userIdQuery(userId: String) = $doc("u" -> userId)
  def makeId(gameId: String, userId: String) = s"$gameId$userId"
  def selectId(gameId: String, userId: String) = $id(makeId(gameId, userId))

  def remove(gameId: String, userId: String): Funit = coll.remove(selectId(gameId, userId)).void
  def remove(selector: Bdoc): Funit = coll.remove(selector).void
}
