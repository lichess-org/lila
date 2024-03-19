package lila.study

import cats.syntax.all.*
import monocle.syntax.all.*
import chess.variant.Variant
import lila.tree.{ Branch, Branches, Root }
import lila.tree.NewRoot

object TreeBuilder:

  private val initialStandardDests = chess.Game(chess.variant.Standard).situation.destinations

  def apply(root: NewRoot, variant: Variant) =
    val dests =
      if variant.standard && root.fen.isInitial then initialStandardDests
      else
        val sit = chess.Game(variant.some, root.fen.some).situation
        sit.playable(false).so(sit.destinations)
    root
      .focus(_.metas)
      .modify(x => x.copy(dests = dests.some))

  // DEBUG should be done in BSONHandler
  def apply(root: Root, variant: Variant): Root =
    val dests =
      if variant.standard && root.fen.isInitial then initialStandardDests
      else
        val sit = chess.Game(variant.some, root.fen.some).situation
        sit.playable(false).so(sit.destinations)
    makeRoot(root, variant).copy(dests = dests.some)

  // DEBUG should be done in BSONHandler
  def toBranch(node: Branch, variant: Variant): Branch =
    node.copy(
      children = toBranches(node.children, variant)
    )

  // DEBUG should be done in BSONHandler
  def makeRoot(root: Root, variant: Variant): Root =
    root.copy(
      children = toBranches(root.children, variant)
    )

  private def toBranches(children: Branches, variant: Variant): Branches =
    // Note, view here was I think not doing anything since .ToList was set afterwards
    Branches(children.nodes.map(toBranch(_, variant)))
