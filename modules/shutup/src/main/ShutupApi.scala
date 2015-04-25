package lila.shutup

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.bson.Macros
import reactivemongo.core.commands._
import scala.concurrent.duration._

import lila.db.BSON._
import lila.db.Types.Coll

final class ShutupApi(
    coll: Coll,
    reporter: akka.actor.ActorSelection) {

  private implicit val doubleListHandler = bsonArrayToListHandler[Double]
  private implicit val UserRecordBSONHandler = Macros.handler[UserRecord]

  def record(userId: String, text: String, textType: TextType): Funit = coll.db.command {
    FindAndModify(
      collection = coll.name,
      query = BSONDocument("_id" -> userId),
      modify = Update(
        update = BSONDocument("$push" -> BSONDocument(
          textType.key -> BSONDocument(
            "$each" -> List(BSONDouble(Analyser(text).ratio)),
            "$slice" -> -textType.rotation)
        )),
        fetchNewObject = true),
      upsert = true
    )
  } map2 UserRecordBSONHandler.read flatMap {
    case None             => fufail(s"can't find user record for $userId")
    case Some(userRecord) => legiferate(userRecord)
  } logFailure "ShutupApi"

  private def legiferate(userRecord: UserRecord): Funit =
    userRecord.reports.exists(_.unacceptable) ?? {
      reporter ! lila.hub.actorApi.report.Shutup(userRecord.userId, reportText(userRecord))
      coll.remove(BSONDocument("_id" -> userRecord.userId)).void
    }

  private def reportText(userRecord: UserRecord) =
    "[AUTOREPORT]\n" + userRecord.reports.collect {
      case r if r.unacceptable =>
        s"${r.textType.name}: ${r.nbBad} dubious (out of ${r.ratios.size})"
    }.mkString("\n")
}
