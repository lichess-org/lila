package lila.tournament

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.WorkQueue
import lila.memo.CacheApi._
import lila.user.User

/*
 * Getting a standing page of a tournament can be very expensive
 * because it can iterate through thousands of mongodb documents.
 * Try to cache the stuff, and limit concurrent access to prevent
 * overloading mongodb.
 */
final class TournamentStandingApi(
    playerRepo: PlayerRepo,
    cached: Cached,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  private val workQueue = new WorkQueue(
    buffer = 256,
    timeout = 5 seconds,
    name = "tournamentStandingApi",
    parallelism = 6
  )

  private val perPage = 10

  def apply(tour: Tournament, forPage: Int, withScores: Boolean): Fu[JsObject] = {
    val page = forPage atLeast 1
    if (page == 1) first get tour.id
    else if (page > 50) {
      if (tour.isCreated) createdCache.get(tour.id -> page)
      else computeMaybe(tour.id, page, withScores)
    } else compute(tour, page, withScores)
  }

  private val first = cacheApi[Tournament.ID, JsObject](64, "tournament.page.first") {
    _.expireAfterWrite(1 second)
      .buildAsyncFuture { compute(_, 1, withScores = true) }
  }

  // useful for highly anticipated, highly populated tournaments
  private val createdCache = cacheApi[(Tournament.ID, Int), JsObject](64, "tournament.page.createdCache") {
    _.expireAfterWrite(15 second)
      .buildAsyncFuture { case (tourId, page) =>
        computeMaybe(tourId, page, withScores = true)
      }
  }

  def clearCache(tour: Tournament): Unit = {
    first invalidate tour.id
    // no need to invalidate createdCache, these are only cached when tour.isCreated
  }

  private def computeMaybe(id: Tournament.ID, page: Int, withScores: Boolean): Fu[JsObject] =
    workQueue {
      compute(id, page, withScores)
    } recover { case _: Exception =>
      lila.mon.tournament.standingOverload.increment()
      Json.obj(
        "failed"  -> true,
        "page"    -> page,
        "players" -> JsArray()
      )
    }

  private def compute(id: Tournament.ID, page: Int, withScores: Boolean): Fu[JsObject] =
    cached.tourCache.byId(id) orFail s"No such tournament: $id" flatMap { compute(_, page, withScores) }

  private def playerIdsOnPage(tour: Tournament, page: Int): Fu[List[User.ID]] =
    cached.ranking(tour).map { ranking =>
      ((page - 1) * perPage until page * perPage).toList.flatMap(ranking.playerIndex.lift)
    }

  private def compute(tour: Tournament, page: Int, withScores: Boolean): Fu[JsObject] =
    for {
      rankedPlayers <- {
        if (page < 10) playerRepo.bestByTourWithRankByPage(tour.id, perPage, page)
        else playerIdsOnPage(tour, page) flatMap { playerRepo.byPlayerIdsOnPage(tour.id, _, page) }
      }
      sheets <- rankedPlayers
        .map { p =>
          cached.sheet(tour, p.player.userId) dmap { p.player.userId -> _ }
        }
        .sequenceFu
        .dmap(_.toMap)
      players <- rankedPlayers
        .map(JsonView.playerJson(lightUserApi, sheets, streakable = tour.streakable, withScores = withScores))
        .sequenceFu
    } yield Json.obj(
      "page"    -> page,
      "players" -> players
    )
}
