package lila.study

import monocle.syntax.all.*
import chess.{ Centis, ErrorStr, Node as PgnNode, Tree, Variation }
import chess.format.UciPath
import chess.format.pgn.{ Glyphs, ParsedPgn, San, Tags, PgnStr, PgnNodeData, Comment as ChessComment }
import lila.tree.Node.{ Comment, Comments, Shapes }

import lila.tree.{ Branch, Branches, Root, Metas, NewTree, NewBranch, NewRoot, Node }

object Helpers:
  import lila.tree.NewTree.*

  def rootToPgn(root: Root): PgnStr = PgnDump
    .rootToPgn(root, Tags.empty)(using PgnDump.WithFlags(true, true, true, true, false, none))
    .render

  def rootToPgn(root: NewRoot): PgnStr = PgnDump
    .rootToPgn(root, Tags.empty)(using PgnDump.WithFlags(true, true, true, true, false, none))
    .render

  extension (root: Root)
    def toNewRoot = NewRoot(root)

    def debug = root.ppAs(rootToPgn)

  extension (newBranch: NewBranch)
    def toBranch(children: Option[NewTree]): Branch = Branch(
      newBranch.id,
      newBranch.ply,
      newBranch.move,
      newBranch.fen,
      newBranch.check,
      newBranch.dests,
      newBranch.drops,
      newBranch.eval,
      newBranch.shapes,
      newBranch.comments,
      newBranch.gamebook,
      newBranch.glyphs,
      children.fold(Branches.empty)(_.toBranches),
      newBranch.opening,
      newBranch.comp,
      newBranch.clock,
      newBranch.crazyData,
      newBranch.forceVariation
    )

  extension (newTree: NewTree)
    // We lost variations here
    // newTree.toBranch == newTree.withoutVariations.toBranch
    def toBranch: Branch = newTree.value.toBranch(newTree.child)

    def toBranches: Branches =
      val variations = newTree.variations.map(_.toNode.toBranch)
      Branches(newTree.value.toBranch(newTree.child) :: variations)

  extension (newRoot: NewRoot)
    def toRoot =
      Root(
        newRoot.ply,
        newRoot.fen,
        newRoot.check,
        newRoot.dests,
        newRoot.drops,
        newRoot.eval,
        newRoot.shapes,
        newRoot.comments,
        newRoot.gamebook,
        newRoot.glyphs,
        newRoot.tree.fold(Branches.empty)(_.toBranches),
        newRoot.opening,
        newRoot.clock,
        newRoot.crazyData
      )

    def debug = newRoot.ppAs(rootToPgn)

  extension (comments: Comments)
    def cleanup: Comments =
      Comments(comments.value.map(_.copy(id = Comment.Id("i"))))

  extension (node: NewBranch)
    def cleanup: NewBranch =
      node
        .focus(_.metas.clock)
        .set(none)
        .focus(_.metas.comments)
        .modify(_.cleanup)

  extension (root: NewRoot)
    def cleanup: NewRoot =
      root
        .focus(_.tree.some)
        .modify(_.map(_.cleanup))
        .focus(_.metas.comments)
        .modify(_.cleanup)

  def sanStr(node: Tree[NewBranch]): String = node.value.move.san.value
