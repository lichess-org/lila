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
import chess.format.{ Fen, Uci, UciCharPair }
import chess.MoveOrDrop.*

import lila.importer.{ ImportData, Preprocessed }
import lila.tree.Node.{ Comment, Comments, Shapes }

import cats.data.Validated
import scala.language.implicitConversions

import lila.tree.{ Branch, Branches, Root, Metas, NewTree, NewBranch, NewRoot, Node }

// in lila.study to have access to PgnImport
class NewTreeTest extends lila.common.LilaTest:

  import PgnImport.*

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
        node.comp,
        node.clock,
        node.crazyData,
        node.forceVariation
      )

  // Convertor
  object NewBranchC:
    def fromBranch(branch: Branch) =
      NewBranch(
        branch.id,
        branch.move,
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
      newBranch.metas.comp,
      newBranch.metas.clock,
      newBranch.metas.crazyData,
      newBranch.metas.forceVariation
    )
  // extension (newBranch: NewBranch)

  // Convertor
  object NewTreeC:
    def fromRoot(root: Root): NewTree =
      // Assumes `Root` has a main move for conveniency in tests
      val first = root.children.first.get
      NewTree.make(
        value = NewBranchC.fromBranch(first),
        child = first.children.first.map(NewTreeC.fromBranch),
        variations = root.children.variations.map(NewTreeC.fromBranch)
      )

    def fromBranch(branch: Branch): NewTree =
      NewTree.make(
        value = NewBranchC.fromBranch(branch),
        child = branch.children.first.map(NewTreeC.fromBranch),
        variations = branch.children.variations.map(NewTreeC.fromBranch)
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
        newRoot.tree.toBranches,
        newRoot.metas.opening,
        newRoot.metas.clock,
        newRoot.metas.crazyData
      )

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  val pgn = """
  { Root comment }
1. e4! $16 $40 $32 (1. d4?? d5 $146 { d5 is a good move }) 1... e6?! { e6 is a naughty move } *
  """
  test("valid tree -> newTree first move") {
    val x       = PgnImport("1. e4 *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.totalNodes, 1)
    assertEquals(newRoot.tree.mainLine.map(_.move.san.value), List("e4"))
  }

  test("tree <-> newTree first move") {
    val x       = PgnImport("1. e4 *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.toRoot, x.root)
  }

  test("valid tree -> newTree first move with variation") {
    val x       = PgnImport("1. e4 (1. d4??) *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.totalNodes, 2)
    assertEquals(newRoot.tree.variations.map(_.value.move.san.value), List("d4"))
    assertEquals(newRoot.toRoot, x.root)
  }

  test("valid tree -> newTree two moves") {
    val x       = PgnImport("1. e4 e6 *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.totalNodes, 2)
    assertEquals(newRoot.tree.mainLine.map(_.move.san.value), List("e4", "e6"))
    assertEquals(newRoot.toRoot, x.root)
  }

  test("valid tree <-> newTree more realistic conversion") {
    val x       = PgnImport(pgn, Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.toRoot, x.root)
  }
