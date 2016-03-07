package lila.timeline

import reactivemongo.bson._

import lila.db.Types.Coll

private[timeline] final class UnsubApi(coll: Coll) {

  private def makeId(channel: String, userId: String) = s"$userId@$channel"

  private def select(channel: String, userId: String) =
    BSONDocument("_id" -> makeId(channel, userId))

  def set(channel: String, userId: String, v: Boolean): Funit = {
    if (v) coll.insert(select(channel, userId)).void
    else coll.remove(select(channel, userId)).void
  } recover {
    case e: Exception => ()
  }

  def get(channel: String, userId: String): Fu[Boolean] =
    coll.count(select(channel, userId).some) map (0 !=)

  def filterUnsub(channel: String, userIds: Set[String]): Fu[Set[String]] =
    coll.distinct[String, Set]("_id", BSONDocument(
      "_id" -> BSONDocument("$in" -> userIds.map { makeId(channel, _) })
    ).some) map { unsubs =>
      userIds diff unsubs.map(_ takeWhile ('@' !=))
    }
}
