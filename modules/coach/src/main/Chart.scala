package lila.coach

import play.api.libs.json._

case class Chart(
  xAxis: Chart.Xaxis,
  yAxis: List[Chart.Yaxis],
  series: List[Chart.Serie])

object Chart {

  case class Xaxis(
    categories: List[String])

  case class Yaxis(
    name: String,
    isSize: Boolean)

  case class Serie(
    name: String,
    isSize: Boolean,
    data: List[Double])

  def fromAnswer[X](answer: Answer[X]): Chart = Chart(
    xAxis = Xaxis(answer.clusters.map(_.x).map(answer.question.xAxis.valueName)),
    yAxis = answer.clusters.headOption.?? { c =>
      List(Yaxis(c.data.name, false), Yaxis(c.size.name, true))
    },
    series = answer.clusters.flatMap { c =>
      c.points.map { c -> _ }
    }.foldLeft(Map.empty[String, Serie]) {
      case (acc, (cluster, point)) => acc.updated(point.name,
        acc.get(point.name) match {
          case None    => Serie(point.name, point.isSize, List(point.y))
          case Some(s) => s.copy(data = point.y :: s.data)
        })
    }.map { _._2 }.toList
  )
}
