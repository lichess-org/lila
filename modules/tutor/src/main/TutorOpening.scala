package lila.tutor

import chess.{ ByColor, Color }
import chess.IntRating
import monocle.syntax.all.*

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
    families.map { f => (f.family, f.performance.some) }
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
    accuracy: TutorBothOption[AccuracyPercent],
    awareness: TutorBothOption[GoodPercent]
):

  def mix: TutorBothOption[GoodPercent] =
    TutorBothValues.mix(accuracy.map(_.map(a => GoodPercent(a.value))), awareness)

private case object TutorOpening:

  import TutorBuilder.*

  val nbOpeningsPerColor = 5

  def compute(user: TutorPlayer)(using InsightApi, Executor): Fu[ByColor[TutorColorOpenings]] =
    ByColor(computeOpenings(user, _))

  def computeOpenings(user: TutorPlayer, color: Color)(using
      InsightApi,
      Executor
  ): Fu[TutorColorOpenings] =
    val wideQuestion = Question(
      InsightDimension.OpeningFamily,
      InsightMetric.Performance,
      List(Filter(color))
    )
    for
      myPerfsWide <- answerMine(wideQuestion, user)
      myPerformance = myPerfsWide.focus(_.answer.clusters).modify(_.take(nbOpeningsPerColor))
      myOpenings = myPerformance.dimensions
      focusedQuestion = wideQuestion.filter(Filter(InsightDimension.OpeningFamily, myOpenings))
      peerPerformance = user.perfStats.rating
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
      myPerformance.list.map: (family, myPerformance) =>
        TutorOpeningFamily(
          family,
          performance = IntRating.from:
            TutorBothValues(myPerformance, peerPerformance.value).map(TutorNumber.roundToInt)
          ,
          accuracy = accuracy.valueMetric(family).map(AccuracyPercent.from),
          awareness = awareness.valueMetric(family).map(GoodPercent.from)
        )

  private val phaseFilter = Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle))

  private def peerOpeningAnswerFromSinglePoint[Dim](
      user: TutorPlayer,
      myAnswer: lila.insight.Answer[Dim],
      color: Color
  )(using insightApi: InsightApi, ec: Executor): Fu[AnswerPeer[Dim]] =
    val question =
      Question(InsightDimension.Color, myAnswer.question.metric, List(phaseFilter, Filter(color)))
    for
      answer <- answerPeer(question, user)
      answerPoint = answer.answer.clusters.headOption.map(_.insight)
      peerAnswer = AnswerPeer:
        myAnswer
          .focus(_.clusters)
          .modify: clusters =>
            answerPoint.so: point =>
              clusters.map(_.set(point, peerNbGames.value))
    yield peerAnswer
