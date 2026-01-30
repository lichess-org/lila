package lila.insight

import play.api.libs.json.*

import lila.common.{ LilaOpeningFamily, SimpleOpening }
import lila.core.i18n.Translate

final class JsonView:

  import lila.insight.{ InsightDimension as D, InsightMetric as M }

  case class Categ(name: String, items: List[JsValue])
  private given Writes[Categ] = Json.writes

  def ui(families: List[LilaOpeningFamily], openings: List[SimpleOpening], asMod: Boolean)(using Translate) =

    val openingFamilyJson = Json.obj(
      "key" -> D.OpeningFamily.key,
      "name" -> D.OpeningFamily.name,
      "position" -> D.OpeningFamily.position,
      "description" -> D.OpeningFamily.description,
      "values" -> families.map(InsightDimension.valueToJson(D.OpeningFamily))
    )

    val openingJson = Json.obj(
      "key" -> D.OpeningVariation.key,
      "name" -> D.OpeningVariation.name,
      "position" -> D.OpeningVariation.position,
      "description" -> D.OpeningVariation.description,
      "values" -> openings.map(InsightDimension.valueToJson(D.OpeningVariation))
    )

    val dimensionCategs = List(
      Categ(
        "Setup",
        List(
          dimWrites.writes(D.Date),
          dimWrites.writes(D.Period),
          dimWrites.writes(D.Perf),
          dimWrites.writes(D.Color),
          dimWrites.writes(D.OpponentStrength),
          dimWrites.writes(D.GameSource)
        )
      ),
      Categ(
        "Game",
        List(
          openingFamilyJson,
          openingJson,
          dimWrites.writes(D.MyCastling),
          dimWrites.writes(D.OpCastling),
          dimWrites.writes(D.QueenTrade)
        )
      ),
      Categ(
        "Move",
        asMod.so {
          List(
            dimWrites.writes(D.Blur),
            dimWrites.writes(D.TimeVariance),
            dimWrites.writes(D.EvalRange),
            dimWrites.writes(D.CplRange)
          )
        } ::: List(
          dimWrites.writes(D.PieceRole),
          dimWrites.writes(D.MovetimeRange),
          dimWrites.writes(D.ClockPercentRange),
          dimWrites.writes(D.MaterialRange),
          dimWrites.writes(D.AccuracyPercentRange),
          dimWrites.writes(D.WinPercentRange),
          dimWrites.writes(D.Phase)
        )
      ),
      Categ(
        "Result",
        List(
          dimWrites.writes(D.Termination),
          dimWrites.writes(D.Result)
        )
      )
    )

    val metricCategs = List(
      Categ(
        "Setup",
        List(
          Json.toJson(M.OpponentRating: InsightMetric)
        )
      ),
      Categ(
        "Move",
        asMod.so {
          List(
            Json.toJson(M.Blurs: InsightMetric),
            Json.toJson(M.TimeVariance: InsightMetric)
          )
        } ::: List(
          Json.toJson(M.Movetime: InsightMetric),
          Json.toJson(M.ClockPercent: InsightMetric),
          Json.toJson(M.PieceRole: InsightMetric),
          Json.toJson(M.Material: InsightMetric),
          Json.toJson(M.NbMoves: InsightMetric)
        )
      ),
      Categ(
        "Evaluation",
        List(
          Json.toJson(M.MeanAccuracy: InsightMetric),
          Json.toJson(M.MeanCpl: InsightMetric),
          Json.toJson(M.CplBucket: InsightMetric),
          Json.toJson(M.Awareness: InsightMetric),
          Json.toJson(M.Luck: InsightMetric)
        )
      ),
      Categ(
        "Result",
        List(
          Json.toJson(M.Termination: InsightMetric),
          Json.toJson(M.Result: InsightMetric),
          Json.toJson(M.Performance: InsightMetric),
          Json.toJson(M.RatingDiff: InsightMetric)
        )
      )
    )

    Json
      .obj(
        "dimensionCategs" -> dimensionCategs,
        "metricCategs" -> metricCategs
      )
      .add("asMod" -> asMod)

  given Writes[InsightPosition] = Writes(p => JsString(p.name))

  private given dimWrites[X](using Translate): Writes[InsightDimension[X]] =
    Writes: d =>
      Json.obj(
        "key" -> d.key,
        "name" -> d.name,
        "position" -> d.position,
        "description" -> d.description,
        "values" -> InsightDimension.valuesOf(d).map(InsightDimension.valueToJson(d))
      )

  given Writes[InsightMetric] = Writes: m =>
    Json.obj(
      "key" -> m.key,
      "name" -> m.name,
      "description" -> m.description,
      "position" -> m.position
    )

  private given Writes[InsightChart.Xaxis] = Json.writes
  private given Writes[InsightChart.Yaxis] = Json.writes
  private given Writes[InsightChart.Serie] = Json.writes
  given chartWrites: Writes[InsightChart] = Json.writes

  def question(metric: String, dimension: String, filters: String) =
    Json.obj(
      "metric" -> metric,
      "dimension" -> dimension,
      "filters" -> (filters
        .split('/')
        .view
        .map(_.split(':'))
        .collect { case Array(key, values) =>
          key -> JsArray(values.split(',').map(JsString.apply))
        }
        .toMap: Map[String, JsArray])
    )
