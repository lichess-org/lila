package lila.study

import shogi.opening._
import shogi.variant.Variant
import lila.tree

object TreeBuilder {

  def apply(root: Node.Root, variant: Variant): tree.Root =
    makeRoot(root, variant)

  def toBranch(node: Node, variant: Variant): tree.Branch =
    tree.Branch(
      id = node.id,
      ply = node.ply,
      usi = node.usi,
      sfen = node.sfen,
      check = node.check,
      shapes = node.shapes,
      comments = node.comments,
      gamebook = node.gamebook,
      glyphs = node.glyphs,
      clock = node.clock,
      eval = node.score.map(_.eval),
      children = toBranches(node.children, variant),
      opening = Variant.openingSensibleVariants(variant) ?? FullOpeningDB.findBySfen(node.sfen),
      forceVariation = node.forceVariation
    )

  def makeRoot(root: Node.Root, variant: Variant): tree.Root =
    tree.Root(
      ply = root.ply,
      sfen = root.sfen,
      check = root.check,
      shapes = root.shapes,
      comments = root.comments,
      gamebook = root.gamebook,
      glyphs = root.glyphs,
      clock = root.clock,
      eval = root.score.map(_.eval),
      children = toBranches(root.children, variant),
      opening = Variant.openingSensibleVariants(variant) ?? FullOpeningDB.findBySfen(root.sfen)
    )

  private def toBranches(children: Node.Children, variant: Variant): List[tree.Branch] =
    children.nodes.view.map(toBranch(_, variant)).toList
}
