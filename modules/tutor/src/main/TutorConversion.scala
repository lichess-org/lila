package lila.tutor

import chess.eval.WinPercent
import lila.db.dsl.{ *, given }
import lila.insight.*
import lila.insight.InsightEntry.BSONFields as F
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

object TutorConversion:

  val maxGames = Max(10_000)

  private[tutor] def compute(
      users: NonEmptyList[TutorUser]
  )(using insightApi: InsightApi, ec: Executor): Fu[TutorBuilder.Answers[PerfType]] =
    val perfs = users.toList.map(_.perfType)
    val question = Question(
      InsightDimension.Perf,
      InsightMetric.Result,
      List(Filter(InsightDimension.Perf, perfs))
    )
    val select =
      $doc(F.analysed -> true, F.moves -> $doc("$elemMatch" -> $doc("w".$gt(WinPercent(66.6)), "i".$gt(1))))
    val compute = TutorCustomInsight(users, question, "conversion", _.conversion) { docs =>
      for
        doc <- docs
        perf <- doc.getAsOpt[PerfType]("_id")
        wins <- doc.getAsOpt[Int]("win")
        size <- doc.int("nb")
        percent = wins * 100d / size
      yield Cluster(perf, Insight.Single(Point(percent)), size, Nil)
    }
    insightApi.coll: coll =>
      import coll.AggregationFramework.*
      val groupByPerf = GroupField(F.perf)(
        "win" -> Sum(
          $doc("$cond" -> $arr($doc("$eq" -> $arr(s"$$${F.result}", Result.Win.id)), 1, 0))
        ),
        "nb" -> SumAll
      )
      compute(coll)(
        aggregateMine = mineSelect =>
          Match(select ++ mineSelect ++ $doc(F.perf.$in(perfs))) -> List(
            Sort(Descending(F.date)),
            Limit(maxGames.value),
            groupByPerf
          ),
        aggregatePeer = peerSelect =>
          Match(select ++ peerSelect) -> List(
            Limit(maxGames.value / 5),
            groupByPerf
          )
      )
