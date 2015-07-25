package lila.coach

import play.api.libs.json._

final class JsonView(jsonWriters: JSONWriters) {

  import jsonWriters._

  def raw(stat: UserStat): Fu[JsObject] = fuccess {
    UserStatWriter writes stat
  }

  def opening(period: Period, color: chess.Color): Fu[JsObject] = fuccess {
    val stat = period.data
    val openings = stat.openings(color).trim
    Json.obj(
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
}
