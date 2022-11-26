package lila.opening

import chess.opening.{ FullOpening, FullOpeningDB }
import cats.data.NonEmptyList

object Opening:

  type NameSection = String
  type PgnMove     = String

  case class Tree(children: List[(Tree.NameOrOpening, Tree)])

  object Tree:

    type NameOrOpening = (NameSection, Option[FullOpening])

    private val emptyNode = TreeNode(Map.empty)

    private case class TreeNode(children: Map[NameOrOpening, TreeNode]):
      def update(path: List[NameOrOpening]): TreeNode = path match
        case Nil         => this
        case last :: Nil => copy(children = children.updatedWith(last)(_ orElse emptyNode.some))
        case p :: rest =>
          copy(children = children.updatedWith(p)(node => (node | emptyNode).update(rest).some))

      def toTree: Tree = Tree(
        children.toList
          .sortBy(_._1._1)
          .map { case (op, node) =>
            (op, node.toTree)
          }
      )

    lazy val compute: Tree =
      FullOpeningDB.shortestLines.values
        .map { op =>
          val sections = Opening.sectionsOf(op.name)
          sections.toList.zipWithIndex map { case (name, i) =>
            (name, FullOpeningDB.shortestLines.get(FullOpening.nameToKey(sections.take(i + 1).mkString("_"))))
          }
        }
        .toList
        .foldLeft(emptyNode)(_ update _)
        .toTree

  /*
   * Given 2 opening names separated by a move,
   * shorten the next name to avoid repeating what's in the prev one.
   * Easy example:
   * "Mieses Opening" -> "Mieses Opening: Reversed Rat" -> "Reversed Rat"
   * For harder ones, see modules/opening/src/test/OpeningTest.scala
   */
  private[opening] def variationName(prev: String, next: String): NameSection =
    sectionsOf(prev).toList
      .zipAll(sectionsOf(next).toList, "", "")
      .dropWhile { case (a, b) => a == b }
      .headOption
      .map(_._2)
      .filter(_.nonEmpty)
      .getOrElse(sectionsOf(next).last)

  def variationName(prev: Option[FullOpening], next: Option[FullOpening]): Option[NameSection] =
    (prev, next) match
      case (Some(p), Some(n)) => variationName(p.name, n.name).some
      case (None, Some(n))    => n.family.name.some
      case _                  => none

  def sectionsOf(openingName: String): NonEmptyList[NameSection] =
    openingName.split(":", 2) match
      case Array(f, v) => NonEmptyList(f, v.split(",").toList.map(_.trim))
      case _           => NonEmptyList(openingName, Nil)
