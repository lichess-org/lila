package lila.timeline

import reactivemongo.bson._

import lila.db.dsl._

private[timeline] final class UnsubApi(coll: Coll) {

  private def makeId(channel: String, userId: String) = s"$userId@$channel"

  private def select(channel: String, userId: String) = $id(makeId(channel, userId))

  def set(channel: String, userId: String, v: Boolean): Funit = {
    if (v) coll.insert(select(channel, userId)).void
    else coll.remove(select(channel, userId)).void
  } recover {
    case e: Exception => ()
  }

  def get(channel: String, userId: String): Fu[Boolean] =
    coll.count(select(channel, userId).some) map (0 !=)

  private def canUnsub(channel: String) = channel startsWith "forum:"

  def filterUnsub(channel: String, userIds: List[String]): Fu[List[String]] =
    canUnsub(channel) ?? coll.distinct[String, List](
      "_id", $inIds(userIds.map { makeId(channel, _) }).some
    ) map { unsubs =>
        userIds diff unsubs.map(_ takeWhile ('@' !=))
      }
}
