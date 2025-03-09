package lila.study

import chess.format.{ UciCharPair, UciPath }

import lila.common.Chronometer
import lila.db.dsl.*
import lila.tree.NewTree.*
import lila.tree.{ Branch, Branches, NewBranch, NewRoot, NewTree, Root }

import BSONHandlers.{ readBranch, writeBranch, readNewBranch, writeNewBranch }

private object StudyFlatTree:

  private case class FlatNode(path: UciPath, data: Bdoc):
    val depth = path.depth

    def toNodeWithChildren(children: Option[Branches]): Option[Branch] =
      path.lastId
        .flatMap { readBranch(data, _) }
        .map:
          _.copy(children = children | Branches.empty)

    def toNodeWithChild(child: Option[NewTree]): Option[NewTree] =
      readNewBranch(data, path).map(NewTree(_, child, Nil))

  object reader:

    def rootChildren(flatTree: Bdoc): Branches =
      Chronometer.syncMon(_.study.tree.read):
        traverse:
          flatTree.elements.toList
            .collect:
              case el if el.name != UciPathDb.rootDbKey =>
                FlatNode(UciPathDb.decodeDbKey(el.name), el.value.asOpt[Bdoc].get)
            .sortBy(-_.depth)

    def newRoot(flatTree: Bdoc): Option[NewTree] =
      Chronometer.syncMon(_.study.tree.read):
        traverseN:
          flatTree.elements.toList
            .collect:
              case el if el.name != UciPathDb.rootDbKey =>
                FlatNode(UciPathDb.decodeDbKey(el.name), el.value.asOpt[Bdoc].get)
            .sortBy(-_.depth)

    private def traverse(children: List[FlatNode]): Branches =
      children
        .foldLeft(Map.empty[UciPath, Branches]) { (roots, flat) =>
          flat
            .toNodeWithChildren(roots.get(flat.path))
            .fold(roots): node =>
              roots
                .removed(flat.path)
                .updatedWith(flat.path.parent):
                  case None           => Branches(List(node)).some
                  case Some(siblings) => siblings.addNode(node).some
        }
        .get(UciPath.root) | Branches.empty

    private def traverseN(xs: List[FlatNode]): Option[NewTree] =
      xs.nonEmpty.so(
        xs.foldLeft(Map.empty[UciPath, NewTree]) { (roots, flat) =>
          // assumes that node has a greater depth than roots (sort beforehand)
          flat
            .toNodeWithChild(roots.get(flat.path))
            .fold(roots): node =>
              roots
                .removed(flat.path)
                .updatedWith(flat.path.parent):
                  case None           => node.some
                  case Some(siblings) => siblings.addVariation(node.toVariation).some
        }.get(UciPath.root)
      )

  object writer:

    def rootChildren(root: Root): List[(String, Bdoc)] =
      Chronometer.syncMon(_.study.tree.write):
        root.children.nodes.flatMap { traverse(_, UciPath.root) }

    def newRootChildren(root: NewRoot): List[(String, Bdoc)] =
      Chronometer.syncMon(_.study.tree.write):
        root.tree.so:
          _.mapAccuml_(UciPath.root)((acc, branch) =>
            val path = acc + branch.id
            path -> (UciPathDb.encodeDbKey(path) -> writeNewBranch(branch))
          ).toList

    private def traverse(node: Branch, parentPath: UciPath): List[(String, Bdoc)] =
      (parentPath.depth < Node.MAX_PLIES).so:
        val path = parentPath + node.id
        node.children.nodes
          .flatMap:
            traverse(_, path)
          .appended(UciPathDb.encodeDbKey(path) -> writeBranch(node))
