package lila.fishnet

import chess.MoveOrDrop.*
import chess.format.pgn.{
  InitialComments,
  Move,
  ParsedPgn,
  ParsedPgnTree,
  Parser,
  Pgn,
  PgnNodeData,
  PgnStr,
  PgnTree,
  SanStr,
  Tags
}
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

final class AnnotatorTest extends munit.FunSuite:

  test("annotated games with fishnet input"):
    TestFixtures.testCases.foreach: tc =>
      val (output, expected) = tc.test
      assertEquals(output, expected)

case class TestCase(sans: List[SanStr], pgn: PgnStr, fishnetInput: String, expected: PgnStr):

  given Executor      = scala.concurrent.ExecutionContextOpportunistic
  val annotator       = Annotator(NetDomain("l.org"))
  val analysisBuilder = AnalysisBuilder(FishnetEvalCache.mock)

  lazy val parsedPgn = Parser.full(pgn).toOption.get
  lazy val dumped    = parsedPgn.toPgn
  val variant        = parsedPgn.tags.variant.getOrElse(Standard)
  val fen            = parsedPgn.tags.fen.getOrElse(variant.initialFen)
  lazy val chessGame = chess.Game(
    variantOption = variant.some,
    fen = fen.some
  )
  lazy val gameWithMoves =
    val (_, xs, _) = chess.Replay.gameMoveWhileValid(sans, fen, variant)
    val game       = xs.last._1
    val moves      = xs.map(_._2.uci.uci).mkString(" ")
    (game, moves)

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

  def test =
    val analysis = parseAnalysis(fishnetInput)
    val p1       = annotator.addEvals(dumped, analysis)
    val p2       = annotator(p1, makeGame(gameWithMoves._1), analysis.some).copy(tags = Tags.empty)
    val output   = annotator.toPgnString(p2)
    (output, expected)

  def parseAnalysis(str: String): lila.analyse.Analysis =
    val xs     = Json.parse(fishnetInput).as[Request.PostAnalysis].analysis.flatten
    val userId = UserId("user")
    val sender = Work.Sender(userId, None, false, false)
    val gameId = "TaHSAsYD"
    val game   = Work.Game(gameId, Some(fen), None, variant, gameWithMoves._2)
    val analysis = Work.Analysis(
      Work.Id("workid"),
      sender,
      game,
      chessGame.ply,
      0,
      None,
      None,
      Nil,
      Instant.ofEpochMilli(1684055956)
    )
    val client = Client.offline
    analysisBuilder(client, analysis, xs).await(1.second, "parse analysis")

case class Context(sit: Situation, ply: Ply)

extension (d: PgnNodeData)
  def toMove(context: Context): Option[(Situation, Move)] =
    d.san(context.sit)
      .toOption
      .map(x =>
        (
          x.situationAfter,
          Move(
            ply = context.ply,
            san = x.toSanStr,
            comments = d.comments,
            glyphs = d.glyphs,
            opening = None,
            result = None,
            secondsLeft = None,
            variationComments = d.variationComments
          )
        )
      )

extension (tree: ParsedPgnTree)
  def toPgn(game: chess.Game): Option[PgnTree] =
    tree.mapAccumlOption_(Context(game.situation, game.ply + 1)) { (ctx, d) =>
      d.toMove(ctx) match
        case Some((sit, m)) => (Context(sit, ctx.ply.next), m.some)
        case None           => (ctx, None)
    }

extension (pgn: ParsedPgn)
  def toPgn: Pgn =
    val game = makeChessGame(pgn.tags)
    Pgn(
      tags = pgn.tags,
      initial = InitialComments(pgn.initialPosition.comments),
      tree = pgn.tree.flatMap(_.toPgn(game))
    )

private def makeChessGame(tags: Tags) =
  val g = chess.Game(
    variantOption = tags.variant,
    fen = tags.fen
  )
  g.copy(
    startedAtPly = g.ply,
    clock = tags.clockConfig.map(Clock.apply)
  )
