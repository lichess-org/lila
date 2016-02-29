package lila.timeline

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import org.joda.time.DateTime
import reactivemongo.bson._

private[timeline] final class EntryRepo(coll: Coll, userMax: Int) {

  import Entry._

  def userEntries(userId: String): Fu[List[Entry]] =
    userEntries(userId, userMax)

  def moreUserEntries(userId: String, nb: Int): Fu[List[Entry]] =
    userEntries(userId, nb)

  private def userEntries(userId: String, max: Int): Fu[List[Entry]] =
    coll.find(BSONDocument("users" -> userId))
      .sort(BSONDocument("date" -> -1))
      .cursor[Entry]()
      .collect[List](max)

  def findRecent(typ: String, since: DateTime) =
    coll.find(BSONDocument(
      "typ" -> typ,
      "date" -> BSONDocument("$gt" -> since)
    )).cursor[Entry]()
      .collect[List]()

  def channelUserIdRecentExists(channel: String, userId: String): Fu[Boolean] =
    coll.count(BSONDocument(
      "users" -> userId,
      "chan" -> channel,
      "date" -> BSONDocument("$gt" -> DateTime.now.minusDays(7))
    ).some) map (0 !=)

  def insert(entry: Entry) = coll insert entry void
}
