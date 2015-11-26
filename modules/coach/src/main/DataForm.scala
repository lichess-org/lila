package lila.coach

import play.api.data._
import play.api.data.Forms._

import lila.common.Form._

final class DataForm {

  val question = Form(mapping(
    "dimension" -> nonEmptyText.verifying(x => Dimension.byKey(x).isDefined),
    "metric" -> nonEmptyText.verifying(x => Metric.byKey(x).isDefined)
  )(QuestionData.apply)(QuestionData.unapply)) fill QuestionData.default
}

case class QuestionData(
    dimension: String,
    metric: String) {

  def question: Option[Question[_]] = Metric byKey metric flatMap { realMetric =>

    def build[X](realDimension: Dimension[X]) =
      (realMetric: Metric, filters: List[Filter[_]]) => Question[X](realDimension, realMetric, filters)

    import Dimension._
    (dimension match {
      case Perf.key             => build(Perf).some
      case Phase.key            => build(Phase).some
      case Result.key           => build(Result).some
      case Color.key            => build(Color).some
      case Opening.key          => build(Opening).some
      case OpponentStrength.key => build(OpponentStrength).some
      case PieceRole.key        => build(PieceRole).some
      case key                  => none
    }) map { _(realMetric, Nil) }
  }
}

object QuestionData {

  val default = QuestionData(Dimension.Opening.key, Metric.MeanCpl.key)
}
