package lila.insight

import play.api.i18n.Lang
import play.api.libs.json._

final class JsonView {

  import lila.insight.{ Dimension => D, Metric => M }
  import writers._

  case class Categ(name: String, items: List[JsValue])
  implicit private val categWrites = Json.writes[Categ]

  def ui(ecos: Set[String], asMod: Boolean)(implicit lang: Lang) = {

    val openingJson = Json.obj(
      "key"         -> D.Opening.key,
      "name"        -> D.Opening.name,
      "position"    -> D.Opening.position,
      "description" -> D.Opening.description.render,
      "values" -> Dimension
        .valuesOf(D.Opening)
        .filter { o =>
          ecos contains o.eco
        }
        .map(Dimension.valueToJson(D.Opening))
    )

    val dimensionCategs = List(
      Categ(
        "Setup",
        List(
          Json.toJson(D.Date: Dimension[_]),
          Json.toJson(D.Period: Dimension[_]),
          Json.toJson(D.Perf: Dimension[_]),
          Json.toJson(D.Color: Dimension[_]),
          Json.toJson(D.OpponentStrength: Dimension[_])
        )
      ),
      Categ(
        "Game",
        List(
          openingJson,
          Json.toJson(D.MyCastling: Dimension[_]),
          Json.toJson(D.OpCastling: Dimension[_]),
          Json.toJson(D.QueenTrade: Dimension[_])
        )
      ),
      Categ(
        "Move",
        List(
          Json.toJson(D.PieceRole: Dimension[_]),
          Json.toJson(D.MovetimeRange: Dimension[_]),
          Json.toJson(D.MaterialRange: Dimension[_]),
          Json.toJson(D.Phase: Dimension[_]),
          Json.toJson(D.CplRange: Dimension[_])
        ) ::: {
          asMod ?? List(Json.toJson(D.Blur: Dimension[_]), Json.toJson(D.TimeVariance: Dimension[_]))
        }
      ),
      Categ(
        "Result",
        List(
          Json.toJson(D.Termination: Dimension[_]),
          Json.toJson(D.Result: Dimension[_])
        )
      )
    )

    val metricCategs = List(
      Categ(
        "Setup",
        List(
          Json.toJson(M.OpponentRating: Metric)
        )
      ),
      Categ(
        "Move",
        List(
          Json.toJson(M.Movetime: Metric),
          Json.toJson(M.PieceRole: Metric),
          Json.toJson(M.Material: Metric),
          Json.toJson(M.NbMoves: Metric)
        ) ++ {
          asMod ?? List(
            Json.toJson(M.Blurs: Metric),
            Json.toJson(M.TimeVariance: Metric)
          )
        }
      ),
      Categ(
        "Evaluation",
        List(
          Json.toJson(M.MeanCpl: Metric),
          Json.toJson(M.CplBucket: Metric),
          Json.toJson(M.Opportunism: Metric),
          Json.toJson(M.Luck: Metric)
        )
      ),
      Categ(
        "Result",
        List(
          Json.toJson(M.Termination: Metric),
          Json.toJson(M.Result: Metric),
          Json.toJson(M.RatingDiff: Metric)
        )
      )
    )

    Json.obj(
      "dimensionCategs" -> dimensionCategs,
      "metricCategs"    -> metricCategs,
      "presets"         -> { if (asMod) Preset.forMod else Preset.base }
    )
  }

  private object writers {

    implicit def presetWriter[X]: Writes[Preset] =
      Writes { p =>
        Json.obj(
          "name"      -> p.name,
          "dimension" -> p.question.dimension.key,
          "metric"    -> p.question.metric.key,
          "filters" -> JsObject(p.question.filters.map { case Filter(dimension, selected) =>
            dimension.key -> JsArray(selected.map(Dimension.valueKey(dimension)).map(JsString.apply))
          })
        )
      }

    implicit def dimensionWriter[X](implicit lang: Lang): Writes[Dimension[X]] =
      Writes { d =>
        Json.obj(
          "key"         -> d.key,
          "name"        -> d.name,
          "position"    -> d.position,
          "description" -> d.description.render,
          "values"      -> Dimension.valuesOf(d).map(Dimension.valueToJson(d))
        )
      }

    implicit val metricWriter: Writes[Metric] = Writes { m =>
      Json.obj(
        "key"         -> m.key,
        "name"        -> m.name,
        "description" -> m.description.render,
        "position"    -> m.position
      )
    }

    implicit val positionWriter: Writes[Position] = Writes { p =>
      JsString(p.name)
    }
  }

  object chart {
    implicit private val xAxisWrites = Json.writes[Chart.Xaxis]
    implicit private val yAxisWrites = Json.writes[Chart.Yaxis]
    implicit private val SerieWrites = Json.writes[Chart.Serie]
    implicit private val ChartWrites = Json.writes[Chart]

    def apply(c: Chart) = ChartWrites writes c
  }

  def question(metric: String, dimension: String, filters: String) =
    Json.obj(
      "metric"    -> metric,
      "dimension" -> dimension,
      "filters" -> (filters
        .split('/')
        .view
        .map(_ split ':')
        .collect { case Array(key, values) =>
          key -> JsArray(values.split(',').map(JsString.apply))
        }
        .toMap: Map[String, JsArray])
    )
}
