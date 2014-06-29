package lila.donation

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Types.Coll
import reactivemongo.bson._

final class DonationApi(coll: Coll) {

  private implicit val donationBSONHandler = Macros.handler[Donation]

  def list = coll.find(BSONDocument())
    .sort(BSONDocument("date" -> -1))
    .cursor[Donation]
    .collect[List]()

  def create(donation: Donation) = coll insert donation recover {
    case e: reactivemongo.core.commands.LastError if e.getMessage.contains("duplicate key error") =>
      println(e.getMessage)
  } void

  // in $ cents
  def donatedByUser(userId: String): Fu[Int] =
    coll.find(
      BSONDocument("userId" -> userId),
      BSONDocument("amount" -> true, "_id" -> false)
    ).cursor[BSONDocument].collect[List]() map2 { (obj: BSONDocument) =>
        ~obj.getAs[Int]("amount")
      } map (_.sum)
}
