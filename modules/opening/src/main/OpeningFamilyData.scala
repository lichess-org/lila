package lila.opening

import lila.common.LilaOpeningFamily
import play.api.libs.json.{ Json, Writes }
import chess.format.pgn.San

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
    Json.obj("name" -> f.full.name, "key" -> f.key.value)
  }
  implicit def familyDataWrite = Json.writes[OpeningFamilyData]
}

case class FamilyDataCollection(all: List[OpeningFamilyData]) {

  val byKey: Map[LilaOpeningFamily.Key, OpeningFamilyData] = all.view.map { d => d.fam.key -> d }.toMap

  type Move     = String
  type TreeMap  = Map[Move, Map[Option[Move], List[OpeningFamilyData]]]
  type TreeList = List[(Move, List[(Option[Move], List[OpeningFamilyData])])]

  val treeMap: TreeMap =
    all.foldLeft(Map.empty: TreeMap) { case (tree, d) =>
      d.fam.full.pgn.split(' ').drop(1).take(2).toList match {
        case first :: rest =>
          tree.updatedWith(first) { subTree =>
            val second = rest.headOption
            (~subTree).updatedWith(second)(ops => (d :: ~ops).some).some
          }
        case Nil => tree
      }
    }

  val treeList: TreeList =
    treeMap.view
      .mapValues {
        _.toList.sortBy { case (second, ops) =>
          (second.isDefined ?? 1, -ops.map(_.nbGames).sum)
        }
      }
      .toList
      .sortBy {
        -_._2.map(_._2.map(_.nbGames).sum).sum
      }
}

private object FamilyDataCollection {
  import reactivemongo.api.bson._
  import lila.db.dsl._
  implicit def historySegmentHandler = Macros.handler[OpeningHistorySegment[Long]]
  implicit val familyHandler         = Macros.handler[OpeningFamilyData]
  implicit val collectionHandler     = Macros.handler[FamilyDataCollection]
}
