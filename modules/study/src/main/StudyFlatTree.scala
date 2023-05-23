package lila.study

import BSONHandlers._
import Node.{ Children, GameMainlineExtension, Root }

import shogi.format.usi.UsiCharPair

import lila.common.Chronometer
import lila.db.dsl._

private object StudyFlatTree {

  private case class FlatNode(key: String, data: Bdoc) {
    val depth = toDepth(key)

    def toNodeWithChildren(children: Option[Children]): Option[Node] =
      toUsiCharPair(key)
        .flatMap { ucp =>
          readNode(data, ucp).map(
            _.copy(children = children | Node.emptyChildren)
          )
        }
  }

  private def toDepth(key: String): Int =
    if (key contains Path.gameMainlineSep)
      key.split(Path.gameMainlineSep) match {
        case Array(plyStr)       => plyStr.toInt
        case Array(plyStr, path) => plyStr.toInt + (path.size / 2)
      }
    else key.size / 2

  private def toUsiCharPair(key: String): Option[UsiCharPair] =
    key.takeRight(2).some.filter(_.sizeIs == 2).map(k => UsiCharPair(k.head, k.last))

  object reader {

    def rootChildren(flatTree: Bdoc): Children =
      childrenByKey(flatTree).get(Path.root.toString) | Node.emptyChildren

    def mergeGameRoot(root: Root, flatTree: Bdoc): Root = {
      val variations = rootVariations(flatTree)
      val gmes       = flatTree.getAsOpt[Bdoc](Path.gameMainlineExtensionDbKey) ?? gameMainlineExtensions

      def appendVariations(c: Children, ply: Int): Children =
        variations.get(ply).fold(c)(v => Children(c.nodes ++ v.nodes))

      val newChildren = root.children updateMainline { node =>
        gmes
          .get(node.ply)
          .fold(node)(_.merge(node))
          .copy(
            children = appendVariations(node.children, node.ply)
          )
      }

      gmes
        .get(root.ply)
        .fold(root)(_.merge(root))
        .copy(
          children = appendVariations(newChildren, root.ply)
        )
    }

    private def gameMainlineExtensions(flatTree: Bdoc): Map[Int, GameMainlineExtension] =
      flatTree.elements.toList.flatMap { el =>
        el.name.toIntOption.map((_, readGameMainlineExtension(el.value.asOpt[Bdoc].get)))
      }.toMap

    private def rootVariations(flatTree: Bdoc): Map[Int, Children] =
      childrenByKey(flatTree).map { case (key, ch) => (toDepth(key), ch) }

    private def childrenByKey(flatTree: Bdoc): Map[String, Children] =
      Chronometer.syncMon(_.study.tree.read) {
        traverse(toFlatNodes(flatTree))
      }

    private def toFlatNodes(flatTree: Bdoc): List[FlatNode] =
      flatTree.elements.toList
        .collect {
          case el if !Path.dbKeys.contains(el.name) =>
            FlatNode(Path.decodeDbKey(el.name), el.value.asOpt[Bdoc].get)
        }
        .sortBy(-_.depth)

    private def traverse(children: List[FlatNode]): Map[String, Children] =
      children
        .foldLeft(Map.empty[String, Children]) { case (allChildren, flat) =>
          update(allChildren, flat)
        }

    // assumes that node has a greater depth than roots (sort beforehand)
    private def update(roots: Map[String, Children], flat: FlatNode): Map[String, Children] = {
      flat.toNodeWithChildren(roots get flat.key).fold(roots) { node =>
        roots.removed(flat.key).updatedWith(flat.key.dropRight(2)) {
          case None           => Children(Vector(node)).some
          case Some(siblings) => siblings.addNode(node).some
        }
      }
    }
  }

  object writer {

    def rootChildren(root: Node.Root): Vector[(String, Bdoc)] =
      Chronometer.syncMon(_.study.tree.write) {
        root.children.nodes.flatMap { traverse(_, Path.root) }
      }

    def gameRoot(root: Node.Root): Vector[(String, Bdoc)] =
      Chronometer.syncMon(_.study.tree.write) {
        rootVariations(root) appended
          (Path.gameMainlineExtensionDbKey -> $doc(gameMainlineExtensions(root)))
      }

    def gameMainlineExtensions(root: Node.Root): Vector[(String, Bdoc)] =
      root.children.first ?? { traverseGameMainlineExtensions(_) }

    private def rootVariations(root: Node.Root): Vector[(String, Bdoc)] =
      root.children.nodes.flatMap {
        traverseVariations(_, Path.root, root.gameMainlinePath.getOrElse(root.mainlinePath))
      }

    private def traverse(node: Node, parentPath: Path): Vector[(String, Bdoc)] =
      (parentPath.depth < Node.MAX_PLIES) ?? {
        val path = parentPath + node.id
        node.children.nodes.flatMap {
          traverse(_, path)
        } appended (Path.encodeDbKey(path) -> writeNode(node))
      }

    private def traverseGameMainlineExtensions(rn: RootOrNode): Vector[(String, Bdoc)] = {
      val rest = rn.children.first ?? {
        traverseGameMainlineExtensions(_)
      }
      if (storeNode(rn)) rest appended (rn.ply.toString -> writeGameMainlineExtension(rn))
      else rest
    }

    private def traverseVariations(
        node: Node,
        parentPath: Path,
        mainline: Path
    ): Vector[(String, Bdoc)] =
      (parentPath.depth < Node.MAX_PLIES) ?? {
        val path = parentPath + node.id
        val rest: Vector[(String, Bdoc)] = node.children.nodes.flatMap { node =>
          traverseVariations(node, path, mainline)
        }
        if (path.isOnPathOf(mainline)) rest
        else {
          val intersection = path.intersect(mainline)
          rest appended (s"${intersection.depth}${Path.gameMainlineSep}${path.drop(intersection.depth)}" -> writeNode(
            node
          ))
        }
      }

    private def storeNode(rn: RootOrNode): Boolean =
      rn.score.isDefined || rn.comments.value.nonEmpty || rn.shapes.value.nonEmpty || !rn.glyphs.isEmpty

  }
}
