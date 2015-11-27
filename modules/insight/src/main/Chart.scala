package lila.insight

import play.api.libs.json._

case class Chart(
  xAxis: Chart.Xaxis,
  valueYaxis: Chart.Yaxis,
  sizeYaxis: Chart.Yaxis,
  series: List[Chart.Serie],
  sizeSerie: Chart.Serie)

object Chart {

  case class Xaxis(
    name: String,
    categories: List[String])

  case class Yaxis(
    name: String,
    dataType: String)

  case class Serie(
    name: String,
    dataType: String,
    stack: Option[String],
    data: List[Double])

  def fromAnswer[X](answer: Answer[X]): Chart = {

    import answer._, question._

    def xAxis = Xaxis(
      name = dimension.name,
      categories = clusters.map(_.x).map(dimension.valueName))

    def sizeSerie = Serie(
      name = metric.position.tellNumber,
      dataType = Metric.DataType.Count.name,
      stack = none,
      data = clusters.map(_.size.toDouble))

    def series = clusters.foldLeft(Map.empty[String, Serie]) {
      case (acc, cluster) =>
        // val clusterName = dimension valueName cluster.x
        cluster.insight match {
          case Insight.Single(point) =>
            val key = metric.name
            acc.updated(key, acc.get(key) match {
              case None => Serie(
                name = metric.name,
                dataType = metric.dataType.name,
                stack = none,
                data = List(point.y))
              case Some(s) => s.copy(data = point.y :: s.data)
            })
          case Insight.Stacked(points) => points.foldLeft(acc) {
            case (acc, (metricValueName, point)) =>
              val key = s"${metric.name}/${metricValueName.name}"
              acc.updated(key, acc.get(key) match {
                case None => Serie(
                  name = metricValueName.name,
                  dataType = metric.dataType.name,
                  stack = metric.name.some,
                  data = List(point.y))
                case Some(s) => s.copy(data = point.y :: s.data)
              })
          }
        }
    }.map {
      case (_, serie) => serie.copy(data = serie.data.reverse)
    }.toList

    Chart(
      xAxis = xAxis,
      valueYaxis = Yaxis(metric.name, metric.dataType.name),
      sizeYaxis = Yaxis(metric.position.tellNumber, Metric.DataType.Count.name),
      series = series,
      sizeSerie = sizeSerie)
  }
}
