package lila.tournament

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.WorkQueue

/*
 * Getting a standing page of a tournament can be very expensive
 * because it can iterate through thousands of mongodb documents.
 * Try to cache the stuff, and limit concurrent access to prevent
 * overloading mongodb.
 */
final class TournamentStandingApi(
    lightUserApi: lila.user.LightUserApi,
    tournamentRepo: TournamentRepo,
    playerRepo: PlayerRepo,
    cached: Cached,
    asyncCache: lila.memo.AsyncCache.Builder
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer) {

  private val workQueue = new WorkQueue(
    buffer = 256,
    name = "tournamentStandingApi",
    parallelism = 16
  )

  def apply(tour: Tournament, page: Int): Fu[JsObject] =
    if (page == 1) first get tour.id
    else if (page > 50) {
      if (tour.isCreated) createdCache.get(tour.id -> page)
      else computeMaybe(tour.id, page)
    } else compute(tour, page)

  private val first = asyncCache.clearable[Tournament.ID, JsObject](
    name = "tournament.page.first",
    id => compute(id, 1),
    expireAfter = _.ExpireAfterWrite(1 second)
  )
  private val createdCache = asyncCache.clearable[(Tournament.ID, Int), JsObject](
    name = "tournament.page.createdCache",
    { case (tourId, page) => computeMaybe(tourId, page) },
    expireAfter = _.ExpireAfterWrite(15 second)
  )

  def clearCache(tour: Tournament): Unit = {
    first invalidate tour.id
    // no need to invalidate createdCache, these are only cached when tour.isCreated
  }

  private def computeMaybe(id: Tournament.ID, page: Int): Fu[JsObject] =
    workQueue {
      compute(id, page)
    } recover {
      case e: Exception =>
        logger.branch("standing").error(s"tour $id page $page", e)
        lila.mon.tournament.standingOverload.increment()
        Json.obj(
          "failed"  -> true,
          "page"    -> page,
          "players" -> JsArray()
        )
    }

  private def compute(id: Tournament.ID, page: Int): Fu[JsObject] =
    tournamentRepo byId id orFail s"No such tournament: $id" flatMap { compute(_, page) }

  private def compute(tour: Tournament, page: Int): Fu[JsObject] =
    for {
      rankedPlayers <- playerRepo.bestByTourWithRankByPage(tour.id, 10, page max 1)
      sheets <- rankedPlayers
        .map { p =>
          cached.sheet(tour, p.player.userId) map { sheet =>
            p.player.userId -> sheet
          }
        }
        .sequenceFu
        .map(_.toMap)
      players <- rankedPlayers.map(JsonView.playerJson(lightUserApi, sheets)).sequenceFu
    } yield Json.obj(
      "page"    -> page,
      "players" -> players
    )
}
