package lila.opening

import com.softwaremill.tagging._
import play.api.libs.json.JsValue
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.LilaOpeningFamily
import lila.memo.CacheApi

final class OpeningApi(
    ws: StandaloneWSClient,
    cacheApi: CacheApi,
    explorerEndpoint: String @@ ExplorerEndpoint
)(implicit ec: ExecutionContext) {

  private val familyCache = cacheApi[LilaOpeningFamily.Key, OpeningFamilyData](64, "opening.family.data") {
    _.expireAfterWrite(1 hour).buildAsyncFuture(key =>
      computeFamilyData(LilaOpeningFamily(key) err s"Can't find opening family $key?!")
    )
  }

  private def computeFamilyData(fam: LilaOpeningFamily): Fu[OpeningFamilyData] =
    fetchHistory(fam).dmap(some).recover { case e: Exception =>
      logger.error(s"Couldn't get opening family data for ${fam.key}", e)
      none
    } map {
      OpeningFamilyData(fam, _)
    }

  private def fetchHistory(fam: LilaOpeningFamily) =
    ws.url(s"$explorerEndpoint/lichess/history")
      .withQueryStringParameters(
        "variant" -> chess.variant.Standard.key,
        "fen"     -> fam.full.fen
      )
      .get()
      .flatMap {
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer ${fam.key}")
        case res =>
          import OpeningHistory.historyJsonRead
          res
            .body[JsValue]
            .validate[OpeningHistory]
            .fold(invalid => fufail(invalid.toString), fuccess)
      }

  def apply(op: LilaOpeningFamily): Fu[OpeningFamilyData] = familyCache.get(op.key)
}
