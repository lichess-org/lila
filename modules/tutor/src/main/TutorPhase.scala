package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.analyse.AccuracyPercent
import lila.insight.{ Filter, InsightApi, InsightDimension, Metric, Phase, Question }
import lila.rating.PerfType
import lila.user.User

case class TutorPhase(
    phase: Phase,
    accuracy: TutorMetricOption[AccuracyPercent],
    awareness: TutorMetricOption[TutorRatio],
    material: TutorMetricOption[Double]
)

private object TutorPhases {

  import TutorBuilder._

  private val accuracyQuestion  = Question(InsightDimension.Phase, Metric.MeanAccuracy)
  private val awarenessQuestion = Question(InsightDimension.Phase, Metric.Awareness)
  private val materialQuestion  = Question(InsightDimension.Phase, Metric.Material)

  def compute(user: TutorUser)(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[List[TutorPhase]] =
    for {
      accuracy  <- answerBoth(accuracyQuestion, user)
      awareness <- answerBoth(awarenessQuestion, user)
      materials <- answerBoth(materialQuestion, user)
    } yield InsightDimension.valuesOf(InsightDimension.Phase).map { phase =>
      TutorPhase(
        phase,
        accuracy = accuracy valueMetric phase map AccuracyPercent.apply,
        awareness = awareness valueMetric phase map TutorRatio.apply,
        material = materials valueMetric phase
      )
    }
}
