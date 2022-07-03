package lila.tutor

import cats.data.NonEmptyList
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
  Question,
  TimePressure
}
import lila.rating.PerfType
import lila.tutor.TutorCompare.{ comparisonOrdering, AnyComparison }
import lila.insight.Result
import lila.common.config

case class TutorPerfReport(
    perf: PerfType,
    stats: InsightPerfStats,
    accuracy: TutorBothValueOptions[AccuracyPercent],
    awareness: TutorBothValueOptions[TutorRatio],
    globalTimePressure: TutorBothValueOptions[TimePressure],
    defeatTimePressure: TutorBothValueOptions[TimePressure],
    openings: Color.Map[TutorColorOpenings],
    phases: List[TutorPhase]
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

  lazy val globalPressureCompare = TutorCompare[PerfType, TimePressure](
    InsightDimension.Perf,
    TutorMetric.GlobalTimePressure,
    List((perf, globalTimePressure))
  )

  lazy val defeatPressureCompare = TutorCompare[PerfType, TimePressure](
    InsightDimension.Perf,
    TutorMetric.DefeatTimePressure,
    List((perf, defeatTimePressure))
  )

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

private object TutorPerfs {

  import TutorBuilder._

  private val accuracyQuestion  = Question(InsightDimension.Perf, Metric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Perf, Metric.Awareness)
  private val pressureQuestion = Question(
    InsightDimension.Perf,
    Metric.TimePressure,
    List(Filter(InsightDimension.Phase, List(Phase.Middle, Phase.End)))
  )

  def compute(
      users: NonEmptyList[TutorUser]
  )(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[List[TutorPerfReport]] = for {
    accuracy       <- answerManyPerfs(accuracyQuestion, users)
    awareness      <- answerManyPerfs(awarenessQuestion, users)
    pressure       <- answerManyPerfs(pressureQuestion, users)
    defeatPressure <- computeDefeatTimePressure(users)
    perfReports <- users.toList.map { user =>
      for {
        openings <- TutorOpening compute user
        phases   <- TutorPhases compute user
      } yield TutorPerfReport(
        user.perfType,
        user.perfStats,
        accuracy = accuracy valueMetric user.perfType map AccuracyPercent.apply,
        awareness = awareness valueMetric user.perfType map TutorRatio.fromPercent,
        globalTimePressure = pressure valueMetric user.perfType map TimePressure.fromPercent,
        defeatTimePressure = defeatPressure valueMetric user.perfType map TimePressure.fromPercent,
        openings,
        phases
      )
    }.sequenceFu

  } yield perfReports

  private def computeDefeatTimePressure(
      users: NonEmptyList[TutorUser]
  )(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[Answers[PerfType]] = {
    import lila.db.dsl._
    import lila.rating.BSONHandlers.perfTypeIdHandler
    import lila.insight.{ Insight, Cluster, Answer, InsightStorage, Point }
    import lila.insight.InsightEntry.{ BSONFields => F }
    val perfs = users.toList.map(_.perfType)
    val question = Question(
      InsightDimension.Perf,
      Metric.TimePressure,
      List(Filter(InsightDimension.Perf, perfs))
    )
    val gameMatcher = $doc(F.result -> Result.Loss.id, F.perf $in perfs)
    def clusterParser(docs: List[Bdoc]) = for {
      doc      <- docs
      perf     <- doc.getAsOpt[PerfType]("_id")
      pressure <- doc.double("tp")
      size     <- doc.int("nb")
    } yield Cluster(perf, Insight.Single(Point(pressure)), size, Nil)
    for {
      mine <- insightApi.coll {
        _.aggregateList(maxDocs = Int.MaxValue) { implicit framework =>
          import framework._
          Match(InsightStorage.selectUserId(users.head.user.id) ++ gameMatcher) -> List(
            Sort(Descending(F.date)),
            Limit(10_000),
            Project($doc(F.perf -> true, F.moves -> $doc("$last" -> s"$$${F.moves}"))),
            UnwindField(F.moves),
            Project($doc(F.perf -> true, "tp" -> s"$$${F.moves}.s")),
            GroupField(F.perf)("tp" -> AvgField("tp"), "nb" -> SumAll)
          )
        } map { docs =>
          docs foreach lila.db.BSON.print
          AnswerMine(Answer(question, clusterParser(docs), Nil))
        }
      }
      peer <- insightApi.coll {
        _.aggregateList(maxDocs = Int.MaxValue) { implicit framework =>
          import framework._
          Match(
            InsightStorage.selectPeers(Question.Peers(users.head.perfStats.rating)) ++ gameMatcher
          ) -> List(
            Limit(10_000),
            Project($doc(F.perf -> true, F.moves -> $doc("$last" -> s"$$${F.moves}"))),
            UnwindField(F.moves),
            Project($doc(F.perf -> true, "tp" -> s"$$${F.moves}.s")),
            GroupField(F.perf)("tp" -> AvgField("tp"), "nb" -> SumAll)
          )
        } map { docs =>
          docs foreach lila.db.BSON.print
          AnswerPeer(Answer(question, clusterParser(docs), Nil))
        }
      }
    } yield Answers(mine, peer)
  }
}
