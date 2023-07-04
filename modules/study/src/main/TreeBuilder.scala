package lila.study

import chess.opening.*
import chess.variant.Variant
import lila.tree.{ Branch, Branches, Root }

object TreeBuilder:

  private val initialStandardDests = chess.Game(chess.variant.Standard).situation.destinations

  // DEBUG should be done in BSONHandler
  def apply(root: Root, variant: Variant): Root =
    val dests =
      if variant.standard && root.fen.isInitial then initialStandardDests
      else
        val sit = chess.Game(variant.some, root.fen.some).situation
        sit.playable(false) so sit.destinations
    makeRoot(root, variant).copy(dests = dests.some)

  // DEBUG should be done in BSONHandler
  def toBranch(node: Branch, variant: Variant): Branch =
    node.copy(
      opening = Variant.list.openingSensibleVariants(variant) so OpeningDb.findByEpdFen(node.fen),
      children = toBranches(node.children, variant)
    )

  // DEBUG should be done in BSONHandler
  def makeRoot(root: Root, variant: Variant): Root =
    root.copy(
      opening = Variant.list.openingSensibleVariants(variant) so OpeningDb.findByEpdFen(root.fen),
      children = toBranches(root.children, variant)
    )

  private def toBranches(children: Branches, variant: Variant): Branches =
    // Note, view here was I think not doing anything since .ToList was set afterwards
    Branches(children.nodes.map(toBranch(_, variant)))
