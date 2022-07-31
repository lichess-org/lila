package lila.opening

import lila.common.LilaOpeningFamily
import play.api.libs.json.{ Json, Writes }

case class OpeningFamilyData(fam: LilaOpeningFamily, history: List[OpeningHistorySegment[Long]]) {

  lazy val nbGames = history.map(_.sum).sum

  def filterNotEmpty = copy(history = history.filter(!_.isEmpty))
}

object OpeningFamilyData {

  case class WithAll(data: OpeningFamilyData, all: List[OpeningHistorySegment[Long]]) {
    def percent = data.history zip all map { case (mine, all) =>
      mine.copy(
        black = (mine.black.toDouble * 100 / all.sum).toFloat,
        draws = (mine.draws.toDouble * 100 / all.sum).toFloat,
        white = (mine.white.toDouble * 100 / all.sum).toFloat
      )
    }
  }

  import OpeningHistory.segmentJsonWrite
  implicit def familyJsonWrite = Writes[LilaOpeningFamily] { f =>
    Json.obj(
      "name" -> f.full.name
    )
  }
}

case class FamilyDataCollection(all: List[OpeningFamilyData]) {

  val byKey: Map[LilaOpeningFamily.Key, OpeningFamilyData] = all.view.map { d => d.fam.key -> d }.toMap
}

private object FamilyDataCollection {
  import reactivemongo.api.bson._
  import lila.db.dsl._
  implicit def historySegmentHandler = Macros.handler[OpeningHistorySegment[Long]]
  implicit val familyHandler         = Macros.handler[OpeningFamilyData]
  implicit val collectionHandler     = Macros.handler[FamilyDataCollection]
}
