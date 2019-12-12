package lila.study

import chess.format.{ FEN, Forsyth, Uci, UciCharPair }
import chess.opening._
import chess.variant.Variant
import lila.tree
import lila.tree.Eval
import lila.tree.Node.Comment

object TreeBuilder {

  private val initialStandardDests = chess.Game(chess.variant.Standard).situation.destinations

  def apply(root: Node.Root, variant: Variant) = {
    val dests =
      if (variant.standard && root.fen.value == chess.format.Forsyth.initial) initialStandardDests
      else {
        val sit = chess.Game(variant.some, root.fen.value.some).situation
        sit.playable(false) ?? sit.destinations
      }
    makeRoot(root, variant).copy(dests = dests.some)
  }

  def toBranch(node: Node, variant: Variant): tree.Branch =
    tree.Branch(
      id = node.id,
      ply = node.ply,
      move = node.move,
      fen = node.fen.value,
      check = node.check,
      shapes = node.shapes,
      comments = node.comments,
      gamebook = node.gamebook,
      glyphs = node.glyphs,
      clock = node.clock,
      crazyData = node.crazyData,
      eval = node.score.map(_.eval),
      children = toBranches(node.children, variant),
      opening = Variant.openingSensibleVariants(variant) ?? FullOpeningDB.findByFen(node.fen.value),
      forceVariation = node.forceVariation
    )

  def makeRoot(root: Node.Root, variant: Variant) =
    tree.Root(
      ply = root.ply,
      fen = root.fen.value,
      check = root.check,
      shapes = root.shapes,
      comments = root.comments,
      gamebook = root.gamebook,
      glyphs = root.glyphs,
      clock = root.clock,
      crazyData = root.crazyData,
      eval = root.score.map(_.eval),
      children = toBranches(root.children, variant),
      opening = Variant.openingSensibleVariants(variant) ?? FullOpeningDB.findByFen(root.fen.value)
    )

  private def toBranches(children: Node.Children, variant: Variant): List[tree.Branch] =
    children.nodes.map(toBranch(_, variant))(scala.collection.breakOut)
}
