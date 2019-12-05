package lila.timeline

import reactivemongo.api.bson._

import lila.db.dsl._

final class UnsubApi(coll: Coll) {

  private def makeId(channel: String, userId: String) = s"$userId@$channel"

  private def select(channel: String, userId: String) = $id(makeId(channel, userId))

  def set(channel: String, userId: String, v: Boolean): Funit = {
    if (v) coll.insert.one(select(channel, userId)).void
    else coll.delete.one(select(channel, userId)).void
  } recover {
    case e: Exception => ()
  }

  def get(channel: String, userId: String): Fu[Boolean] =
    coll.countSel(select(channel, userId)) dmap (0 !=)

  private def canUnsub(channel: String) = channel startsWith "forum:"

  def filterUnsub(channel: String, userIds: List[String]): Fu[List[String]] =
    canUnsub(channel) ?? coll.distinctEasy[String, List](
      "_id", $inIds(userIds.map { makeId(channel, _) })
    ) dmap { unsubs =>
        userIds diff unsubs.map(_ takeWhile ('@' !=))
      }
}
