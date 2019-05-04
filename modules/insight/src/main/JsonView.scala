package lila.insight

import play.api.libs.json._

final class JsonView {

  import lila.insight.{ Dimension => D, Metric => M }

  case class Categ(name: String, items: List[JsValue])
  private implicit val categWrites = Json.writes[Categ]

  def ui(ecos: Set[String]) = {

    val openingJson = Json.obj(
      "key" -> D.Opening.key,
      "name" -> D.Opening.name,
      "position" -> D.Opening.position,
      "description" -> D.Opening.description.render,
      "values" -> Dimension.valuesOf(D.Opening).filter { o =>
        ecos contains o.eco
      }.map(Dimension.valueToJson(D.Opening))
    )

    val dimensionCategs = List(
      Categ("Setup", List(
        Json toJson D.Date,
        Json toJson D.Period,
        Json toJson D.Perf,
        Json toJson D.Color,
        Json toJson D.OpponentStrength
      )),
      Categ("Game", List(
        openingJson,
        Json toJson D.MyCastling,
        Json toJson D.OpCastling,
        Json toJson D.QueenTrade
      )),
      Categ("Move", List(
        Json toJson D.PieceRole,
        Json toJson D.MovetimeRange,
        Json toJson D.MaterialRange,
        Json toJson D.Phase
      )),
      Categ("Result", List(
        Json toJson D.Termination,
        Json toJson D.Result
      ))
    )

    Json.obj(
      "dimensionCategs" -> dimensionCategs,
      "metricCategs" -> metricCategs,
      "presets" -> Preset.all
    )
  }

  private val metricCategs = List(
    Categ("Setup", List(
      Json toJson M.OpponentRating
    )),
    Categ("Move", List(
      Json toJson M.Movetime,
      Json toJson M.PieceRole,
      Json toJson M.Material,
      Json toJson M.NbMoves
    )),
    Categ("Evaluation", List(
      Json toJson M.MeanCpl,
      Json toJson M.Opportunism,
      Json toJson M.Luck
    )),
    Categ("Result", List(
      Json toJson M.Termination,
      Json toJson M.Result,
      Json toJson M.RatingDiff
    ))
  )

  private implicit def presetWriter[X]: OWrites[Preset] = OWrites { p =>
    Json.obj(
      "name" -> p.name,
      "dimension" -> p.question.dimension.key,
      "metric" -> p.question.metric.key,
      "filters" -> JsObject(p.question.filters.map {
        case Filter(dimension, selected) =>
          dimension.key -> JsArray(selected.map(Dimension.valueKey(dimension)).map(JsString.apply))
      })
    )
  }

  private implicit def dimensionWriter[X]: OWrites[Dimension[X]] = OWrites { d =>
    Json.obj(
      "key" -> d.key,
      "name" -> d.name,
      "position" -> d.position,
      "description" -> d.description.render,
      "values" -> Dimension.valuesOf(d).map(Dimension.valueToJson(d))
    )
  }

  private implicit def metricWriter: OWrites[Metric] = OWrites { m =>
    Json.obj(
      "key" -> m.key,
      "name" -> m.name,
      "description" -> m.description.render,
      "position" -> m.position
    )
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
    }(scala.collection.breakOut): Map[String, JsArray])
  )
}
