package lila.opening

import chess.opening.{ Opening, OpeningDb, OpeningKey, OpeningName }

case class OpeningTree(children: List[(OpeningTree.NameOrOpening, OpeningTree)])

object OpeningTree:

  type NameOrOpening = (NameSection, Option[Opening])

  private val emptyNode = Node(Map.empty)

  private case class Node(children: Map[NameOrOpening, Node]):
    def update(path: List[NameOrOpening]): Node = path match
      case Nil => this
      case last :: Nil => copy(children = children.updatedWith(last)(_.orElse(emptyNode.some)))
      case p :: rest =>
        copy(children = children.updatedWith(p)(node => (node | emptyNode).update(rest).some))

    def toTree: OpeningTree = OpeningTree(
      children.toList
        .sortBy(_._1._1)(using stringOrdering)
        .map { (op, node) =>
          (op, node.toTree)
        }
    )

  lazy val compute: OpeningTree =
    OpeningDb.shortestLines.values
      .map: op =>
        val sections = NameSection.sectionsOf(op.name)
        sections.toList.mapWithIndex: (name, i) =>
          (
            name,
            OpeningDb.shortestLines.get(
              OpeningKey.fromName(OpeningName(sections.take(i + 1).mkString("_")))
            )
          )
      .toList
      .foldLeft(emptyNode)(_.update(_))
      .toTree
