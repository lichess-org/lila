package lila.coach

import play.api.libs.json._

final class JsonView {

  import JSONWriters._

  def raw(stat: UserStat): Fu[JsObject] = fuccess {
    UserStatWriter writes stat
  }

  def opening(stat: UserStat): Fu[JsObject] = fuccess {
    OpeningApiDataWriter writes OpeningApiData(
      results = stat.results.base,
      colorResults = stat.colorResults,
      openings = stat.openings,
      families = OpeningFamilies(
        white = familiesOf(stat.openings.white),
        black = familiesOf(stat.openings.black)
      )
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
