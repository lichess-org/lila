package lila.study

import BSONHandlers.{ readBranch, writeBranch }

import lila.common.Chronometer
import lila.db.dsl.{ *, given }
import chess.format.UciPath
import lila.tree.{ Root, Branch, Branches, NewTree, NewRoot }

private object StudyFlatTree:

  private case class FlatNode(path: UciPath, data: Bdoc):
    val depth = path.depth

    def toNodeWithChildren(children: Option[Branches]): Option[Branch] =
      path.lastId.flatMap { readBranch(data, _) }.map {
        _.copy(children = children | Branches.empty)
      }

  object reader:

    def rootChildren(flatTree: Bdoc): Branches =
      Chronometer.syncMon(_.study.tree.read) {
        traverse {
          flatTree.elements.toList
            .collect {
              case el if el.name != UciPathDb.rootDbKey =>
                FlatNode(UciPathDb.decodeDbKey(el.name), el.value.asOpt[Bdoc].get)
            }
            .sortBy(-_.depth)
        }
      }

    def newRootChildren(flatTree: Bdoc): Option[NewTree] = ???

    private def traverse(children: List[FlatNode]): Branches =
      children
        .foldLeft(Map.empty[UciPath, Branches]) { (roots, flat) =>
          // assumes that node has a greater depth than roots (sort beforehand)
          flat.toNodeWithChildren(roots get flat.path).fold(roots) { node =>
            roots.removed(flat.path).updatedWith(flat.path.parent) {
              case None           => Branches(List(node)).some
              case Some(siblings) => siblings.addNode(node).some
            }
          }
        }
        .get(UciPath.root) | Branches.empty

  object writer:

    def rootChildren(root: Root): List[(String, Bdoc)] =
      Chronometer.syncMon(_.study.tree.write) {
        root.children.nodes.flatMap { traverse(_, UciPath.root) }
      }

    def newRootChildren(root: NewRoot): List[(String, Bdoc)] = ???

    private def traverse(node: Branch, parentPath: UciPath): List[(String, Bdoc)] =
      (parentPath.depth < Node.MAX_PLIES) ?? {
        val path = parentPath + node.id
        node.children.nodes.flatMap {
          traverse(_, path)
        } appended (UciPathDb.encodeDbKey(path) -> writeBranch(node))
      }
