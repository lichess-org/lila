package lila.message

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

import lila.db.dsl._
import lila.user.User

object ThreadRepo {

  // dirty
  private val coll = Env.current.threadColl

  type ID = String

  def byUser(user: ID): Fu[List[Thread]] =
    coll.find(userQuery(user)).sort(recentSort).cursor[Thread]().gather[List]()

  def visibleOrDeletedByUser(user: ID, nb: Int): Fu[List[Thread]] =
    for {
      visible <- visibleByUser(user, nb)
      deleted <- coll.find(deletedByUserQuery(user)).sort(recentSort).list[Thread](nb)
    } yield (visible ::: deleted).sortBy(_.updatedAt)(Ordering[DateTime].reverse).take(nb)

  def visibleByUser(user: ID, nb: Int): Fu[List[Thread]] =
    coll.find(visibleByUserQuery(user)).sort(recentSort).list[Thread](nb)

  def visibleByUserByIds(user: User, ids: List[String]): Fu[List[Thread]] =
    coll.find($inIds(ids) ++ visibleByUserQuery(user.id)).list[Thread]()

  def createdByUser(user: ID): Fu[List[Thread]] =
    coll.find(visibleByUserQuery(user) ++ $doc("creatorId" -> user)).list[Thread]()

  // super heavy. For GDPR only.
  private[message] def byAndForWithoutIndex(user: User): Fu[List[Thread]] =
    coll.find($or(
      $doc("creatorId" -> user.id),
      $doc("invitedId" -> user.id)
    )).list[Thread](999, readPreference = ReadPreference.secondaryPreferred)

  def setReadFor(user: User)(thread: Thread): Funit = {
    val indexes = thread.unreadIndexesBy(user)
    indexes.nonEmpty ?? coll.update($id(thread.id), $doc("$set" -> indexes.foldLeft($empty) {
      case (s, index) => s ++ $doc(s"posts.$index.isRead" -> true)
    })).void
  }

  def setUnreadFor(user: User)(thread: Thread): Funit =
    thread.readIndexesBy(user).lastOption ?? { index =>
      coll.update($id(thread.id), $set(s"posts.$index.isRead" -> false)).void
    }

  def unreadCount(userId: String): Fu[Int] = {
    import reactivemongo.bson.BSONNull
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateOne(
      Match($doc(
        "visibleByUserIds" -> userId,
        "updatedAt" $gt DateTime.now.minusMonths(1),
        "posts.isRead" -> false
      )),
      List(
        Project($doc(
          "m" -> $doc("$eq" -> $arr("$creatorId", userId)),
          "posts.isByCreator" -> true,
          "posts.isRead" -> true
        )),
        UnwindField("posts"),
        Match($doc(
          "posts.isRead" -> false
        )),
        Project($doc(
          "u" -> $doc("$ne" -> $arr("$posts.isByCreator", "$m"))
        )),
        Match($doc(
          "u" -> true
        )),
        Group(BSONNull)("nb" -> SumValue(1))
      ),
      readPreference = ReadPreference.secondaryPreferred
    ).map {
        ~_.flatMap(_.getAs[Int]("nb"))
      }
  }

  def deleteFor(user: ID)(thread: ID) =
    coll.update($id(thread), $doc($pull("visibleByUserIds", user), $push("deletedByUserIds", user))).void

  def userQuery(user: String) = $doc("userIds" -> user)

  def visibleByUserQuery(user: String) = $doc("visibleByUserIds" -> user)

  def deletedByUserQuery(user: String) = $doc("deletedByUserIds" -> user)

  val recentSort = $sort desc "updatedAt"
}
