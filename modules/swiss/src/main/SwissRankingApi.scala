package lila.swiss

import reactivemongo.api.bson.*
import scala.concurrent.duration.*

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.user.User

final private class SwissRankingApi(
    mongo: SwissMongo,
    cacheApi: CacheApi
)(using ec: scala.concurrent.ExecutionContext):
  import BsonHandlers.given

  def apply(swiss: Swiss): Fu[Ranking] =
    fuccess(scoreCache.getIfPresent(swiss.id)) getOrElse {
      dbCache get swiss.id
    }

  def update(res: SwissScoring.Result): Unit =
    scoreCache.put(
      res.swiss.id,
      res.leaderboard.zipWithIndex.map { case ((p, _), i) =>
        p.userId -> Rank(i + 1)
      }.toMap
    )

  private val scoreCache = cacheApi.scaffeine
    .expireAfterWrite(60 minutes)
    .build[SwissId, Ranking]()

  private val dbCache = cacheApi[SwissId, Ranking](512, "swiss.ranking") {
    _.expireAfterAccess(1 hour)
      .maximumSize(1024)
      .buildAsyncFuture(computeRanking)
  }

  private def computeRanking(id: SwissId): Fu[Ranking] =
    SwissPlayer.fields { f =>
      mongo.player.primitive[User.ID]($doc(f.swissId -> id), $sort desc f.score, f.userId)
    } map {
      _.view.zipWithIndex
        .map { (user, i) =>
          (user, Rank(i + 1))
        }
        .toMap
    }
