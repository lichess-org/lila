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
      case (acc, (code, _)) => chess.Openings.codeFamily.get(code).fold(acc) { family =>
        acc + (family -> (code :: acc.getOrElse(family, Nil)))
      }
    }.map {
      case (family, codes) => OpeningFamily(family, codes)
    }.toList
}
