package lila.message

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._

import lila.db.dsl._

object ThreadRepo {

  // dirty
  private val coll = Env.current.threadColl

  type ID = String

  def byUser(user: ID): Fu[List[Thread]] =
    coll.find(userQuery(user)).sort(recentSort).cursor[Thread]().gather[List]()

  def visibleByUser(user: ID): Fu[List[Thread]] =
    coll.find(visibleByUserQuery(user)).sort(recentSort).cursor[Thread]().gather[List]()

  def visibleByUser(user: ID, nb: Int): Fu[List[Thread]] =
    coll.find(visibleByUserQuery(user)).sort(recentSort).cursor[Thread]().gather[List](nb)

  def setRead(thread: Thread): Funit = {
    List.fill(thread.nbUnread) {
      coll.update(
        $id(thread.id) ++ $doc("posts.isRead" -> false),
        $set("posts.$.isRead" -> true)
      ).void
    }
  }.sequenceFu.void

  def deleteFor(user: ID)(thread: ID) =
    coll.update($id(thread), $pull("visibleByUserIds", user)).void

  def reallyDeleteByCreatorId(user: ID) = coll.remove($doc("creatorId" -> user))

  def visibleByUserContainingExists(user: ID, containing: String): Fu[Boolean] =
    coll.exists(visibleByUserQuery(user) ++ $doc(
      "posts.0.text".$regex(containing, "")))

  def userQuery(user: String) = $doc("userIds" -> user)

  def visibleByUserQuery(user: String) = $doc("visibleByUserIds" -> user)

  val recentSort = $sort desc "updatedAt"
}
