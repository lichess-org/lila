package lila.study

import BSONHandlers._
import Node.Children

import lila.db.dsl._
import lila.tree.Eval
import lila.tree.Eval.Score

private object StudyFlatTree {

  private case class FlatNode(path: Path, data: Bdoc) {
    val depth = path.ids.size

    def toNodeWithChildren(children: Option[Children]): Node = {
      readNode(data, path.ids.last)
    }.copy(children = children | Node.emptyChildren)
  }

  def rootChildren(flatTree: Bdoc): Children = traverse {
    flatTree.elements.toList
      .collect {
        case el if el.name.nonEmpty => FlatNode(Path(el.name), el.value.asOpt[Bdoc].get)
      }
      .sortBy(-_.depth)
  }

  private def traverse(children: List[FlatNode]): Children =
    children
      .foldLeft(Map.empty[Path, Children]) { case (allChildren, flat) =>
        update(allChildren, flat)
      }
      .get(Path.root) | Node.emptyChildren

  // assumes that node has a greater depth than roots (sort beforehand)
  private def update(roots: Map[Path, Children], flat: FlatNode): Map[Path, Children] = {
    val node = flat.toNodeWithChildren(roots get flat.path)
    roots.removed(flat.path).updatedWith(flat.path.init) {
      case None           => Children(Vector(node)).some
      case Some(siblings) => siblings.addNode(node).some
    }
  }
}
