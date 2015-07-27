package lila.coach

import play.api.libs.json._

final class JsonView(jsonWriters: JSONWriters) {

  import jsonWriters._

  def opening(period: Period, color: chess.Color): Fu[JsObject] = fuccess {
    val stat = period.data
    val openings = stat.openings(color).trim
    Json.obj(
      "from" -> period.from,
      "to" -> period.to,
      "color" -> color.name,
      "results" -> stat.results.base,
      "colorResults" -> stat.colorResults(color),
      "openings" -> openings.m.map {
        case (eco, results) => eco -> Json.obj(
          "opening" -> EcopeningDB.allByEco.get(eco),
          "results" -> results)
      },
      "openingResults" -> openings.results,
      "families" -> Ecopening.makeFamilies {
        openings.m.keys.flatMap(EcopeningDB.allByEco.get)
      }.values.toList.sortBy(-_.ecos.size).map { fam =>
        Json.obj(
          "family" -> fam,
          "results" -> fam.ecos.flatMap(openings.m.get).foldLeft(Results.empty) {
            (res, oRes) => res merge oRes
          })
      }
    )
  }

  def move(period: Period): Fu[JsObject] = fuccess {
    val stat = period.data
    Json.obj(
      "from" -> period.from,
      "to" -> period.to,
      "perfs" -> (Json.obj(
        "perf" -> Json.obj(
          "key" -> "global",
          "name" -> "Global",
          "icon" -> "C"),
        "results" -> stat.results
      ) :: lila.rating.PerfType.nonPuzzle.flatMap { pt =>
          stat.perfResults.m.get(pt) map { results =>
            Json.obj(
              "perf" -> pt,
              "results" -> results)
          }
        })
    )
  }
}
