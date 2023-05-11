package lila.study

import BSONHandlers.{ readNode, writeNode }
import Node.Children

import lila.common.Chronometer
import lila.db.dsl.*
import chess.format.UciPath

private object StudyFlatTree:

  private case class FlatNode(path: UciPath, data: Bdoc):
    val depth = path.depth

    def toNodeWithChildren(children: Option[Children]): Option[Node] =
      path.lastId.flatMap { readNode(data, _) }.map {
        _.copy(children = children | Node.emptyChildren)
      }

  object reader:

    def rootChildren(flatTree: Bdoc): Children =
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

    private def traverse(children: List[FlatNode]): Children =
      children
        .foldLeft(Map.empty[UciPath, Children]) { (roots, flat) =>
          // assumes that node has a greater depth than roots (sort beforehand)
          flat.toNodeWithChildren(roots get flat.path).fold(roots) { node =>
            roots.removed(flat.path).updatedWith(flat.path.parent) {
              case None           => Children(Vector(node)).some
              case Some(siblings) => siblings.addNode(node).some
            }
          }
        }
        .get(UciPath.root) | Node.emptyChildren

  object writer:

    def rootChildren(root: Node.Root): Vector[(String, Bdoc)] =
      Chronometer.syncMon(_.study.tree.write) {
        root.children.nodes.flatMap { traverse(_, UciPath.root) }
      }

    private def traverse(node: Node, parentPath: UciPath): Vector[(String, Bdoc)] =
      (parentPath.depth < Node.MAX_PLIES) ?? {
        val path = parentPath + node.id
        node.children.nodes.flatMap {
          traverse(_, path)
        } appended (UciPathDb.encodeDbKey(path) -> writeNode(node))
      }
