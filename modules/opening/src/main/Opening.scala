package lila.opening

import chess.opening.{ FullOpening, FullOpeningDB }

import lila.common.SimpleOpening

object Opening {

  val shortestLines: Map[String, FullOpening] = FullOpeningDB.all
    .foldLeft(Map.empty[String, FullOpening]) { case (acc, op) =>
      acc.updatedWith(op.key) {
        case Some(prev) if prev.uci.size < op.uci.size => prev.some
        case _                                         => op.some
      }
    }

  def isShortest(op: FullOpening) = shortestLines.get(op.key).has(op)
}
