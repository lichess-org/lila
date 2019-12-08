package lila.message

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

import lila.db.dsl._
import lila.user.User

final class ThreadRepo(coll: Coll) {

  type ID = String

  def byUser(user: ID): Fu[List[Thread]] =
    coll.ext.find(userQuery(user)).sort(recentSort).cursor[Thread]().gather[List]()

  def visibleOrDeletedByUser(user: ID, nb: Int): Fu[List[Thread]] =
    for {
      visible <- visibleByUser(user, nb)
      deleted <- coll.ext.find(deletedByUserQuery(user)).sort(recentSort).list[Thread](nb)
    } yield (visible ::: deleted).sortBy(_.updatedAt)(Ordering[DateTime].reverse).take(nb)

  def visibleByUser(user: ID, nb: Int): Fu[List[Thread]] =
    coll.ext.find(visibleByUserQuery(user)).sort(recentSort).list[Thread](nb)

  def visibleByUserByIds(user: User, ids: List[String]): Fu[List[Thread]] =
    coll.ext.find($inIds(ids) ++ visibleByUserQuery(user.id)).list[Thread]()

  def createdByUser(user: ID): Fu[List[Thread]] =
    coll.ext.find(visibleByUserQuery(user) ++ $doc("creatorId" -> user)).list[Thread]()

  // super heavy. For GDPR only.
  private[message] def byAndForWithoutIndex(user: User): Fu[List[Thread]] =
    coll.ext.find($or(
      $doc("creatorId" -> user.id),
      $doc("invitedId" -> user.id)
    )).list[Thread](999, readPreference = ReadPreference.secondaryPreferred)

  def setReadFor(user: User)(thread: Thread): Funit = {
    val indexes = thread.unreadIndexesBy(user)
    indexes.nonEmpty ?? coll.update.one($id(thread.id), $doc("$set" -> indexes.foldLeft($empty) {
      case (s, index) => s ++ $doc(s"posts.$index.isRead" -> true)
    })).void
  }

  def setUnreadFor(user: User)(thread: Thread): Funit =
    thread.readIndexesBy(user).lastOption ?? { index =>
      coll.update.one($id(thread.id), $set(s"posts.$index.isRead" -> false)).void
    }

  def unreadCount(userId: String): Fu[Int] = {
    import reactivemongo.api.bson.BSONNull
    coll.aggregateWith(
      readPreference = ReadPreference.secondaryPreferred
    ) { framework =>
      import framework._
      Match($doc(
        "visibleByUserIds" -> userId,
        "updatedAt" $gt DateTime.now.minusMonths(1),
        "posts.isRead" -> false
      )) -> List(
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
        Group(BSONNull)("nb" -> SumAll)
      )
    }.headOption.map {
      ~_.flatMap(_ int "nb")
    }
  }

  def deleteFor(user: ID)(thread: ID) =
    coll.update.one($id(thread), $doc($pull("visibleByUserIds" -> user), $push("deletedByUserIds" -> user))).void

  def userQuery(user: String) = $doc("userIds" -> user)

  def visibleByUserQuery(user: String) = $doc("visibleByUserIds" -> user)

  def deletedByUserQuery(user: String) = $doc("deletedByUserIds" -> user)

  val recentSort = $sort desc "updatedAt"
}
