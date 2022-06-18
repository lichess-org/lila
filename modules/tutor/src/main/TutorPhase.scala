package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.insight.{ Filter, InsightApi, InsightDimension, Metric, Phase, Question }
import lila.rating.PerfType
import lila.user.User

case class TutorPhase(
    phase: Phase,
    acpl: TutorMetricOption[Double]
)

private object TutorPhases {

  import TutorBuilder._

  private val acplQuestion = Question(
    InsightDimension.Phase,
    Metric.MeanCpl,
    List(Filter(InsightDimension.Perf, PerfType.standard))
  )

  def compute(user: TutorUser)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ): Fu[List[TutorPhase]] =
    for {
      acpls <- answers(acplQuestion, user)
    } yield InsightDimension.valuesOf(InsightDimension.Phase).map { phase =>
      TutorPhase(
        phase,
        acpl = acpls.valueMetric(phase)
      )
    }
}
