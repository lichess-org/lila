package lila.study
import chess.variant.Variant
import monocle.syntax.all.*

import lila.tree.{ NewRoot, Root }

object TreeBuilder:

  private val initialStandardDests = chess.Game(chess.variant.Standard).position.destinations

  def apply(root: NewRoot, variant: Variant) =
    val dests =
      if variant.standard && root.fen.isInitial then initialStandardDests
      else
        val position = chess.Position(variant, root.fen)
        position.playable(false).so(position.destinations)
    root
      .focus(_.metas)
      .modify(_.copy(dests = dests.some))

  def apply(root: Root, variant: Variant): Root =
    val dests =
      if variant.standard && root.fen.isInitial then initialStandardDests
      else
        val position = chess.Position(variant, root.fen)
        position.playable(false).so(position.destinations)
    root.copy(dests = dests.some)
