package lila.study

import monocle.syntax.all.*
import chess.{ Centis, ErrorStr, Node as PgnNode, Tree, Variation }
import chess.format.UciPath
import chess.format.pgn.{ Glyphs, ParsedPgn, San, Tags, PgnStr, PgnNodeData, Comment as ChessComment }
import lila.tree.Node.{ Comment, Comments, Shapes }

import lila.tree.{ Branch, Branches, Root, Metas, NewTree, NewBranch, NewRoot, Node }

object Helpers:
  import lila.tree.NewTree.*

  def rootToPgn(root: Root) = PgnDump
    .rootToPgn(root, Tags.empty)(using PgnDump.WithFlags(true, true, true, true, false))
    .render

  object NewRootC:
    def fromRoot(root: Root) =
      NewRoot(NewTree.fromNode(root), NewTree(root))

  extension (newBranch: NewBranch)
    def toBranch(children: Option[NewTree]): Branch = Branch(
      newBranch.id,
      newBranch.metas.ply,
      newBranch.move,
      newBranch.metas.fen,
      newBranch.metas.check,
      newBranch.metas.dests,
      newBranch.metas.drops,
      newBranch.metas.eval,
      newBranch.metas.shapes,
      newBranch.metas.comments,
      newBranch.metas.gamebook,
      newBranch.metas.glyphs,
      children.fold(Branches.empty)(_.toBranches),
      newBranch.metas.opening,
      newBranch.comp,
      newBranch.metas.clock,
      newBranch.metas.crazyData,
      newBranch.forceVariation
    )

  extension (newTree: NewTree)
    def toBranch: Branch = newTree.value.toBranch(newTree.child)
    def toBranches: Branches =
      val variations = newTree.variations.map(_.toNode.toBranch)
      Branches(newTree.value.toBranch(newTree.child) :: variations)

  extension (newRoot: NewRoot)
    def toRoot =
      Root(
        newRoot.metas.ply,
        newRoot.metas.fen,
        newRoot.metas.check,
        newRoot.metas.dests,
        newRoot.metas.drops,
        newRoot.metas.eval,
        newRoot.metas.shapes,
        newRoot.metas.comments,
        newRoot.metas.gamebook,
        newRoot.metas.glyphs,
        newRoot.tree.fold(Branches.empty)(_.toBranches),
        newRoot.metas.opening,
        newRoot.metas.clock,
        newRoot.metas.crazyData
      )

  extension (comments: Comments)
    def cleanup: Comments =
      Comments(comments.value.map(_.copy(id = Comment.Id("i"))))

  extension (node: NewBranch)
    def cleanup: NewBranch =
      node
        .focus(_.metas.comments)
        .modify(_.cleanup)
        .copy(path = UciPath.root)

  extension (root: NewRoot)
    def cleanup: NewRoot =
      root
        .focus(_.tree.some)
        .modify(_.map(_.cleanup))
        .focus(_.metas.comments)
        .modify(_.cleanup)

  def sanStr(node: Tree[NewBranch]): String = node.value.move.san.value
