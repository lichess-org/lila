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

  private def computeOpenings(user: TutorPlayer, color: Color)(using
      InsightApi,
      Executor
  ): Fu[TutorColorOpenings] =

    val wideQuestion = Question(
      InsightDimension.OpeningFamily,
      InsightMetric.Performance,
      List(Filter(color))
    )

    val peerMatch = user.peerMatch.flatMap(_.openings(color).families.headOption)

    def peerOpeningPoint(mine: AnswerMine[?], fromPeerMatch: TutorOpeningFamily => Option[Double])(using
        InsightApi,
        Executor
    ): Fu[Option[Double]] =
      peerMatch.flatMap(fromPeerMatch) match
        case Some(found) => fuccess(found.some)
        case None =>
          val question =
            Question(InsightDimension.Color, mine.answer.question.metric, List(phaseFilter, Filter(color)))
          answerPeer(question, user).map:
            _.answer.clusters.headOption
              .map(_.insight)
              .collect:
                case Insight.Single(point) => point.value

    for
      myPerfsWide <- answerMine(wideQuestion, user)
      myPerformance = myPerfsWide.focus(_.answer.clusters).modify(_.take(nbOpeningsPerColor))
      focusedQuestion = wideQuestion.filter(Filter(InsightDimension.OpeningFamily, myPerformance.dimensions))
      peerPerformance = user.perfStats.rating
      myAccuracyQuestion = focusedQuestion.withMetric(InsightMetric.MeanAccuracy).filter(phaseFilter)
      myAccuracy <- answerMine(myAccuracyQuestion, user)
      peerAccuracy <- peerOpeningPoint(myAccuracy, _.accuracy.map(_.peer.value))
      myAwarenessQuestion = myAccuracyQuestion.withMetric(InsightMetric.Awareness)
      myAwareness <- answerMine(myAwarenessQuestion, user)
      peerAwareness <- peerOpeningPoint(myAwareness, _.awareness.map(_.peer.value))
    yield TutorColorOpenings:
      myPerformance.list.map: (family, myPerformance) =>
        TutorOpeningFamily(
          family,
          performance = IntRating.from:
            TutorBothValues(myPerformance, peerPerformance.value).map(TutorNumber.roundToInt)
          ,
          accuracy = for
            mine <- myAccuracy.get(family)
            peer <- peerAccuracy
          yield AccuracyPercent.from(TutorBothValues(mine, peer)),
          awareness = for
            mine <- myAwareness.get(family)
            peer <- peerAwareness
          yield GoodPercent.from(TutorBothValues(mine, peer))
        )

  private val phaseFilter = Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle))
