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
  type TreeList = List[(Move, List[(LilaOpeningFamily, Int, List[OpeningData])])]

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
      .map { case (first, sub) =>
        (
          first,
          sub.view
            .mapValues(_.sortBy(-_.nbGames))
            .toList
            .map { case (fam, ops) =>
              // if (Set("Kings_Pawn_Game", "Sicilian_Defense")(fam.key.value)) {
              if (Set("Kings_Pawn_Game")(fam.key.value)) {
                val famOp = ops.find(_.opening.isFamily)
                println(first)
                println(s"${fam.key} ${ops.size} ${famOp.map(_.opening.key)} ${famOp
                    .map(_.opening.ref.pgn)} ${famOp.map(_.nbGames)}")
                println(ops.headOption.map(_.opening))
              }
              (
                fam,
                ops.find(_.opening.isFamily).fold(ops.map(_.nbGames).sum)(_.nbGames),
                ops.filterNot(_.opening.isFamily)
              )
            }
            .sortBy(-_._2)
        )
      }
      .toList
      .sortBy(-_._2.map(_._2).sum)

  val (treeByMove, treeOthers) = treeList.partition(_._2.map(_._3.size).sum > 10) match {
    case (byMove, others) => byMove -> others.flatMap(_._2).sortBy(-_._2)
  }
}
