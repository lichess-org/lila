package lila.history

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.api._
import lila.db.Types.Coll
import lila.user.Perfs

final class HistoryApi(coll: Coll) {

  // import History.BSONHandler

  // def set(userId: String, history: Iterable[HistoryEntry]): Funit =
  //   coll.insert(BSONDocument(
  //     "_id" -> userId,
  //     "e" -> BSONArray(history map write)
  //   )).void

  // def addEntry(userId: String, entry: HistoryEntry): Funit =
  //   coll.update(
  //     BSONDocument("_id" -> userId),
  //     BSONDocument("$push" -> BSONDocument("e" -> write(entry))),
  //     upsert = true).void

  // def create(perfs: Perfs) = coll.insert(BSONDocument(
  //     "_id" -> user.id,
  //     "e" -> BSONArray(write(HistoryEntry(
  //       DateTime.now,
  //       user.perfs.global.glicko.intRating,
  //       user.perfs.global.glicko.intDeviation,
  //       Glicko.default.intRating)))
  //   )).void
}
