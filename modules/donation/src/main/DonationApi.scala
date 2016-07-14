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
    bus: lila.common.Bus) {

  private implicit val donationBSONHandler = Macros.handler[Donation]

  private val minAmount = 200

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
    .gather[List](nb)

  def top(nb: Int): Fu[List[lila.user.User.ID]] = coll.aggregate(
    Match(BSONDocument("userId" -> BSONDocument("$exists" -> true))), List(
      GroupField("userId")("total" -> SumField("net")),
      Sort(Descending("total")),
      Limit(nb))).map {
      _.documents.flatMap { _.getAs[String]("_id") }
    }

  def create(donation: Donation) = {
    coll insert donation recover
      lila.db.recoverDuplicateKey(e => println(e.getMessage)) void
  }
}
