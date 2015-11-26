package lila.coach

import play.api.data._
import play.api.data.Forms._

import lila.common.Form._

final class DataForm {

  val search = Form(mapping(
    "xAxis" -> nonEmptyText.verifying(x => Dimension.byKey(x).isDefined),
    "yAxis" -> nonEmptyText.verifying(x => Metric.byKey(x).isDefined)
  )(QuestionData.apply)(QuestionData.unapply)) fill QuestionData.default
}

case class QuestionData(
    xAxis: String,
    yAxis: String) {

  def question: Option[Question[_]] = Metric byKey yAxis flatMap { realYaxis =>

    def build[X](realXaxis: Dimension[X]) =
      (realYaxis: Metric, filters: List[Filter[_]]) => Question[X](realXaxis, realYaxis, filters)

    import Dimension._
    (xAxis match {
      case Perf.key             => build(Perf).some
      case Phase.key            => build(Phase).some
      case Result.key           => build(Result).some
      case Color.key            => build(Color).some
      case Opening.key          => build(Opening).some
      case OpponentStrength.key => build(OpponentStrength).some
      case PieceRole.key        => build(PieceRole).some
      case key                  => none
    }) map { _(realYaxis, Nil) }
  }
}

object QuestionData {

  val default = QuestionData(Dimension.Opening.key, Metric.MeanCpl.key)
}
