package lila.opening

import com.softwaremill.tagging._
import play.api.libs.json.JsValue
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.LilaOpeningFamily
import lila.memo.CacheApi
import play.api.Mode

final class OpeningApi(
    ws: StandaloneWSClient,
    cacheApi: CacheApi,
    mongoCache: lila.memo.MongoCache.Api,
    explorerEndpoint: String @@ ExplorerEndpoint
)(implicit ec: ExecutionContext, mode: Mode) {

  def families: Fu[FamilyDataCollection] = collectionCache.get(())

  def apply(op: LilaOpeningFamily): Fu[Option[OpeningFamilyData.WithAll]] = for {
    coll <- families
    all  <- allGamesHistory.get(())
  } yield coll.byKey get op.key map {
    OpeningFamilyData.WithAll(_, all)
  }

  private val allGamesHistory = cacheApi.unit[List[OpeningHistorySegment[Long]]] {
    _.refreshAfterWrite(1 hour)
      .buildAsyncFuture { _ =>
        fetchHistory(none)
      }
  }

  import FamilyDataCollection.collectionHandler
  val collectionCache = mongoCache.unit[FamilyDataCollection](
    "opening:families",
    25 hours
  ) { loader =>
    _.refreshAfterWrite(24 hours)
      .buildAsyncFuture {
        loader { _ =>
          val fams =
            if (mode == Mode.Prod) LilaOpeningFamily.familyList
            else LilaOpeningFamily.familyList take 50
          fams
            .map(computeFamilyData)
            .sequenceFu
            .map(_.sortBy(-_.nbGames)) map FamilyDataCollection.apply
        }
      }
  }

  private def computeFamilyData(fam: LilaOpeningFamily): Fu[OpeningFamilyData] = for {
    history <- fetchHistory(fam.some).recover { case e: Exception =>
      logger.error(s"Couldn't get opening family data for ${fam.key}", e)
      Nil
    }
    allGames <- allGamesHistory.get(())
  } yield OpeningFamilyData(fam, history)

  private def fetchHistory(fam: Option[LilaOpeningFamily]): Fu[List[OpeningHistorySegment[Long]]] =
    ws.url(s"$explorerEndpoint/lichess/history")
      .withQueryStringParameters(
        (List(
          "since"   -> "2015-01",
          "variant" -> chess.variant.Standard.key
        ) ::: fam.map(f => "fen" -> f.full.fen.pp("fetch")).orElse(Some("play" -> "")).toList,
        ): _*
      )
      .get()
      .flatMap {
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: ${fam.fold("initial")(_.key.value)}")
        case res =>
          import OpeningHistory.segmentJsonRead
          (res.body[JsValue] \ "history")
            .validate[List[OpeningHistorySegment[Long]]]
            .fold(invalid => fufail(invalid.toString), fuccess)
      }
}
