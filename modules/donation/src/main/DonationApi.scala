package lila.donation

import org.joda.time.{ DateTime, DateTimeConstants }
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._
import scala.concurrent.duration._
import scala.util.Try

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final class DonationApi(
    coll: Coll,
    weeklyGoal: Int,
    serverDonors: Set[String],
    bus: lila.common.Bus) {

  private implicit val donationBSONHandler = Macros.handler[Donation]

  private val minAmount = 200

  private val donorCache = lila.memo.AsyncCache[String, Boolean](
    userId => donatedByUser(userId).map(_ >= minAmount),
    maxCapacity = 5000)

  // in $ cents
  private def donatedByUser(userId: String): Fu[Int] =
    coll.aggregate(
      Match(decentAmount ++ BSONDocument("userId" -> userId)), List(
        Group(BSONNull)("net" -> SumField("net"))
      )).map {
        ~_.documents.headOption.flatMap { _.getAs[Int]("net") }
      }

  private val decentAmount = BSONDocument("gross" -> BSONDocument("$gte" -> BSONInteger(minAmount)))

  def list(nb: Int) = coll.find(decentAmount)
    .sort(BSONDocument("date" -> -1))
    .cursor[Donation]()
    .collect[List](nb)

  def top(nb: Int): Fu[List[lila.user.User.ID]] = coll.aggregate(
    Match(BSONDocument("userId" -> BSONDocument("$exists" -> true))), List(
      GroupField("userId")("total" -> SumField("net")),
      Sort(Descending("total")),
      Limit(nb))).map {
      _.documents.flatMap { _.getAs[String]("_id") }
    }

  def isDonor(userId: String) =
    if (serverDonors contains userId) fuccess(true)
    else donorCache(userId)

  def create(donation: Donation) = {
    coll insert donation recover
      lila.db.recoverDuplicateKey(e => println(e.getMessage)) void
  } >> progressCache.clear >>
    donation.userId.??(donorCache.remove) >>-
    progress.foreach { prog =>
      bus.publish(lila.hub.actorApi.DonationEvent(
        userId = donation.userId,
        gross = donation.gross,
        net = donation.net,
        message = donation.message.trim.some.filter(_.nonEmpty),
        progress = prog.percent), 'donation)
    }

  private val progressCache = lila.memo.AsyncCache.single[Progress](
    computeProgress,
    timeToLive = 10 seconds)

  def progress: Fu[Progress] = progressCache(true)

  private def computeProgress: Fu[Progress] = {
    val from = Try {
      val now = DateTime.now
      val a = now withDayOfWeek DateTimeConstants.SUNDAY withHourOfDay 0 withMinuteOfHour 0 withSecondOfMinute 0
      if (a isBefore now) a else a minusWeeks 1
    }.toOption.getOrElse(DateTime.now withDayOfWeek 1)
    val to = from plusWeeks 1
    coll.find(
      BSONDocument("date" -> BSONDocument(
        "$gte" -> from,
        "$lt" -> to
      )),
      BSONDocument("net" -> true, "_id" -> false)
    ).cursor[BSONDocument]().collect[List]() map2 { (obj: BSONDocument) =>
        ~obj.getAs[Int]("net")
      } map (_.sum) map { amount =>
        Progress(from, weeklyGoal, amount)
      } addEffect { prog =>
        lila.mon.donation.goal(prog.goal)
        lila.mon.donation.current(prog.current)
        lila.mon.donation.percent(prog.percent)
      }
  }
}
