package lidraughts.study

import draughts.format.{ FEN, Forsyth, Uci, UciCharPair }
import draughts.opening._
import draughts.variant.Variant
import lidraughts.tree
import lidraughts.tree.Eval
import lidraughts.tree.Node.Comment

object TreeBuilder {

  private val initialStandardDests = draughts.DraughtsGame(draughts.variant.Standard).situation.allDestinations

  def apply(root: Node.Root, variant: Variant) = {
    val situation =
      if (variant.standard && root.fen.value == draughts.format.Forsyth.initial) None
      else draughts.DraughtsGame(variant.some, root.fen.value.some).situation.some
    val dests = situation match {
      case Some(sit) => sit.playable(false) ?? sit.allDestinations
      case _ => initialStandardDests
    }
    val captLen = situation match {
      case Some(sit) => sit.playable(false) ?? sit.allMovesCaptureLength
      case _ => 0
    }
    makeRoot(root, variant).copy(dests = dests.some, captureLength = captLen.some)
  }

  def toBranch(node: Node, variant: Variant): tree.Branch =
    tree.Branch(
      id = node.id,
      ply = node.ply,
      move = node.move,
      fen = node.fen.value,
      shapes = node.shapes,
      comments = node.comments,
      gamebook = node.gamebook,
      glyphs = node.glyphs,
      clock = node.clock,
      eval = node.score.map(_.eval),
      children = toBranches(node.children, variant),
      opening = Variant.openingSensibleVariants(variant) ?? FullOpeningDB.findByFen(node.fen.value)
    )

  def makeRoot(root: Node.Root, variant: Variant) =
    tree.Root(
      ply = root.ply,
      fen = root.fen.value,
      shapes = root.shapes,
      comments = root.comments,
      gamebook = root.gamebook,
      glyphs = root.glyphs,
      clock = root.clock,
      eval = root.score.map(_.eval),
      children = toBranches(root.children, variant),
      opening = Variant.openingSensibleVariants(variant) ?? FullOpeningDB.findByFen(root.fen.value)
    )

  private def toBranches(children: Node.Children, variant: Variant): List[tree.Branch] =
    children.nodes.map(toBranch(_, variant))(scala.collection.breakOut)
}
