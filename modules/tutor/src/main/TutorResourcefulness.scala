package lila.tutor

import lila.insight.*
import lila.rating.PerfType
import lila.common.config
import lila.db.dsl.*
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.insight.InsightEntry.{ BSONFields as F }
import lila.insight.BSONHandlers.given
import lila.analyse.WinPercent
import reactivemongo.api.bson.BSONNull

object TutorResourcefulness:

  val maxGames = config.Max(10_000)

  private[tutor] def compute(
      user: TutorUser
  )(using insightApi: InsightApi, ec: Executor): Fu[TutorBothValueOptions[Double]] =
    val question = Question(
      InsightDimension.Perf,
      InsightMetric.MeanAccuracy,
      List(TutorBuilder.perfFilter(user))
    )
    val select = $doc(
      F.analysed -> true,
      F.moves    -> $doc("$elemMatch" -> $doc("w" $lt WinPercent(33.3), "i" $lt -1))
    )
    val compute = TutorCustomInsight(user, question, "resourcefulness", _.resourcefulness): doc =>
      for
        perf <- doc.getAsOpt[PerfType]("_id")
        loss <- doc.getAsOpt[Int]("loss")
        size <- doc.int("nb")
        percent = (size - loss) * 100d / size
      yield ValueCount(percent, size)

    insightApi.coll: coll =>
      import coll.AggregationFramework.*
      val group = Group(BSONNull)(
        "loss" -> Sum(
          $doc("$cond" -> $arr($doc("$eq" -> $arr(s"$$${F.result}", Result.Loss.id)), 1, 0))
        ),
        "nb" -> SumAll
      )
      compute(coll)(
        aggregateMine = mineSelect =>
          Match(select ++ mineSelect ++ $doc(F.perf -> user.perfType)) -> List(
            Sort(Descending(F.date)),
            Limit(maxGames.value),
            group
          ),
        aggregatePeer = peerSelect =>
          Match(select ++ peerSelect) -> List(
            Limit(maxGames.value / 2),
            group
          )
      )
