package lila.tutor

import chess.Color
import scala.concurrent.duration._

import lila.common.Heapsort
import lila.insight.{ Insight, InsightDimension, InsightPerfStats, Metric, Phase }
import lila.rating.PerfType
import lila.tutor.TutorCompare.comparisonOrdering

case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase]
) {
  lazy val estimateTotalTime: Option[FiniteDuration] = (perf != PerfType.Correspondence) option stats.time * 2

  lazy val phaseAcplCompare = TutorCompare[Phase, Acpl](
    InsightDimension.Phase,
    Metric.MeanCpl,
    phases.map { phase => (phase.phase, phase.acpl) }
  )

  lazy val phaseAwarenessCompare = TutorCompare[Phase, TutorRatio](
    InsightDimension.Phase,
    Metric.Awareness,
    phases.map { phase => (phase.phase, phase.awareness) }
  )

  def phaseCompares = List(phaseAcplCompare, phaseAwarenessCompare)

  def openingCompares = openings.all.toList.flatMap { op =>
    List(op.acplCompare, op.awarenessCompare, op.performanceCompare)
  }

  lazy val allCompares: List[TutorCompare[_, _]] =
    phaseAcplCompare :: phaseAwarenessCompare :: openingCompares

  def openingDimensionHighlights(nb: Int) =
    Heapsort.topNToList(openingCompares.flatMap(_.dimComparisons), nb, comparisonOrdering)

  def openingPeerHighlights(nb: Int) =
    Heapsort.topNToList(openingCompares.flatMap(_.peerComparisons), nb, comparisonOrdering)

  def phaseDimensionHighlights(nb: Int) =
    Heapsort.topNToList(phaseCompares.flatMap(_.dimComparisons), nb, comparisonOrdering)

  def phasePeerHighlights(nb: Int) =
    Heapsort.topNToList(phaseCompares.flatMap(_.peerComparisons), nb, comparisonOrdering)

  def dimensionHighlights(nb: Int) =
    Heapsort.topNToList(allCompares.flatMap(_.dimComparisons), nb, comparisonOrdering)

  def peerHighlights(nb: Int) =
    Heapsort.topNToList(allCompares.flatMap(_.peerComparisons), nb, comparisonOrdering)
}
