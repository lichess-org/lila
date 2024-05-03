package lila.fishnet

import monocle.syntax.all.*
import chess.MoveOrDrop.*
import chess.format.pgn.{ InitialComments, Move, Parser, Pgn, PgnStr, SanStr, Tags }
import chess.variant.Standard
import chess.{ ByColor, Clock, MoveOrDrop, Ply, Situation }
import play.api.libs.json.Json

import java.time.Instant

import lila.analyse.{ Analysis, Annotator }
import lila.core.config.NetDomain

import JsonApi.*
import readers.given
import lila.core.game.Player
import lila.core.id.GamePlayerId
import chess.format.FullFen
import chess.variant.Variant

import lila.tree.{ ExportOptions, TreeBuilder, NewTreeBuilder, NewRoot, Tree, NewBranch }
import lila.tree.Node.{ Comment, Comments }

final class TreeBuilderTest extends munit.FunSuite:

  test("tree builder test"):
    TestFixtures.treeBuilderTestCases.foreach: tc =>
      val (output, expected) = tc.test
      assertEquals(output.cleanup, NewRoot(expected).cleanup)

  extension (root: NewRoot)
    def cleanup: NewRoot =
      root
        .focus(_.tree.some)
        .modify(_.map(_.cleanup))
        .focus(_.metas.comments)
        .modify(_.cleanup)

  extension (node: NewBranch)
    def cleanup: NewBranch =
      node
        .focus(_.metas.clock)
        .set(none)
        .focus(_.metas.comments)
        .modify(_.cleanup)

  extension (comments: Comments)
    def cleanup: Comments =
      Comments(comments.value.map(_.copy(id = Comment.Id("i"))))

object TreeBuilderTest:

  case class TestCase(sans: List[SanStr], pgn: PgnStr, fishnetInput: String):

    given Executor = scala.concurrent.ExecutionContextOpportunistic
    val annotator  = Annotator(NetDomain("l.org"))
    val builder    = AnalysisBuilder(FishnetEvalCache.mock)

    lazy val parsedPgn      = Parser.full(pgn).toOption.get
    def logError(s: String) = ()
    val variant             = parsedPgn.tags.variant.getOrElse(Standard)
    val fen                 = parsedPgn.tags.fen.getOrElse(variant.initialFen)

    def makeGame(g: chess.Game) =
      lila.core.game
        .newGame(
          g,
          ByColor(Player(GamePlayerId("abcd"), _, none)),
          mode = chess.Mode.Casual,
          source = lila.core.game.Source.Api,
          pgnImport = none
        )
        .sloppy

    val ply           = chess.Game(variant.some, fen.some).ply
    val (game, moves) = AnnotatorTest.gameWithMoves(sans, fen, variant)
    val analysis      = AnnotatorTest.parse(builder, fishnetInput, fen.some, variant, moves, ply)

    def test =
      val x = TreeBuilder(makeGame(game), analysis.some, fen, ExportOptions.default, logError)
      val y = NewTreeBuilder(makeGame(game), analysis.some, fen, ExportOptions.default, logError)
      y -> x

    def testJson =
      val x = Tree.makeMinimalJsonString(makeGame(game), analysis.some, fen, ExportOptions.default, logError)
      val y =
        Tree.makeMinimalJsonStringNew(makeGame(game), analysis.some, fen, ExportOptions.default, logError)
      y -> x
