package lila.insight

import play.api.i18n.Lang
import play.api.libs.json._
import lila.common.{ LilaOpening, LilaOpeningFamily }

final class JsonView {

  import lila.insight.{ InsightDimension => D, Metric => M }
  import writers._

  case class Categ(name: String, items: List[JsValue])
  implicit private val categWrites = Json.writes[Categ]

  def ui(families: List[LilaOpeningFamily], openings: List[LilaOpening], asMod: Boolean)(implicit
      lang: Lang
  ) = {

    val openingFamilyJson = Json.obj(
      "key"         -> D.OpeningFamily.key,
      "name"        -> D.OpeningFamily.name,
      "position"    -> D.OpeningFamily.position,
      "description" -> D.OpeningFamily.description,
      "values"      -> families.map(InsightDimension.valueToJson(D.OpeningFamily))
    )

    val openingJson = Json.obj(
      "key"         -> D.OpeningVariation.key,
      "name"        -> D.OpeningVariation.name,
      "position"    -> D.OpeningVariation.position,
      "description" -> D.OpeningVariation.description,
      "values"      -> openings.map(InsightDimension.valueToJson(D.OpeningVariation))
    )

    val dimensionCategs = List(
      Categ(
        "Setup",
        List(
          Json.toJson(D.Date: InsightDimension[_]),
          Json.toJson(D.Period: InsightDimension[_]),
          Json.toJson(D.Perf: InsightDimension[_]),
          Json.toJson(D.Color: InsightDimension[_]),
          Json.toJson(D.OpponentStrength: InsightDimension[_])
        )
      ),
      Categ(
        "Game",
        List(
          openingFamilyJson,
          openingJson,
          Json.toJson(D.MyCastling: InsightDimension[_]),
          Json.toJson(D.OpCastling: InsightDimension[_]),
          Json.toJson(D.QueenTrade: InsightDimension[_])
        )
      ),
      Categ(
        "Move",
        List(
          Json.toJson(D.PieceRole: InsightDimension[_]),
          Json.toJson(D.MovetimeRange: InsightDimension[_]),
          Json.toJson(D.MaterialRange: InsightDimension[_]),
          Json.toJson(D.EvalRange: InsightDimension[_]),
          Json.toJson(D.Phase: InsightDimension[_]),
          Json.toJson(D.CplRange: InsightDimension[_])
        ) ::: {
          asMod ?? List(
            Json.toJson(D.Blur: InsightDimension[_]),
            Json.toJson(D.TimeVariance: InsightDimension[_])
          )
        }
      ),
      Categ(
        "Result",
        List(
          Json.toJson(D.Termination: InsightDimension[_]),
          Json.toJson(D.Result: InsightDimension[_])
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
            dimension.key -> JsArray(selected.map(InsightDimension.valueKey(dimension)).map(JsString.apply))
          })
        )
      }

    implicit def dimensionWriter[X](implicit lang: Lang): Writes[InsightDimension[X]] =
      Writes { d =>
        Json.obj(
          "key"         -> d.key,
          "name"        -> d.name,
          "position"    -> d.position,
          "description" -> d.description,
          "values"      -> InsightDimension.valuesOf(d).map(InsightDimension.valueToJson(d))
        )
      }

    implicit val metricWriter: Writes[Metric] = Writes { m =>
      Json.obj(
        "key"         -> m.key,
        "name"        -> m.name,
        "description" -> m.description,
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
