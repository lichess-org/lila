package lila.timeline

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User

final class UnsubApi(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  private def makeId(channel: String, userId: User.ID) = s"$userId@$channel"

  private def select(channel: String, userId: User.ID) = $id(makeId(channel, userId))

  def set(channel: String, userId: User.ID, v: Boolean): Funit = {
    if (v) coll.insert.one(select(channel, userId)).void
    else coll.delete.one(select(channel, userId)).void
  } recover {
    case _: Exception => ()
  }

  def get(channel: String, userId: User.ID): Fu[Boolean] =
    coll.countSel(select(channel, userId)) dmap (0 !=)

  private def canUnsub(channel: String) = channel startsWith "forum:"

  def filterUnsub(channel: String, userIds: List[User.ID]): Fu[List[String]] =
    canUnsub(channel) ?? coll.distinctEasy[String, List](
      "_id",
      $inIds(userIds.map { makeId(channel, _) })
    ) dmap { unsubs =>
      userIds diff unsubs.map(_ takeWhile ('@' !=))
    }
}
