package lila.opening

import chess.opening.{ Opening, OpeningName }

opaque type NameSection = String
object NameSection extends OpaqueString[NameSection]:
  /*
   * Given 2 opening names separated by a move,
   * shorten the next name to avoid repeating what's in the prev one.
   * Easy example:
   * "Mieses Opening" -> "Mieses Opening: Reversed Rat" -> "Reversed Rat"
   * For harder ones, see modules/opening/src/test/OpeningTest.scala
   */
  private[opening] def variationName(prev: OpeningName, next: OpeningName): NameSection =
    sectionsOf(prev).toList
      .zipAll(sectionsOf(next).toList, "", "")
      .dropWhile { (a, b) => a == b }
      .headOption
      ._2F
      .filter(_.nonEmpty)
      .getOrElse(sectionsOf(next).last)

  def variationName(prev: Option[Opening], next: Option[Opening]): Option[NameSection] =
    (prev, next) match
      case (Some(p), Some(n)) => variationName(p.name, n.name).some
      case (None, Some(n)) => n.family.name.into(NameSection).some
      case _ => none

  def sectionsOf(openingName: OpeningName): NonEmptyList[NameSection] =
    openingName.value.split(":", 2) match
      case Array(f, v) => NonEmptyList(f, v.split(",").toList.map(_.trim))
      case _ => NonEmptyList(openingName.into(NameSection), Nil)
