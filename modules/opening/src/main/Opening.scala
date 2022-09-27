package lila.opening

import chess.opening.{ FullOpening, FullOpeningDB }

object Opening {

  type Key = String

  val shortestLines: Map[Key, FullOpening] = FullOpeningDB.all
    .foldLeft(Map.empty[Key, FullOpening]) { case (acc, op) =>
      acc.updatedWith(op.key) {
        case Some(prev) if prev.uci.size < op.uci.size => prev.some
        case _                                         => op.some
      }
    }

  def isShortest(op: FullOpening) = shortestLines.get(op.key).has(op)

  /*
   * Given 2 opening names separated by a move,
   * shorten the next name to avoid repeating what's in the prev one.
   * Easy example:
   * "Mieses Opening" -> "Mieses Opening: Reversed Rat" -> "Reversed Rat"
   * For harder ones, see modules/opening/src/test/OpeningTest.scala
   */
  def variationName(prev: String, next: String): Option[String] = {
    val name =
      prev
        .zipAll(next, ' ', ' ')
        .dropWhile { case (a, b) => a == b }
        .map(_._2)
        .mkString
        .dropWhile(Set(' ', ':', ',', '-').contains _)
    name.nonEmpty option name
  }
}
