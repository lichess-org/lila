package lila.user

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.api._
import tube.historyColl

case class HistoryEntry(date: DateTime, rating: Int, deviation: Int, opponent: Int)

object HistoryRepo {

  private def write(o: HistoryEntry) = BSONArray(
    BSONInteger(o.date.getSeconds.toInt),
    BSONInteger(o.rating),
    BSONInteger(o.deviation),
    BSONInteger(o.opponent)
  )

  def set(userId: String, history: Iterable[HistoryEntry]): Funit = successful {
    historyColl.insert(BSONDocument(
      "_id" -> userId,
      "e" -> BSONArray(history map write)
    ))
  }

  def addEntry(userId: String, entry: HistoryEntry): Funit = successful {
    historyColl.update(
      BSONDocument("_id" -> userId),
      BSONDocument("$push" -> BSONDocument("e" -> write(entry))),
      upsert = true)
  }

  def create(user: User) = successful {
    historyColl.insert(BSONDocument(
      "_id" -> user.id,
      "e" -> BSONArray(write(HistoryEntry(
        DateTime.now,
        user.perfs.global.glicko.intRating,
        user.perfs.global.glicko.intDeviation,
        Glicko.default.intRating)))
    ))
  }

  private val arrayReader = implicitly[BSONReader[_ <: BSONValue, BSONArray]].asInstanceOf[BSONReader[BSONValue, BSONArray]]
  def userRatings(userId: String, slice: Option[Int] = None): Fu[List[HistoryEntry]] = {
    historyColl.find(
      BSONDocument("_id" -> userId),
      BSONDocument("_id" -> false) ++ slice.fold(BSONDocument()) { s ⇒
        BSONDocument("e" -> BSONDocument("$slice" -> s))
      }
    ).one[BSONDocument] map { historyOption ⇒
        ~(for {
          history ← historyOption
          entries ← history.getAs[BSONArray]("e")
          stream = entries.values map arrayReader.readOpt
          elems = stream collect {
            case Some(array) ⇒ HistoryEntry(
              new DateTime(~array.getAs[Int](0) * 1000l),
              ~array.getAs[Int](1),
              ~array.getAs[Int](2),
              ~array.getAs[Int](3))
          }
        } yield elems.toList)
      }
  }
}
