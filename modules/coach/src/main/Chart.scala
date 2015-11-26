package lila.coach

import play.api.libs.json._

case class Chart(
  xAxis: Chart.Xaxis,
  yAxis: List[Chart.Yaxis],
  series: List[Chart.Serie])

object Chart {

  case class Xaxis(
    name: String,
    categories: List[String])

  case class Yaxis(
    name: String,
    isSize: Boolean)

  case class Serie(
    name: String,
    dataType: String,
    isSize: Boolean,
    data: List[Double])

  def fromAnswer[X](answer: Answer[X]): Chart = {

    def startSerie(point: Point) =
      if (point.isSize) Serie(point.name, Metric.DataType.Count.name, true, List(point.y))
      else Serie(point.name, answer.question.metric.dataType.name, false, List(point.y))

    Chart(
      xAxis = Xaxis(
        answer.question.dimension.name,
        answer.clusters.map(_.x).map(answer.question.dimension.valueName)),
      yAxis = answer.clusters.headOption.?? { c =>
        List(Yaxis(c.data.name, false), Yaxis(c.size.name, true))
      },
      series = answer.clusters.flatMap { c =>
        c.points.map { c -> _ }
      }.foldLeft(Map.empty[String, Serie]) {
        case (acc, (cluster, point)) => acc.updated(point.key,
          acc.get(point.key) match {
            case None    => startSerie(point)
            case Some(s) => s.copy(data = point.y :: s.data)
          })
      }.map {
        case (_, serie) => serie.copy(data = serie.data.reverse)
      }.toList.foldLeft(List.empty[Serie]) {
        case (acc, s) if s.isSize && acc.exists(_.name == s.name) => acc
        case (acc, s) => acc :+ s
      }
    )
  }
}
