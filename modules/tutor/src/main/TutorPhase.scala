package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.analyse.AccuracyPercent
import lila.insight.{ Filter, InsightApi, InsightDimension, InsightMetric, Phase, Question }
import lila.rating.PerfType
import lila.user.User

case class TutorPhase(
    phase: Phase,
    accuracy: TutorBothValueOptions[AccuracyPercent],
    awareness: TutorBothValueOptions[GoodPercent]
):

  def mix: TutorBothValueOptions[GoodPercent] = accuracy.map(_ into GoodPercent) mix awareness

private object TutorPhases:

  import TutorBuilder.*

  private val accuracyQuestion  = Question(InsightDimension.Phase, InsightMetric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Phase, InsightMetric.Awareness)

  def compute(user: TutorUser)(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[List[TutorPhase]] =
    for {
      accuracy  <- answerBoth(accuracyQuestion, user)
      awareness <- answerBoth(awarenessQuestion, user)
    } yield InsightDimension.valuesOf(InsightDimension.Phase).toList.map { phase =>
      TutorPhase(
        phase,
        accuracy = AccuracyPercent.from(accuracy valueMetric phase),
        awareness = GoodPercent.from(awareness valueMetric phase)
      )
    }
