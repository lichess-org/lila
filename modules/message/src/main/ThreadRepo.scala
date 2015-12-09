package lila.message

import scala.concurrent.Future

import play.api.libs.json.Json

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import tube.threadTube

object ThreadRepo {
  import play.modules.reactivemongo.json._

  type ID = String

  def byUser(user: ID): Fu[List[Thread]] =
    $find($query(userQuery(user)) sort recentSort)

  def visibleByUser(user: ID): Fu[List[Thread]] =
    $find($query(visibleByUserQuery(user)) sort recentSort)

  def visibleByUser(user: ID, nb: Int): Fu[List[Thread]] =
    $find($query(visibleByUserQuery(user)) sort recentSort, nb)

  def userUnreadIds(userId: String): Fu[List[String]] = {
    import reactivemongo.bson._
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    threadTube.coll.aggregate(
      Match(BSONDocument(
        "visibleByUserIds" -> userId,
        "posts.isRead" -> false
      )),
      List(
        Project(BSONDocument(
          "creatorId" -> true,
          "posts.isByCreator" -> true,
          "posts.isRead" -> true
        )),
        Unwind("posts"),
        Match(BSONDocument(
          "posts.isRead" -> false,
          "posts.isByCreator" -> BSONDocument(
            "$ne" -> BSONArray("$creatorId", userId)
          )
        )),
        Project(BSONDocument("_id" -> true))
      ),
      allowDiskUse = false
    ).map {
        _.documents.flatMap { _.getAs[String]("_id") }.toList
      }
  }

  def setRead(thread: Thread): Funit = {
    List.fill(thread.nbUnread) {
      $update(
        $select(thread.id) ++ Json.obj("posts.isRead" -> false),
        $set("posts.$.isRead" -> true)
      )
    }
  }.sequenceFu.void

  def deleteFor(user: ID)(thread: ID) =
    $update($select(thread), $pull("visibleByUserIds", user))

  def reallyDeleteByCreatorId(user: ID) = $remove(Json.obj("creatorId" -> user))

  def visibleByUserContainingExists(user: ID, containing: String): Fu[Boolean] =
    $count.exists(visibleByUserQuery(user) ++ Json.obj(
      "posts.0.text" -> $regex(containing)))

  def userQuery(user: String) = Json.obj("userIds" -> user)

  def visibleByUserQuery(user: String) = Json.obj("visibleByUserIds" -> user)

  val recentSort = $sort desc "updatedAt"
}
