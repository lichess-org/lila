package lila.fishnet

import JsonApi.*
import play.api.libs.json.Reads
import play.api.libs.json.*
import lila.game.PgnImport
import lila.fishnet.JsonApi.Request.Evaluation
import chess.format.pgn.{
  Move,
  Dumper,
  SanStr,
  Pgn,
  PgnStr,
  Initial,
  Tag,
  Tags,
  Parser,
  PgnTree,
  ParsedPgnTree,
  ParsedPgn,
  PgnNodeData
}
import chess.{ Node, Ply, MoveOrDrop, Situation }
import chess.MoveOrDrop.*
import lila.analyse.{ Analysis, Annotator }
import lila.common.config.NetDomain
import lila.game.PgnDump
import lila.common.config.BaseUrl
import chess.variant.Standard
import lila.fishnet.Work.Analysis
import java.time.Instant
import chess.Clock

final class AnnotatorTest extends munit.FunSuite:

  import readers.given
  val reader = summon[Reads[Request.PostAnalysis]]

  given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  given Conversion[String, PgnStr]        = PgnStr(_)
  given Conversion[PgnStr, String]        = _.value

  test("readable"):
    val json = Json.parse(fishnetInput)
    assert(reader.reads(json).isSuccess)

  val analysisBuilder = AnalysisBuilder(FishnetEvalCache.mock)

  def parseFishnetInput(str: String): List[Evaluation.EvalOrSkip] =
    Json.parse(fishnetInput).as[Request.PostAnalysis].analysis.flatten

  def parseAnalysis(str: String): lila.analyse.Analysis =
    val xs     = parseFishnetInput(str)
    val userId = UserId("user")
    val sender = Work.Sender(userId, None, false, false)
    val gameId = "TaHSAsYD"
    val game   = Work.Game(gameId, None, None, Standard, gameWithMoves._2)
    val analysis = Work.Analysis(
      Work.Id("workid"),
      sender,
      game,
      Ply.initial,
      0,
      None,
      None,
      Nil,
      Instant.ofEpochMilli(1684055956)
    )
    val client = Client.offline
    analysisBuilder(client, analysis, xs).await(1.second, "parse analysis")

  val annotator = Annotator(NetDomain("l.org"))

  lazy val gameWithMoves = {
    val (_, xs, _) =
      chess.Replay.gameMoveWhileValid(gameSans, chess.format.EpdFen.initial, chess.variant.Standard)
    val game  = xs.last._1
    val moves = xs.map(_._2.uci.uci).mkString(" ")
    (game, moves)
  }

  // Parse pgn and then convert it to Pgn directly
  lazy val dumped = Parser.full(gamePgn).toOption.get.toPgn

  def makeGame(g: chess.Game) =
    lila.game.Game
      .make(
        g,
        whitePlayer = lila.game.Player.make(chess.White, none),
        blackPlayer = lila.game.Player.make(chess.Black, none),
        mode = chess.Mode.Casual,
        source = lila.game.Source.Api,
        pgnImport = none
      )
      .sloppy

  lazy val dumper = PgnDump(BaseUrl("l.org/"), lila.user.LightUserApi.mock)

  // test("annotated game without fishnet input"):
  //   assertEquals(
  //     annotator(dumped, makeGame(gameWithMoves._1), none).render,
  //     PgnStr(gamePgn)
  //   )

  test("annotated game with fishnet input"):
    val analysis = parseAnalysis(fishnetInput)
    assertEquals(
      annotator(dumped, makeGame(gameWithMoves._1), analysis.some).copy(tags = Tags.empty).render,
      PgnStr(annotatedPgn)
    )

  case class Context(sit: Situation, ply: Ply)

  extension (d: PgnNodeData)
    def toMove(context: Context): Option[(Situation, Move)] =
      def toSan(mv: MoveOrDrop): SanStr =
        mv.fold(x => Dumper(context.sit, x, x.situationAfter), x => Dumper(x, x.situationAfter))

      d.san(context.sit)
        .toOption
        .map(x =>
          (
            x.situationAfter,
            Move(
              ply = context.ply,
              san = toSan(x),
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
      tree.mapAccumlOption_(Context(game.situation, game.ply)) { (ctx, d) =>
        d.toMove(ctx) match
          case Some((sit, m)) => (Context(sit, ctx.ply.next), m.some)
          case None           => (ctx, None)
      }

  extension (pgn: ParsedPgn)
    def toPgn: Pgn =
      val game = makeChessGame(pgn.tags)
      Pgn(
        tags = pgn.tags,
        initial = Initial(pgn.initialPosition.comments),
        tree = pgn.tree.flatMap(_.toPgn(game))
      )

  private def makeChessGame(tags: Tags) =
    val g = chess.Game(
      variantOption = tags(_.Variant) flatMap chess.variant.Variant.byName,
      fen = tags.fen
    )
    g.copy(
      startedAtPly = g.ply,
      clock = tags.clockConfig map Clock.apply
    )

  val gameSans =
    "e4 c5 Nf3 Nc6 Bb5 Qb6 Nc3 Nd4 Bc4 e6 O-O a6 d3 d6 Re1 Nf6 Rb1 Be7 Be3 Nxf3+ Qxf3 Qc7 a4 O-O Qg3 Kh8 f4 Qd8 e5 Nd7 exd6 Bxd6 Ne4 Be7 Qf2 Qc7 Ra1 a5 Bb5 b6 Qg3 Bb7 Bd2 Nf6 Ng5 h6 Qh3 Nd5 Rf1 Kg8 Ne4 f5 Ng3 Bf6 c3 Rad8 Rae1 Qf7 Nh5 Kh7 Nxf6+ Qxf6 Re5 Bc8 Rfe1 Nc7 Bc4 Rde8 Qf3 Re7 Be3 Rfe8 Bf2 Rd8 d4 Nd5 dxc5 bxc5 Bb5 Bb7 Qg3 Nc7 Bc4 Rd2 b3 Red7 Bxc5 Rd1 Bd4 Rxe1+ Qxe1 Qf7 Qe2 Nd5 Qf2 Bc6 Re1"
      .split(" ")
      .toList
      .map(SanStr(_))

  val gamePgn =
    """
1. e4 { [%clk 0:10:00] } 1... c5 { [%clk 0:10:00] } 2. Nf3 { [%clk 0:09:57] } 2... Nc6 { [%clk 0:09:58] } 3. Bb5 { [%clk 0:09:55] } { B30 Sicilian Defense: Nyezhmetdinov-Rossolimo Attack } 3... Qb6 { [%clk 0:09:56] } 4. Nc3 { [%clk 0:09:47] } 4... Nd4 { [%clk 0:09:54] } 5. Bc4 { [%clk 0:09:38] } 5... e6 { [%clk 0:09:52] } 6. O-O { [%clk 0:09:30] } 6... a6 { [%clk 0:09:50] } 7. d3 { [%clk 0:09:26] } 7... d6 { [%clk 0:09:46] } 8. Re1 { [%clk 0:09:13] } 8... Nf6 { [%clk 0:09:37] } 9. Rb1 { [%clk 0:08:43] } 9... Be7 { [%clk 0:09:13] } 10. Be3 { [%clk 0:08:33] } 10... Nxf3+ { [%clk 0:09:01] } 11. Qxf3 { [%clk 0:08:31] } 11... Qc7 { [%clk 0:08:58] } 12. a4 { [%clk 0:08:18] } 12... O-O { [%clk 0:08:51] } 13. Qg3 { [%clk 0:07:56] } 13... Kh8 { [%clk 0:08:47] } 14. f4 { [%clk 0:07:48] } 14... Qd8 { [%clk 0:08:28] } 15. e5 { [%clk 0:07:33] } 15... Nd7 { [%clk 0:08:12] } 16. exd6 { [%clk 0:07:12] } 16... Bxd6 { [%clk 0:08:08] } 17. Ne4 { [%clk 0:07:11] } 17... Be7 { [%clk 0:08:06] } 18. Qf2 { [%clk 0:05:35] } 18... Qc7 { [%clk 0:07:12] } 19. Ra1 { [%clk 0:03:29] } 19... a5 { [%clk 0:07:06] } 20. Bb5 { [%clk 0:03:02] } 20... b6 { [%clk 0:07:00] } 21. Qg3 { [%clk 0:02:46] } 21... Bb7 { [%clk 0:06:47] } 22. Bd2 { [%clk 0:02:42] } 22... Nf6 { [%clk 0:06:27] } 23. Ng5 { [%clk 0:02:37] } 23... h6 { [%clk 0:06:16] } 24. Qh3 { [%clk 0:02:34] } 24... Nd5 { [%clk 0:05:37] } 25. Rf1 { [%clk 0:02:24] } 25... Kg8 { [%clk 0:04:45] } 26. Ne4 { [%clk 0:02:20] } 26... f5 { [%clk 0:04:33] } 27. Ng3 { [%clk 0:02:15] } 27... Bf6 { [%clk 0:04:13] } 28. c3 { [%clk 0:02:14] } 28... Rad8 { [%clk 0:03:50] } 29. Rae1 { [%clk 0:02:11] } 29... Qf7 { [%clk 0:03:47] } 30. Nh5 { [%clk 0:02:09] } 30... Kh7 { [%clk 0:03:08] } 31. Nxf6+ { [%clk 0:01:57] } 31... Qxf6 { [%clk 0:03:07] } 32. Re5 { [%clk 0:01:50] } 32... Bc8 { [%clk 0:02:41] } 33. Rfe1 { [%clk 0:01:47] } 33... Nc7 { [%clk 0:02:34] } 34. Bc4 { [%clk 0:01:45] } 34... Rde8 { [%clk 0:02:31] } 35. Qf3 { [%clk 0:01:40] } 35... Re7 { [%clk 0:02:15] } 36. Be3 { [%clk 0:01:37] } 36... Rfe8 { [%clk 0:01:55] } 37. Bf2 { [%clk 0:01:31] } 37... Rd8 { [%clk 0:01:19] } 38. d4 { [%clk 0:01:29] } 38... Nd5 { [%clk 0:00:53] } 39. dxc5 { [%clk 0:01:26] } 39... bxc5 { [%clk 0:00:52] } 40. Bb5 { [%clk 0:01:08] } 40... Bb7 { [%clk 0:00:47] } 41. Qg3 { [%clk 0:01:02] } 41... Nc7 { [%clk 0:00:37] } 42. Bc4 { [%clk 0:00:57] } 42... Rd2 { [%clk 0:00:33] } 43. b3 { [%clk 0:00:51] } 43... Red7 { [%clk 0:00:27] } 44. Bxc5 { [%clk 0:00:48] } 44... Rd1 { [%clk 0:00:16] } 45. Bd4 { [%clk 0:00:41] } 45... Rxe1+ { [%clk 0:00:14] } 46. Qxe1 { [%clk 0:00:39] } 46... Qf7 { [%clk 0:00:10] } 47. Qe2 { [%clk 0:00:34] } 47... Nd5 { [%clk 0:00:06] } 48. Qf2 { [%clk 0:00:30] } 48... Bc6 { [%clk 0:00:01] } 49. Re1 { [%clk 0:00:27] } { White wins on time. } 1-0
  """.trim

  val annotatedPgn =
    """
1. e4 { [%eval 0.0] [%clk 0:10:00] } 1... c5 { [%eval 0.2] [%clk 0:09:57] } 2. Nf3 { [%eval 0.0] [%clk 0:09:55] } 2... Nc6 { [%eval 0.0] [%clk 0:09:47] } 3. Bb5 { [%eval 0.0] [%clk 0:09:38] } { B30 Sicilian Defense: Nyezhmetdinov-Rossolimo Attack } 3... Qb6?! { (0.00 → 0.64) Inaccuracy. e6 was best. } { [%eval 0.64] [%clk 0:09:30] } 4. Nc3 { [%eval 0.52] [%clk 0:09:26] } 4... Nd4 { [%eval 0.86] [%clk 0:09:13] } 5. Bc4 { [%eval 0.63] [%clk 0:08:43] } 5... e6 { [%eval 0.76] [%clk 0:08:33] } 6. O-O { [%eval 0.55] [%clk 0:08:31] } 6... a6 { [%eval 0.83] [%clk 0:08:18] } 7. d3 { [%eval 0.94] [%clk 0:07:56] } 7... d6 { [%eval 0.89] [%clk 0:07:48] } 8. Re1 { [%eval 0.66] [%clk 0:07:33] } 8... Nf6 { [%eval 0.74] [%clk 0:07:12] } 9. Rb1?! { (0.74 → 0.05) Inaccuracy. Nxd4 was best. } { [%eval 0.05] [%clk 0:07:11] } 9... Be7 { [%eval 0.33] [%clk 0:05:35] } 10. Be3 { [%eval 0.19] [%clk 0:03:29] } 10... Nxf3+ { [%eval -0.01] [%clk 0:03:02] } 11. Qxf3 { [%eval -0.09] [%clk 0:02:46] } 11... Qc7 { [%eval 0.0] [%clk 0:02:42] } 12. a4 { [%eval -0.12] [%clk 0:02:37] } 12... O-O { [%eval 0.0] [%clk 0:02:34] } 13. Qg3 { [%eval -0.08] [%clk 0:02:24] } 13... Kh8 { [%eval 0.0] [%clk 0:02:20] } 14. f4 { [%eval -0.5] [%clk 0:02:15] } 14... Qd8?! { (-0.50 → 0.23) Inaccuracy. Bd7 was best. } { [%eval 0.23] [%clk 0:02:14] } 15. e5 { [%eval 0.05] [%clk 0:02:11] } 15... Nd7 { [%eval 0.46] [%clk 0:02:09] } 16. exd6 { [%eval 0.3] [%clk 0:01:57] } 16... Bxd6 { [%eval 0.78] [%clk 0:01:50] } 17. Ne4 { [%eval 0.59] [%clk 0:01:47] } 17... Be7 { [%eval 0.79] [%clk 0:01:45] } 18. Qf2?! { (0.79 → -0.21) Inaccuracy. Bf2 was best. } { [%eval -0.21] [%clk 0:01:40] } 18... Qc7?! { (-0.21 → 0.77) Inaccuracy. b6 was best. } { [%eval 0.77] [%clk 0:01:37] } 19. Ra1 { [%eval 0.67] [%clk 0:01:31] } 19... a5 { [%eval 0.98] [%clk 0:01:29] } 20. Bb5 { [%eval 0.73] [%clk 0:01:26] } 20... b6 { [%eval 0.31] [%clk 0:01:08] } 21. Qg3 { [%eval 0.2] [%clk 0:01:02] } 21... Bb7?! { (0.20 → 0.91) Inaccuracy. Nf6 was best. } { [%eval 0.91] [%clk 0:00:57] } 22. Bd2?! { (0.91 → 0.15) Inaccuracy. f5 was best. } { [%eval 0.15] [%clk 0:00:51] } 22... Nf6 { [%eval 0.24] [%clk 0:00:48] } 23. Ng5 { [%eval 0.09] [%clk 0:00:41] } 23... h6 { [%eval 0.0] [%clk 0:00:39] } 24. Qh3 { [%eval -0.45] [%clk 0:00:34] } 24... Nd5 { [%eval -0.17] [%clk 0:00:30] } 25. Rf1 { [%eval -0.55] [%clk 0:00:27] } 25... Kg8 { [%eval -0.37] } 26. Ne4 { [%eval -0.18] } 26... f5 { [%eval -0.35] } 27. Ng3 { [%eval -0.66] } 27... Bf6 { [%eval -0.78] } 28. c3 { [%eval -0.85] } 28... Rad8 { [%eval -0.61] } 29. Rae1 { [%eval -0.91] } 29... Qf7 { [%eval -0.76] } 30. Nh5 { [%eval -0.84] } 30... Kh7 { [%eval -0.64] } 31. Nxf6+ { [%eval -0.67] } 31... Qxf6 { [%eval -0.25] } 32. Re5 { [%eval -0.19] } 32... Bc8 { [%eval 0.0] } 33. Rfe1 { [%eval -0.29] } 33... Nc7?! { (-0.29 → 0.41) Inaccuracy. Rd6 was best. } { [%eval 0.41] } 34. Bc4 { [%eval 0.27] } 34... Rde8?! { (0.27 → 1.37) Inaccuracy. Nd5 was best. } { [%eval 1.37] } 35. Qf3 { [%eval 1.35] } 35... Re7?? { (1.35 → 3.95) Blunder. Qd8 was best. } { [%eval 3.95] } 36. Be3?? { (3.95 → 1.44) Blunder. Qc6 was best. } { [%eval 1.44] } 36... Rfe8?! { (1.44 → 2.15) Inaccuracy. Ba6 was best. } { [%eval 2.15] } 37. Bf2 { [%eval 1.61] } 37... Rd8?! { (1.61 → 2.83) Inaccuracy. Nd5 was best. } { [%eval 2.83] } 38. d4 { [%eval 2.71] } 38... Nd5 { [%eval 2.51] } 39. dxc5 { [%eval 2.81] } 39... bxc5 { [%eval 2.33] } 40. Bb5? { (2.33 → 1.07) Mistake. Bxc5 was best. } { [%eval 1.07] } 40... Bb7?? { (1.07 → 3.39) Blunder. Rc7 was best. } { [%eval 3.39] } 41. Qg3? { (3.39 → 1.52) Mistake. Bxc5 was best. } { [%eval 1.52] } 41... Nc7? { (1.52 → 2.97) Mistake. Rc8 was best. } { [%eval 2.97] } 42. Bc4? { (2.97 → 1.20) Mistake. Bxc5 was best. } { [%eval 1.2] } 42... Rd2 { [%eval 1.2] } 43. b3?! { (1.20 → 0.39) Inaccuracy. Rxc5 was best. } { [%eval 0.39] } 43... Red7 { [%eval 0.39] } 44. Bxc5?? { (0.39 → -3.95) Blunder. Rxc5 was best. } { [%eval -3.95] } 44... Rd1?? { (-3.95 → 3.11) Blunder. Rxg2+ was best. } { [%eval 3.11] } 45. Bd4 { [%eval 3.09] } 45... Rxe1+ { [%eval 3.51] } 46. Qxe1 { [%eval 3.04] } 46... Qf7?! { (3.04 → 4.29) Inaccuracy. Qg6 was best. } { [%eval 4.29] } 47. Qe2? { (4.29 → 2.30) Mistake. Rxa5 was best. } { [%eval 2.3] } 47... Nd5 { [%eval 2.5] } 48. Qf2 { [%eval 2.53] } 48... Bc6 { [%eval 2.71] } 49. Re1 { [%eval 2.36] } { White wins on time. } 1-0
  """.trim

  val fishnetInput = """
{
  "fishnet": {
    "version": "2.6.10",
    "apikey": "offline"
  },
  "stockfish": {
    "flavor": "nnue"
  },
  "analysis": [
    {
      "pv": "d2d4 g8f6",
      "score": {
        "cp": 22
      },
      "depth": 25,
      "nodes": 3000045,
      "time": 2053,
      "nps": 1461298
    },
    {
      "pv": "c7c6 d2d4",
      "score": {
        "cp": -46
      },
      "depth": 24,
      "nodes": 3000126,
      "time": 2113,
      "nps": 1419841
    },
    {
      "pv": "g1f3 b8c6 f1b5 e7e5 e1h1 f8d6 c2c3 a7a6 b5a4 g8e7 d2d4 c5d4 c3d4 c6d4 f3d4 e5d4 d1d4 d8c7 g2g3 e8h8 b1c3 b7b5 a4b3 d6c5 d4d3 c8b7 b3c2",
      "score": {
        "cp": 33
      },
      "depth": 25,
      "nodes": 3000906,
      "time": 2132,
      "nps": 1407554
    },
    {
      "pv": "e7e6 d2d4 c5d4 f3d4 b8c6 b1c3 g8f6 d4b5 d7d6 c1f4 e6e5 f4g5 a7a6 b5a3 b7b5 g5f6 g7f6 c3d5 f8g7 c2c4 e8h8 c4b5 c6d4 f1d3 f6f5",
      "score": {
        "cp": -47
      },
      "depth": 25,
      "nodes": 3001336,
      "time": 2136,
      "nps": 1405119
    },
    {
      "pv": "d2d4 c5d4 f3d4 g8f6 b1c3 e7e5 d4b5 d7d6 c1g5 a7a6 b5a3 b7b5 c3d5 f8e7 g5f6 e7f6 c2c4 b5b4 a3c2 a6a5 g2g3 f6g5 h2h4 g5h6 f1h3 c8e6 b2b3 e8h8 h3g4",
      "score": {
        "cp": 34
      },
      "depth": 25,
      "nodes": 3000754,
      "time": 2123,
      "nps": 1413449
    },
    {
      "pv": "e7e6",
      "score": {
        "cp": -42
      },
      "depth": 25,
      "nodes": 3000315,
      "time": 2149,
      "nps": 1396144
    },
    {
      "pv": "b5a4 g8f6 b1c3 e7e6 e4e5 f6d5 c3d5 e6d5 d2d4 c6d4 f3d4 c5d4 e1h1 f8c5 a2a3 a7a5 b2b4 a5b4 a3b4 b6b4",
      "score": {
        "cp": 61
      },
      "depth": 25,
      "nodes": 3001263,
      "time": 2178,
      "nps": 1377990
    },
    {
      "pv": "e7e6 b5c6 b7c6 e4e5 b6c7 d2d3 g8e7 c1f4 e7d5 f4g3 d5c3 b2c3 c5c4 f3g5 f8e7 g5e4 c4d3 d1d3 c6c5 e1h1 c8b7 e4d6 e7d6",
      "score": {
        "cp": -56
      },
      "depth": 26,
      "nodes": 3000670,
      "time": 2188,
      "nps": 1371421
    },
    {
      "pv": "f3d4 c5d4",
      "score": {
        "cp": 91
      },
      "depth": 27,
      "nodes": 3001143,
      "time": 2145,
      "nps": 1399134
    },
    {
      "pv": "e7e6 e1h1",
      "score": {
        "cp": -66
      },
      "depth": 25,
      "nodes": 3001055,
      "time": 2171,
      "nps": 1382337
    },
    {
      "pv": "e1h1 f8d6 d2d3 d6b8 f3d4 c5d4 d1g4 g8e7 c3e2 e8h8 c4b3 a7a5 a2a4 a8a6 g4g5 f7f6 g5b5 b8a7 b5b6 a7b6 b3c4 a6a8 b2b3 e7c6 c1a3",
      "score": {
        "cp": 62
      },
      "depth": 25,
      "nodes": 3000149,
      "time": 2133,
      "nps": 1406539
    },
    {
      "pv": "d4c6",
      "score": {
        "cp": -61
      },
      "depth": 23,
      "nodes": 3000103,
      "time": 2108,
      "nps": 1423198
    },
    {
      "pv": "f3d4",
      "score": {
        "cp": 72
      },
      "depth": 27,
      "nodes": 3000096,
      "time": 2123,
      "nps": 1413139
    },
    {
      "pv": "d4c6 c4b3 b6c7 f1e1 d7d6 c1f4 g8f6 d3d4 c5d4 c3d5 e6d5 e4d5 c6e7 c2c4 e8d8 f4d6 c7d6 f3e5 d8e8 c4c5 d6c5 a1c1 c5b6",
      "score": {
        "cp": -47
      },
      "depth": 25,
      "nodes": 3000906,
      "time": 2117,
      "nps": 1417527
    },
    {
      "pv": "f3d4 c5d4",
      "score": {
        "cp": 77
      },
      "depth": 25,
      "nodes": 3001167,
      "time": 2183,
      "nps": 1374790
    },
    {
      "pv": "d4c6 c3e2",
      "score": {
        "cp": -44
      },
      "depth": 24,
      "nodes": 3000231,
      "time": 2268,
      "nps": 1322853
    },
    {
      "pv": "f3d4",
      "score": {
        "cp": 62
      },
      "depth": 28,
      "nodes": 3000740,
      "time": 2098,
      "nps": 1430285
    },
    {
      "pv": "d4c6",
      "score": {
        "cp": -19
      },
      "depth": 26,
      "nodes": 3001146,
      "time": 2108,
      "nps": 1423693
    },
    {
      "pv": "f3d4 c5d4 c3e2 e8h8 c4b3 d6d5 e4d5 f6d5 b3d5 e6d5 c1f4 b6f6 e2g3 e7b4 f4e5 f6c6 e1e2 b4c5 b2b4 c5b6 h2h3",
      "score": {
        "cp": 40
      },
      "depth": 28,
      "nodes": 3001189,
      "time": 2089,
      "nps": 1436662
    },
    {
      "pv": "d4f3",
      "score": {
        "cp": -7
      },
      "depth": 26,
      "nodes": 3000857,
      "time": 2128,
      "nps": 1410177
    },
    {
      "pv": "d1f3 b6c7 a2a4 e8h8 e3f4 c8d7 b1a1 g8h8 c4b3 d7c6 a4a5 a8e8 f3h3 f6d7 f4e3 b7b5 a5b6 c7b6 a1a2 b6b7 e1a1 d6d5 e4d5",
      "score": {
        "cp": 16
      },
      "depth": 25,
      "nodes": 3000764,
      "time": 2188,
      "nps": 1371464
    },
    {
      "pv": "b6c7 a2a4 e8h8 e3f4 c8d7 b1a1 d7c6 a4a5 h7h6 f3h3 g8h7 f4d2 f8g8 c3a4 a8f8 a4b6 g7g5 b2b4 c5b4 d2b4",
      "score": {
        "cp": -12
      },
      "depth": 25,
      "nodes": 3001257,
      "time": 2171,
      "nps": 1382430
    },
    {
      "pv": "a2a4 e8h8",
      "score": {
        "cp": 3
      },
      "depth": 26,
      "nodes": 3000810,
      "time": 2146,
      "nps": 1398327
    },
    {
      "pv": "e8h8 b2b3 c8d7 d3d4 c7a5 c3e2 b7b5 a4b5 a6b5 c4d3 f8c8 d4c5 d6c5 e4e5 f6d5 d3e4 d7c6 c2c4 d5b4 e2c3 c6e4 f3e4 b5c4 b3c4 a5c7 c3b5",
      "score": {
        "cp": 0
      },
      "depth": 24,
      "nodes": 3000411,
      "time": 2228,
      "nps": 1346683
    },
    {
      "pv": "e3f4",
      "score": {
        "cp": 13
      },
      "depth": 24,
      "nodes": 3000222,
      "time": 2115,
      "nps": 1418544
    },
    {
      "pv": "g8h8 b1a1",
      "score": {
        "cp": 0
      },
      "depth": 26,
      "nodes": 3001281,
      "time": 2106,
      "nps": 1425109
    },
    {
      "pv": "b1a1",
      "score": {
        "cp": 5
      },
      "depth": 23,
      "nodes": 3000724,
      "time": 2101,
      "nps": 1428236
    },
    {
      "pv": "c8d7",
      "score": {
        "cp": 37
      },
      "depth": 22,
      "nodes": 3000101,
      "time": 2011,
      "nps": 1491845
    },
    {
      "pv": "e3f2 c8d7",
      "score": {
        "cp": 10
      },
      "depth": 24,
      "nodes": 3000171,
      "time": 2052,
      "nps": 1462071
    },
    {
      "pv": "f6e8 e5d6",
      "score": {
        "cp": 12
      },
      "depth": 28,
      "nodes": 3000048,
      "time": 2026,
      "nps": 1480773
    },
    {
      "pv": "e5d6",
      "score": {
        "cp": 70
      },
      "depth": 24,
      "nodes": 3000278,
      "time": 2040,
      "nps": 1470724
    },
    {
      "pv": "e7h4",
      "score": {
        "cp": -52
      },
      "depth": 23,
      "nodes": 3000981,
      "time": 2083,
      "nps": 1440701
    },
    {
      "pv": "c3e4 d6e7 e3f2 b7b6 f4f5 d7f6 e4f6 e7f6 f5e6 f7e6 h2h3 f8e8 g3g4 d8d7 a4a5 b6a5 e1e2 c8b7 c4e6 d7c6 b1e1 f6b2",
      "score": {
        "cp": 79
      },
      "depth": 25,
      "nodes": 3001238,
      "time": 2042,
      "nps": 1469754
    },
    {
      "pv": "d6e7",
      "score": {
        "cp": -70
      },
      "depth": 25,
      "nodes": 3000359,
      "time": 2040,
      "nps": 1470764
    },
    {
      "pv": "e3f2 b7b6 f4f5 d7f6 e4f6 e7f6 f5e6 f7e6 h2h3 a6a5 e1e4 a8a7 c4e6 c8e6 e4e6 a7f7 g1h1 f6d4 f2d4 d8d4 e6e4 d4f2 g3f2",
      "score": {
        "cp": 80
      },
      "depth": 24,
      "nodes": 3000538,
      "time": 2055,
      "nps": 1460115
    },
    {
      "pv": "b7b6 f4f5 e6f5 f2f5 d7f6 f5f3 c8b7 f3f5 b7c8",
      "score": {
        "cp": 0
      },
      "depth": 28,
      "nodes": 3000608,
      "time": 2064,
      "nps": 1453782
    },
    {
      "pv": "f4f5",
      "score": {
        "cp": 69
      },
      "depth": 24,
      "nodes": 3001218,
      "time": 2056,
      "nps": 1459736
    },
    {
      "pv": "b7b6 f4f5",
      "score": {
        "cp": -64
      },
      "depth": 24,
      "nodes": 3000786,
      "time": 2043,
      "nps": 1468813
    },
    {
      "pv": "f4f5 e6f5 e3f4 d7e5 f2g3 f7f6 e4f6 e7f6 f4e5 c7d8 g3e3 f6e5 e3e5 f5f4 e5c5 f4f3 g2f3 f8f3 e1f1 c8g4 a1e1 d8h4 c4d5 f3f5 f1f5 h4e1 f5f1",
      "score": {
        "cp": 97
      },
      "depth": 25,
      "nodes": 3001186,
      "time": 2046,
      "nps": 1466855
    },
    {
      "pv": "b7b6",
      "score": {
        "cp": -52
      },
      "depth": 25,
      "nodes": 3000225,
      "time": 2059,
      "nps": 1457127
    },
    {
      "pv": "a1d1 d7f6 e4f6 e7f6 d3d4 c5d4 e3d4 f6d4 d1d4 c8a6 d4d7 c7c8 d7d6 a6b5 a4b5 a8b8 d6c6 c8d8 c2c3 h7h6 h2h3 h8h7 f4f5 e6f5 f2f5 h7g8 g1h2 d8d2",
      "score": {
        "cp": 49
      },
      "depth": 26,
      "nodes": 3001172,
      "time": 2064,
      "nps": 1454056
    },
    {
      "pv": "d7f6",
      "score": {
        "cp": -22
      },
      "depth": 25,
      "nodes": 3000520,
      "time": 2066,
      "nps": 1452333
    },
    {
      "pv": "f4f5 e6e5 e3d2 c5c4 b5c4 d7f6 e4c3 f6h5 g3f2 e7c5 d2e3 c5e3 f2e3 a8e8 a1d1 h5f4 d1d2 f7f6 d2f2 e8d8 b2b3 h7h6 h2h4 h8h7 g1h2",
      "score": {
        "cp": 86
      },
      "depth": 25,
      "nodes": 3001052,
      "time": 2123,
      "nps": 1413590
    },
    {
      "pv": "d7f6 d2c3",
      "score": {
        "cp": -58
      },
      "depth": 26,
      "nodes": 3000159,
      "time": 2050,
      "nps": 1463492
    },
    {
      "pv": "d2c3",
      "score": {
        "cp": 65
      },
      "depth": 25,
      "nodes": 3001026,
      "time": 2082,
      "nps": 1441414
    },
    {
      "pv": "f6d5 g5e4 f7f5 e4g5 e7g5 f4g5 f5f4 g3h3 f4f3 g2f3 d5f4 d2f4 c7f4 h3g4 f4d2 g4g2 d2b4 c2c3 b4f4 g2g4 f4d2 g4g2",
      "score": {
        "cp": 0
      },
      "depth": 25,
      "nodes": 3001398,
      "time": 2088,
      "nps": 1437451
    },
    {
      "pv": "g5f3 a8d8 d2c3 e7d6 c3e5 d6e5 f3e5 f6h5 g3h4 h5f6",
      "score": {
        "cp": 0
      },
      "depth": 27,
      "nodes": 3000641,
      "time": 2159,
      "nps": 1389829
    },
    {
      "pv": "f6d5 e1f1",
      "score": {
        "cp": 42
      },
      "depth": 25,
      "nodes": 3000678,
      "time": 2203,
      "nps": 1362087
    },
    {
      "pv": "e1f1 h8g8 g5e4 f7f5 e4c3 d5b4 a1c1 f8f6 f1f2 g8h7 b5c4 c7d7 c3b5 f6g6 c1e1 a8d8 d2c3 b4d5 c3e5 g6g4 g2g3 g7g5 h3h5 e7f6",
      "score": {
        "cp": -32
      },
      "depth": 25,
      "nodes": 3001469,
      "time": 2198,
      "nps": 1365545
    },
    {
      "pv": "h8g8 a1e1",
      "score": {
        "cp": 43
      },
      "depth": 25,
      "nodes": 3000377,
      "time": 2162,
      "nps": 1387778
    },
    {
      "pv": "g5e4 f7f5 e4c3 d5b4 a1c1 f8f6 f1f2 g8h8 b5c4 c7d7 c3b5 f6g6 d2c3 b4d5 c1e1 a8f8 c3e5 e7f6 e5f6 f8f6 h3f3 b7c6 c4d5 c6d5 f3e3 d5c6",
      "score": {
        "cp": -42
      },
      "depth": 25,
      "nodes": 3000588,
      "time": 2101,
      "nps": 1428171
    },
    {
      "pv": "f7f5 e4c3 d5b4 a1c1 f8f6 f1f2 f6g6 b5c4 a8f8 c3b5 c7d7 c2c3 b4d5 c1e1 b7c6 g2g3 f8e8 h3h5 g8h7 f2e2 e7d8 h5h3 d8f6 h3h5 e8e7 c4b3 g6g4 h5h3",
      "score": {
        "cp": 54
      },
      "depth": 27,
      "nodes": 3001155,
      "time": 2165,
      "nps": 1386214
    },
    {
      "pv": "e4c3 d5b4 a1c1 f8f6 b5c4 c7d7 f1f2 g8h7 c3b5 a8f8 d2c3 f6g6 c1e1 b4d5 c3e5 e7f6 b2b3 g6g4 g2g3 f6e5 e1e5 g7g5 f4g5 g4g5 d3d4 f8e8 c4d5 b7d5",
      "score": {
        "cp": -44
      },
      "depth": 25,
      "nodes": 3000981,
      "time": 2101,
      "nps": 1428358
    },
    {
      "pv": "f8f6",
      "score": {
        "cp": 91
      },
      "depth": 24,
      "nodes": 3001357,
      "time": 2143,
      "nps": 1400539
    },
    {
      "pv": "a1e1 c7d6 d2c1 b7c6 c2c3 c6b5 a4b5 f6e7 g3e2 a8d8 g2g4 d6d7 f1f3 f8f6 g4g5 f6g6 h3h5 g8h7 h2h4 d7b5 c3c4",
      "score": {
        "cp": -67
      },
      "depth": 25,
      "nodes": 3000233,
      "time": 2123,
      "nps": 1413204
    },
    {
      "pv": "b7c6 b5c6",
      "score": {
        "cp": 66
      },
      "depth": 24,
      "nodes": 3000115,
      "time": 2135,
      "nps": 1405206
    },
    {
      "pv": "g3h5 b7c6 b5c4 d8d6 h5f6 f8f6 b2b3 d5e7 f1f2 c6d5 c4d5 e7d5 a1e1 c7d7 f2f3 f6f7 c3c4 d5b4 d2b4 a5b4 f3e3 f7f8 h3f3 f8d8 f3e2",
      "score": {
        "cp": -68
      },
      "depth": 22,
      "nodes": 3000651,
      "time": 2200,
      "nps": 1363932
    },
    {
      "pv": "c7f7",
      "score": {
        "cp": 74
      },
      "depth": 24,
      "nodes": 3001125,
      "time": 2139,
      "nps": 1403050
    },
    {
      "pv": "g3h5 f6e7",
      "score": {
        "cp": -88
      },
      "depth": 24,
      "nodes": 3000552,
      "time": 2104,
      "nps": 1426117
    },
    {
      "pv": "f6e7",
      "score": {
        "cp": 79
      },
      "depth": 25,
      "nodes": 3001521,
      "time": 2192,
      "nps": 1369307
    },
    {
      "pv": "h5f6 f7f6",
      "score": {
        "cp": -55
      },
      "depth": 24,
      "nodes": 3000524,
      "time": 2122,
      "nps": 1414007
    },
    {
      "pv": "f7f6",
      "score": {
        "cp": 52
      },
      "depth": 24,
      "nodes": 3001179,
      "time": 2126,
      "nps": 1411655
    },
    {
      "pv": "f1f2 d5c7 b5c4 d8d6 b2b4 c5b4 c3b4 c7d5 b4b5 d5c3 d2c3 f6c3 h3e3 b7d5 c4d5 d6d5 d3d4 c3d4",
      "score": {
        "cp": -43
      },
      "depth": 24,
      "nodes": 3001043,
      "time": 2116,
      "nps": 1418262
    },
    {
      "pv": "d8d6 h3h5",
      "score": {
        "cp": 36
      },
      "depth": 23,
      "nodes": 3000065,
      "time": 2078,
      "nps": 1443727
    },
    {
      "pv": "h3f3",
      "score": {
        "cp": -12
      },
      "depth": 23,
      "nodes": 3001277,
      "time": 2097,
      "nps": 1431224
    },
    {
      "pv": "d8d6 h3f3",
      "score": {
        "cp": 27
      },
      "depth": 27,
      "nodes": 3001154,
      "time": 2034,
      "nps": 1475493
    },
    {
      "pv": "c3c4 d8d4",
      "score": {
        "cp": 67
      },
      "depth": 24,
      "nodes": 3000796,
      "time": 2042,
      "nps": 1469537
    },
    {
      "pv": "d8d6",
      "score": {
        "cp": -18
      },
      "depth": 25,
      "nodes": 3000543,
      "time": 2037,
      "nps": 1473020
    },
    {
      "pv": "b2b4 c5b4 c3b4 a5b4 h3e3 e8d8 d2b4 f8f7 e3b6 c7d5 c4d5 d8d5 e5d5 e6d5 b6f6 f7f6 g1f2 f6b6 b4c5 b6a6 e1e8 c8d7 e8e7 d7a4 c5d4 a6g6 g2g3 a4d1 d4e5 d1h5 e7a7 h7g8 f2e3",
      "score": {
        "cp": 120
      },
      "depth": 27,
      "nodes": 3000051,
      "time": 1975,
      "nps": 1519013
    },
    {
      "pv": "f6d8 d2e3",
      "score": {
        "cp": -100
      },
      "depth": 23,
      "nodes": 3000029,
      "time": 2024,
      "nps": 1482227
    },
    {
      "pv": "f3c6 f6g6 c6b6 f8f6 e1e2 g6e8 b6a5 c8b7 a5c5 f6g6 a4a5 e8a4 a5a6 a4d1 d2e1 b7g2 e2g2 g6g2 g1g2",
      "score": {
        "cp": 343
      },
      "depth": 24,
      "nodes": 3001181,
      "time": 1993,
      "nps": 1505861
    },
    {
      "pv": "c8a6",
      "score": {
        "cp": -121
      },
      "depth": 25,
      "nodes": 3000649,
      "time": 1967,
      "nps": 1525495
    },
    {
      "pv": "d3d4 c8a6 c4a6 c7a6 f3e2 a6c7 d4c5 b6c5 e3c5 e7d7 c5d4 f6d8 e2f2 d7d5 h2h3 d8d7 b2b3 e8b8 e1b1 b8a8 g1h2 d5d6 e5c5 c7d5 d4e5",
      "score": {
        "cp": 237
      },
      "depth": 26,
      "nodes": 3000671,
      "time": 2005,
      "nps": 1496594
    },
    {
      "pv": "c7d5 d3d4",
      "score": {
        "cp": -182
      },
      "depth": 23,
      "nodes": 3000592,
      "time": 2125,
      "nps": 1412043
    },
    {
      "pv": "f3c6 d8d7 c6b6 c7d5 b6c5 d7c7 c5d4 c8b7 f2g3 f6f7 h2h3 e7e8 c4b5 b7c6 c3c4 d5b4 d4c3 e8d8 d3d4 c6e4 e1e4 f5e4 e5e4 f7g6",
      "score": {
        "cp": 311
      },
      "depth": 26,
      "nodes": 3001057,
      "time": 2061,
      "nps": 1456116
    },
    {
      "pv": "c7d5 d4c5",
      "score": {
        "cp": -255
      },
      "depth": 23,
      "nodes": 3001159,
      "time": 2061,
      "nps": 1456166
    },
    {
      "pv": "d4c5 b6c5 f2c5 e7c7 f3f2 f6g6 c4f1 c7b7 c5d4 g6g4 g2g3 g4g6 f1a6 d8d6 a6b7 c8b7 d4c5 d6a6 f2f3 g6f7 f3e2 d5f6 e5e6 a6e6",
      "score": {
        "cp": 248
      },
      "depth": 24,
      "nodes": 3001564,
      "time": 2048,
      "nps": 1465607
    },
    {
      "pv": "b6c5 f2c5 e7c7 f3f2 f6g6 c4f1 c7b7 c5d4 g6g4 h2h3 g4f4 f2f4 d5f4 e5b5 h6h5 g2g3 f4g6 b5b7 c8b7 e1e6 h5h4 g1f2 h4g3 f2g3 f5f4 g3f2 b7d5",
      "score": {
        "cp": -246
      },
      "depth": 24,
      "nodes": 3001286,
      "time": 2079,
      "nps": 1443620
    },
    {
      "pv": "f2c5 e7c7 f3f2 f6g6 c4f1 c7c6 c5d4 c6d6 f2g3 g6f7 f1c4 d5e7 e5e2 e7d5 d4e5 d6b6 c4b5 c8a6 e5d4 a6b5 d4b6 b5e2 b6d8",
      "score": {
        "cp": 252
      },
      "depth": 24,
      "nodes": 3000225,
      "time": 2046,
      "nps": 1466385
    },
    {
      "pv": "e7c7",
      "score": {
        "cp": -84
      },
      "depth": 22,
      "nodes": 3001151,
      "time": 2047,
      "nps": 1466121
    },
    {
      "pv": "f2c5 e7c7 e5e6 f6f7 f3f2 d5f6 c5e7 d8d1 e1d1 f7e6 e7d6 c7c8 d6e5 f6g4 f2e2 g4e5 f4e5 b7d5 h2h3 c8c5 e2e3 c5c7 e3d3 d5e4 d3d6 e6f7 e5e6 f7g6 d1d2 g6g5 d2e2 c7e7 b2b4 a5b4 c3b4",
      "score": {
        "cp": 298
      },
      "depth": 25,
      "nodes": 3000954,
      "time": 2003,
      "nps": 1498229
    },
    {
      "pv": "d8c8",
      "score": {
        "cp": -153
      },
      "depth": 24,
      "nodes": 3000837,
      "time": 2097,
      "nps": 1431014
    },
    {
      "pv": "f2c5 e7f7 g3f2 d8d5 b5f1 d5e5 e1e5 c7d5 c5d4 f6e7 h2h3 e7d7 f1b5 d7d6 b5d3 d6c7 g1h2",
      "score": {
        "cp": 244
      },
      "depth": 24,
      "nodes": 3000675,
      "time": 2099,
      "nps": 1429573
    },
    {
      "pv": "d8d2",
      "score": {
        "cp": -92
      },
      "depth": 27,
      "nodes": 3000840,
      "time": 2029,
      "nps": 1478974
    },
    {
      "pv": "e5e2",
      "score": {
        "cp": 104
      },
      "depth": 26,
      "nodes": 3000618,
      "time": 2019,
      "nps": 1486190
    },
    {
      "pv": "e7d7",
      "score": {
        "cp": -35
      },
      "depth": 23,
      "nodes": 3000941,
      "time": 2037,
      "nps": 1473216
    },
    {
      "pv": "e5e2",
      "score": {
        "cp": 56
      },
      "depth": 23,
      "nodes": 3000750,
      "time": 2041,
      "nps": 1470235
    },
    {
      "pv": "d2g2 g3g2 b7g2 g1g2 d7d2 e1e2 d2e2 c4e2 c7d5 c5e3 f6h4 e5e6 h4e1 e2c4 d5e3 g2f3 e1g1 e6e3 g1g4 f3f2 g4f4 f2e2 f4h2 e2d3 h6h5 c4d5 h2d6 c3c4 h5h4 d3d4 d6b4 e3e6 b4b3 d4e5 b3d3 c4c5 h4h3",
      "score": {
        "cp": 406
      },
      "depth": 23,
      "nodes": 3000706,
      "time": 1769,
      "nps": 1696272
    },
    {
      "pv": "c5d4 d1e1 g3e1 f6d8 h2h3 b7d5 e1g3 d8e7 g1h2 e7d6 g3e3 d6a3 c4d3 a3b3 e5f5 h7g8 f5h5 b3a2 e3g3 a2d2 h5h6 d5g2 d3h7 g8f8 d4c5 f8e8",
      "score": {
        "cp": 252
      },
      "depth": 24,
      "nodes": 3000710,
      "time": 2019,
      "nps": 1486235
    },
    {
      "pv": "d1e1 g3e1",
      "score": {
        "cp": -258
      },
      "depth": 23,
      "nodes": 3000445,
      "time": 1998,
      "nps": 1501724
    },
    {
      "pv": "g3e1 f6d8",
      "score": {
        "cp": 263
      },
      "depth": 24,
      "nodes": 3001389,
      "time": 1958,
      "nps": 1532885
    },
    {
      "pv": "f6d8 h2h4 d7d4 c3d4 d8d4 e1f2 d4d1 g1h2 d1d6 f2e1 b7e4 e1a5 c7d5 a5d2 d6d8 h2g3 d8b6 a4a5 b6g1 c4d5",
      "score": {
        "cp": -280
      },
      "depth": 24,
      "nodes": 3000877,
      "time": 1969,
      "nps": 1524061
    },
    {
      "pv": "e5a5 c7d5 e1f2 g7g5 f4g5 h6g5 f2g3 h7g6 h2h4 f7e7 a5a7 e6e5 d4e5 g5g4 e5d4 d5f6 a4a5 f6h5 g3e5 e7e5 d4e5 g4g3 e5d4 d7e7 c4f1",
      "score": {
        "cp": 337
      },
      "depth": 24,
      "nodes": 3000226,
      "time": 2032,
      "nps": 1476489
    },
    {
      "pv": "b7d5 h2h3 f7f8 e2c2 f8d6 g1h2 d6c6 c4d3 h7g8 e5e1 c6b7 e1b1 c7e8 b3b4 a5b4 b1b4 b7a8 d4e5 d7d8 d3c4 d5c4 b4c4",
      "score": {
        "cp": -237
      },
      "depth": 25,
      "nodes": 3000322,
      "time": 2058,
      "nps": 1457882
    },
    {
      "pv": "e2f2",
      "score": {
        "cp": 246
      },
      "depth": 24,
      "nodes": 3000055,
      "time": 1999,
      "nps": 1500777
    },
    {
      "pv": "d7d6 h2h3 f7g6 c4f1 d6d7 g1h2 d7d6 f2g3 g6f7 f1c4 d6d7 g3f2 d7d8 e5e2 d8e8 f2g3 e8g8 e2e5 g7g5 f4g5 g8g5",
      "score": {
        "cp": -257
      },
      "depth": 26,
      "nodes": 3000751,
      "time": 2011,
      "nps": 1492168
    },
    {
      "pv": "f2g3 c6b7 h2h3 d7d6 g1h2 d6d7 e5e2 d7c7 d4e5 c7c8 g3d3 b7a8 d3d4 f7e7 e2d2 e7b7 c4d5 e6d5 d4d3 b7f7 d3a6 c8e8 a6a5 d5d4",
      "score": {
        "cp": 262
      },
      "depth": 28,
      "nodes": 3000783,
      "time": 2017,
      "nps": 1487745
    },
    {
      "pv": "c6b7 h2h3 f7g6 e1e2 g6f7 g1h2 g7g5 f4g5 h6g5 g2g3 d7c7 e2e1 c7d7",
      "score": {
        "cp": -247
      },
      "depth": 25,
      "nodes": 3000699,
      "time": 2116,
      "nps": 1418099
    }
  ]
}
  """
