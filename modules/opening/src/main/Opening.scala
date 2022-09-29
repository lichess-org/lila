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
  def variationName(prev: String, next: String): String =
    sectionsOf(prev)
      .zipAll(sectionsOf(next), "", "")
      .dropWhile { case (a, b) => a == b }
      .headOption
      .map(_._2)
      .filter(_.nonEmpty)
      .orElse(sectionsOf(next).lastOption)
      .getOrElse(next.takeWhile(':' !=))

  def variationName(prev: Option[FullOpening], next: Option[FullOpening]): Option[String] =
    (prev, next) match {
      case (Some(p), Some(n)) => variationName(p.name, n.name).some
      case (None, Some(n))    => n.family.name.some
      case _                  => none
    }

  def sectionsOf(openingName: String): List[String] =
    openingName.split(":", 2) match {
      case Array(f, v) => f :: v.split(",").toList.map(_.trim)
      case _           => openingName :: Nil
    }
}
