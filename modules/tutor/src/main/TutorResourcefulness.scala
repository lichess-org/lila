package lila.tutor

import chess.eval.WinPercent
import lila.db.dsl.{ *, given }
import lila.insight.*
import lila.insight.InsightEntry.BSONFields as F
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

object TutorResourcefulness:

  val maxGamesPerPerf = Max(10_000)

  private[tutor] def compute(
      users: NonEmptyList[TutorPlayer]
  )(using insightApi: InsightApi, ec: Executor): Fu[TutorBuilder.Answers[PerfType]] =
    val perfs = users.toList.map(_.perfType)
    val question = Question(
      InsightDimension.Perf,
      InsightMetric.MeanAccuracy,
      List(lila.insight.Filter(InsightDimension.Perf, perfs))
    )
    val select = $doc(
      F.analysed -> true,
      F.moves -> $doc("$elemMatch" -> $doc("w".$lt(WinPercent(33.3)), "i".$lt(-1)))
    )
    val compute = TutorCustomInsight(users, question, "resourcefulness", _.resourcefulness): docs =>
      for
        doc <- docs
        perf <- doc.getAsOpt[PerfType]("_id")
        loss <- doc.getAsOpt[Int]("loss")
        size <- doc.int("nb")
        percent = (size - loss) * 100d / size
      yield Cluster(perf, Insight.Single(Point(percent)), size, Nil)

    insightApi.coll: coll =>
      import coll.AggregationFramework.*
      val groupByPerf = GroupField(F.perf)(
        "loss" -> Sum(
          $doc("$cond" -> $arr($doc("$eq" -> $arr(s"$$${F.result}", Result.Loss.id)), 1, 0))
        ),
        "nb" -> SumAll
      )
      compute(coll)(
        aggregateMine = mineSelect =>
          Match(select ++ mineSelect ++ $doc(F.perf.$in(perfs))) -> List(
            Sort(Descending(F.date)),
            Limit(maxGamesPerPerf.value),
            groupByPerf
          ),
        aggregatePeer = peerSelect =>
          Match(select ++ peerSelect) -> List(
            Limit(maxGamesPerPerf.value / 3),
            groupByPerf
          )
      )
