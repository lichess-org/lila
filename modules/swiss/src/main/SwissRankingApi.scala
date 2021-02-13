package lila.swiss

import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

final private class SwissRankingApi(
    colls: SwissColls,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {
  import BsonHandlers._

  def apply(swiss: Swiss): Fu[Ranking] =
    fuccess(scoreCache.getIfPresent(swiss.id)) getOrElse {
      dbCache get swiss.id
    }

  def update(res: SwissScoring.Result): Unit =
    scoreCache.put(
      res.swiss.id,
      res.leaderboard.zipWithIndex.map {
        case ((p, _), i) => p.userId -> (i + 1)
      }.toMap
    )

  private val scoreCache = cacheApi.scaffeine
    .expireAfterWrite(60 minutes)
    .build[Swiss.Id, Ranking]()

  private val dbCache = cacheApi[Swiss.Id, Ranking](512, "swiss.ranking") {
    _.expireAfterAccess(1 hour)
      .maximumSize(1024)
      .buildAsyncFuture(computeRanking)
  }

  private def computeRanking(id: Swiss.Id): Fu[Ranking] =
    SwissPlayer.fields { f =>
      colls.player.primitive[User.ID]($doc(f.swissId -> id), $sort desc f.score, f.userId)
    } map {
      _.view.zipWithIndex.map {
        case (user, i) => (user, i + 1)
      }.toMap
    }
}
