package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.common.{ Heapsort, LilaOpeningFamily }
import lila.insight.{ Filter, InsightApi, InsightDimension, Metric, Phase, Question }
import lila.rating.PerfType
import lila.tutor.TutorCompare.comparisonOrdering

case class TutorColorOpenings(
    families: List[TutorOpeningFamily]
) {
  lazy val acplCompare = TutorCompare[LilaOpeningFamily, Acpl](
    InsightDimension.OpeningFamily,
    Metric.MeanCpl,
    families.map { f => (f.family, f.acpl) }
  )
  lazy val performanceCompare = TutorCompare[LilaOpeningFamily, Rating](
    InsightDimension.OpeningFamily,
    Metric.Performance,
    families.map { f => (f.family, f.performance.toOption) }
  )

  def dimensionHighlights(nb: Int) =
    Heapsort.topNToList(
      acplCompare.dimComparisons ::: performanceCompare.dimComparisons,
      nb,
      comparisonOrdering
    )
  def peerHighlights(nb: Int) =
    Heapsort.topNToList(
      acplCompare.dimComparisons ::: performanceCompare.dimComparisons,
      nb,
      comparisonOrdering
    )
}

case class TutorOpeningFamily(
    family: LilaOpeningFamily,
    performance: TutorMetric[Rating],
    acpl: TutorMetricOption[Acpl],
    awareness: TutorMetricOption[TutorRatio]
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
      acplQuestion = myPerfs.alignedQuestion
        .withMetric(Metric.MeanCpl)
        .filter(Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle)))
      acpls <- answerBoth(acplQuestion, user)
      awarenessQuestion = acplQuestion.withMetric(Metric.Awareness)
      awareness <- answerBoth(awarenessQuestion, user)
    } yield TutorColorOpenings {
      performances.mine.list.map { case (family, myPerformance) =>
        TutorOpeningFamily(
          family,
          performance = performances.valueMetric(family, myPerformance) map Rating.apply,
          acpl = acpls valueMetric family map Acpl.apply,
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
