package lila.tutor

import chess.Color
import scala.concurrent.duration._

import lila.insight.Phase
import lila.insight.{ Insight, InsightDimension, InsightPerfStats, Metric, Phase }
import lila.rating.PerfType
import lila.common.Heapsort
import lila.tutor.TutorCompare.comparisonOrdering

case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase]
) {
  lazy val estimateTotalTime: Option[FiniteDuration] = (perf != PerfType.Correspondence) option stats.time * 2

  lazy val acplCompare = TutorCompare[Phase, Acpl](
    InsightDimension.Phase,
    Metric.MeanCpl,
    phases.map { phase => (phase.phase, phase.acpl) }
  )

  lazy val awarenessCompare = TutorCompare[Phase, TutorRatio](
    InsightDimension.Phase,
    Metric.Awareness,
    phases.map { phase => (phase.phase, phase.awareness) }
  )

  def dimensionHighlights(nb: Int) =
    Heapsort.topNToList(
      acplCompare.dimComparisons ::: awarenessCompare.dimComparisons,
      nb,
      comparisonOrdering
    )
  def peerHighlights(nb: Int) =
    Heapsort.topNToList(
      acplCompare.peerComparisons.pp ::: awarenessCompare.peerComparisons.pp,
      nb,
      comparisonOrdering
    )
}
