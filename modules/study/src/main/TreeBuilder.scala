package lila.study

import chess.format.{ Forsyth, Uci, UciCharPair }
import chess.opening._
import lila.socket.tree

import play.api.libs.json._

object TreeBuilder {

  private type Ply = Int
  private type OpeningOf = String => Option[FullOpening]

  def apply(root: Node.Root) = tree.Root(
    ply = root.ply,
    fen = root.fen.value,
    check = root.check,
    children = toBranches(root.children),
    crazyData = none)

  private def toBranch(node: Node): tree.Branch = tree.Branch(
    id = node.id,
    ply = node.ply,
    move = node.move,
    fen = node.fen.value,
    check = node.check,
    children = toBranches(node.children),
    crazyData = none)

  private def toBranches(children: Node.Children) =
    children.nodes.toList.map(toBranch)
}
