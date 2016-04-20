package lila.study

import chess.format.{ Uci, UciCharPair, Forsyth, FEN }

import lila.user.User

case class Node(
    id: UciCharPair,
    ply: Int,
    move: Uci.WithSan,
    fen: FEN,
    check: Boolean,
    by: User.ID,
    children: Node.Children) {

  import Node.Children

  def withChildren(f: Children => Children) = copy(children = f(children))
}

object Node {

  case class Children(nodes: Vector[Node]) {

    def addNodeAt(node: Node, path: Path): Children = path.split match {
      case None if has(node.id) => this
      case None                 => copy(nodes = nodes :+ node)
      case Some((head, tail))   => updateChildren(head, _.addNodeAt(node, tail))
    }

    def deleteNodeAt(path: Path): Children = path.split match {
      case None                    => this
      case Some((head, Path(Nil))) => copy(nodes = nodes.filterNot(_.id == head))
      case Some((head, tail))      => updateChildren(head, _.deleteNodeAt(tail))
    }

    def promoteNodeAt(path: Path): Children = path.split match {
      case None => this
      case Some((head, Path(Nil))) => get(head).fold(this) { node =>
        copy(nodes = node +: nodes.filterNot(node ==))
      }
      case Some((head, tail)) => updateChildren(head, _.promoteNodeAt(tail))
    }

    def get(id: UciCharPair): Option[Node] = nodes.find(_.id == id)

    def has(id: UciCharPair): Boolean = nodes.exists(_.id == id)

    def updateWith(id: UciCharPair, op: Node => Node): Children = get(id) match {
      case None        => this
      case Some(child) => update(op(child))
    }

    def updateChildren(id: UciCharPair, f: Children => Children): Children =
      updateWith(id, _ withChildren f)

    def update(child: Node) = copy(
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
      children: Children) {

    def withChildren(f: Children => Children) = copy(children = f(children))
  }

  object Root {

    val default = Root(
      ply = 0,
      fen = FEN(Forsyth.initial),
      check = false,
      children = emptyChildren)
  }

  def fromBranchBy(userId: User.ID)(b: lila.socket.tree.Branch): Node = Node(
    id = b.id,
    ply = b.ply,
    move = b.move,
    fen = FEN(b.fen),
    check = b.check,
    by = userId,
    children = Children(b.children.toVector.map(fromBranchBy(userId))))
}
