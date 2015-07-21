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
      "families" -> familiesOf(stat.openings(color)),
      "moves" -> Json.toJson(stat.openings(color).m.keys.flatMap { familyName =>
        chess.Openings.familyMoveList get familyName map formatMoves map familyName.->
      }.toMap)
    )
  }

  private def formatMoves(moves: List[String]): String = moves.grouped(2).zipWithIndex.map {
    case (List(w, b), i) => s"${i + 1}. $w $b"
    case (List(w), i)    => s"${i + 1}. $w"
    case _               => ""
  }.mkString(" ")

  private def familiesOf(opMap: Openings.OpeningsMap) =
    opMap.m.foldLeft(Map[String, List[String]]()) {
      case (acc, (family, _)) => chess.Openings.familyFirstMove.get(family).fold(acc) { firstMove =>
        acc + (firstMove -> (family :: acc.getOrElse(firstMove, Nil)))
      }
    }.map {
      case (firstMove, families) => OpeningFamily(
        firstMove = firstMove,
        results = families.foldLeft(Results.empty) {
          case (results, familyName) => opMap.m.get(familyName).fold(results)(results.merge)
        },
        families = families)
    }.toList
}
