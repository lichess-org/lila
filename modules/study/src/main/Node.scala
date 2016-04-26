package lila.study

import chess.format.{ Uci, UciCharPair, Forsyth, FEN }

import lila.user.User
import lila.socket.tree.Node.Shape

sealed trait RootOrNode {
  val ply: Int
  val fen: FEN
  val check: Boolean
  val shapes: List[Shape]
  val children: Node.Children
}

case class Node(
    id: UciCharPair,
    ply: Int,
    move: Uci.WithSan,
    fen: FEN,
    check: Boolean,
    shapes: List[Shape],
    by: User.ID,
    children: Node.Children) extends RootOrNode {

  import Node.Children

  def withChildren(f: Children => Option[Children]) =
    f(children) map { newChildren =>
      copy(children = newChildren)
    }
}

object Node {

  case class Children(nodes: Vector[Node]) {

    def nodeAt(path: Path): Option[Node] = path.split match {
      case None                               => none
      case Some((head, tail)) if tail.isEmpty => get(head)
      case Some((head, tail))                 => get(head) flatMap (_.children nodeAt tail)
    }

    def addNodeAt(node: Node, path: Path): Option[Children] = path.split match {
      case None if has(node.id) => this.some
      case None                 => copy(nodes = nodes :+ node).some
      case Some((head, tail))   => updateChildren(head, _.addNodeAt(node, tail))
    }

    def deleteNodeAt(path: Path): Option[Children] = path.split match {
      case None                                 => none
      case Some((head, Path(Nil))) if has(head) => copy(nodes = nodes.filterNot(_.id == head)).some
      case Some((_, Path(Nil)))                 => none
      case Some((head, tail))                   => updateChildren(head, _.deleteNodeAt(tail))
    }

    def promoteNodeAt(path: Path): Option[Children] = path.split match {
      case None => none
      case Some((head, Path(Nil))) => get(head).map { node =>
        copy(nodes = node +: nodes.filterNot(node ==))
      }
      case Some((head, tail)) => updateChildren(head, _.promoteNodeAt(tail))
    }

    def setShapesAt(shapes: List[Shape], path: Path): Option[Children] = path.split match {
      case None                    => none
      case Some((head, Path(Nil))) => updateWith(head, _.copy(shapes = shapes).some)
      case Some((head, tail))      => updateChildren(head, _.setShapesAt(shapes, tail))
    }

    def get(id: UciCharPair): Option[Node] = nodes.find(_.id == id)

    def has(id: UciCharPair): Boolean = nodes.exists(_.id == id)

    def updateWith(id: UciCharPair, op: Node => Option[Node]): Option[Children] =
      get(id) flatMap op map update

    def updateChildren(id: UciCharPair, f: Children => Option[Children]): Option[Children] =
      updateWith(id, _ withChildren f)

    def update(child: Node): Children = copy(
      nodes = nodes.map {
        case n if child.id == n.id => child
        case n                     => n
      })
  }
  val emptyChildren = Children(Vector.empty)

  case class Root(
      ply: Int,
      fen: FEN,
      check: Boolean,
      shapes: List[Shape],
      children: Children) extends RootOrNode {

    def withChildren(f: Children => Option[Children]) =
      f(children) map { newChildren =>
        copy(children = newChildren)
      }

    def nodeAt(path: Path): Option[RootOrNode] =
      if (path.isEmpty) this.some else children nodeAt path

    def pathExists(path: Path): Boolean = nodeAt(path).isDefined
  }

  object Root {

    val default = Root(
      ply = 0,
      fen = FEN(Forsyth.initial),
      check = false,
      shapes = Nil,
      children = emptyChildren)

    def fromRootBy(userId: User.ID)(b: lila.socket.tree.Root): Root = Root(
      ply = b.ply,
      fen = FEN(b.fen),
      check = b.check,
      shapes = Nil,
      children = Children(b.children.toVector.map(fromBranchBy(userId))))
  }

  def fromBranchBy(userId: User.ID)(b: lila.socket.tree.Branch): Node = Node(
    id = b.id,
    ply = b.ply,
    move = b.move,
    fen = FEN(b.fen),
    check = b.check,
    shapes = Nil,
    by = userId,
    children = Children(b.children.toVector.map(fromBranchBy(userId))))
}
