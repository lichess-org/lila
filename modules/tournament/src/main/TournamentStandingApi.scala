package lila.tournament

import play.api.libs.json._
import scala.concurrent.duration._

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
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(tour: Tournament, page: Int): Fu[JsObject] =
    if (page == 1) first get tour.id
    else if (page > 50 && tour.isCreated) deep.get(tour.id -> page)
    else compute(tour, page)

  private val first = asyncCache.clearable[Tournament.ID, JsObject](
    name = "tournament.page.first",
    id => compute(id, 1),
    expireAfter = _.ExpireAfterWrite(1 second)
  )
  private val deep = asyncCache.clearable[(Tournament.ID, Int), JsObject](
    name = "tournament.page.deep",
    t => compute(t._1, t._2),
    expireAfter = _.ExpireAfterWrite(15 second)
  )

  def clearCache(tour: Tournament): Unit = {
    first invalidate tour.id
    // no need to invalidate, these are only cached when tour.isCreated
    // if (tour.nbPlayers > 500) (51 to math.ceil(tour.nbPlayers / 10d).toInt) foreach { page =>
    //   deep.invalidate(tour.id -> page)
    // }
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
