package lila.tutor

import chess.{ ByColor, Color }

import lila.analyse.AccuracyPercent
import lila.common.LilaOpeningFamily
import lila.insight.*
import lila.rating.PerfType
import lila.tutor.TutorCompare.AnyComparison

// for simplicity, all metrics should be positive: higher is better
case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    peers: PeersRatingRange,
    accuracy: TutorBothValueOptions[AccuracyPercent],
    awareness: TutorBothValueOptions[GoodPercent],
    resourcefulness: TutorBothValueOptions[GoodPercent],
    conversion: TutorBothValueOptions[GoodPercent],
    globalClock: TutorBothValueOptions[ClockPercent],
    clockUsage: TutorBothValueOptions[ClockPercent],
    openings: ByColor[TutorColorOpenings],
    phases: List[TutorPhase],
    flagging: TutorFlagging
):
  lazy val estimateTotalTime: Option[FiniteDuration] =
    (perf != PerfType.Correspondence).option(stats.time * 2)

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

  lazy val globalAccuracyCompare = TutorCompare[PerfType, AccuracyPercent](
    InsightDimension.Perf,
    TutorMetric.Accuracy,
    List((perf, accuracy))
  )

  lazy val globalAwarenessCompare = TutorCompare[PerfType, GoodPercent](
    InsightDimension.Perf,
    TutorMetric.Awareness,
    List((perf, awareness))
  )

  lazy val globalResourcefulnessCompare = TutorCompare[PerfType, GoodPercent](
    InsightDimension.Perf,
    TutorMetric.Resourcefulness,
    List((perf, resourcefulness))
  )

  lazy val globalConversionCompare = TutorCompare[PerfType, GoodPercent](
    InsightDimension.Perf,
    TutorMetric.Conversion,
    List((perf, conversion))
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

  def skillCompares =
    List(globalAccuracyCompare, globalAwarenessCompare, globalResourcefulnessCompare, globalConversionCompare)

  def phaseCompares = List(phaseAccuracyCompare, phaseAwarenessCompare)

  val clockCompares = List(globalPressureCompare, timeUsageCompare)

  def openingCompares: List[TutorCompare[LilaOpeningFamily, ?]] = Color.all.flatMap: color =>
    val op = openings(color)
    List(op.accuracyCompare, op.awarenessCompare, op.performanceCompare).map(_.as(color))

  lazy val allCompares: List[TutorCompare[?, ?]] = openingCompares ::: phaseCompares

  val skillHighlights = TutorCompare.mixedBag(skillCompares.flatMap(_.peerComparisons))

  val openingHighlights = TutorCompare.mixedBag(openingCompares.flatMap(_.allComparisons))

  val phaseHighlights = TutorCompare.mixedBag(phaseCompares.flatMap(_.peerComparisons))

  val timeHighlights = TutorCompare.mixedBag(clockCompares.flatMap(_.peerComparisons))

  val relevantComparisons: List[AnyComparison] =
    openingCompares.flatMap(_.allComparisons) :::
      phaseCompares.flatMap(_.peerComparisons) :::
      clockCompares.flatMap(_.peerComparisons) :::
      skillCompares.flatMap(_.peerComparisons)
  val relevantHighlights = TutorCompare.mixedBag(relevantComparisons)

  def openingFrequency(color: Color, fam: TutorOpeningFamily) =
    GoodPercent(fam.performance.mine.count, stats.nbGames(color))

private object TutorPerfReport:

  case class PeerMatch(report: TutorPerfReport):
    export report.*

  import TutorBuilder.*

  private val accuracyQuestion = Question(InsightDimension.Perf, InsightMetric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Perf, InsightMetric.Awareness)
  private val globalClockQuestion = Question(
    InsightDimension.Perf,
    InsightMetric.ClockPercent,
    List(Filter(InsightDimension.Phase, List(Phase.Middle, Phase.End)))
  )

  def compute(users: NonEmptyList[TutorPlayer])(using InsightApi, Executor): Fu[List[TutorPerfReport]] =
    for
      accuracy <- answerManyPerfs(accuracyQuestion, users)
      awareness <- answerManyPerfs(awarenessQuestion, users)
      resourcefulness <- TutorResourcefulness.compute(users)
      conversion <- TutorConversion.compute(users)
      clockUsers = users.filter(_.perfType != PerfType.Correspondence).toNel
      globalClock <- clockUsers.traverse(answerManyPerfs(globalClockQuestion, _))
      clockUsage <- clockUsers.traverse(TutorClockUsage.compute)
      perfReports <- users.toList.sequentially: user =>
        for
          openings <- TutorOpening.compute(user)
          phases <- TutorPhases.compute(user)
          flagging <- TutorFlagging.computeIfRelevant(user)
        yield TutorPerfReport(
          user.perfType,
          user.perfStats,
          user.perfStats.peers,
          accuracy = AccuracyPercent.from(accuracy.valueMetric(user.perfType)),
          awareness = GoodPercent.from(awareness.valueMetric(user.perfType)),
          resourcefulness = GoodPercent.from(resourcefulness.valueMetric(user.perfType)),
          conversion = GoodPercent.from(conversion.valueMetric(user.perfType)),
          globalClock = ClockPercent.from(globalClock.so(_.valueMetric(user.perfType))),
          clockUsage = ClockPercent.from(clockUsage.so(_.valueMetric(user.perfType))),
          openings,
          phases,
          flagging
        )
    yield perfReports
