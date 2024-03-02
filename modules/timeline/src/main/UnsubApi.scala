package lila.timeline

import reactivemongo.api.bson.*

import lila.db.dsl.*

final class UnsubApi(coll: Coll)(using Executor):

  private def makeId(channel: String, userId: UserId) = s"$userId@$channel"

  private def select(channel: String, userId: UserId) = $id(makeId(channel, userId))

  def set(channel: String, userId: UserId, v: Boolean): Funit = {
    if v then coll.insert.one(select(channel, userId)).void
    else coll.delete.one(select(channel, userId)).void
  }.recover { case _: Exception =>
    ()
  }

  def get(channel: String, userId: UserId): Fu[Boolean] =
    coll.countSel(select(channel, userId)).dmap(0 !=)

  private def canUnsub(channel: String) = channel.startsWith("forum:")

  def filterUnsub(channel: String, userIds: List[UserId]): Fu[List[UserId]] =
    canUnsub(channel)
      .so(
        coll.distinctEasy[String, List](
          "_id",
          $inIds(userIds.map { makeId(channel, _) })
        )
      )
      .dmap { unsubs =>
        userIds.diff(unsubs.map(id => UserId(id.takeWhile('@' !=))))
      }
