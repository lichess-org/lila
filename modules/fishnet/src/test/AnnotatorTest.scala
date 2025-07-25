package lila.fishnet
import chess.format.FullFen
import chess.format.pgn.{ Parser, Pgn, PgnStr, SanStr, Tags }
import chess.variant.{ Standard, Variant }
import chess.{ ByColor, Ply }
import play.api.libs.json.Json

import java.time.Instant

import lila.analyse.Annotator
import lila.core.config.NetDomain
import lila.core.game.Player
import lila.core.id.GamePlayerId

import JsonApi.*
import readers.given

final class AnnotatorTest extends munit.FunSuite:

  test("annotated games with fishnet input"):
    TestFixtures.annotatorTestCases.foreach: tc =>
      val (output, expected) = tc.test
      assertEquals(output, expected)

object AnnotatorTest:

  case class TestCase(sans: List[SanStr], pgn: PgnStr, fishnetInput: String, expected: PgnStr):

    given Executor = scala.concurrent.ExecutionContextOpportunistic
    val annotator = Annotator(NetDomain("l.org"))
    val builder = AnalysisBuilder(FishnetEvalCache.mock)

    lazy val parsedPgn = Parser.full(pgn).toOption.get
    lazy val dumped = parsedPgn.toPgn

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

    def test =
      val ply = chess.Game(variant.some, fen.some).ply
      val (game, moves) = AnnotatorTest.gameWithMoves(sans, fen, variant)
      val analysis = AnnotatorTest.parse(builder, fishnetInput, fen.some, variant, moves, ply)
      val p1 = annotator.addEvals(dumped, analysis)
      val p2 = annotator(p1, makeGame(game), analysis.some).copy(tags = Tags.empty)
      val output = annotator.toPgnString(p2)
      (output, expected)

  def gameWithMoves(sans: List[SanStr], fen: FullFen, variant: Variant): (chess.Game, String) =
    val (state = game, moves = moves) =
      chess.Game(variant, fen.some).playWhileValid(sans, Ply.initial)(_.move.toUci.uci).toOption.get
    game -> moves.mkString(" ")

  def parse(
      builder: AnalysisBuilder,
      fishnetInput: String,
      fen: Option[FullFen],
      variant: Variant,
      moves: String,
      ply: Ply
  ): lila.analyse.Analysis =
    val xs = Json.parse(fishnetInput).as[Request.PostAnalysis].analysis.flatten
    val analysis = Work.Analysis(
      Work.Id("workid"),
      Work.Sender(UserId("user"), None, false, false),
      Work.Game("TaHSAsYD", fen, None, variant, moves),
      ply,
      0,
      None,
      None,
      Nil,
      Instant.ofEpochMilli(1684055956),
      Work.Origin.manualRequest.some
    )
    builder(Client.offline, analysis, xs).await(1.second, "parse analysis")
