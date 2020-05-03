package lila.swiss

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.{ LightUser, WorkQueue }
import lila.memo.CacheApi._
import lila.db.dsl._

/*
 * Getting a standing page of a tournament can be very expensive
 * because it can iterate through thousands of mongodb documents.
 * Try to cache the stuff, and limit concurrent access to prevent
 * overloading mongodb.
 */
final class SwissStandingApi(
    colls: SwissColls,
    cached: SwissCache,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer) {

  import BsonHandlers._

  private val workQueue = new WorkQueue(
    buffer = 256,
    timeout = 5 seconds,
    name = "swissStandingApi",
    parallelism = 4
  )

  def apply(swiss: Swiss, page: Int): Fu[JsObject] =
    if (page == 1) first get swiss.id
    else if (page > 50) {
      if (swiss.isCreated) createdCache.get(swiss.id -> page)
      else computeMaybe(swiss.id, page)
    } else compute(swiss, page)

  private val first = cacheApi[Swiss.Id, JsObject](16, "swiss.page.first") {
    _.expireAfterWrite(1 second)
      .buildAsyncFuture { compute(_, 1) }
  }

  private val createdCache = cacheApi[(Swiss.Id, Int), JsObject](2, "swiss.page.createdCache") {
    _.expireAfterWrite(15 second)
      .buildAsyncFuture {
        case (swissId, page) => computeMaybe(swissId, page)
      }
  }

  def clearCache(swiss: Swiss): Unit = {
    first invalidate swiss.id
    // no need to invalidate createdCache, these are only cached when tour.isCreated
  }

  private def computeMaybe(id: Swiss.Id, page: Int): Fu[JsObject] =
    workQueue {
      compute(id, page)
    } recover {
      case _: Exception =>
        lila.mon.swiss.standingOverload.increment()
        Json.obj(
          "failed"  -> true,
          "page"    -> page,
          "players" -> JsArray()
        )
    }

  private def compute(id: Swiss.Id, page: Int): Fu[JsObject] =
    colls.swiss.byId[Swiss](id.value) orFail s"No such tournament: $id" flatMap { compute(_, page) }

  private def compute(swiss: Swiss, page: Int): Fu[JsObject] =
    for {
      rankedPlayers <- bestWithRankByPage(swiss.id, 10, page atLeast 1)
      pairings <- colls.pairing.ext
        .find($doc("s" -> swiss.id, "u" $in rankedPlayers.map(_.player.id)))
        .sort($sort asc "r")
        .list[SwissPairing]()
        .map(SwissPairing.toMap)
      users <- lightUserApi asyncMany rankedPlayers.map(_.player.userId)
    } yield Json.obj(
      "page" -> page,
      "players" -> rankedPlayers.zip(users).map {
        case (p, u) =>
          SwissJson.playerJson(
            swiss,
            p,
            u | LightUser.fallback(p.player.userId),
            ~pairings.get(p.player.number)
          )
      }
    )

  private[swiss] def bestWithRank(id: Swiss.Id, nb: Int, skip: Int = 0): Fu[List[SwissPlayer.Ranked]] =
    best(id, nb, skip).map { res =>
      res
        .foldRight(List.empty[SwissPlayer.Ranked] -> (res.size + skip)) {
          case (p, (res, rank)) => (SwissPlayer.Ranked(rank, p) :: res, rank - 1)
        }
        ._1
    }

  private[swiss] def bestWithRankByPage(id: Swiss.Id, nb: Int, page: Int): Fu[List[SwissPlayer.Ranked]] =
    bestWithRank(id, nb, (page - 1) * nb)

  private[swiss] def best(id: Swiss.Id, nb: Int, skip: Int = 0): Fu[List[SwissPlayer]] =
    colls.player.ext.find($doc("s" -> id)).sort($sort desc "s").skip(skip).list[SwissPlayer](nb)
}
