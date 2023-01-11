package lila.tutor

import cats.data.NonEmptyList
import scala.concurrent.ExecutionContext

import lila.insight.*
import lila.rating.PerfType
import lila.common.config
import lila.db.dsl.{ *, given }
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.insight.InsightEntry.{ BSONFields as F }
import lila.insight.BSONHandlers.given

object TutorClockUsage:

  val maxGames = config.Max(10_000)

  private[tutor] def compute(
      users: NonEmptyList[TutorUser]
  )(using InsightApi, ExecutionContext): Fu[TutorBuilder.Answers[PerfType]] =
    val perfs = users.toList.map(_.perfType)
    TutorCustomInsight.compute(
      users = users,
      question = Question(
        InsightDimension.Perf,
        InsightMetric.ClockPercent,
        List(Filter(InsightDimension.Perf, perfs.filter(_ != PerfType.Correspondence)))
      ),
      aggregate = (insightApi, select, sort) =>
        insightApi.coll {
          _.aggregateList(maxDocs = Int.MaxValue) { framework =>
            import framework.*
            Match($doc(F.result -> Result.Loss.id, F.perf $in perfs) ++ select) -> List(
              sort option Sort(Descending(F.date)),
              Limit(maxGames.value).some,
              Project($doc(F.perf -> true, F.moves -> $doc("$last" -> s"$$${F.moves}"))).some,
              UnwindField(F.moves).some,
              Project($doc(F.perf -> true, "cp" -> s"$$${F.moves}.s")).some,
              GroupField(F.perf)("cp" -> AvgField("cp"), "nb" -> SumAll).some,
              Project($doc(F.perf -> true, "nb" -> true, "cp" -> $doc("$toInt" -> "$cp"))).some
            ).flatten
          }
        },
      clusterParser = docs =>
        for
          doc          <- docs
          perf         <- doc.getAsOpt[PerfType]("_id")
          clockPercent <- doc.getAsOpt[ClockPercent]("cp")
          size         <- doc.int("nb")
        yield Cluster(perf, Insight.Single(Point(100 - clockPercent.value)), size, Nil),
      monitoringKey = "clock_usage"
    )
