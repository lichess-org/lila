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

object TutorConversion:

  val maxGames = config.Max(10_000)

  private[tutor] def compute(
      user: TutorUser
  )(using insightApi: InsightApi, ec: Executor): Fu[TutorBothValueOptions[GoodPercent]] =
    val question = Question(
      InsightDimension.Perf,
      InsightMetric.Result,
      List(TutorBuilder.perfFilter(user))
    )
    val select =
      $doc(F.analysed -> true, F.moves -> $doc("$elemMatch" -> $doc("w" $gt WinPercent(66.6), "i" $gt 1)))
    val compute = TutorCustomInsight(user, question, "conversion", _.conversion): doc =>
      for
        perf <- doc.getAsOpt[PerfType]("_id")
        wins <- doc.getAsOpt[Int]("win")
        size <- doc.int("nb")
        percent = GoodPercent(wins * 100d / size)
      yield ValueCount(percent, size)
    insightApi.coll: coll =>
      import coll.AggregationFramework.*
      val group = Group(BSONNull)(
        "win" -> Sum($doc("$cond" -> $arr($doc("$eq" -> $arr(s"$$${F.result}", Result.Win.id)), 1, 0))),
        "nb"  -> SumAll
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
            Limit(maxGames.value / 5),
            group
          )
      )
