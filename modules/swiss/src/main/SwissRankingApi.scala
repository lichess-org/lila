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
    if (swiss.isFinished) finishedRanking get swiss.id
    else ongoingRanking get swiss.id

  // only applies to ongoing tournaments
  private val ongoingRanking = cacheApi[Swiss.Id, Ranking](32, "swiss.ongoingRanking") {
    _.expireAfterWrite(3 seconds)
      .buildAsyncFuture(computeRanking)
  }

  // only applies to finished tournaments
  private val finishedRanking = cacheApi[Swiss.Id, Ranking](512, "swiss.finishedRanking") {
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
