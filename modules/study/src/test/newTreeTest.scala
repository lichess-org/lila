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
        node.crazyData,
        node.dropFirstChild,
        node.clock,
        node.forceVariation
      ) 

  // Convertor
  object NewTreeC:
    def fromRoot(root: Root): NewTree =
      // Assumes `Root` has a main move
      val first = root.children.first.get
      NewTree.make(
        value=NewBranchC.fromBranch(first),
        child=NewTreeC.fromBranch(first),
        variations=root.children.variations.flatMap(NewTreeC.fromBranch)
      )
    
    def fromBranch(branch: Branch): Option[NewTree] = 
      branch.children.first.map { first =>
      NewTree.make(
        value=NewBranchC.fromBranch(first),
        child=NewTreeC.fromBranch(first),
        variations=branch.children.variations.flatMap(NewTreeC.fromBranch)
      )
    }
  // Convertor
  object NewBranchC:
    def fromBranch(branch: Branch) =
      NewBranch(
        branch.id,
        MetasC.fromNode(branch),
      )
  // Convertor
  object NewRootC:
    def fromRoot(root: Root) = 
      NewRoot(MetasC.fromNode(root), NewTreeC.fromRoot(root))

  val pgn                          = """
  { Root comment }
1. e4! $16 $40 $32 (1. d4?? d5 $146 { d5 is a good move }) 1... e6?! { e6 is a naughty move } *
  """

  // test("valid tree -> newTree conversion") {}
