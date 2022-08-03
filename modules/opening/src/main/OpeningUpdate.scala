package lila.opening

import akka.stream.scaladsl._
import com.softwaremill.tagging._
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import play.api.libs.ws.StandaloneWSClient
import reactivemongo.akkastream.cursorProducer
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ LilaOpening, LilaStream, Markdown }
import lila.db.dsl._

final private class OpeningUpdate(
    ws: StandaloneWSClient,
    coll: Coll @@ OpeningColl,
    explorerEndpoint: String @@ ExplorerEndpoint
)(implicit ec: ExecutionContext, mat: akka.stream.Materializer) {

  private[opening] def all: Funit = addMissingOpenings >> updateHistories

  private def addMissingOpenings: Funit =
    coll.distinctEasy[String, Set]("_id", $empty) flatMap { existingIds =>
      val missingOpenings =
        LilaOpening.openingList.filterNot(op => existingIds.contains(op.familyKeyOrKey.value))
      lila.common.Future.applySequentially(missingOpenings) { op =>
        coll.insert
          .one(OpeningData(op.familyKeyOrKey, op, Nil, DateTime.now minusYears 1, nbGames = 0, Markdown("")))
          .void
      }
    }

  private def updateHistories: Funit = {
    coll
      .find($doc("historyAt" $lt DateTime.now.minusWeeks(1)))
      .cursor[OpeningData]()
      .documentSource()
      .throttle(50, 1 minute) // exactly matches the nginx rate limiting of the explorer
      .mapAsync(1) { op => fetchHistory(op.opening.some) map (op -> _) }
      .mapAsync(1) { case (opening, history) =>
        import OpeningHistory.historySegmentsHandler
        coll.update.one(
          $id(opening.key.value),
          $set(
            "history"   -> history,
            "historyAt" -> DateTime.now,
            "nbGames"   -> history.map(_.sum).sum
          )
        )
      }
      .runWith(LilaStream.sinkCount)
      .chronometer
      .log(logger)(count => s"Done fetching $count opening histories from the explorer")
      .result
      .void
  }

  private[opening] def fetchHistory(op: Option[LilaOpening]): Fu[List[OpeningHistorySegment[Int]]] =
    ws.url(s"$explorerEndpoint/lichess/history")
      .withQueryStringParameters(
        (List(
          "since" -> OpeningData.firstMonth,
          "until" -> OpeningData.lastMonth
        ) ::: op.map(o => "fen" -> o.ref.fen).orElse(Some("play" -> "")).toList): _*
      )
      .get()
      .flatMap {
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: ${op.fold("initial")(_.key.value)}")
        case res =>
          import play.api.libs.ws.JsonBodyReadables._
          import OpeningHistory.segmentJsonRead
          (res.body[JsValue] \ "history")
            .validate[List[OpeningHistorySegment[Int]]]
            .fold(invalid => fufail(invalid.toString), fuccess)
      }
}
