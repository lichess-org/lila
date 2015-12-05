package lila.insight

import play.api.libs.json._

final class JsonView {

  private def D = Dimension

  case class Categ(key: String, name: String, items: List[JsValue])
  private implicit val categWrites = Json.writes[Categ]

  def ui(ecos: Set[String]) = {

    val openingJson = Json.obj(
      "key" -> D.Opening.key,
      "name" -> D.Opening.name,
      "position" -> D.Opening.position,
      "description" -> D.Opening.description.body,
      "values" -> Dimension.valuesOf(D.Opening).filter { o =>
        ecos contains o.eco
      }.map(Dimension.valueToJson(D.Opening)))

    Json.obj(
      "dimensionCategs" -> List(
        Categ("setup", "Setup", List(
          Json toJson D.Perf,
          Json toJson D.Color,
          Json toJson D.OpponentStrength
        )),
        //game
        Categ("game", "Game", List(
          openingJson,
          Json toJson D.MyCastling,
          Json toJson D.OpCastling,
          Json toJson D.QueenTrade
        )),
        // move
        Categ("move", "Move", List(
          Json toJson D.PieceRole,
          Json toJson D.MovetimeRange,
          Json toJson D.MaterialRange,
          Json toJson D.Phase
        )),
        // result
        Categ("result", "Result", List(
          Json toJson D.Termination,
          Json toJson D.Result))
      ),
      "metrics" -> Metric.all)
  }

  private implicit def dimensionWriter[X]: OWrites[Dimension[X]] = OWrites { d =>
    Json.obj(
      "key" -> d.key,
      "name" -> d.name,
      "position" -> d.position,
      "description" -> d.description.body,
      "values" -> Dimension.valuesOf(d).map(Dimension.valueToJson(d)))
  }

  private implicit def metricWriter: OWrites[Metric] = OWrites { m =>
    Json.obj(
      "key" -> m.key,
      "name" -> m.name,
      "description" -> m.description.body,
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
