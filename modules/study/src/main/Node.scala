package lila.study

import chess.format.{ Uci, UciCharPair, Forsyth }

import lila.user.User

case class Node(
    id: UciCharPair,
    ply: Int,
    move: Uci.WithSan,
    fen: String,
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
      case Some((head, tail))   => updateWith(head, _.withChildren(_.addNodeAt(node, tail)))
    }

    def get(id: UciCharPair): Option[Node] = nodes.find(_.id == id)

    def has(id: UciCharPair): Boolean = nodes.exists(_.id == id)

    def updateWith(id: UciCharPair, op: Node => Node): Children = get(id) match {
      case None        => this
      case Some(child) => update(op(child))
    }

    def update(child: Node) = copy(
      nodes = nodes.map {
        case n if child.id == n.id => child
        case n                     => n
      })
  }
  val emptyChildren = Children(Vector.empty)

  case class Root(
      ply: Int,
      fen: String,
      check: Boolean,
      children: Children) {

    def withChildren(f: Children => Children) = copy(children = f(children))
  }

  object Root {

    val default = Root(
      ply = 0,
      fen = Forsyth.initial,
      check = false,
      children = emptyChildren)
  }

  def fromBranchBy(userId: User.ID)(b: lila.socket.tree.Branch): Node = Node(
    id = b.id,
    ply = b.ply,
    move = b.move,
    fen = b.fen,
    check = b.check,
    by = userId,
    children = Children(b.children.toVector.map(fromBranchBy(userId))))
}
