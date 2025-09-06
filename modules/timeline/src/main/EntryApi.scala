package lila.timeline

import reactivemongo.api.bson.*

import lila.core.timeline.Atom
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final class EntryApi(
    coll: Coll,
    userMax: Max,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  import Entry.given

  private val projection = $doc("users" -> false)

  def userEntries(userId: UserId): Fu[Vector[Entry]] =
    userEntries(userId, userMax, since = none).flatMap(broadcast.interleave(none))

  def moreUserEntries(userId: UserId, nb: Max, since: Option[Instant]): Fu[Vector[Entry]] =
    userEntries(userId, nb, since).flatMap(broadcast.interleave(since))

  private def userEntries(userId: UserId, max: Max, since: Option[Instant]): Fu[Vector[Entry]] =
    (max > 0).so(
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
    )

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

  def insert(e: Entry.ForUsers) =
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

  // entries everyone can see
  // they have no db `users` field
  object broadcast:

    private val cache = cacheApi.unit[Vector[Entry]]:
      _.refreshAfterWrite(1.hour).buildAsyncTimeout(): _ =>
        coll
          .find($doc("users".$exists(false), "date".$gt(nowInstant.minusWeeks(2))))
          .sort($sort.desc("date"))
          .cursor[Entry](ReadPref.pri) // must be on primary for cache refresh to work
          .vector(3)

    private[EntryApi] def interleave(
        since: Option[Instant]
    )(entries: Vector[Entry]): Fu[Vector[Entry]] =
      cache.getUnit.map: bcs =>
        bcs.headOption
          .filter(bc => since.forall(bc.date.isAfter))
          .fold(entries): mostRecentBc =>
            val interleaved =
              val oldestEntry = entries.lastOption
              if oldestEntry.forall(_.date.isBefore(mostRecentBc.date)) then
                (entries ++ bcs).sortBy(-_.date.toMillis)
              else entries
            // sneak recent broadcast at first place
            if mostRecentBc.date.isAfter(nowInstant.minusDays(1)) then
              mostRecentBc +: interleaved.filter(mostRecentBc !=)
            else interleaved

    def insert(atom: Atom): Funit =
      for _ <- coll.insert.one(Entry.make(atom)) yield cache.invalidateUnit()
