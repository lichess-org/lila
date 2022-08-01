package lila.opening

import lila.common.LilaOpening
import lila.common.LilaOpeningFamily

case class PopularOpenings(all: List[OpeningData]) {

  val byKey: Map[LilaOpening.Key, OpeningData] =
    all.view.map { d =>
      d.key -> d
    }.toMap

  type Move     = String
  type TreeMap  = Map[Move, Map[LilaOpeningFamily, List[OpeningData]]]
  type TreeList = List[(Move, List[(LilaOpeningFamily, List[OpeningData])])]

  // only keeps openings with at least two moves
  val treeMap: TreeMap =
    all
      .filter { op =>
        op.opening.nbMoves > 1
      }
      .foldLeft(Map.empty: TreeMap) { case (tree, d) =>
        d.opening.ref.pgn.split(' ').drop(1).headOption.fold(tree) { move =>
          tree.updatedWith(move) { subTree =>
            (~subTree)
              .updatedWith(d.opening.family) { ops =>
                (d :: ~ops).some
              }
              .some
          }
        }
      }

  val treeList: TreeList =
    treeMap.view
      .mapValues {
        _.view
          .mapValues(_.sortBy(-_.nbGames))
          .toList
          .sortBy(-_._2.map(_.nbGames).sum)
      }
      .toList
      .sortBy(-_._2.map(_._2.map(_.nbGames).sum).sum)

  val (treeByMove, treeOthers) = treeList.partition(_._2.map(_._2.size).sum > 10) match {
    case (byMove, others) => byMove -> others.flatMap(_._2).sortBy(-_._2.map(_.nbGames).sum)
  }
}
