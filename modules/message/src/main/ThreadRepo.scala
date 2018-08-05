package lila.message

import reactivemongo.api.ReadPreference
import lila.db.dsl._
import lila.user.User

object ThreadRepo {

  // dirty
  private val coll = Env.current.threadColl

  type ID = String

  def byUser(user: ID): Fu[List[Thread]] =
    coll.find(userQuery(user)).sort(recentSort).cursor[Thread]().list

  def visibleByUser(user: ID, nb: Int): Fu[List[Thread]] =
    coll.find(visibleByUserQuery(user)).sort(recentSort)
      .cursor[Thread]().list(nb)

  def visibleByUserByIds(user: User, ids: List[String]): Fu[List[Thread]] =
    coll.find($inIds(ids) ++ visibleByUserQuery(user.id)).cursor[Thread]().list

  def createdByUser(user: ID): Fu[List[Thread]] =
    coll.find(visibleByUserQuery(user) ++ $doc("creatorId" -> user))
      .cursor[Thread]().list

  // super heavy. For GDPR only.
  private[message] def byAndForWithoutIndex(user: User): Fu[List[Thread]] =
    coll.find($or(
      $doc("creatorId" -> user.id),
      $doc("invitedId" -> user.id)
    )).cursor[Thread](ReadPreference.secondaryPreferred).list(999)

  def setReadFor(user: User)(thread: Thread): Funit = {
    val indexes = thread.unreadIndexesBy(user)
    indexes.nonEmpty ?? coll.update.one(
      $id(thread.id), $doc("$set" -> indexes.foldLeft($empty) {
        case (s, index) => s ++ $doc(s"posts.$index.isRead" -> true)
      })
    ).void
  }

  def setUnreadFor(user: User)(thread: Thread): Funit =
    thread.readIndexesBy(user).lastOption ?? { index =>
      coll.update.one(
        $id(thread.id), $set(s"posts.$index.isRead" -> false)
      ).void
    }

  def deleteFor(user: ID)(thread: ID): Funit =
    coll.update.one($id(thread), $pull("visibleByUserIds" -> user)).void

  def userQuery(user: String) = $doc("userIds" -> user)

  def visibleByUserQuery(user: String) = $doc("visibleByUserIds" -> user)

  val recentSort = $sort desc "updatedAt"
}
