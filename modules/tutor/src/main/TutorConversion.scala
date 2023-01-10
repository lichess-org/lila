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

object TutorConversion:

  val maxGames = config.Max(10_000)

  private[tutor] def compute(
      users: NonEmptyList[TutorUser]
  )(using InsightApi, ExecutionContext): Fu[TutorBuilder.Answers[PerfType]] =
    val perfs = users.toList.map(_.perfType)
    TutorCustomInsight.compute(
      monitoringKey = "conversion",
      users = users,
      question = Question(
        InsightDimension.Perf,
        InsightMetric.Result,
        List(Filter(InsightDimension.Perf, perfs))
      ),
      aggregate = (insightApi, select, sort) =>
        insightApi.coll {
          _.aggregateList(maxDocs = Int.MaxValue) { framework =>
            import framework.*
            Match(
              $doc(
                F.analysed -> true,
                F.perf $in perfs,
                F.moves -> $doc("$elemMatch" -> $doc("w" $gt 66.6, "i" $gt 1))
              ) ++ select
            ) -> List(
              sort option Sort(Descending(F.date)),
              Limit(maxGames.value).some,
              GroupField(F.perf)(
                "win" -> Sum(
                  $doc("$cond" -> $arr($doc("$eq" -> $arr(s"$$${F.result}", Result.Win.id)), 1, 0))
                ),
                "nb" -> SumAll
              ).some
            ).flatten
          }
        },
      clusterParser = docs =>
        for
          doc  <- docs
          perf <- doc.getAsOpt[PerfType]("_id")
          wins <- doc.getAsOpt[Int]("win")
          size <- doc.int("nb")
          percent = wins * 100d / size
        yield Cluster(perf, Insight.Single(Point(percent)), size, Nil)
    )
