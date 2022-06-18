package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.insight.{ Filter, InsightApi, InsightDimension, Metric, Phase, Question }
import lila.rating.PerfType
import lila.user.User

case class TutorPhase(
    phase: Phase,
    acpl: TutorMetricOption[Double],
    awareness: TutorMetricOption[TutorRatio],
    material: TutorMetricOption[Double]
)

private object TutorPhases {

  import TutorBuilder._

  private val acplQuestion = Question(
    InsightDimension.Phase,
    Metric.MeanCpl,
    List(Filter(InsightDimension.Perf, PerfType.standard))
  )
  private val awarenessQuestion = acplQuestion.copy(metric = Metric.Awareness)
  private val materialQuestion  = acplQuestion.copy(metric = Metric.Material)

  def compute(user: TutorUser)(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[List[TutorPhase]] =
    for {
      acpls     <- answers(acplQuestion, user)
      awareness <- answers(awarenessQuestion, user)
      materials <- answers(materialQuestion, user)
    } yield InsightDimension.valuesOf(InsightDimension.Phase).map { phase =>
      TutorPhase(
        phase,
        acpl = acpls valueMetric phase,
        awareness = awareness valueMetric phase map TutorRatio.apply,
        material = materials valueMetric phase
      )
    }
}
