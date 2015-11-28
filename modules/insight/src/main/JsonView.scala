package lila.insight

import play.api.libs.json._

final class JsonView {

  private def D = Dimension

  lazy val stringifiedUi = Json stringify {
    Json.obj(
      "dimensions" -> List(
        Json toJson D.Perf,
        Json toJson D.Phase,
        Json toJson D.Result,
        Json toJson D.Color,
        Json toJson D.Opening,
        Json toJson D.OpponentStrength,
        Json toJson D.PieceRole),
      "metrics" -> Metric.all)
  }

  private implicit def dimensionWriter[X]: OWrites[Dimension[X]] = OWrites { d =>
    Json.obj(
      "key" -> d.key,
      "name" -> d.name,
      "position" -> d.position,
      "values" -> Dimension.valuesOf(d).map(Dimension.valueToJson(d)))
  }

  private implicit def metricWriter: OWrites[Metric] = OWrites { m =>
    Json.obj(
      "key" -> m.key,
      "name" -> m.name,
      "position" -> m.position)
  }

  private implicit def positionWriter: Writes[Position] = Writes { p =>
    JsString(p.name)
  }

  object chart {
    private implicit val xAxisWrites = Json.writes[Chart.Xaxis]
    private implicit val yAxisWrites = Json.writes[Chart.Yaxis]
    private implicit val SerieWrites = Json.writes[Chart.Serie]
    private implicit val ChartWrites = Json.writes[Chart]

    def apply(c: Chart) = ChartWrites writes c
  }

  def question(metric: String, dimension: String, filters: String) = Json.obj(
    "metric" -> metric,
    "dimension" -> dimension,
    "filters" -> (filters.split('/').map(_ split ':').collect {
      case Array(key, values) => key -> JsArray(values.split(',').map(JsString.apply))
    }.toMap: Map[String, JsArray])
  )
}
