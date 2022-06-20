package lila.insight

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

import lila.db.dsl._
import lila.rating.PerfType
import lila.user.User
import chess.Centis

case class InsightPerfStats(rating: MeanRating, nbGames: Int, time: FiniteDuration)

final class InsightPerfStatsApi(
    storage: InsightStorage,
    pipeline: AggregationPipeline
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(user: User, perfTypes: List[PerfType]): Fu[Map[PerfType, InsightPerfStats]] =
    storage.coll {
      _.aggregateList(perfTypes.size) { framework =>
        import framework._
        import InsightEntry.{ BSONFields => F }
        val filters = List(lila.insight.Filter(InsightDimension.Perf, perfTypes))
        Match(InsightStorage.selectUserId(user.id) ++ pipeline.gameMatcher(filters)) -> List(
          Sort(Descending(F.date)),
          Limit(pipeline.maxGames.value),
          Project($doc(F.perf -> true, F.rating -> true, "t" -> $doc("$sum" -> s"$$${F.moves("t")}"))),
          GroupField(F.perf)("r" -> AvgField(F.rating), "n" -> SumAll, "t" -> SumField("t"))
        )
      }.map { docs =>
        for {
          doc <- docs
          id  <- doc int "_id"
          pt  <- PerfType(id)
          ra  <- doc double "r"
          nb  <- doc int "n"
          t   <- doc.getAsOpt[Centis]("t")
        } yield pt -> InsightPerfStats(MeanRating(ra.toInt), nb, t.toDuration)
      }.map(_.toMap)
    }
}
