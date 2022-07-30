package lila.opening

import lila.common.LilaOpeningFamily
import play.api.libs.json.{ Json, Writes }

case class OpeningFamilyData(fam: LilaOpeningFamily, history: Option[OpeningHistory])

object OpeningFamilyData {
  import OpeningHistory.historyJsonWrite
  implicit def familyJsonWrite = Writes[LilaOpeningFamily] { f =>
    Json.obj(
      "name" -> f.full.name
    )
  }
  private def familyDataJsonWrite = Json.writes[OpeningFamilyData]
}
