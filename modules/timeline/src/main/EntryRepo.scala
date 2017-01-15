package lila.timeline

import lila.db.dsl._
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.bson._

private[timeline] final class EntryRepo(coll: Coll, userMax: Int) {

  import Entry._

  private val projection = $doc("users" -> false)

  def userEntries(userId: String): Fu[List[Entry]] =
    userEntries(userId, userMax)

  def moreUserEntries(userId: String, nb: Int): Fu[List[Entry]] =
    userEntries(userId, nb)

  private def userEntries(userId: String, max: Int): Fu[List[Entry]] =
    coll.find($doc("users" -> userId), projection)
      .sort($sort desc "date")
      .cursor[Entry](ReadPreference.secondaryPreferred)
      .gather[List](max)

  def findRecent(typ: String, since: DateTime, max: Int) =
    coll.find(
      $doc("typ" -> typ, "date" $gt since),
      projection
    ).sort($sort desc "date")
      .cursor[Entry](ReadPreference.secondaryPreferred)
      .gather[List](max)

  def channelUserIdRecentExists(channel: String, userId: String): Fu[Boolean] =
    coll.count($doc(
      "users" -> userId,
      "chan" -> channel,
      "date" $gt DateTime.now.minusDays(7)
    ).some) map (0 !=)

  def insert(e: Entry.ForUsers) =
    coll.insert(EntryBSONHandler.write(e.entry) ++ $doc("users" -> e.userIds)) void
}
