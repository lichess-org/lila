package lila.coach

import play.api.libs.json._

case class Chart(
  xAxis: Chart.Xaxis,
  series: List[Chart.Serie])

object Chart {

  case class Xaxis(
    categories: List[String])

  case class Serie(
    name: String,
    data: List[Double])

  def fromAnswer[X](answer: Answer[X]): Chart = Chart(
    xAxis = Xaxis(answer.clusters.map(_.x).map(answer.question.xAxis.valueName)),
    series = answer.clusters.flatMap { c =>
      c.points.map { c -> _ }
    }.foldLeft(Map.empty[String, List[Double]]) {
      case (acc, (cluster, point)) => acc.updated(point.name,
        acc.get(point.name) match {
          case None     => List(point.y)
          case Some(ds) => point.y :: ds
        })
    }.map {
      case (name, data) => Serie(name, data)
    }.toList)
}
