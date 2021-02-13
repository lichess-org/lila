package lila.timeline

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.config.Max
import lila.db.dsl._
import lila.hub.actorApi.timeline.Atom
import lila.memo.CacheApi._
import lila.user.User

final class EntryApi(
    coll: Coll,
    userMax: Max,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Entry._

  private val projection = $doc("users" -> false)

  def userEntries(userId: User.ID): Fu[Vector[Entry]] =
    userEntries(userId, userMax) flatMap broadcast.interleave

  def moreUserEntries(userId: User.ID, nb: Max): Fu[Vector[Entry]] =
    userEntries(userId, nb) flatMap broadcast.interleave

  private def userEntries(userId: User.ID, max: Max): Fu[Vector[Entry]] =
    (max.value > 0) ?? coll
      .find(
        $doc(
          "users" -> userId,
          "date" $gt DateTime.now.minusWeeks(2)
        ),
        projection.some
      )
      .sort($sort desc "date")
      .cursor[Entry](ReadPreference.secondaryPreferred)
      .vector(max.value)

  def findRecent(typ: String, since: DateTime, max: Max) =
    coll
      .find(
        $doc("typ" -> typ, "date" $gt since),
        projection.some
      )
      .sort($sort desc "date")
      .cursor[Entry](ReadPreference.secondaryPreferred)
      .vector(max.value)

  def channelUserIdRecentExists(channel: String, userId: User.ID): Fu[Boolean] =
    coll.countSel(
      $doc(
        "users" -> userId,
        "chan"  -> channel,
        "date" $gt DateTime.now.minusDays(7)
      )
    ) map (0 !=)

  def insert(e: Entry.ForUsers) =
    coll.insert.one(EntryBSONHandler.writeTry(e.entry).get ++ $doc("users" -> e.userIds)) void

  // can't remove from capped collection,
  // so we set a date in the past instead.
  private[timeline] def removeRecentFollowsBy(userId: User.ID): Funit =
    coll.update
      .one(
        $doc("typ"  -> "follow", "data.u1" -> userId, "date" $gt DateTime.now().minusHours(1)),
        $set("date" -> DateTime.now().minusDays(365)),
        multi = true
      )
      .void

  // entries everyone can see
  // they have no db `users` field
  object broadcast {

    private val cache = cacheApi.unit[Vector[Entry]] {
      _.refreshAfterWrite(1 hour)
        .buildAsyncFuture(_ => fetch)
    }

    private def fetch: Fu[Vector[Entry]] =
      coll
        .find(
          $doc(
            "users" $exists false,
            "date" $gt DateTime.now.minusWeeks(2)
          )
        )
        .sort($sort desc "date")
        .cursor[Entry](ReadPreference.primary) // must be on primary for cache refresh to work
        .vector(3)

    private[EntryApi] def interleave(entries: Vector[Entry]): Fu[Vector[Entry]] =
      cache.getUnit map { bcs =>
        bcs.headOption.fold(entries) { mostRecentBc =>
          val interleaved = {
            val oldestEntry = entries.lastOption
            if (oldestEntry.fold(true)(_.date isBefore mostRecentBc.date))
              (entries ++ bcs).sortBy(-_.date.getMillis)
            else entries
          }
          // sneak recent broadcast at first place
          if (mostRecentBc.date.isAfter(DateTime.now minusDays 1))
            mostRecentBc +: interleaved.filter(mostRecentBc !=)
          else interleaved
        }
      }

    def insert(atom: Atom): Funit = coll.insert.one(Entry make atom).void >>- cache.invalidateUnit()
  }
}
