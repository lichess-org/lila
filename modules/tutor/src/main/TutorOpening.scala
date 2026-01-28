package lila.tutor

import chess.{ ByColor, Color }
import chess.IntRating
import monocle.syntax.all.*

import lila.analyse.AccuracyPercent
import lila.common.LilaOpeningFamily
import lila.insight.{ Insight, Filter, InsightApi, InsightDimension, InsightMetric, Phase, Question }

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

  val nbOpeningsPerColor = 5

  def compute(user: TutorPlayer)(using InsightApi, Executor): Fu[ByColor[TutorColorOpenings]] =
    ByColor(computeOpenings(user, _))

  def computeOpenings(user: TutorPlayer, color: Color)(using
      InsightApi,
      Executor
  ): Fu[TutorColorOpenings] = for
    wideQuestion = Question(
      InsightDimension.OpeningFamily,
      InsightMetric.Performance,
      List(Filter(color))
    )
    myPerfsWide <- answerMine(wideQuestion, user)
    myPerfs = myPerfsWide.focus(_.answer.clusters).modify(_.take(nbOpeningsPerColor))
    myOpenings = myPerfs.dimensions
    focusedQuestion = wideQuestion.filter(Filter(InsightDimension.OpeningFamily, myOpenings))
    meanRatingPoint = Insight.Single(lila.insight.Point(user.perfStats.rating.value.toDouble))
    peerPerfs = AnswerPeer:
      myPerfs.answer
        .focus(_.clusters)
        .modify(_.map(_.set(meanRatingPoint, peerNbGames.value)))
    performances = Answers(myPerfs, peerPerfs)
    myAccuracyQuestion = focusedQuestion
      .withMetric(InsightMetric.MeanAccuracy)
      .filter(phaseFilter)
    myAccuracy <- answerMine(myAccuracyQuestion, user)
    peerAccuracy <- peerOpeningAnswerFromSinglePoint(user, myAccuracy.answer, color)
    accuracy = Answers(myAccuracy, peerAccuracy)
    myAwarenessQuestion = myAccuracyQuestion.withMetric(InsightMetric.Awareness)
    myAwareness <- answerMine(myAwarenessQuestion, user)
    peerAwareness <- peerOpeningAnswerFromSinglePoint(user, myAwareness.answer, color)
    awareness = Answers(myAwareness, peerAwareness)
  yield TutorColorOpenings:
    performances.mine.list.map: (family, myPerformance) =>
      TutorOpeningFamily(
        family,
        performance =
          IntRating.from(performances.valueMetric(family, myPerformance).map(TutorNumber.roundToInt)),
        accuracy = AccuracyPercent.from(accuracy.valueMetric(family)),
        awareness = GoodPercent.from(awareness.valueMetric(family))
      )

  private val phaseFilter = Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle))

  private def peerOpeningAnswerFromSinglePoint[Dim](
      user: TutorPlayer,
      myAnswer: lila.insight.Answer[Dim],
      color: Color
  )(using insightApi: InsightApi, ec: Executor): Fu[AnswerPeer[Dim]] = for
    question = Question(InsightDimension.Color, myAnswer.question.metric, List(phaseFilter, Filter(color)))
    answer <- answerPeer(question, user)
    answerPoint = answer.answer.clusters.headOption.map(_.insight)
    peerAnswer = AnswerPeer:
      myAnswer
        .focus(_.clusters)
        .modify: clusters =>
          answerPoint.so: point =>
            clusters.map(_.set(point, peerNbGames.value))
  yield peerAnswer
