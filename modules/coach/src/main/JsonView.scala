package lila.coach

import play.api.libs.json._

final class JsonView {

  private def D = Dimension

  lazy val stringifiedUi = Json stringify {
    Json.obj(
      "dimensions" -> Json.obj(
        D.Perf.key -> D.Perf,
        D.Phase.key -> D.Phase,
        D.Result.key -> D.Result,
        D.Color.key -> D.Color,
        D.Opening.key -> D.Opening,
        D.OpponentStrength.key -> D.OpponentStrength,
        D.PieceRole.key -> D.PieceRole),
      "metrics" -> Metric.all)
  }

  private implicit def dimensionWriter[X]: OWrites[Dimension[X]] = OWrites { d =>
    Json.obj(
      "key" -> d.key,
      "name" -> d.name,
      "values" -> Dimension.valuesOf(d).map(Dimension.valueToJson(d)))
  }

  private implicit def metricWriter: OWrites[Metric] = OWrites { m =>
    Json.obj("key" -> m.key, "name" -> m.name)
  }
}
