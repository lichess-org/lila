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

  private val allGamesHistory = cacheApi.unit[OpeningHistory] {
    _.refreshAfterWrite(1 hour)
      .buildAsyncFuture { _ =>
        fetchHistory(none)
      }
  }

  private val familyCache = cacheApi[LilaOpeningFamily.Key, OpeningFamilyData](64, "opening.family.data") {
    _.expireAfterWrite(1 hour).buildAsyncFuture(key =>
      computeFamilyData(LilaOpeningFamily(key) err s"Can't find opening family $key?!")
    )
  }

  private def computeFamilyData(fam: LilaOpeningFamily): Fu[OpeningFamilyData] = for {
    history <- fetchHistory(fam.some).dmap(some).recover { case e: Exception =>
      logger.error(s"Couldn't get opening family data for ${fam.key}", e)
      none
    }
    allGames <- allGamesHistory.get(())
    relative = history.map(_ perMilOf allGames)
  } yield OpeningFamilyData(fam, relative)

  private def fetchHistory(fam: Option[LilaOpeningFamily]) =
    ws.url(s"$explorerEndpoint/lichess/history")
      .withQueryStringParameters(
        (List(
          "since"   -> "2015-01",
          "variant" -> chess.variant.Standard.key
          // "ratings" -> "2000,2200,2500"
          // "speeds"      -> (~openingSpeeds.get(game.speed)).map(_.key).mkString(",")
        ) ::: fam.map(f => "fen" -> f.full.fen).orElse(Some("play" -> "")).toList,
        ): _*
      )
      .get()
      .flatMap {
        case res if res.status != 200 =>
          fufail(s"Couldn't reach the opening explorer: ${fam.fold("initial")(_.key.value)}")
        case res =>
          import OpeningHistory.historyJsonRead
          res
            .body[JsValue]
            .validate[OpeningHistory]
            .fold(invalid => fufail(invalid.toString), fuccess)
            .map(_.filterNotEmpty)
      }

  def apply(op: LilaOpeningFamily): Fu[OpeningFamilyData] = familyCache.get(op.key)
}
