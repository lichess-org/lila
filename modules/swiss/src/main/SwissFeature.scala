package lila.swiss

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi
import lila.memo.CacheApi._

final class SwissFeature(
    colls: SwissColls,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  private val cache = cacheApi.unit[FeaturedSwisses] {
    _.refreshAfterWrite(10 seconds)
      .buildAsyncFuture { _ =>
        cacheCompute($doc("$lt" -> DateTime.now)) zip
          cacheCompute($doc("$gt" -> DateTime.now, "$lt" -> DateTime.now.plusHours(1))) map {
          case (a, b) => FeaturedSwisses(a, b)
        }
      }
  }

  private def cacheCompute(startsAtRange: Bdoc): Fu[List[Swiss]] =
    colls.swiss
      .find(
        $doc(
          "featurable" -> true,
          "settings.i" $lte 600, // hits the partial index
          "startsAt" -> startsAtRange
        )
      )
      .sort($sort desc "nbPlayers")
      .cursor[Swiss]()
      .list(5)

  def get = cache.getUnit
}
