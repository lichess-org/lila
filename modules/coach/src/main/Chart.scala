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
    xAxis = Xaxis(answer.clusters.map(_.x).map(answer.question.dimension.valueName)),
    yAxis = answer.clusters.headOption.?? { c =>
      List(Yaxis(c.data.name, false), Yaxis(c.size.name, true))
    },
    series = answer.clusters.flatMap { c =>
      c.points.map { c -> _ }
    }.foldLeft(Map.empty[String, Serie]) {
      case (acc, (cluster, point)) => acc.updated(point.key,
        acc.get(point.key) match {
          case None    => Serie(point.name, point.isSize, List(point.y))
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
