package lidraughts.timeline

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lidraughts.db.dsl._
import lidraughts.hub.actorApi.timeline.Atom
import lidraughts.user.User

final class EntryApi(
    coll: Coll,
    userMax: Int,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  import Entry._

  private val projection = $doc("users" -> false)

  def userEntries(userId: User.ID): Fu[Vector[Entry]] =
    userEntries(userId, userMax) flatMap broadcast.interleave

  def moreUserEntries(userId: User.ID, nb: Int): Fu[Vector[Entry]] =
    userEntries(userId, nb) flatMap broadcast.interleave

  private def userEntries(userId: User.ID, max: Int): Fu[Vector[Entry]] =
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

  def channelUserIdRecentExists(channel: String, userId: User.ID): Fu[Boolean] =
    coll.count($doc(
      "users" -> userId,
      "chan" -> channel,
      "date" $gt DateTime.now.minusDays(7)
    ).some) map (0 !=)

  def insert(e: Entry.ForUsers) =
    coll.insert(EntryBSONHandler.write(e.entry) ++ $doc("users" -> e.userIds)) void

  // can't remove from capped collection,
  // so we set a date in the past instead.
  private[timeline] def removeRecentFollowsBy(userId: User.ID): Funit =
    coll.update(
      $doc("typ" -> "follow", "data.u1" -> userId, "date" $gt DateTime.now().minusHours(1)),
      $set("date" -> DateTime.now().minusDays(365)),
      multi = true
    ).void

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
