package lila.coach

import play.api.libs.json._

final class JsonView(jsonWriters: JSONWriters) {

  import jsonWriters._

  def raw(stat: UserStat): Fu[JsObject] = fuccess {
    UserStatWriter writes stat
  }

  def opening(stat: UserStat, color: chess.Color): Fu[JsObject] = fuccess {
    Json.obj(
      "color" -> color.name,
      "results" -> stat.results.base,
      "colorResults" -> stat.colorResults(color),
      "openings" -> stat.openings(color).m.map {
        case (eco, results) => eco -> Json.obj(
          "opening" -> EcopeningDB.allByEco.get(eco),
          "results" -> results)
      },
      "openingNbGames" -> stat.openings(color).nbGames,
      "families" -> Ecopening.makeFamilies {
        stat.openings(color).m.keys.flatMap(EcopeningDB.allByEco.get)
      }.values.toList.sortBy(-_.ecos.size).map { fam =>
        Json.obj(
          "family" -> fam,
          "results" -> fam.ecos.flatMap(stat.openings(color).m.get).foldLeft(Results.empty) {
            (res, oRes) => res merge oRes
          })
      }
    )
  }
}
