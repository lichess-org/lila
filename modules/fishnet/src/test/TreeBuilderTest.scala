package lila.fishnet

import chess.ByColor
import chess.format.pgn.{ Parser, PgnStr, SanStr }
import chess.variant.Standard
import monocle.syntax.all.*
import play.api.libs.json.JsValue

import lila.analyse.Annotator
import lila.core.config.NetDomain
import lila.core.game.Player
import lila.core.id.GamePlayerId
import lila.tree.Node.{ Comment, Comments }
import lila.tree.{
  Branch,
  Branches,
  ExportOptions,
  NewBranch,
  NewRoot,
  NewTree,
  NewTreeBuilder,
  Node,
  Root,
  TreeBuilder
}

final class TreeBuilderTest extends munit.FunSuite:

  test("tree builder test"):
    TestFixtures.treeBuilderTestCases
      .map(_.test)
      .foreach: (output, expected) =>
        assertEquals(output, expected)

  test("tree builder json"):
    TestFixtures.treeBuilderTestCases
      .flatMap(_.testJson)
      .foreach: (output, expected) =>
        assertEquals(output, expected)

object TreeBuilderTest:

  case class TestCase(sans: List[SanStr], pgn: PgnStr, fishnetInput: String):

    given Executor = scala.concurrent.ExecutionContextOpportunistic
    val annotator = Annotator(NetDomain("l.org"))
    val builder = AnalysisBuilder(FishnetEvalCache.mock)

    lazy val parsedPgn = Parser.full(pgn).toOption.get
    def logError(s: String) = ()
    val variant = parsedPgn.tags.variant.getOrElse(Standard)
    val fen = parsedPgn.tags.fen.getOrElse(variant.initialFen)

    def makeGame(g: chess.Game) =
      lila.core.game
        .newGame(
          g,
          ByColor(Player(GamePlayerId("abcd"), _, none)),
          rated = chess.Rated.No,
          source = lila.core.game.Source.Api,
          pgnImport = none
        )
        .sloppy

    val ply = chess.Game(variant.some, fen.some).ply
    val (game, moves) = AnnotatorTest.gameWithMoves(sans, fen, variant)
    val analysis = AnnotatorTest.parse(builder, fishnetInput, fen.some, variant, moves, ply)

    def test =
      val x = NewRoot(
        TreeBuilder(makeGame(game), analysis.some, fen, ExportOptions.default, logError)
      ).cleanup
      val y = NewTreeBuilder(makeGame(game), analysis.some, fen, ExportOptions.default, logError).cleanup
      y -> x

    val exportOptions: List[ExportOptions] =
      for
        movetimes <- List(true, false)
        division <- List(true, false)
        clocks <- List(true, false)
        blurs <- List(true, false)
        rating <- List(true, false)
        puzzles <- List(true, false)
        nvui <- List(true, false)
        lichobileCompat <- List(true, false)
      yield ExportOptions(
        movetimes,
        division,
        clocks,
        blurs,
        rating,
        puzzles,
        nvui,
        lichobileCompat
      )

    def takeRandomN[A](n: Int)(as: List[A]) =
      scala.util.Random.shuffle(as).take(n)

    def testJson: List[(JsValue, JsValue)] =
      for
        option <- takeRandomN(11)(exportOptions)
        analysis <- List(analysis.some, none)
      yield
        val x = Node.minimalNodeJsonWriter.writes:
          TreeBuilder(makeGame(game), analysis, fen, option, logError).cleanCommentIds
        val y = NewRoot.minimalNodeJsonWriter.writes:
          NewTreeBuilder(makeGame(game), analysis, fen, option, logError).cleanup
        y -> x

    extension (root: Root)
      def cleanCommentIds: Root =
        NewRoot(root).cleanup.toRoot

    extension (newRoot: NewRoot)
      def toRoot =
        Root(
          newRoot.ply,
          newRoot.fen,
          newRoot.eval,
          newRoot.shapes,
          newRoot.comments,
          newRoot.gamebook,
          newRoot.glyphs,
          newRoot.tree.fold(Branches.empty)(_.toBranches),
          newRoot.clock,
          newRoot.crazyData
        )

      def cleanup: NewRoot =
        newRoot
          .focus(_.tree.some)
          .modify(_.map(_.cleanup))
          .focus(_.metas.comments)
          .modify(_.cleanup)

    extension (node: NewBranch)
      def cleanup: NewBranch =
        node
          .focus(_.metas.clock)
          .replace(none)
          .focus(_.metas.comments)
          .modify(_.cleanup)

    extension (comments: Comments)
      def cleanup: Comments =
        Comments(comments.value.map(_.copy(id = Comment.Id("i"))))

    extension (newBranch: NewBranch)
      def toBranch(children: Option[NewTree]): Branch = Branch(
        newBranch.ply,
        newBranch.move,
        newBranch.fen,
        newBranch.eval,
        newBranch.shapes,
        newBranch.comments,
        newBranch.gamebook,
        newBranch.glyphs,
        children.fold(Branches.empty)(_.toBranches),
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
