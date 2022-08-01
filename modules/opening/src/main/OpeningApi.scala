package lila.opening

import akka.stream.scaladsl._
import com.softwaremill.tagging._
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import reactivemongo.akkastream.cursorProducer
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.LilaOpening
import lila.common.LilaOpeningFamily
import lila.common.LilaStream
import lila.common.Markdown
import lila.db.dsl._
import lila.memo.CacheApi

final class OpeningApi(
    ws: StandaloneWSClient,
    cacheApi: CacheApi,
    coll: Coll @@ OpeningColl,
    explorerEndpoint: String @@ ExplorerEndpoint
)(implicit ec: ExecutionContext, mat: akka.stream.Materializer) {

  def popular: Fu[PopularOpenings] = popularCache.get(())

  def find(key: String): Fu[Option[OpeningData.WithAll]] = LilaOpening.find(key) ?? apply

  def apply(op: LilaOpening): Fu[Option[OpeningData.WithAll]] =
    popular map {
      _.byKey get op.key
    } orElse {
      coll.byId[OpeningData](op.key.value)
    } flatMap {
      _ ?? { opening =>
        allGamesHistory.get(()) map { OpeningData.WithAll(opening, _).some }
      }
    }

  private val popularCache = cacheApi.unit[PopularOpenings] {
    _.refreshAfterWrite(1 hour)
      .buildAsyncFuture { _ =>
        import OpeningData.openingDataHandler
        coll.find($empty).sort($sort desc "nbGames").cursor[OpeningData]().list(500) map PopularOpenings
      }
  }

  private[opening] def updateOpenings: Funit =
    addMissingOpenings >> updateHistories

  private def addMissingOpenings: Funit =
    coll.distinctEasy[String, Set]("_id", $empty) flatMap { existingIds =>
      val missingKeys = LilaOpening.openings.keySet diff existingIds.map(LilaOpening.Key)
      lila.common.Future.applySequentially(missingKeys.toList) { key =>
        LilaOpening(key) ?? { op =>
          coll.insert
            .one(OpeningData(key, op, Nil, DateTime.now minusYears 1, nbGames = 0, Markdown("")))
            .void
        }
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

  private val allGamesHistory = cacheApi.unit[List[OpeningHistorySegment[Int]]] {
    _.refreshAfterWrite(1 hour)
      .buildAsyncFuture { _ =>
        fetchHistory(none)
      }
  }

  private def fetchHistory(op: Option[LilaOpening]): Fu[List[OpeningHistorySegment[Int]]] =
    ws.url(s"$explorerEndpoint/lichess/history")
      .withQueryStringParameters(
        (List(
          "since" -> OpeningData.firstMonth,
          "until" -> OpeningData.lastMonth
        ) ::: op.map(o => "fen" -> o.ref.fen).orElse(Some("play" -> "")).toList,
        ): _*
      )
      .get()
      .flatMap {
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: ${op.fold("initial")(_.key.value)}")
        case res =>
          import OpeningHistory.segmentJsonRead
          (res.body[JsValue] \ "history")
            .validate[List[OpeningHistorySegment[Int]]]
            .fold(invalid => fufail(invalid.toString), fuccess)
      }
}
