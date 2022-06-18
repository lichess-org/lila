package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.insight.{ Filter, InsightApi, InsightDimension, Metric, Phase, Question }
import lila.rating.PerfType
import lila.user.User

case class TutorPhase(
    phase: Phase,
    acpl: TutorMetricOption[Double],
    opportunism: TutorMetricOption[Double],
    material: TutorMetricOption[Double]
)

private object TutorPhases {

  import TutorBuilder._

  private val acplQuestion = Question(
    InsightDimension.Phase,
    Metric.MeanCpl,
    List(Filter(InsightDimension.Perf, PerfType.standard))
  )
  private val opportunismQuestion = acplQuestion.copy(metric = Metric.Opportunism)
  private val materialQuestion    = acplQuestion.copy(metric = Metric.Material)

  def compute(user: TutorUser)(implicit insightApi: InsightApi, ec: ExecutionContext): Fu[List[TutorPhase]] =
    for {
      acpls        <- answers(acplQuestion, user)
      opportunisms <- answers(opportunismQuestion, user)
      materials    <- answers(materialQuestion, user)
    } yield InsightDimension.valuesOf(InsightDimension.Phase).map { phase =>
      TutorPhase(
        phase,
        acpl = acpls valueMetric phase,
        opportunism = opportunisms valueMetric phase,
        material = materials valueMetric phase
      )
    }
}
