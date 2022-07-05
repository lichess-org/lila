package lila.tutor

import cats.data.NonEmptyList
import chess.Color
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.analyse.AccuracyPercent
import lila.common.Heapsort.implicits._
import lila.common.LilaOpeningFamily
import lila.insight._
import lila.rating.PerfType
import lila.tutor.TutorCompare.{ comparisonOrdering, AnyComparison }
import lila.insight.Result
import lila.common.config

// for simplicity, all metrics should be positive: higher is better
case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    accuracy: TutorBothValueOptions[AccuracyPercent],
    awareness: TutorBothValueOptions[TutorRatio],
    globalClock: TutorBothValueOptions[ClockPercent],
    defeatClock: TutorBothValueOptions[ClockPercent], // lower is better
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase],
    flagging: TutorFlagging
) {
  lazy val estimateTotalTime: Option[FiniteDuration] = (perf != PerfType.Correspondence) option stats.time * 2

  // Dimension comparison is not interesting for phase accuracy (opening always better)
  // But peer comparison is gold
  lazy val phaseAccuracyCompare = TutorCompare[Phase, AccuracyPercent](
    InsightDimension.Phase,
    TutorMetric.Accuracy,
    phases.map { phase => (phase.phase, phase.accuracy) }
  )

  lazy val phaseAwarenessCompare = TutorCompare[Phase, TutorRatio](
    InsightDimension.Phase,
    TutorMetric.Awareness,
    phases.map { phase => (phase.phase, phase.awareness) }
  )

  lazy val globalPressureCompare = TutorCompare[PerfType, ClockPercent](
    InsightDimension.Perf,
    TutorMetric.GlobalClock,
    List((perf, globalClock))
  )

  lazy val defeatPressureCompare = TutorCompare[PerfType, ClockPercent](
    InsightDimension.Perf,
    TutorMetric.DefeatClock,
    List((perf, defeatClock))
  )(TutorNumber.clockPercentIsTutorNumber.reverseCompare)

  def phaseCompares = List(phaseAccuracyCompare, phaseAwarenessCompare)

  val timePressureCompares = List(globalPressureCompare, defeatPressureCompare)

  def openingCompares: List[TutorCompare[LilaOpeningFamily, _]] = openings.all.toList.flatMap { op =>
    List(op.accuracyCompare, op.awarenessCompare, op.performanceCompare)
  }

  lazy val allCompares: List[TutorCompare[_, _]] =
    openingCompares ::: phaseCompares

  val openingHighlights = TutorCompare.mixedBag(openingCompares.flatMap(_.allComparisons)) _

  val phaseHighlights = TutorCompare.mixedBag(phaseCompares.flatMap(_.peerComparisons)) _

  val timeHighlights = TutorCompare.mixedBag(timePressureCompares.flatMap(_.peerComparisons)) _

  val relevantComparisons: List[AnyComparison] =
    openingCompares.flatMap(_.allComparisons) :::
      phaseCompares.flatMap(_.peerComparisons) :::
      timePressureCompares.flatMap(_.peerComparisons)

  def openingFrequency(color: Color, fam: TutorOpeningFamily) =
    TutorRatio(fam.performance.mine.count, stats.nbGames(color))
}

private object TutorPerfReport {

  import TutorBuilder._

  private val accuracyQuestion  = Question(InsightDimension.Perf, InsightMetric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Perf, InsightMetric.Awareness)
  private val globalClockQuestion = Question(
    InsightDimension.Perf,
    InsightMetric.ClockPercent,
    List(Filter(InsightDimension.Phase, List(Phase.Middle, Phase.End)))
  )

  def compute(
      users: NonEmptyList[TutorUser]
  )(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[List[TutorPerfReport]] = for {
    accuracy    <- answerManyPerfs(accuracyQuestion, users)
    awareness   <- answerManyPerfs(awarenessQuestion, users)
    globalClock <- answerManyPerfs(globalClockQuestion, users)
    defeatClock <- TutorDefeatClock compute users
    perfReports <- users.toList.map { user =>
      for {
        openings <- TutorOpening compute user
        phases   <- TutorPhases compute user
        flagging <- TutorFlagging compute user
      } yield TutorPerfReport(
        user.perfType,
        user.perfStats,
        accuracy = accuracy valueMetric user.perfType map AccuracyPercent.apply,
        awareness = awareness valueMetric user.perfType map TutorRatio.fromPercent,
        globalClock = globalClock valueMetric user.perfType map ClockPercent.fromPercent,
        defeatClock = defeatClock valueMetric user.perfType map ClockPercent.fromPercent,
        openings,
        phases,
        flagging
      )
    }.sequenceFu

  } yield perfReports
}
