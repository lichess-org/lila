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
) {

  def mix: TutorBothValueOptions[GoodPercent] = accuracy.map(a => GoodPercent(a.value)) mix awareness
}

private object TutorPhases {

  import TutorBuilder._

  private val accuracyQuestion  = Question(InsightDimension.Phase, InsightMetric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Phase, InsightMetric.Awareness)

  def compute(user: TutorUser)(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[List[TutorPhase]] =
    for {
      accuracy  <- answerBoth(accuracyQuestion, user)
      awareness <- answerBoth(awarenessQuestion, user)
    } yield InsightDimension.valuesOf(InsightDimension.Phase).map { phase =>
      TutorPhase(
        phase,
        accuracy = accuracy valueMetric phase map AccuracyPercent.apply,
        awareness = awareness valueMetric phase map GoodPercent.apply
      )
    }
}
