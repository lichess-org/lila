package lila.opening

import lila.common.LilaOpening

case class PopularOpenings(all: List[OpeningData]) {

  val byKey: Map[LilaOpening.Key, OpeningData] = all.view.map { d => d.key -> d }.toMap

  type Move     = String
  type TreeMap  = Map[Move, List[OpeningData]]
  type TreeList = List[(Move, List[OpeningData])]

  // only keeps openings with at least two moves
  val treeMap: TreeMap =
    all.filter(_.opening.ref.uci.count(' ' ==) > 0).foldLeft(Map.empty: TreeMap) { case (tree, d) =>
      d.opening.ref.pgn.split(' ').drop(1).headOption.fold(tree) { move =>
        tree.updatedWith(move) { ops =>
          (d :: ~ops).some
        }
      }
    }

  val treeList: TreeList =
    treeMap.view
      .mapValues {
        _.toList.sortBy(-_.nbGames)
      }
      .toList
      .sortBy(-_._2.map(_.nbGames).sum)

  val (treeByMove, treeOthers) = treeList.partition(_._2.size > 3) match {
    case (byMove, others) => byMove -> others.flatMap(_._2).sortBy(-_.nbGames)
  }
}
