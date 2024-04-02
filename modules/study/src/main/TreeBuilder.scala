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

  def apply(root: Root, variant: Variant): Root =
    val dests =
      if variant.standard && root.fen.isInitial then initialStandardDests
      else
        val sit = chess.Game(variant.some, root.fen.some).situation
        sit.playable(false).so(sit.destinations)
    root.copy(dests = dests.some)
