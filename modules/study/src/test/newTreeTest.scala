package lila.study

import cats.syntax.all.*
import chess.{ Centis, ErrorStr }
import chess.format.pgn.{
  Dumper,
  Glyphs,
  ParsedPgn,
  San,
  Tags,
  PgnStr,
  PgnNodeData,
  Comment as ChessComment,
  Node as PgnNode
}
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.MoveOrDrop.*

import lila.importer.{ ImportData, Preprocessed }
import lila.tree.Node.{ Comment, Comments, Shapes }

import cats.data.Validated
import scala.language.implicitConversions

import lila.tree.{ Branch, Branches, Root, Metas, NewTree, NewBranch, NewRoot, Node }

// in lila.study to have access to PgnImport
class NewTreeTest extends lila.common.LilaTest:

  import PgnImport.*
  import lila.tree.NewTree.*

  object MetasC:
    def fromNode(node: Node) =
      Metas(
        node.ply,
        node.fen,
        node.check,
        node.dests,
        node.drops,
        node.eval,
        node.shapes,
        node.comments,
        node.gamebook,
        node.glyphs,
        node.opening,
        node.clock,
        node.crazyData
      )

  // Convertor
  object NewBranchC:
    def fromBranch(branch: Branch) =
      NewBranch(
        branch.id,
        UciPath.root,
        branch.move,
        branch.comp,
        branch.forceVariation,
        MetasC.fromNode(branch)
      )

  extension (newBranch: NewBranch)
    def toBranch(children: List[NewTree]): Branch = Branch(
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
      Branches(children.map(_.toBranch)),
      newBranch.metas.opening,
      newBranch.comp,
      newBranch.metas.clock,
      newBranch.metas.crazyData,
      newBranch.forceVariation
    )
  // extension (newBranch: NewBranch)

  // Convertor
  object NewTreeC:
    def fromRoot(root: Root): Option[NewTree] =
      root.children.first.map(first =>
        NewTree(
          value = NewBranchC.fromBranch(first),
          child = first.children.first.map(NewTreeC.fromBranch),
          variation = root.children.variations.toVariations(fromBranch)
        )
      )

    def fromBranch(branch: Branch): NewTree =
      NewTree(
        value = NewBranchC.fromBranch(branch),
        child = branch.children.first.map(NewTreeC.fromBranch),
        variation = branch.children.variations.toVariations(fromBranch)
      )
  extension (newTree: NewTree)
    def toBranch: Branch = newTree.value.toBranch(newTree.children)
    def toBranches: Branches = Branches(
      newTree.value.toBranch(newTree.children) :: newTree.variations.map(_.toBranch)
    )
  // Convertor
  object NewRootC:
    def fromRoot(root: Root) =
      NewRoot(MetasC.fromNode(root), NewTreeC.fromRoot(root))

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

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  test("valid tree -> newTree first move") {
    val x       = PgnImport("1. e4 *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.get.size, 1L)
    assertEquals(newRoot.tree.get.mainLine.map(_.move.san.value), List("e4"))
    assertEquals(newRoot.toRoot, x.root)
  }

  test("valid tree -> newTree first move with variation") {
    val x       = PgnImport("1. e4 (1. d4??) *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.get.size, 2L)
    assertEquals(newRoot.tree.get.variations.map(_.value.move.san.value), List("d4"))
    assertEquals(newRoot.toRoot, x.root)
  }

  test("valid tree -> newTree two moves") {
    val x       = PgnImport("1. e4 e6 *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.get.size, 2L)
    assertEquals(newRoot.tree.get.mainLine.map(_.move.san.value), List("e4", "e6"))
    assertEquals(newRoot.toRoot, x.root)
  }

  test("valid tree <-> newTree more realistic conversion"):
    PgnFixtures.all foreach { pgn =>
      val x       = PgnImport(pgn, Nil).toOption.get
      val newRoot = NewPgnImport(pgn, Nil).toOption.get.root.pp
      assertEquals(newRoot.toRoot, x.root)
    }
