package lila.swiss

import play.api.libs.json.*

import lila.db.dsl.{ *, given }

/*
 * Getting a standing page of a tournament can be very expensive
 * because it can iterate through thousands of mongodb documents.
 * Try to cache the stuff, and limit concurrent access to prevent
 * overloading mongodb.
 */
final class SwissStandingApi(
    mongo: SwissMongo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.core.user.LightUserApi
)(using Executor):

  import BsonHandlers.given

  private val perPage = 10

  private val pageCache = lila.memo.CacheApi.scaffeine
    .expireAfterWrite(60.minutes)
    .build[(SwissId, Int), JsObject]()

  def apply(swiss: Swiss, forPage: Int): Fu[JsObject] =
    val page = forPage.atMost(Math.ceil(swiss.nbPlayers.toDouble / perPage).toInt).atLeast(1)
    fuccess(pageCache.getIfPresent(swiss.id -> page)).getOrElse:
      if page == 1 then first.get(swiss.id)
      else compute(swiss, page)

  def update(res: SwissScoring.Result): Funit =
    lightUserApi
      .asyncManyFallback(res.leaderboard.map(_._1.userId))
      .map:
        _.zip(res.leaderboard).zipWithIndex
          .grouped(perPage)
          .toList
          .foldLeft(0): (i, pagePlayers) =>
            val page = i + 1
            pageCache.put(
              res.swiss.id -> page,
              Json.obj(
                "page" -> page,
                "players" -> pagePlayers
                  .map { case ((user, (player, sheet)), r) =>
                    SwissJson.playerJson(
                      res.swiss,
                      SwissPlayer.View(
                        player = player,
                        rank = r + 1,
                        user = user,
                        ~res.pairings.get(player.userId),
                        sheet
                      )
                    )
                  }
              )
            )
            page
      .map: lastPage =>
        // make sure there's no extra page in the cache in case of players leaving the tournament
        pageCache.invalidate(res.swiss.id -> (lastPage + 1))

  private val first = cacheApi[SwissId, JsObject](256, "swiss.page.first"):
    _.expireAfterWrite(1.minute)
      .buildAsyncFuture { compute(_, 1) }

  private def compute(id: SwissId, page: Int): Fu[JsObject] =
    mongo.swiss.byId[Swiss](id).orFail(s"No such tournament: $id").flatMap { compute(_, page) }

  private def compute(swiss: Swiss, page: Int): Fu[JsObject] =
    for
      rankedPlayers <- bestWithRankByPage(swiss.id, perPage, page.atLeast(1))
      pairings <- (!swiss.isCreated).so(SwissPairing.fields { f =>
        mongo.pairing
          .find($doc(f.swissId -> swiss.id, f.players.$in(rankedPlayers.map(_.player.userId))))
          .sort($sort.asc(f.round))
          .cursor[SwissPairing]()
          .listAll()
          .map(SwissPairing.toMap)
      })
      sheets = SwissSheet.many(swiss, rankedPlayers.map(_.player), pairings)
      users <- lightUserApi.asyncManyFallback(rankedPlayers.map(_.player.userId))
    yield Json.obj(
      "page" -> page,
      "players" -> rankedPlayers
        .zip(users)
        .zip(sheets)
        .map { case ((SwissPlayer.WithRank(player, rank), user), sheet) =>
          SwissJson.playerJson(
            swiss,
            SwissPlayer.View(player, rank, user, ~pairings.get(player.userId), sheet)
          )
        }
    )

  private def bestWithRank(id: SwissId, nb: Int, skip: Int): Fu[List[SwissPlayer.WithRank]] =
    best(id, nb, skip).map { res =>
      res
        .foldRight(List.empty[SwissPlayer.WithRank] -> (res.size + skip)) { case (p, (res, rank)) =>
          (SwissPlayer.WithRank(p, rank) :: res, rank - 1)
        }
        ._1
    }

  private def bestWithRankByPage(id: SwissId, nb: Int, page: Int): Fu[List[SwissPlayer.WithRank]] =
    bestWithRank(id, nb, (page - 1) * nb)

  private def best(id: SwissId, nb: Int, skip: Int): Fu[List[SwissPlayer]] =
    SwissPlayer.fields { f =>
      mongo.player
        .find($doc(f.swissId -> id))
        .sort($sort.desc(f.score))
        .skip(skip)
        .cursor[SwissPlayer]()
        .list(nb)
    }
