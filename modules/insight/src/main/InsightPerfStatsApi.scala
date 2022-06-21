package lila.insight

import chess.Centis
import reactivemongo.api.bson._
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

import lila.db.dsl._
import lila.game.Game
import lila.rating.PerfType
import lila.user.User
import lila.common.config

case class InsightPerfStats(
    rating: MeanRating,
    nbGames: Int,
    time: FiniteDuration
)

object InsightPerfStats {
  case class WithGameIds(stats: InsightPerfStats, gameIds: List[Game.ID])
}

final class InsightPerfStatsApi(
    storage: InsightStorage,
    pipeline: AggregationPipeline
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(
      user: User,
      perfTypes: List[PerfType],
      gameIdsPerPerf: config.Max
  ): Fu[Map[PerfType, InsightPerfStats.WithGameIds]] =
    storage.coll {
      _.aggregateList(perfTypes.size) { framework =>
        import framework._
        import InsightEntry.{ BSONFields => F }
        val filters = List(lila.insight.Filter(InsightDimension.Perf, perfTypes))
        Match(InsightStorage.selectUserId(user.id) ++ pipeline.gameMatcher(filters)) -> List(
          Sort(Descending(F.date)),
          Limit(pipeline.maxGames.value),
          Project($doc(F.perf -> true, F.rating -> true, "t" -> $doc("$sum" -> s"$$${F.moves("t")}"))),
          GroupField(F.perf)(
            "r"   -> AvgField(F.rating),
            "n"   -> SumAll,
            "t"   -> SumField("t"),
            "ids" -> PushField("_id")
          ),
          Match($doc("n" $gte 5)),
          AddFields($doc("ids" -> $doc("$slice" -> $arr("$ids", gameIdsPerPerf.value))))
        )
      }.map { docs =>
        for {
          doc <- docs
          id  <- doc int "_id"
          pt  <- PerfType(id)
          ra  <- doc double "r"
          nb  <- doc int "n"
          t   <- doc.getAsOpt[Centis]("t")
          ids <- doc.getAsOpt[List[String]]("ids")
          gameIds = ids map Game.takeGameId
        } yield pt -> InsightPerfStats.WithGameIds(
          InsightPerfStats(MeanRating(ra.toInt), nb, t.toDuration),
          gameIds
        )
      }.map(_.toMap)
    }
}
