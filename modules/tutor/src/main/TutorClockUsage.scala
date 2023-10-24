package lila.tutor

import lila.insight.*
import lila.rating.PerfType
import lila.common.config
import lila.db.dsl.*
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.insight.InsightEntry.{ BSONFields as F }
import lila.insight.BSONHandlers.given
import reactivemongo.api.bson.BSONNull

object TutorClockUsage:

  val maxGames = config.Max(10_000)

  private[tutor] def compute(
      user: TutorUser
  )(using insightApi: InsightApi, ec: Executor): Fu[TutorBothValueOptions[ClockPercent]] =
    val question = Question(
      InsightDimension.Perf,
      InsightMetric.ClockPercent,
      List(TutorBuilder.perfFilter(user))
    )
    val select = $doc(F.result -> Result.Loss.id)
    val compute = TutorCustomInsight[ClockPercent](user, question, "clock_usage", _.clockUsage): doc =>
      for
        clockPercent <- doc.getAsOpt[ClockPercent]("cp")
        size         <- doc.int("nb")
      yield ValueCount(ClockPercent(100 - clockPercent.value), size)
    insightApi.coll: coll =>
      import coll.AggregationFramework.*
      val sharedPipeline = List(
        Project($doc(F.moves -> $doc("$last" -> s"$$${F.moves}"))),
        UnwindField(F.moves),
        Project($doc("cp" -> s"$$${F.moves}.s")),
        Group(BSONNull)("cp" -> AvgField("cp"), "nb" -> SumAll),
        Project($doc("nb" -> true, "cp" -> $doc("$toInt" -> "$cp")))
      )
      compute(coll)(
        aggregateMine = mineSelect =>
          Match(mineSelect ++ select ++ $doc(F.perf -> user.perfType)) -> List(
            Sort(Descending(F.date)),
            Limit(maxGames.value)
          ).appendedAll(sharedPipeline),
        aggregatePeer = peerSelect =>
          Match(peerSelect ++ select) -> List(Limit(maxGames.value / 2)).appendedAll(sharedPipeline)
      )
