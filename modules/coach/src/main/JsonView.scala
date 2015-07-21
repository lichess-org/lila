package lila.coach

import play.api.libs.json._

final class JsonView {

  import JSONWriters._

  def raw(stat: UserStat): Fu[JsObject] = fuccess {
    UserStatWriter writes stat
  }

  def opening(stat: UserStat, color: chess.Color): Fu[JsObject] = fuccess {
    Json.obj(
      "color" -> color.name,
      "results" -> stat.results.base,
      "colorResults" -> stat.colorResults(color),
      "openings" -> stat.openings(color),
      "families" -> familiesOf(stat.openings(color))
    )
  }

  private def familiesOf(opMap: Openings.OpeningsMap) =
    opMap.m.foldLeft(Map[String, List[String]]()) {
      case (acc, (family, _)) => chess.Openings.familyFirstMove.get(family).fold(acc) { firstMove =>
        acc + (firstMove -> (family :: acc.getOrElse(firstMove, Nil)))
      }
    }.map {
      case (firstMove, families) => OpeningFamily(firstMove, families)
    }.toList
}
