package lila.donation

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import org.joda.time.DateTime
import reactivemongo.bson._

final class DonationApi(coll: Coll, monthlyGoal: Int) {

  private implicit val donationBSONHandler = Macros.handler[Donation]

  private val decentAmount = BSONDocument("amount" -> BSONDocument("$gte" -> BSONInteger(2)))

  def list = coll.find(decentAmount)
    .sort(BSONDocument("date" -> -1))
    .cursor[Donation]
    .collect[List]()

  def top(nb: Int) = coll.find(BSONDocument())
    .sort(BSONDocument("amount" -> -1, "date" -> -1))
    .cursor[Donation]
    .collect[List](nb)

  def create(donation: Donation) = coll insert donation recover {
    case e: reactivemongo.core.commands.LastError if e.getMessage.contains("duplicate key error") =>
      println(e.getMessage)
  } void

  // in $ cents
  def donatedByUser(userId: String): Fu[Int] =
    coll.find(
      decentAmount ++ BSONDocument("userId" -> userId),
      BSONDocument("amount" -> true, "_id" -> false)
    ).cursor[BSONDocument].collect[List]() map2 { (obj: BSONDocument) =>
        ~obj.getAs[Int]("amount")
      } map (_.sum)

  def progress: Fu[Progress] = {
    val from = DateTime.now withDayOfMonth 1 withHourOfDay 0 withMinuteOfHour 0 withSecondOfMinute 0
    val to = from plusMonths 1
    coll.find(
      BSONDocument("date" -> BSONDocument(
        "$gte" -> from,
        "$lt" -> to
      )),
      BSONDocument("amount" -> true, "_id" -> false)
    ).cursor[BSONDocument].collect[List]() map2 { (obj: BSONDocument) =>
        Donation afterFees ~obj.getAs[Int]("amount")
      } map (_.sum) map { amount =>
        Progress(from, monthlyGoal, amount)
      }
  }
}
