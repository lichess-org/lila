package lila.tournament

import akka.stream.scaladsl.*
import play.api.libs.json.*
import reactivemongo.api.ReadPreference

import lila.memo.CacheApi.*

/*
 * Getting a standing page of a tournament can be very expensive
 * because it can iterate through thousands of mongodb documents.
 * Try to cache the stuff, and limit concurrent access to prevent
 * overloading mongodb.
 */
final class TournamentStandingApi(
    playerRepo: PlayerRepo,
    cached: TournamentCache,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(using
    ec: Executor,
    mat: akka.stream.Materializer
):

  private val perPage = 10

  def fullStanding(tour: Tournament): Fu[JsArray] =
    playerRepo
      .sortedCursor(tour.id, 100, ReadPreference.primary)
      .documentSource()
      .zipWithIndex
      .mapAsync(16) { case (player, index) =>
        for {
          sheet <- cached.sheet(tour, player.userId)
          json <- JsonView.playerJson(
            lightUserApi,
            sheet.some,
            RankedPlayer(Rank(index.toInt + 1), player),
            streakable = tour.streakable,
            withScores = true
          )
        } yield json
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(JsArray(_))

  def apply(tour: Tournament, forPage: Int, withScores: Boolean): Fu[JsObject] =
    val page = forPage atMost Math.ceil(tour.nbPlayers.toDouble / perPage).toInt atLeast 1
    if (page == 1) first get tour.id
    else if (page > 50 && tour.isCreated) createdCache.get(tour.id -> page)
    else compute(tour, page, withScores)

  private val first = cacheApi[TourId, JsObject](64, "tournament.page.first") {
    _.expireAfterWrite(1 second)
      .buildAsyncFuture { compute(_, 1, withScores = true) }
  }

  // useful for highly anticipated, highly populated tournaments
  private val createdCache = cacheApi[(TourId, Int), JsObject](64, "tournament.page.createdCache") {
    _.expireAfterWrite(15 second)
      .buildAsyncFuture { case (tourId, page) =>
        compute(tourId, page, withScores = true)
      }
  }

  def clearCache(tour: Tournament): Unit =
    first invalidate tour.id
    // no need to invalidate createdCache, these are only cached when tour.isCreated

  private def compute(id: TourId, page: Int, withScores: Boolean): Fu[JsObject] =
    cached.tourCache.byId(id) orFail s"No such tournament: $id" flatMap { compute(_, page, withScores) }

  private def playerIdsOnPage(tour: Tournament, page: Int): Fu[List[TourPlayerId]] =
    cached.ranking(tour).map { ranking =>
      ((page - 1) * perPage until page * perPage).toList.flatMap(ranking.playerIndex.lift)
    }

  private def compute(tour: Tournament, page: Int, withScores: Boolean): Fu[JsObject] =
    for {
      rankedPlayers <-
        if (page < 10) playerRepo.bestByTourWithRankByPage(tour.id, perPage, page)
        else playerIdsOnPage(tour, page) flatMap { playerRepo.byPlayerIdsOnPage(_, page) }
      sheets <- rankedPlayers
        .map { p =>
          cached.sheet(tour, p.player.userId) dmap { p.player.userId -> _ }
        }
        .parallel
        .dmap(_.toMap)
      players <- rankedPlayers
        .map(JsonView.playerJson(lightUserApi, sheets, streakable = tour.streakable, withScores = withScores))
        .parallel
    } yield Json.obj(
      "page"    -> page,
      "players" -> players
    )
