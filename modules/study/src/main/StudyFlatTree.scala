package lila.study

import chess.format.UciPath

import lila.mon.Chronometer.syncMon
import lila.db.dsl.*
import lila.tree.{ Branch, Branches, Root }

import BSONHandlers.{ readBranch, writeBranch }

private object StudyFlatTree:

  private case class FlatNode(path: UciPath, data: Bdoc):
    val depth = path.depth

    def toNodeWithChildren(children: Option[Branches]): Option[Branch] =
      readBranch(data).map:
        _.copy(children = children | Branches.empty)

  object reader:

    def rootChildren(flatTree: Bdoc): Branches =
      syncMon(lila.mon.study.tree.read):
        traverse:
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
                  case None => Branches(List(node)).some
                  case Some(siblings) => siblings.addNode(node).some
        }
        .get(UciPath.root) | Branches.empty

  object writer:

    def rootChildren(root: Root): List[(String, Bdoc)] =
      syncMon(lila.mon.study.tree.write):
        root.children.toList.flatMap { traverse(_, UciPath.root) }

    private def traverse(node: Branch, parentPath: UciPath): List[(String, Bdoc)] =
      (parentPath.depth < Node.MAX_PLIES).so:
        val path = parentPath + node.id
        node.children.toList
          .flatMap:
            traverse(_, path)
          .appended(UciPathDb.encodeDbKey(path) -> writeBranch(node))
