package lila.donation

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import org.joda.time.DateTime
import reactivemongo.bson._

final class DonationApi(
    coll: Coll,
    monthlyGoal: Int,
    serverDonors: Set[String],
    bus: lila.common.Bus) {

  private implicit val donationBSONHandler = Macros.handler[Donation]

  private val minAmount = 200

  private val donorCache = lila.memo.AsyncCache[String, Boolean](
    userId => donatedByUser(userId).map(_ >= minAmount),
    maxCapacity = 5000)

  private val decentAmount = BSONDocument("gross" -> BSONDocument("$gte" -> BSONInteger(minAmount)))

  def list(nb: Int) = coll.find(decentAmount)
    .sort(BSONDocument("date" -> -1))
    .cursor[Donation]()
    .collect[List](nb)

  def top(nb: Int) = coll.find(BSONDocument(
    "userId" -> BSONDocument("$exists" -> true)
  )).sort(BSONDocument(
    "gross" -> -1,
    "date" -> -1
  )).cursor[Donation]()
    .collect[List](nb)

  def isDonor(userId: String) =
    if (serverDonors contains userId) fuccess(true)
    else donorCache(userId)

  def create(donation: Donation) = {
    coll insert donation recover
      lila.db.recoverDuplicateKey(e => println(e.getMessage)) void
  } >> donation.userId.??(donorCache.remove) >>- progress.foreach { prog =>
    bus.publish(lila.hub.actorApi.DonationEvent(
      userId = donation.userId,
      gross = donation.gross,
      net = donation.net,
      message = donation.message.trim.some.filter(_.nonEmpty),
      progress = prog.percent), 'donation)
  }

  // in $ cents
  def donatedByUser(userId: String): Fu[Int] =
    coll.find(
      decentAmount ++ BSONDocument("userId" -> userId),
      BSONDocument("net" -> true, "_id" -> false)
    ).cursor[BSONDocument]().collect[List]() map2 { (obj: BSONDocument) =>
        ~obj.getAs[Int]("net")
      } map (_.sum)

  def progress: Fu[Progress] = {
    val from = DateTime.now withDayOfMonth 1 withHourOfDay 0 withMinuteOfHour 0 withSecondOfMinute 0
    val to = from plusMonths 1
    coll.find(
      BSONDocument("date" -> BSONDocument(
        "$gte" -> from,
        "$lt" -> to
      )),
      BSONDocument("net" -> true, "_id" -> false)
    ).cursor[BSONDocument]().collect[List]() map2 { (obj: BSONDocument) =>
        ~obj.getAs[Int]("net")
      } map (_.sum) map { amount =>
        Progress(from, monthlyGoal, amount)
      }
  }
}
