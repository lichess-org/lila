package lila.swiss

import scala.concurrent.duration._
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.memo.CacheApi

final private class SwissRankingApi(
    colls: SwissColls,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {
  import BsonHandlers._

  def apply(swiss: Swiss): Fu[Ranking] =
    fuccess(scoreCache.getIfPresent(swiss.id)) getOrElse {
      dbCache get swiss.id
    }

  def update(res: SwissScoring.Result) =
    scoreCache.put(
      res.swiss.id,
      res.players
        .sortBy(-_.score.value)
        .zipWithIndex
        .map {
          case (p, i) => p.number -> (i + 1)
        }
        .toMap
    )

  private val scoreCache = cacheApi.scaffeine
    .expireAfterWrite(60 minutes)
    .build[Swiss.Id, Ranking]

  private val dbCache = cacheApi[Swiss.Id, Ranking](512, "swiss.ranking") {
    _.expireAfterAccess(1 hour)
      .maximumSize(1024)
      .buildAsyncFuture(computeRanking)
  }

  private def computeRanking(id: Swiss.Id): Fu[Ranking] =
    SwissPlayer.fields { f =>
      colls.player
        .aggregateWith[Bdoc]() { framework =>
          import framework._
          Match($doc(f.swissId -> id)) -> List(
            Sort(Descending(f.score)),
            Group(BSONNull)("players" -> PushField(f.number))
          )
        }
        .headOption map {
        _ ?? {
          _ get "players" match {
            case Some(BSONArray(players)) =>
              // mutable optimized implementation
              val b = Map.newBuilder[SwissPlayer.Number, Int]
              var r = 0
              for (u <- players) {
                b += (SwissPlayer.Number(u.asInstanceOf[BSONInteger].value) -> r)
                r = r + 1
              }
              b.result
            case _ => Map.empty
          }
        }
      }
    }
}
