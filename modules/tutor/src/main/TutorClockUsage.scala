package lila.tutor

import lila.db.dsl.*
import lila.insight.*
import lila.insight.BSONHandlers.given
import lila.insight.InsightEntry.BSONFields as F
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

private object TutorClockUsage:

  val maxGamesPerPerf = Max(5_000)

  def compute(
      users: NonEmptyList[TutorPlayer]
  )(using insightApi: InsightApi, ec: Executor): Fu[TutorBuilder.Answers[PerfType]] =
    val perfs = users.toList.map(_.perfType)
    val question = Question(
      InsightDimension.Perf,
      InsightMetric.ClockPercent,
      List(Filter(InsightDimension.Perf, perfs.filter(_ != PerfType.Correspondence)))
    )
    val select = $doc(F.result -> Result.Loss.id)
    val insightRunner = TutorCustomInsight(users, question, "clock_usage", _.clockUsage): docs =>
      for
        doc <- docs
        perf <- doc.getAsOpt[PerfType]("_id")
        clockPercent <- doc.getAsOpt[ClockPercent]("cp")
        size <- doc.int("nb")
      yield Cluster(perf, Insight.Single(Point(100 - clockPercent.value)), size, Nil)

    insightApi.coll: coll =>
      import coll.AggregationFramework.*
      val sharedPipeline = List(
        Project($doc(F.perf -> true, F.moves -> $doc("$last" -> s"$$${F.moves}"))),
        UnwindField(F.moves),
        Project($doc(F.perf -> true, "cp" -> s"$$${F.moves}.s")),
        GroupField(F.perf)("cp" -> AvgField("cp"), "nb" -> SumAll),
        Project($doc(F.perf -> true, "nb" -> true, "cp" -> $doc("$toInt" -> "$cp")))
      )
      insightRunner(coll)(
        aggregateMine = mineSelect =>
          Match(mineSelect ++ select ++ $doc(F.perf.$in(perfs))) -> List(
            Sort(Descending(F.date)),
            Limit(maxGamesPerPerf.value)
          ).appendedAll(sharedPipeline),
        aggregatePeer = peerSelect =>
          Match(peerSelect ++ select) -> List(Limit(maxGamesPerPerf.value)).appendedAll(sharedPipeline)
      )
