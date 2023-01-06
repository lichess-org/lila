package lila.tutor

import cats.data.NonEmptyList
import chess.Color
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import lila.analyse.AccuracyPercent
import lila.common.Heapsort.given
import lila.common.LilaOpeningFamily
import lila.insight.*
import lila.rating.PerfType
import lila.tutor.TutorCompare.{ compOrder, AnyComparison }
import lila.insight.Result
import lila.common.config

// for simplicity, all metrics should be positive: higher is better
case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    accuracy: TutorBothValueOptions[AccuracyPercent],
    awareness: TutorBothValueOptions[GoodPercent],
    globalClock: TutorBothValueOptions[ClockPercent],
    clockUsage: TutorBothValueOptions[ClockPercent],
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase],
    flagging: TutorFlagging
):
  lazy val estimateTotalTime: Option[FiniteDuration] = (perf != PerfType.Correspondence) option stats.time * 2

  // Dimension comparison is not interesting for phase accuracy (opening always better)
  // But peer comparison is gold
  lazy val phaseAccuracyCompare = TutorCompare[Phase, AccuracyPercent](
    InsightDimension.Phase,
    TutorMetric.Accuracy,
    phases.map { phase => (phase.phase, phase.accuracy) }
  )

  lazy val phaseAwarenessCompare = TutorCompare[Phase, GoodPercent](
    InsightDimension.Phase,
    TutorMetric.Awareness,
    phases.map { phase => (phase.phase, phase.awareness) }
  )

  lazy val globalPressureCompare = TutorCompare[PerfType, ClockPercent](
    InsightDimension.Perf,
    TutorMetric.GlobalClock,
    List((perf, globalClock))
  )

  lazy val timeUsageCompare = TutorCompare[PerfType, ClockPercent](
    InsightDimension.Perf,
    TutorMetric.ClockUsage,
    List((perf, clockUsage))
  )

  // lazy val flaggingCompare = TutorCompare[PerfType, ClockPercent](
  //   InsightDimension.Perf,
  //   TutorMetric.Flagging,
  //   List((perf, flagging))
  // )

  def phaseCompares = List(phaseAccuracyCompare, phaseAwarenessCompare)

  val clockCompares = List(globalPressureCompare, timeUsageCompare)

  def openingCompares: List[TutorCompare[LilaOpeningFamily, ?]] = openings.all.toList.flatMap { op =>
    List(op.accuracyCompare, op.awarenessCompare, op.performanceCompare)
  }

  lazy val allCompares: List[TutorCompare[?, ?]] = openingCompares ::: phaseCompares

  val openingHighlights = TutorCompare.mixedBag(openingCompares.flatMap(_.allComparisons))

  val phaseHighlights = TutorCompare.mixedBag(phaseCompares.flatMap(_.peerComparisons))

  val timeHighlights = TutorCompare.mixedBag(clockCompares.flatMap(_.peerComparisons))

  val relevantComparisons: List[AnyComparison] =
    openingCompares.flatMap(_.allComparisons) :::
      phaseCompares.flatMap(_.peerComparisons) :::
      clockCompares.flatMap(_.peerComparisons)

  def openingFrequency(color: Color, fam: TutorOpeningFamily) =
    GoodPercent(fam.performance.mine.count, stats.nbGames(color))

private object TutorPerfReport:

  import TutorBuilder.*

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
    clockUsage  <- TutorClockUsage compute users
    perfReports <- scala.concurrent.Future sequence users.toList.map { user =>
      for {
        openings <- TutorOpening compute user
        phases   <- TutorPhases compute user
        flagging <- TutorFlagging compute user
      } yield TutorPerfReport(
        user.perfType,
        user.perfStats,
        accuracy = AccuracyPercent.from(accuracy valueMetric user.perfType),
        awareness = GoodPercent.from(awareness valueMetric user.perfType),
        globalClock = ClockPercent.from(globalClock valueMetric user.perfType),
        clockUsage = ClockPercent.from(clockUsage valueMetric user.perfType),
        openings,
        phases,
        flagging
      )
    }

  } yield perfReports
