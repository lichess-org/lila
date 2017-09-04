package lila.timeline

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.hub.actorApi.timeline.Atom

final class EntryApi(
    coll: Coll,
    userMax: Int,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import Entry._

  private val projection = $doc("users" -> false)

  def userEntries(userId: String): Fu[Vector[Entry]] =
    userEntries(userId, userMax) flatMap broadcast.interleave

  def moreUserEntries(userId: String, nb: Int): Fu[Vector[Entry]] =
    userEntries(userId, nb) flatMap broadcast.interleave

  private def userEntries(userId: String, max: Int): Fu[Vector[Entry]] =
    coll.find($doc(
      "users" -> userId,
      "date" $gt DateTime.now.minusWeeks(2)
    ), projection)
      .sort($sort desc "date")
      .cursor[Entry](ReadPreference.secondaryPreferred)
      .gather[Vector](max)

  def findRecent(typ: String, since: DateTime, max: Int) =
    coll.find(
      $doc("typ" -> typ, "date" $gt since),
      projection
    ).sort($sort desc "date")
      .cursor[Entry](ReadPreference.secondaryPreferred)
      .gather[Vector](max)

  def channelUserIdRecentExists(channel: String, userId: String): Fu[Boolean] =
    coll.count($doc(
      "users" -> userId,
      "chan" -> channel,
      "date" $gt DateTime.now.minusDays(7)
    ).some) map (0 !=)

  def insert(e: Entry.ForUsers) =
    coll.insert(EntryBSONHandler.write(e.entry) ++ $doc("users" -> e.userIds)) void

  // entries everyone can see
  // they have no db `users` field
  object broadcast {

    private val cache = asyncCache.single(
      name = "timeline.broadcastCache",
      f = fetch,
      expireAfter = _.ExpireAfterWrite(1 hour)
    )

    private def fetch: Fu[Vector[Entry]] = coll
      .find($doc(
        "users" $exists false,
        "date" $gt DateTime.now.minusWeeks(2)
      ))
      .sort($sort desc "date")
      .cursor[Entry]() // must be on primary for cache refresh to work
      .gather[Vector](3)

    private[EntryApi] def interleave(entries: Vector[Entry]): Fu[Vector[Entry]] = cache.get map { bcs =>
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

    def insert(atom: Atom): Funit = coll.insert(Entry make atom).void >>- cache.refresh
  }
}
