package lila.opening

import com.softwaremill.tagging._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ LilaOpening, LilaOpeningFamily }
import lila.db.dsl._
import lila.memo.CacheApi
import com.github.blemale.scaffeine.AsyncLoadingCache

final class OpeningApi(
    cacheApi: CacheApi,
    coll: Coll @@ OpeningColl,
    allGamesHistory: AsyncLoadingCache[Unit, List[OpeningHistorySegment[Int]]] @@ AllGamesHistory
)(implicit ec: ExecutionContext) {

  def getPopular: Fu[PopularOpenings] = popularCache.get(())

  def find(key: String): Fu[Option[OpeningData.WithAll]] = apply(LilaOpening.Key(key))

  def apply(key: LilaOpening.Key): Fu[Option[OpeningData.WithAll]] =
    getPopular map {
      _.byKey get key
    } orElse {
      coll.byId[OpeningData](key.value)
    } flatMap {
      _ ?? { opening =>
        allGamesHistory.get(()) map { OpeningData.WithAll(opening, _).some }
      }
    }

  def variationsOf(fam: LilaOpeningFamily): Fu[List[OpeningData.Preview]] =
    variationsCache.get(fam.key)

  private val variationsCache =
    cacheApi[LilaOpeningFamily.Key, List[OpeningData.Preview]](64, "opening.variations") {
      _.expireAfterWrite(1 hour)
        .buildAsyncFuture { key =>
          coll
            .find($doc("_id" $startsWith s"${key}_"), OpeningData.previewProjection.some)
            .sort($sort desc "nbGames")
            .cursor[OpeningData.Preview]()
            .list(64)
        }
    }

  private val popularCache = cacheApi.unit[PopularOpenings] {
    _.refreshAfterWrite(5 second)
      .buildAsyncFuture { _ =>
        import OpeningData.openingDataHandler
        coll.find($empty).sort($sort desc "nbGames").cursor[OpeningData]().list(500) map PopularOpenings
      }
  }
}
