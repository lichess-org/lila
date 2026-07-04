package lila.timeline

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class EntryApi(coll: Coll, userMax: Max)(using Executor):

  import Entry.given

  private val projection = $doc("users" -> false)

  def userEntries(userId: UserId): Fu[Vector[Entry]] =
    userEntries(userId, userMax, since = none)

  def moreUserEntries(userId: UserId, nb: Max, since: Option[Instant]): Fu[Vector[Entry]] =
    userEntries(userId, nb, since)

  private def userEntries(userId: UserId, max: Max, since: Option[Instant]): Fu[Vector[Entry]] =
    (max > 0).so:
      coll
        .find(
          $doc(
            "users" -> userId,
            "date".$gt(since.getOrElse(nowInstant.minusWeeks(2)))
          ),
          projection.some
        )
        .sort($sort.desc("date"))
        .cursor[Entry](ReadPref.sec)
        .vector(max.value)

  def findRecent(typ: String, since: Instant, max: Max) =
    coll
      .find(
        $doc("typ" -> typ, "date".$gt(since)),
        projection.some
      )
      .sort($sort.desc("date"))
      .cursor[Entry](ReadPref.sec)
      .vector(max.value)

  def channelUserIdRecentExists(channel: String, userId: UserId): Fu[Boolean] =
    coll.exists:
      $doc(
        "users" -> userId,
        "chan" -> channel,
        "date".$gt(nowInstant.minusDays(7))
      )

  private[timeline] def insert(e: Entry.ForUsers) =
    coll.insert.one(bsonWriteObjTry(e.entry).get ++ $doc("users" -> e.userIds)).void

  // can't remove from capped collection,
  // so we set a date in the past instead.
  private[timeline] def removeRecentFollowsBy(userId: UserId): Funit =
    coll.update
      .one(
        $doc("typ" -> "follow", "data.u1" -> userId, "date".$gt(nowInstant.minusHours(1))),
        $set("date" -> nowInstant.minusDays(365)),
        multi = true
      )
      .void
