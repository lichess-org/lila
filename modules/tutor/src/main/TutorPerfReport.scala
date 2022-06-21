package lila.tutor

import chess.Color
import scala.concurrent.duration._

import lila.common.Heapsort.implicits._
import lila.common.LilaOpeningFamily
import lila.insight.{ Insight, InsightDimension, InsightPerfStats, Metric, Phase }
import lila.rating.PerfType
import lila.tutor.TutorCompare.{ comparisonOrdering, AnyComparison }

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

  def openingCompares: List[TutorCompare[LilaOpeningFamily, _]] = openings.all.toList.flatMap { op =>
    List(op.acplCompare, op.awarenessCompare, op.performanceCompare)
  }

  lazy val allCompares: List[TutorCompare[_, _]] =
    phaseAcplCompare :: phaseAwarenessCompare :: openingCompares

  val highlights        = TutorCompare.allHighlights(allCompares)
  val openingHighlights = TutorCompare.allHighlights(openingCompares)
  val phaseHighlights   = TutorCompare.allHighlights(phaseCompares)

  def openingFrequency(color: Color, fam: TutorOpeningFamily) =
    TutorRatio(fam.performance.mine.count, stats.nbGames(color))

  // def openingPeerHighlights(nb: Int) =
  //   Heapsort.topNToList(openingCompares.flatMap(_.peerComparisons), nb, comparisonOrdering)

  // def phaseDimensionHighlights(nb: Int) =
  //   Heapsort.topNToList(phaseCompares.flatMap(_.dimComparisons), nb, comparisonOrdering)

  // def phasePeerHighlights(nb: Int) =
  //   Heapsort.topNToList(phaseCompares.flatMap(_.peerComparisons), nb, comparisonOrdering)

  // def dimensionHighlights = TutorCompare.highlights(allCompares) _
  // Heapsort.topNToList(allCompares.flatMap(_.dimComparisons), nb, comparisonOrdering)

  // def peerHighlights(nb: Int) =
  //   Heapsort.topNToList(allCompares.flatMap(_.peerComparisons), nb, comparisonOrdering)
}
