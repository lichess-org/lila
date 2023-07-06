package lila.study

import BSONHandlers.{ readBranch, writeBranch, readNewBranch, writeNewBranch }

import lila.common.Chronometer
import lila.db.dsl.*
import chess.format.UciPath
import lila.tree.{ Root, Branch, Branches, NewBranch, NewTree, NewRoot }
import chess.format.UciCharPair
import lila.tree.NewTree.*
import chess.Variation

private object StudyFlatTree:

  private case class FlatNode(path: UciPath, data: Bdoc):
    val depth = path.depth

    def toNodeWithChildren(children: Option[Branches]): Option[Branch] =
      path.lastId.flatMap { readBranch(data, _) }.map {
        _.copy(children = children | Branches.empty)
      }

    def toNodeWithChildren1(child: Option[NewTree]): Option[NewTree] =
      readNewBranch(data, path).map(NewTree(_, child, Nil))

  object reader:

    def rootChildren(flatTree: Bdoc): Branches =
      Chronometer.syncMon(_.study.tree.read):
        traverse:
          flatTree.elements.toList
            .collect {
              case el if el.name != UciPathDb.rootDbKey =>
                FlatNode(UciPathDb.decodeDbKey(el.name), el.value.asOpt[Bdoc].get)
            }
            .sortBy(-_.depth)

    def newRoot(flatTree: Bdoc): Option[NewTree] =
      Chronometer.syncMon(_.study.tree.read):
        traverseN:
          flatTree.elements.toList
            .collect {
              case el if el.name != UciPathDb.rootDbKey =>
                FlatNode(UciPathDb.decodeDbKey(el.name), el.value.asOpt[Bdoc].get)
            }
            .sortBy(-_.depth)

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

    private def traverseN(xs: List[FlatNode]): Option[NewTree] =
      xs.nonEmpty so
        xs.foldLeft(Map.empty[UciPath, NewTree]) { (roots, flat) =>
          flat.toNodeWithChildren1(roots.get(flat.path)).fold(roots) { node =>
            roots.removed(flat.path).updatedWith(flat.path.parent) {
              case None           => node.some
              case Some(siblings) => siblings.addVariation(node.toVariation).some
            }
          }
        }.get(UciPath.root)

  object writer:

    def rootChildren(root: Root): List[(String, Bdoc)] =
      Chronometer.syncMon(_.study.tree.write):
        root.children.nodes.flatMap { traverse(_, UciPath.root) }

    def newRootChildren(root: NewRoot): List[(String, Bdoc)] =
      Chronometer.syncMon(_.study.tree.write):
        root.tree so {
          _.foldLeft(List.empty)((acc, branch) => acc :+ writeBranch_(branch))
        }

    private def writeBranch_(branch: NewBranch) =
      val order = branch.path.computeIds.size > 1 option
        branch.path.computeIds.toList
      UciPathDb.encodeDbKey(branch.path) -> writeNewBranch(branch, order)

    private def traverse(node: Branch, parentPath: UciPath): List[(String, Bdoc)] =
      (parentPath.depth < Node.MAX_PLIES) so {
        val path = parentPath + node.id
        node.children.nodes.flatMap {
          traverse(_, path)
        } appended (UciPathDb.encodeDbKey(path) -> writeBranch(node))
      }
