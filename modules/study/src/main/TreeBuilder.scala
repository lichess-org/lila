package lila.study

import chess.opening.*
import chess.{ Tree, Square }
import chess.variant.Variant
import lila.tree.{ Metas, NewBranch, NewRoot }
import lila.tree.Branch
import lila.tree.Branches

object TreeBuilder:

  private val initialStandardDests = chess.Game(chess.variant.Standard).situation.destinations

  // DEBUG should be done in BSONHandler
  def apply(root: NewRoot, variant: Variant): NewRoot =
    val dests =
      if (variant.standard && root.fen.isInitial) initialStandardDests
      else
        val sit = chess.Game(variant.some, root.fen.some).situation
        sit.playable(false) ?? sit.destinations
    makeRoot(root, variant, dests)

  // DEBUG should be done in BSONHandler
  def makeRoot(root: NewRoot, variant: Variant, dests: Map[Square, List[Square]]): NewRoot =
    root
      .copy(
        metas = root.metas.updateOpening(variant).copy(dests = dests.some)
      )
      .mapChild(x => x.copy(metas = x.metas.updateOpening(variant)))

  extension (m: Metas)
    def updateOpening(variant: Variant): Metas =
      m.copy(opening = Variant.list.openingSensibleVariants(variant) ?? OpeningDb.findByEpdFen(m.fen))

  def toBranch(node: Tree[Branch], variant: Variant): Tree[Branch] = ???

  private def toBranches(children: Branches, variant: Variant): Branches = ???

