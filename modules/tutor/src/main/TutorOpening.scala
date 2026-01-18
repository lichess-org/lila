package lila.tutor

import chess.{ ByColor, Color }
import chess.IntRating

import lila.analyse.AccuracyPercent
import lila.common.LilaOpeningFamily
import lila.insight.{ Filter, InsightApi, InsightDimension, InsightMetric, Phase, Question }

case class TutorColorOpenings(families: List[TutorOpeningFamily]):
  lazy val accuracyCompare = TutorCompare[LilaOpeningFamily, AccuracyPercent](
    InsightDimension.OpeningFamily,
    TutorMetric.Accuracy,
    families.map { f => (f.family, f.accuracy) }
  )
  lazy val performanceCompare = TutorCompare[LilaOpeningFamily, IntRating](
    InsightDimension.OpeningFamily,
    TutorMetric.Performance,
    families.map { f => (f.family, f.performance.toOption) }
  )
  lazy val awarenessCompare = TutorCompare[LilaOpeningFamily, GoodPercent](
    InsightDimension.OpeningFamily,
    TutorMetric.Awareness,
    families.map { f => (f.family, f.awareness) }
  )

  // lazy val allCompares = List(accuracyCompare, performanceCompare, awarenessCompare)

  def find(fam: LilaOpeningFamily) = families.find(_.family == fam)

case class TutorOpeningFamily(
    family: LilaOpeningFamily,
    performance: TutorBothValues[IntRating],
    accuracy: TutorBothValueOptions[AccuracyPercent],
    awareness: TutorBothValueOptions[GoodPercent]
):

  def mix: TutorBothValueOptions[GoodPercent] = accuracy.map(a => GoodPercent(a.value)).mix(awareness)

private case object TutorOpening:

  import TutorBuilder.*

  val nbOpeningsPerColor = 8

  def compute(user: TutorUser)(using InsightApi, Executor): Fu[ByColor[TutorColorOpenings]] =
    ByColor(computeOpenings(user, _))

  def computeOpenings(user: TutorUser, color: Color)(using
      InsightApi,
      Executor
  ): Fu[TutorColorOpenings] = for
    myPerfsFull <- answerMine(perfQuestion(color), user)
    myPerfs = myPerfsFull.copy(answer =
      myPerfsFull.answer.copy(
        clusters = myPerfsFull.answer.clusters.take(nbOpeningsPerColor)
      )
    )
    peerPerfs <- answerPeer(myPerfs.alignedQuestion, user, Max(10_000))
    performances = Answers(myPerfs, peerPerfs)
    accuracyQuestion = myPerfs.alignedQuestion
      .withMetric(InsightMetric.MeanAccuracy)
      .filter(Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle)))
    accuracy <- answerBoth(accuracyQuestion, user, Max(1000))
    awarenessQuestion = accuracyQuestion.withMetric(InsightMetric.Awareness)
    awareness <- answerBoth(awarenessQuestion, user, Max(1000))
  yield TutorColorOpenings:
    performances.mine.list.map { (family, myPerformance) =>
      TutorOpeningFamily(
        family,
        performance =
          IntRating.from(performances.valueMetric(family, myPerformance).map(TutorNumber.roundToInt)),
        accuracy = AccuracyPercent.from(accuracy.valueMetric(family)),
        awareness = GoodPercent.from(awareness.valueMetric(family))
      )
    }

  def perfQuestion(color: Color) = Question(
    InsightDimension.OpeningFamily,
    InsightMetric.Performance,
    List(colorFilter(color))
  )
