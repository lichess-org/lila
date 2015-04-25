package lila.playban

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.bson.Macros
import reactivemongo.core.commands._
import scala.concurrent.duration._

import lila.db.BSON._
import lila.db.Types.Coll

final class PlaybanApi(coll: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val OutcomeBSONHandler = new BSONHandler[BSONInteger, Outcome] {
    def read(bsonInt: BSONInteger): Outcome = Outcome(bsonInt.value) err s"No such playban outcome: ${bsonInt.value}"
    def write(x: Outcome) = BSONInteger(x.id)
  }
  private implicit val UserRecordBSONHandler = Macros.handler[UserRecord]

  def record(userId: String, outcome: Outcome): Funit = coll.db.command {
    FindAndModify(
      collection = coll.name,
      query = BSONDocument("_id" -> userId),
      modify = Update(
        update = BSONDocument("$push" -> BSONDocument(
          "h" -> BSONDocument(
            "$each" -> List(outcome),
            "$slice" -> -30)
        )),
        fetchNewObject = true),
      upsert = true
    )
  } map2 UserRecordBSONHandler.read flatMap {
    case None             => fufail(s"can't find record for user $userId")
    case Some(userRecord) => funit
  } logFailure "PlaybanApi"
}
