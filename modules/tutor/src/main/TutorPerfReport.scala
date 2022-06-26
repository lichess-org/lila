package lila.tutor

import chess.Color
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.analyse.AccuracyPercent
import lila.common.Heapsort.implicits._
import lila.common.LilaOpeningFamily
import lila.insight.{
  Filter,
  Insight,
  InsightApi,
  InsightDimension,
  InsightPerfStats,
  Metric,
  Phase,
  Question
}
import lila.rating.PerfType
import lila.tutor.TutorCompare.{ comparisonOrdering, AnyComparison }
import cats.data.NonEmptyList

case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    accuracy: TutorMetricOption[AccuracyPercent],
    awareness: TutorMetricOption[TutorRatio],
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase]
) {
  lazy val estimateTotalTime: Option[FiniteDuration] = (perf != PerfType.Correspondence) option stats.time * 2

  // Dimension comparison is not interesting for phase accuracy (opening always better)
  // But peer comparison is gold
  lazy val phaseAccuracyCompare = TutorCompare[Phase, AccuracyPercent](
    InsightDimension.Phase,
    Metric.MeanAccuracy,
    phases.map { phase => (phase.phase, phase.accuracy) }
  )

  lazy val phaseAwarenessCompare = TutorCompare[Phase, TutorRatio](
    InsightDimension.Phase,
    Metric.Awareness,
    phases.map { phase => (phase.phase, phase.awareness) }
  )

  def phaseCompares = List(phaseAccuracyCompare, phaseAwarenessCompare)

  def openingCompares: List[TutorCompare[LilaOpeningFamily, _]] = openings.all.toList.flatMap { op =>
    List(op.accuracyCompare, op.awarenessCompare, op.performanceCompare)
  }

  lazy val allCompares: List[TutorCompare[_, _]] =
    openingCompares ::: phaseCompares

  val openingHighlights = TutorCompare.mixedBag(openingCompares.flatMap(_.allComparisons)) _

  val phaseHighlights = TutorCompare.mixedBag(phaseCompares.flatMap(_.peerComparisons)) _

  val relevantComparisons: List[AnyComparison] =
    openingCompares.flatMap(_.allComparisons) ::: phaseCompares.flatMap(_.peerComparisons)

  def openingFrequency(color: Color, fam: TutorOpeningFamily) =
    TutorRatio(fam.performance.mine.count, stats.nbGames(color))
}

private object TutorPerfs {

  import TutorBuilder._

  private val accuracyQuestion  = Question(InsightDimension.Perf, Metric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Perf, Metric.Awareness)

  def compute(
      users: NonEmptyList[TutorUser]
  )(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[List[TutorPerfReport]] = for {
    accuracy  <- answerManyPerfs(accuracyQuestion, users)
    awareness <- answerManyPerfs(awarenessQuestion, users)
    perfReports <- users.toList.map { user =>
      for {
        openings <- TutorOpening compute user
        phases   <- TutorPhases compute user
      } yield TutorPerfReport(
        user.perfType,
        user.perfStats,
        accuracy = accuracy valueMetric user.perfType map AccuracyPercent.apply,
        awareness = awareness valueMetric user.perfType map TutorRatio.apply,
        openings,
        phases
      )
    }.sequenceFu

  } yield perfReports

}
