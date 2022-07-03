package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.analyse.AccuracyPercent
import lila.common.{ Heapsort, LilaOpeningFamily }
import lila.insight.{ Filter, InsightApi, InsightDimension, Metric, Phase, Question }
import lila.rating.PerfType
import lila.tutor.TutorCompare.comparisonOrdering

case class TutorColorOpenings(
    families: List[TutorOpeningFamily]
) {
  lazy val accuracyCompare = TutorCompare[LilaOpeningFamily, AccuracyPercent](
    InsightDimension.OpeningFamily,
    TutorMetric.Accuracy,
    families.map { f => (f.family, f.accuracy) }
  )
  lazy val performanceCompare = TutorCompare[LilaOpeningFamily, Rating](
    InsightDimension.OpeningFamily,
    TutorMetric.Performance,
    families.map { f => (f.family, f.performance.toOption) }
  )
  lazy val awarenessCompare = TutorCompare[LilaOpeningFamily, TutorRatio](
    InsightDimension.OpeningFamily,
    TutorMetric.Awareness,
    families.map { f => (f.family, f.awareness) }
  )

  lazy val allCompares = List(accuracyCompare, performanceCompare, awarenessCompare)
}

case class TutorOpeningFamily(
    family: LilaOpeningFamily,
    performance: TutorBothValues[Rating],
    accuracy: TutorBothValueOptions[AccuracyPercent],
    awareness: TutorBothValueOptions[TutorRatio]
)

private case object TutorOpening {

  import TutorBuilder._

  def compute(user: TutorUser)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ): Fu[Color.Map[TutorColorOpenings]] = for {
    whiteOpenings <- computeOpenings(user, Color.White)
    blackOpenings <- computeOpenings(user, Color.Black)
  } yield Color.Map(whiteOpenings, blackOpenings)

  def computeOpenings(user: TutorUser, color: Color)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ): Fu[TutorColorOpenings] = {
    for {
      myPerfs   <- answerMine(perfQuestion(color), user)
      peerPerfs <- answerPeer(myPerfs.alignedQuestion, user)
      performances = Answers(myPerfs, peerPerfs)
      accuracyQuestion = myPerfs.alignedQuestion
        .withMetric(Metric.MeanAccuracy)
        .filter(Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle)))
      accuracy <- answerBoth(accuracyQuestion, user)
      awarenessQuestion = accuracyQuestion withMetric Metric.Awareness
      awareness <- answerBoth(awarenessQuestion, user)
    } yield TutorColorOpenings {
      performances.mine.list.map { case (family, myPerformance) =>
        TutorOpeningFamily(
          family,
          performance = performances.valueMetric(family, myPerformance) map Rating.apply,
          accuracy = accuracy valueMetric family map AccuracyPercent.apply,
          awareness = awareness valueMetric family map TutorRatio.fromPercent
        )
      }
    }
  }

  def perfQuestion(color: Color) = Question(
    InsightDimension.OpeningFamily,
    Metric.Performance,
    List(colorFilter(color))
  )
}
