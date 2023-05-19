package lila.fishnet

import play.api.libs.json.Json

import java.time.Instant
import chess.format.pgn.{
  Dumper,
  SanStr,
  PgnStr,
  PgnNodeData,
  Move,
  Pgn,
  Initial,
  Parser,
  Tags,
  PgnTree,
  ParsedPgnTree,
  ParsedPgn
}
import chess.format.{ EpdFen, Uci }
import chess.variant.Standard
import chess.{ Clock, Node, Ply, MoveOrDrop, Situation }
import chess.MoveOrDrop.*

import lila.common.config.NetDomain
import lila.analyse.{ Analysis, Annotator }
import JsonApi.*
import readers.given

case class TestCase(sans: List[SanStr], pgn: PgnStr, fishnetInput: String, expected: PgnStr):

  import TestFixtures.*

  given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val annotator                           = Annotator(NetDomain("l.org"))
  val analysisBuilder                     = AnalysisBuilder(FishnetEvalCache.mock)

  lazy val gameWithMoves =
    val (_, xs, _) = chess.Replay.gameMoveWhileValid(sans, EpdFen.initial, Standard)
    val game       = xs.last._1
    val moves      = xs.map(_._2.uci.uci).mkString(" ")
    (game, moves)

  // Parse pgn and then convert it to Pgn directly
  lazy val dumped = Parser.full(pgn).toOption.get.toPgn

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

object TestFixtures:

  lazy val testCases = List(
    TestCase(sans1, PgnStr(pgn1), fish1, PgnStr(expected1))
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

  val sans1 =
    "e4 c5 Nf3 Nc6 Bb5 Qb6 Nc3 Nd4 Bc4 e6 O-O a6 d3 d6 Re1 Nf6 Rb1 Be7 Be3 Nxf3+ Qxf3 Qc7 a4 O-O Qg3 Kh8 f4 Qd8 e5 Nd7 exd6 Bxd6 Ne4 Be7 Qf2 Qc7 Ra1 a5 Bb5 b6 Qg3 Bb7 Bd2 Nf6 Ng5 h6 Qh3 Nd5 Rf1 Kg8 Ne4 f5 Ng3 Bf6 c3 Rad8 Rae1 Qf7 Nh5 Kh7 Nxf6+ Qxf6 Re5 Bc8 Rfe1 Nc7 Bc4 Rde8 Qf3 Re7 Be3 Rfe8 Bf2 Rd8 d4 Nd5 dxc5 bxc5 Bb5 Bb7 Qg3 Nc7 Bc4 Rd2 b3 Red7 Bxc5 Rd1 Bd4 Rxe1+ Qxe1 Qf7 Qe2 Nd5 Qf2 Bc6 Re1"
      .split(" ")
      .toList
      .map(SanStr(_))

  val pgn1 =
    """
1. e4 c5 2. Nf3 Nc6 3. Bb5 Qb6 4. Nc3 Nd4 5. Bc4 e6 6. O-O a6 7. d3 d6 8. Re1 Nf6 9. Rb1 Be7 10. Be3 Nxf3+ 11. Qxf3 Qc7 12. a4 O-O 13. Qg3 Kh8 14. f4 Qd8 15. e5 Nd7 16. exd6 Bxd6 17. Ne4 Be7 18. Qf2 Qc7 19. Ra1 a5 20. Bb5 b6 21. Qg3 Bb7 22. Bd2 Nf6 23. Ng5 h6 24. Qh3 Nd5 25. Rf1 Kg8 26. Ne4 f5 27. Ng3 Bf6 28. c3 Rad8 29. Rae1 Qf7 30. Nh5 Kh7 31. Nxf6+ Qxf6 32. Re5 Bc8 33. Rfe1 Nc7 34. Bc4 Rde8 35. Qf3 Re7 36. Be3 Rfe8 37. Bf2 Rd8 38. d4 Nd5 39. dxc5 bxc5 40. Bb5 Bb7 41. Qg3 Nc7 42. Bc4 Rd2 43. b3 Red7 44. Bxc5 Rd1 45. Bd4 Rxe1+ 46. Qxe1 Qf7 47. Qe2 Nd5 48. Qf2 Bc6 49. Re1 { White wins. } 1-0
  """.trim

  val expected1 =
    """1. e4 { [%eval 0.48] } 1... c5 { [%eval 0.35] } 2. Nf3 { [%eval 0.4] } 2... Nc6 { [%eval 0.42] } 3. Bb5 { [%eval 0.35] } { B30 Sicilian Defense: Nyezhmetdinov-Rossolimo Attack } 3... Qb6 { [%eval 0.73] } 4. Nc3 { [%eval 0.63] } 4... Nd4 { [%eval 0.87] } 5. Bc4 { [%eval 0.66] } 5... e6 { [%eval 0.69] } 6. O-O { [%eval 0.72] } 6... a6 { [%eval 0.59] } 7. d3 { [%eval 0.49] } 7... d6 { [%eval 0.75] } 8. Re1 { [%eval 0.53] } 8... Nf6 { [%eval 0.6] } 9. Rb1 { [%eval 0.11] } 9... Be7 { [%eval 0.37] } 10. Be3 { [%eval 0.04] } 10... Nxf3+ { [%eval 0.08] } 11. Qxf3 { [%eval 0.0] } 11... Qc7 { [%eval 0.1] } 12. a4 { [%eval 0.06] } 12... O-O { [%eval 0.05] } 13. Qg3 { [%eval -0.12] } 13... Kh8 { [%eval 0.03] } 14. f4 { [%eval -0.41] } 14... Qd8?! { (-0.41 → 0.14) Inaccuracy. Bd7 was best. } { [%eval 0.14] } (14... Bd7) 15. e5 { [%eval -0.09] } 15... Nd7?! { (-0.09 → 0.77) Inaccuracy. Ne8 was best. } { [%eval 0.77] } (15... Ne8) 16. exd6 { [%eval 0.73] } 16... Bxd6 { [%eval 0.7] } 17. Ne4 { [%eval 0.82] } 17... Be7 { [%eval 0.8] } 18. Qf2?! { (0.80 → 0.00) Inaccuracy. Bf2 was best. } { [%eval 0.0] } (18. Bf2 b6) 18... Qc7?! { (0.00 → 0.62) Inaccuracy. b6 was best. } { [%eval 0.62] } (18... b6 19. f5 exf5 20. Qxf5 Nf6 21. Qf3 Bb7 22. Qf5) 19. Ra1 { [%eval 0.67] } 19... a5 { [%eval 0.95] } 20. Bb5 { [%eval 0.46] } 20... b6 { [%eval 0.56] } 21. Qg3 { [%eval 0.36] } 21... Bb7 { [%eval 0.9] } 22. Bd2 { [%eval 0.71] } 22... Nf6 { [%eval 0.7] } 23. Ng5?! { (0.70 → 0.04) Inaccuracy. Bc3 was best. } { [%eval 0.04] } (23. Bc3 Nh5 24. Qg4 Nf6 25. Qg5 Ne8 26. Qg3 f6 27. Re3 Rd8 28. Rf1 Bc6 29. Bxc6 Qxc6) 23... h6 { [%eval 0.0] } 24. Qh3?! { (0.00 → -0.62) Inaccuracy. Nf3 was best. } { [%eval -0.62] } (24. Nf3 Rad8 25. Bc3 Bd6 26. Be5 Kh7 27. c3 Nh5 28. Qg4 g6 29. Bxd6 Qxd6 30. Ne5 Ng7) 24... Nd5 { [%eval -0.3] } 25. Rf1 { [%eval -0.57] } 25... Kg8 { [%eval -0.54] } 26. Ne4 { [%eval -0.39] } 26... f5 { [%eval -0.46] } 27. Ng3 { [%eval -0.85] } 27... Bf6 { [%eval -0.65] } 28. c3 { [%eval -0.87] } 28... Rad8 { [%eval -0.78] } 29. Rae1 { [%eval -0.48] } 29... Qf7 { [%eval -0.78] } 30. Nh5 { [%eval -0.8] } 30... Kh7 { [%eval -0.62] } 31. Nxf6+ { [%eval -0.03] } 31... Qxf6 { [%eval -0.66] } 32. Re5 { [%eval -0.57] } 32... Bc8 { [%eval -0.17] } 33. Rfe1 { [%eval -0.32] } 33... Nc7? { (-0.32 → 0.94) Mistake. Rd6 was best. } { [%eval 0.94] } (33... Rd6 34. Qf3 Qd8 35. Bc4 Bd7 36. Qd1 Rf6 37. Bxd5 Rxd5 38. Rxd5 exd5 39. d4 Bc6 40. Be3) 34. Bc4?! { (0.94 → 0.32) Inaccuracy. c4 was best. } { [%eval 0.32] } (34. c4 Rd4 35. Bc3 Rxf4 36. Rxe6 Nxe6 37. Bxf6 Rxf6 38. Be8 Rg4 39. Qf3 Ng5 40. Qa8 Be6) 34... Rde8?! { (0.32 → 1.24) Inaccuracy. Rd6 was best. } { [%eval 1.24] } (34... Rd6) 35. Qf3 { [%eval 0.86] } 35... Re7?? { (0.86 → 3.37) Blunder. Qd8 was best. } { [%eval 3.37] } (35... Qd8) 36. Be3?? { (3.37 → 1.39) Blunder. Qc6 was best. } { [%eval 1.39] } (36. Qc6 Ba6 37. Bxa6 Nxa6 38. Qxb6 Ra8 39. Rxe6 Rxe6 40. Qxe6 Rd8 41. Qxf6 gxf6 42. Re6 Rxd3) 36... Rfe8 { [%eval 1.97] } 37. Bf2 { [%eval 1.65] } 37... Rd8? { (1.65 → 3.21) Mistake. Nd5 was best. } { [%eval 3.21] } (37... Nd5 38. d4) 38. d4?! { (3.21 → 2.39) Inaccuracy. Qc6 was best. } { [%eval 2.39] } (38. Qc6 Red7 39. Qxb6 Nd5 40. Qxe6 Qxe6 41. Rxe6 Nxf4 42. Re8 Bb7 43. Bxc5 Nxd3 44. Rxd8 Rxd8) 38... Nd5 { [%eval 2.35] } 39. dxc5 { [%eval 2.36] } 39... bxc5 { [%eval 2.37] } 40. Bb5? { (2.37 → 0.95) Mistake. Bxc5 was best. } { [%eval 0.95] } (40. Bxc5 Rc7 41. Qf2 Qg6 42. Bf1 Rc6 43. Bd4 Rcd6 44. Bc4 Bb7 45. R5e2 Ba6 46. Bb5 Bxb5) 40... Bb7?? { (0.95 → 3.03) Blunder. Rc7 was best. } { [%eval 3.03] } (40... Rc7) 41. Qg3? { (3.03 → 1.51) Mistake. Bxc5 was best. } { [%eval 1.51] } (41. Bxc5 Rc7 42. Rxe6 Qf7 43. Qf2 Nf6 44. Be7 Rd1 45. Rxd1 Qxe6 46. Bd6 Rc8 47. Qe2 Ne4) 41... Nc7?! { (1.51 → 2.21) Inaccuracy. Rc8 was best. } { [%eval 2.21] } (41... Rc8 42. Bc4 Rcc7 43. b3 Qf7 44. h3 Rc8 45. Kh2 Ree8 46. Bb5 Bc6 47. Ba6 Bb7 48. Bxb7) 42. Bc4?! { (2.21 → 1.13) Inaccuracy. Bxc5 was best. } { [%eval 1.13] } (42. Bxc5 Rf7 43. Bd4 Rxd4 44. cxd4 Qd8 45. Qc3 Be4 46. h3 Nxb5 47. axb5 Rc7 48. Qe3 Bd5) 42... Rd2 { [%eval 1.02] } 43. b3 { [%eval 0.47] } 43... Red7 { [%eval 0.35] } 44. Bxc5?? { (0.35 → -3.99) Blunder. Rxc5 was best. } { [%eval -3.99] } (44. Rxc5 Be4) 44... Rd1?? { (-3.99 → 2.63) Blunder. Rxg2+ was best. } { [%eval 2.63] } (44... Rxg2+ 45. Qxg2 Bxg2 46. Kxg2 Rd2+ 47. R1e2 Rxe2+ 48. Bxe2 Nd5 49. Be3 Qh4 50. Bd2 Nxf4+ 51. Bxf4) 45. Bd4 { [%eval 2.76] } 45... Rxe1+ { [%eval 2.88] } 46. Qxe1 { [%eval 2.74] } 46... Qf7 { [%eval 3.31] } 47. Qe2?! { (3.31 → 2.40) Inaccuracy. Rxa5 was best. } { [%eval 2.4] } (47. Rxa5 Be4 48. Re5 Qg6 49. Bf1 Nd5 50. Qg3 Qf7 51. h3 Rd6 52. Bc4 Nb4 53. Kh2 Nc2) 47... Nd5 { [%eval 2.45] } 48. Qf2 { [%eval 2.38] } 48... Bc6 { [%eval 2.66] } 49. Re1 { [%eval 2.32] } { White wins. }


"""

  val fish1 = """
{
  "fishnet": {
    "version": "2.6.11-dev",
    "apikey": ""
  },
  "stockfish": {
    "flavor": "nnue"
  },
  "analysis": [
    {
      "pv": "d2d4 d7d5",
      "score": {
        "cp": 31
      },
      "depth": 23,
      "nodes": 1500597,
      "time": 1510,
      "nps": 993772
    },
    {
      "pv": "e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1h1 f6e4 d2d4 b7b5 a4b3 d7d5 d4e5 c8e6 c2c3 f8e7 b3c2 e8h8 h2h3",
      "score": {
        "cp": -48
      },
      "depth": 23,
      "nodes": 1500505,
      "time": 1606,
      "nps": 934311
    },
    {
      "pv": "g1f3",
      "score": {
        "cp": 35
      },
      "depth": 22,
      "nodes": 1500437,
      "time": 1666,
      "nps": 900622
    },
    {
      "pv": "d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1e3 f6g4 e3g5 h7h6 g5h4 g7g5 f1e2 g5h4 e2g4",
      "score": {
        "cp": -40
      },
      "depth": 22,
      "nodes": 1500170,
      "time": 1669,
      "nps": 898843
    },
    {
      "pv": "d2d4 c5d4",
      "score": {
        "cp": 42
      },
      "depth": 22,
      "nodes": 1500498,
      "time": 1706,
      "nps": 879541
    },
    {
      "pv": "e7e6 e1h1 g8e7 f1e1 c6d4 f3d4 c5d4 c2c3 e7c6 d2d3 f8e7 c1f4 e8h8 e4e5 d4c3 b2c3",
      "score": {
        "cp": -35
      },
      "depth": 23,
      "nodes": 1500028,
      "time": 1813,
      "nps": 827373
    },
    {
      "pv": "b1c3 e7e6 b5c6 b7c6 e4e5 b6c7 e1h1 f7f6 f1e1 f8e7 d2d4 c5d4 d1d4 f6e5 f3e5 e7f6 d4g4 g8e7 e5c4 h7h5",
      "score": {
        "cp": 73
      },
      "depth": 23,
      "nodes": 1500669,
      "time": 1761,
      "nps": 852168
    },
    {
      "pv": "e7e6",
      "score": {
        "cp": -63
      },
      "depth": 24,
      "nodes": 1501175,
      "time": 1631,
      "nps": 920401
    },
    {
      "pv": "f3d4 c5d4",
      "score": {
        "cp": 87
      },
      "depth": 23,
      "nodes": 1500901,
      "time": 1760,
      "nps": 852784
    },
    {
      "pv": "e7e6 e1h1 g8e7 d2d3 e7c6 f3d4 c5d4 c3e2 f8e7 e2g3 e8h8 c4b3 g8h8 c1d2 a7a5 a2a4 b6d8 f2f4 f7f5 e4f5 e6f5 d1f3 b7b6 f1e1 a8a7 e1e2 e7c5 a1e1 c6e7",
      "score": {
        "cp": -66
      },
      "depth": 24,
      "nodes": 1500119,
      "time": 1857,
      "nps": 807818
    },
    {
      "pv": "e1h1",
      "score": {
        "cp": 69
      },
      "depth": 21,
      "nodes": 1501247,
      "time": 1838,
      "nps": 816782
    },
    {
      "pv": "d4c6 f1e1",
      "score": {
        "cp": -72
      },
      "depth": 21,
      "nodes": 1500764,
      "time": 1798,
      "nps": 834685
    },
    {
      "pv": "f3d4 c5d4",
      "score": {
        "cp": 59
      },
      "depth": 23,
      "nodes": 1500537,
      "time": 1865,
      "nps": 804577
    },
    {
      "pv": "d4c6",
      "score": {
        "cp": -49
      },
      "depth": 22,
      "nodes": 1501068,
      "time": 1999,
      "nps": 750909
    },
    {
      "pv": "f3d4 c5d4 c3e2 g7g6 d1e1 f8g7 f2f4 d6d5 e4d5 g8e7 e2g3 e8h8 f4f5 e7f5 g3f5 e6f5 c1f4 c8d7 e1e7 d7a4 d5d6 a8e8 e7c7 b6c7 d6c7",
      "score": {
        "cp": 75
      },
      "depth": 23,
      "nodes": 1500795,
      "time": 1812,
      "nps": 828253
    },
    {
      "pv": "d4f3 d1f3",
      "score": {
        "cp": -53
      },
      "depth": 22,
      "nodes": 1500839,
      "time": 1898,
      "nps": 790747
    },
    {
      "pv": "f3d4 c5d4 c3e2 f8e7 h2h3 e8h8 a2a4 c8d7 a4a5 b6a7 c2c3 d4c3 c1e3 c3c2 d1c2 a7b8 e2c3 f8c8 c2b3 e7d8 e1c1 d7c6 d3d4 c6e4 c3e4 f6e4 d4d5",
      "score": {
        "cp": 60
      },
      "depth": 24,
      "nodes": 1500212,
      "time": 1791,
      "nps": 837639
    },
    {
      "pv": "d4c6",
      "score": {
        "cp": -11
      },
      "depth": 24,
      "nodes": 1500459,
      "time": 1794,
      "nps": 836376
    },
    {
      "pv": "f3d4",
      "score": {
        "cp": 37
      },
      "depth": 24,
      "nodes": 1501083,
      "time": 1888,
      "nps": 795065
    },
    {
      "pv": "d4f3 d1f3 b6c7 a2a4 e8h8 f3g3 g8h8 b2b4 c5b4 b1b4 f6h5 e3b6 c7b6 b4b6 h5g3 h2g3 e7f6 c3e2 c8d7 b6b7 d7a4",
      "score": {
        "cp": -4
      },
      "depth": 24,
      "nodes": 1500653,
      "time": 1807,
      "nps": 830466
    },
    {
      "pv": "d1f3 b6c7",
      "score": {
        "cp": 8
      },
      "depth": 23,
      "nodes": 1500548,
      "time": 1894,
      "nps": 792263
    },
    {
      "pv": "b6c7",
      "score": {
        "cp": 0
      },
      "depth": 22,
      "nodes": 1501288,
      "time": 1913,
      "nps": 784782
    },
    {
      "pv": "a2a4 e8h8",
      "score": {
        "cp": 10
      },
      "depth": 22,
      "nodes": 1501009,
      "time": 1910,
      "nps": 785868
    },
    {
      "pv": "e8h8 b2b3",
      "score": {
        "cp": -6
      },
      "depth": 21,
      "nodes": 1500085,
      "time": 1930,
      "nps": 777246
    },
    {
      "pv": "f3g3 g8h8",
      "score": {
        "cp": 5
      },
      "depth": 21,
      "nodes": 1500202,
      "time": 2098,
      "nps": 715062
    },
    {
      "pv": "g8h8 b1a1 b7b6 c3b1 c7d8 b1d2 d6d5 c4a2 c8b7 e4d5 b7d5 d2c4 b6b5 c4e5 d5a2 a1a2",
      "score": {
        "cp": 12
      },
      "depth": 22,
      "nodes": 1500586,
      "time": 1832,
      "nps": 819097
    },
    {
      "pv": "b1a1 b7b6 c3b1 c7d8 b1d2 d6d5 c4b3 c8b7 e4d5 b7d5 d3d4 d5b3 d2b3 c5c4 b3d2 b6b5 a4b5 a6b5 a1a8 d8a8",
      "score": {
        "cp": 3
      },
      "depth": 22,
      "nodes": 1501300,
      "time": 1907,
      "nps": 787257
    },
    {
      "pv": "c8d7",
      "score": {
        "cp": 41
      },
      "depth": 21,
      "nodes": 1501142,
      "time": 1775,
      "nps": 845713
    },
    {
      "pv": "e3f2 c8d7",
      "score": {
        "cp": 14
      },
      "depth": 21,
      "nodes": 1500963,
      "time": 1881,
      "nps": 797960
    },
    {
      "pv": "f6e8",
      "score": {
        "cp": 9
      },
      "depth": 24,
      "nodes": 1500962,
      "time": 1798,
      "nps": 834795
    },
    {
      "pv": "e5d6 e7d6 c3e4 d6e7 e3f2 b7b6 f4f5 d7f6 e4f6 e7f6 f5e6 f7e6 a4a5 b6a5 f2c5 f6h4 g3d6 h4f2 g1h1 d8d6 c5d6 f2e1 d6f8 e1b4 f8b4 a5b4 c2c3 h8g8 c3b4 g8f7 b1f1 f7e7",
      "score": {
        "cp": 77
      },
      "depth": 22,
      "nodes": 1501320,
      "time": 1821,
      "nps": 824448
    },
    {
      "pv": "e7h4 g3h3 h4e1 b1e1 b7b6 f4f5 d7e5 h3g3 f7f6 f5e6 d8d6 c4d5 a8a7 a4a5 c8e6 a5b6 a7e7",
      "score": {
        "cp": -73
      },
      "depth": 21,
      "nodes": 1500467,
      "time": 1734,
      "nps": 865321
    },
    {
      "pv": "c3e4 d6e7",
      "score": {
        "cp": 70
      },
      "depth": 21,
      "nodes": 1500660,
      "time": 1725,
      "nps": 869947
    },
    {
      "pv": "d6e7 e3f2 b7b6 f4f5 d7f6 e4f6 e7f6 f5e6 c8e6 c4e6 f7e6 g3g4 d8d7 e1e4 a8e8 a4a5 d7c6 a5b6 c6b6 b2b3 f6c3 e4c4 c3b4 f2g3",
      "score": {
        "cp": -82
      },
      "depth": 23,
      "nodes": 1500574,
      "time": 1684,
      "nps": 891077
    },
    {
      "pv": "e3f2 b7b6",
      "score": {
        "cp": 80
      },
      "depth": 22,
      "nodes": 1500490,
      "time": 1852,
      "nps": 810199
    },
    {
      "pv": "b7b6 f4f5 e6f5 f2f5 d7f6 f5f3 c8b7 f3f5",
      "score": {
        "cp": 0
      },
      "depth": 23,
      "nodes": 1501364,
      "time": 1800,
      "nps": 834091
    },
    {
      "pv": "b1a1 b7b6",
      "score": {
        "cp": 62
      },
      "depth": 22,
      "nodes": 1501011,
      "time": 1824,
      "nps": 822922
    },
    {
      "pv": "a8b8 a4a5",
      "score": {
        "cp": -67
      },
      "depth": 21,
      "nodes": 1501168,
      "time": 1716,
      "nps": 874806
    },
    {
      "pv": "f4f5 e6f5",
      "score": {
        "cp": 95
      },
      "depth": 22,
      "nodes": 1500666,
      "time": 1785,
      "nps": 840709
    },
    {
      "pv": "b7b6",
      "score": {
        "cp": -46
      },
      "depth": 22,
      "nodes": 1500686,
      "time": 1792,
      "nps": 837436
    },
    {
      "pv": "a1d1",
      "score": {
        "cp": 56
      },
      "depth": 23,
      "nodes": 1500374,
      "time ": 1794,
      "nps": 836328
    },
    {
      "pv": "d7f6 e3d2 f6e4 d3e4 e7f6 e4e5 f6e7 a1d1 c8d7 c2c4 d7b5 a4b5 a8d8 d2c3 d8d1 e1d1 f8d8 d1d6 e7f8 d6c6 d8d1 g1f2 c7d8 g3f3 d1c1",
      "score": {
        "cp": -36
      },
      "depth": 23,
      "nodes": 1500520,
      "time": 1705,
      "nps": 880070
    },
    {
      "pv": "f4f5 e6e5",
      "score": {
        "cp": 90
      },
      "depth": 22,
      "nodes": 1500952,
      "time": 1786,
      "nps": 840398
    },
    {
      "pv": "d7f6 d2c3",
      "score": {
        "cp": -71
      },
      "depth": 23,
      "nodes": 1500306,
      "time": 1775,
      "nps": 845242
    },
    {
      "pv": "d2c3 f6h5 g3g4 h5f6 g4g5 f6e8 g5g3 f7f6 e1e3 a8d8 a1f1 b7c6 b5c6 c7c6 b2b3 e8c7 g3h4 c7d5 e3h3 h7h6 c3b2 d5b4 f1c1 h8h7 h3g3",
      "score": {
        "cp": 70
      },
      "depth": 24,
      "nodes": 1500137,
      "time": 1856,
      "nps": 808263
    },
    {
      "pv": "f6d5 e1e2",
      "score": {
        "cp": -4
      },
      "depth": 21,
      "nodes": 1500397,
      "time": 1840,
      "nps": 815433
    },
    {
      "pv": "g5f3 a8d8 d2c3 e7d6 c3e5 h8h7 c2c3 f6h5 g3g4 g7g6 e5d6 c7d6 f3e5 h5g7 e1e2 d6c7 a1f1 f8g8 g4h3",
      "score": {
        "cp": 0
      },
      "depth": 23,
      "nodes": 1500321,
      "time": 1927,
      "nps": 778578
    },
    {
      "pv": "f6d5",
      "score": {
        "cp": 62
      },
      "depth": 22,
      "nodes": 1501132,
      "time": 1859,
      "nps": 807494
    },
    {
      "pv": "e1f1",
      "score": {
        "cp": -30
      },
      "depth": 21,
      "nodes": 1500406,
      "time": 1824,
      "nps": 822591
    },
    {
      "pv": "h8g8",
      "score": {
        "cp": 57
      },
      "depth": 22,
      "nodes": 1500950,
      "time": 1896,
      "nps": 791640
    },
    {
      "pv": "g5e4",
      "score": {
        "cp": -54
      },
      "depth": 21,
      "nodes": 1500495,
      "time": 1910,
      "nps": 785599
    },
    {
      "pv": "f7f5 e4c3 d5b4 a1c1 e7f6 f1e1 c7f7 b5c4 b7d5 c4b5 d5c6 b5c6 b4c6 b2b3 c6d4 g1h1 f7d7 c3b1 a8e8 h3g3 e6e5 f4e5",
      "score": {
        "cp": 39
      },
      "depth": 23,
      "nodes": 1500978,
      "time": 1837,
      "nps": 817081
    },
    {
      "pv": "e4c3 d5b4",
      "score": {
        "cp": -46
      },
      "depth": 21,
      "nodes": 1500011,
      "time": 1805,
      "nps": 831031
    },
    {
      "pv": "f8f6 a1e1 f6g6 g3h5 g8h7 f1f3 c7d6 f3f2 d5c7 b5c4 d6d4 h5g3 a8f8 d2c3 d4d7 c3e5 g6g4 e5c7 d7c7 e1e6 e7h4 c2c3",
      "score": {
        "cp": 85
      },
      "depth": 22,
      "nodes": 1500787,
      "time": 1742,
      "nps": 861530
    },
    {
      "pv": "a1e1",
      "score": {
        "cp": -65
      },
      "depth": 21,
      "nodes": 1500506,
      "time": 1877,
      "nps": 799417
    },
    {
      "pv": "b7c6 b5c6 c7c6 g3h5 a8d8 h3f3 c6d6 f3h3 d6d7 a1e1 d5c7 f1f3 d7a4 h5f6 f8f6 c3c4 f6g6 f3g3 g6g3 h3g3",
      "score": {
        "cp": 87
      },
      "depth": 23,
      "nodes": 1500842,
      "time": 1824,
      "nps": 822830
    },
    {
      "pv": "g3h5 b7c6",
      "score": {
        "cp": -78
      },
      "depth": 20,
      "nodes": 1500699,
      "time": 1892,
      "nps": 793181
    },
    {
      "pv": "c7d6 g3h5",
      "score": {
        "cp": 48
      },
      "depth": 22,
      "nodes": 1500529,
      "time": 1801,
      "nps": 833164
    },
    {
      "pv": "d2c1 g8h7",
      "score": {
        "cp": -78
      },
      "depth": 20,
      "nodes": 1500062,
      "time": 1752,
      "nps": 856199
    },
    {
      "pv": "f6e7",
      "score": {
        "cp": 80
      },
      "depth": 23,
      "nodes": 1500545,
      "time": 1784,
      "nps": 841112
    },
    {
      "pv": "g2g4 f5g4",
      "score": {
        "cp": -62
      },
      "depth": 21,
      "nodes": 1500230,
      "time": 1894,
      "nps": 792096
    },
    {
      "pv": "g7f6 b5c4",
      "score": {
        "cp": 3
      },
      "depth": 21,
      "nodes": 1500583,
      "time": 1823,
      "nps": 823139
    },
    {
      "pv": "e1e5 d8d6 h3h5 d5c7 d2e1 c7b5 a4b5 d6d3 e1h4 f6f7 h5f7 f8f7 e5e6 b7e4 e6b6 f7d7 b6a6 d3d2 f1f2 c5c4 a6a5",
      "score": {
        "cp": -66
      },
      "depth": 19,
      "nodes": 1501056,
      "time": 1835,
      "nps": 818014
    },
    {
      "pv": "d8d6 h3f3",
      "score": {
        "cp": 57
      },
      "depth": 21,
      "nodes": 1500447,
      "time": 1796,
      "nps": 835438
    },
    {
      "pv": "b5c4 d8d6",
      "score": {
        "cp": -17
      },
      "depth": 21,
      "nodes": 1500149,
      "time": 1763,
      "nps": 850906
    },
    {
      "pv": "d8d6 h3f3 f6d8 b5c4 c8d7 f3d1 f8f6 c4d5 d6d5 e5d5 e6d5 d3d4 d7c6 d2e3 d8d7 e3f2 c6a4 d1d2 c5c4 e1e5 d7c6 f2h4",
      "score": {
        "cp": 32
      },
      "depth": 24,
      "nodes": 1500202,
      "time": 1765,
      "nps": 849972
    },
    {
      "pv": "c3c4 d8d4 d2c3 d4f4 e5e6 c7e6 c3f6 f8f6 b5e8 f4g4 h3f3 e6g5 f3a8 c8e6 e1e2 g5h3 g1f1 h3f4 e2d2 f4g6 a8b8 e6g8 d2e2 g4d4 e8g6 f6g6",
      "score": {
        "cp": 94
      },
      "depth": 22,
      "nodes": 1500486,
      "time": 1681,
      "nps": 892615
    },
    {
      "pv": "d8d6",
      "score": {
        "cp": -32
      },
      "depth": 22,
      "nodes": 1501258,
      "time": 1753,
      "nps": 856393
    },
    {
      "pv": "b2b4",
      "score": {
        "cp": 124
      },
      "depth": 22,
      "nodes": 1500251,
      "time": 1647,
      "nps": 910899
    },
    {
      "pv": "f6d8",
      "score": {
        "cp": -86
      },
      "depth": 22,
      "nodes": 1500991,
      "time": 1802,
      "nps": 832958
    },
    {
      "pv": "f3c6 c8a6 c4a6 c7a6 c6b6 f8a8 e5e6 e7e6 b6e6 a8d8 e6f6 g7f6 e1e6 d8d3 e6a6 d3d2 a6a5 d2b2 a5c5 b2c2 a4a5",
      "score": {
        "cp": 337
      },
      "depth": 22,
      "nodes": 1501050,
      "time": 1806,
      "nps": 831146
    },
    {
      "pv": "c8a6 c4a6 c7a6 d3d4 f6f7 d4c5 a6c5 e3c5 b6c5 e5c5 e7b7 c5b5 b7b5 a4b5 f7c7 f3c6 c7f4 c6e6 a5a4 e6d5 f4h4 d5e5 h4c4 h2h3 c4b3 e1e2 b3d1 g1h2 f8f6",
      "score": {
        "cp": -139
      },
      "depth": 23,
      "nodes": 1501571,
      "time": 1735,
      "nps": 865458
    },
    {
      "pv": "d3d4 c8a6 c4a6 c7a6 f3e2 c5c4 e2c4 a6c7 c4d3 c7d5 e3d2 f6f7 c3c4 d5f6 h2h3 h7g8 e5b5 e7b7 d2c3 f6e4 e1e4 f5e4 d3e4 b7b8 g1h2 f7c7 b2b3 e8e7 b5h5 e7f7 h5e5",
      "score": {
        "cp": 197
      },
      "depth": 23,
      "nodes": 1501223,
      "time": 1820,
      "nps": 824847
    },
    {
      "pv": "c7d5 d3d4",
      "score": {
        "cp": -165
      },
      "depth": 21,
      "nodes": 1500223,
      "time": 1826,
      "nps": 821589
    },
    {
      "pv": "f3c6 e7d7 c6b6 c7d5 b6e6 f6e6 e5e6 d5f4 e6e8 c8b7 f2c5 f4d3 e8d8 d7d8 c5e7 d3e1 e7d8 e1g2 d8a5 g2e3 c4b5 e3d1 b2b3 f5f4 c3c4",
      "score": {
        "cp": 321
      },
      "depth": 23,
      "nodes": 1500406,
      "time": 1753,
      "nps": 855907
    },
    {
      "pv": "c7d5 d4c5 b6c5 f2c5 e7c7 f3f2 c7c6 c4f1 f6g6 c5d4 c6d6 b2b3 c8a6 f1a6 d6a6 h2h3 a6c6 ",
      "score": {
        "cp": -239
      },
      "depth": 22,
      "nodes": 1500832,
      "time": 1715,
      "nps": 875120
    },
    {
      "pv": "d4c5 b6c5",
      "score": {
        "cp": 235
      },
      "depth": 22,
      "nodes": 1500290,
      "time": 1715,
      "nps": 874804
    },
    {
      "pv": "b6c5 f2c5 e7c7 f3f2 f6g6 c4f1 c7c6 c5d4 c6d6 f2g3 g6f7 h2h3 d5e7 e5c5 e7c6 d4f2 f7e7 g3e3 c8b7",
      "score": {
        "cp": -236
      },
      "depth": 22,
      "nodes": 1500034,
      "time": 1794,
      "nps": 836139
    },
    {
      "pv": "f2c5 e7c7 f3f2 f6g6 c4f1 c7c6 c5d4 c6d6 f1c4 c8b7 e5e2 b7a6 c4b5 a6b5 a4b5 g6f7 d4e5 d6d7 h2h3 a5a4 f2c5",
      "score": {
        "cp": 237
      },
      "depth": 23,
      "nodes": 1500636,
      "time": 1751,
      "nps": 857016
    },
    {
      "pv": "e7c7",
      "score": {
        "cp": -95
      },
      "depth": 19,
      "nodes": 1501077,
      "time": 1743,
      "nps": 861203
    },
    {
      "pv": "f2c5 e7c7 e5e6 f6f7 f3f2 d5f6 c5e7 d8d1 e1d1 f7e6 e7d6 c7c8 f2e2 f6e4 d6e5 b7c6 h2h3 e6e8 d1d4 e8e6 g1h2 e6g6 e2e1 c8e8 c3c4 e8e7 b5c6 g6c6 e1a5",
      "score": {
        "cp": 303
      },
      "depth": 23,
      "nodes": 1500506,
      "time": 1593,
      "nps": 941937
    },
    {
      "pv": "d8c8 b5c4 c8c7 b2b3 f6f7 h2h3 c7c8 g1h2 e7e8 c4b5 b7c6 b5a6 c6b7 a6b7 f7b7 c3c4 d5f6 e5e6 e8e6 e1e6 b7f7 e6e5 f6e4 g3f3",
      "score": {
        "cp": -151
      },
      "depth": 23,
      "nodes": 1500982,
      "time": 1756,
      "nps": 854773
    },
    {
      "pv": "f2c5 e7f7 c5d4 d8d4 c3d4 f6d8 g3c3 b7e4 h2h3 c7b5 a4b5 f7c7 c3e3 e4d5 e1c1 c7c1 e3c1 a5a4 c1c3 d8h4 b5b6 h4f4 c3e3 f4h4",
      "score": {
        "cp": 221
      },
      "depth": 23,
      "nodes": 1500676,
      "time": 1685,
      "nps": 890608
    },
    {
      "pv": "d8d2 e5e2 d2e2 c4e2 e7d7 f2c5 d7d2 c5d4 f6f7 b2b3 c7e8 g3e3 f7g6 g2g3 d2a2 c3c4 b7e4 e2f1 e8f6 e1e2 a2a3 d4b2 a3a2 e2d2 g6f7 h2h3 f7g6",
      "score": {
        "cp": -113
      },
      "depth": 24,
      "nodes": 1500108,
      "time": 1702,
      "nps": 881379
    },
    {
      "pv": "e5e2 d2e2 c4e2 e7d7 f2c5 d7d2 c5d4 f6f7 b2b3 c7e8 g3e3 d2a2 c3c4 e8f6 e2f1 f6e4 e1e2 a2a3 d4b2 a3a2 b2c3 e4c3 e3c3 a2a3",
      "score": {
        "cp": 102
      },
      "depth": 23,
      "nodes": 1500961,
      "time": 1695,
      "nps": 885522
    },
    {
      "pv": "e7d7 h2h3 d2c2 c4d3 c2d2 d3e2 d2b2 e5c5 b7e4 f2d4 d7d4 c3d4 c7d5 c5c4 d5b6 c4c1 f6d4 g1h2 b6d5 e2f1",
      "score": {
        "cp": -47
      },
      "depth": 21,
      "nodes": 1500416,
      "time": 1848,
      "nps": 811913
    },
    {
      "pv": "e5c5 b7e4",
      "score": {
        "cp": 35
      },
      "depth": 19,
      "nodes": 1501354,
      "time": 1621,
      "nps": 926190
    },
    {
      "pv": "d2g2 g3g2 b7g2 g1g2 d7d2 e1e2 d2e2 c4e2 c7d5 c5e3 f6h4 e3d2 d5f4 d2f4 h4f4 e5e6 f4d2 e6e5 h7g6 b3b4 g6f6 e5e8 a5b4 c3b4 d2b4 e8b8 b4d2 g2f2 d2f4 f2g2 f4g5 g2f1",
      "score": {
        "cp": 399
      },
      "depth": 21,
      "nodes": 1500519,
      "time": 1353,
      "nps": 1109031
    },
    {
      "pv": "c5d4",
      "score": {
        "cp": 263
      },
      "depth": 22,
      "nodes": 1500676,
      "time": 1687,
      "nps": 889553
    },
    {
      "pv": "d7d4 c3d4",
      "score": {
        "cp": -276
      },
      "depth": 21,
      "nodes": 1500714,
      "time": 1546,
      "nps": 970707
    },
    {
      "pv": "g3e1 f6g6 e1g3 g6f7 e5a5 b7d5 h2h3 d5c4 b3c4 c7e8 a5e5 e8d6 g3d3 d6e4 a4a5 f7f6",
      "score": {
        "cp": 288
      },
      "depth": 22,
      "nodes": 1500613,
      "time": 1597,
      "nps": 939644
    },
    {
      "pv": "f6d8 h2h4 d8e8 e5a5 c7d5 e1g3 d5f6 a5e5 d7d6 e5e1 f6h5 g3f2 e8g6 d4e5 d6d8 a4a5 h5f6 e5f6 g6f6 a5a6 b7e4 e1c1 f6e7",
      "score": {
        "cp": -274
      },
      "depth": 22,
      "nodes": 1500386,
      "time": 1626,
      "nps": 922746
    },
    {
      "pv": "e5a5 b7e4 a5e5 f7g6 c4f1 c7d5 e1g3 g6f7 h2h3 d7d6 f1c4 d5b4 g1h2 b4c2",
      "score": {
        "cp": 331
      },
      "depth": 21,
      "nodes": 1500263,
      "time": 1600,
      "nps": 937664
    },
    {
      "pv": "b7d5 h2h3 f7f8 e2c2 f8d6 g1h2 h7g8 c2e2 d6c6 c4d5 c7d5",
      "score": {
        "cp": -240
      },
      "depth": 23,
      "nodes": 1501095,
      "time": 1589,
      "nps": 944679
    },
    {
      "pv": "e2f2",
      "score": {
        "cp": 245
      },
      "depth": 22,
      "nodes": 1500240,
      "time": 1736,
      "nps": 864193
    },
    {
      "pv": "d7d6 h2h3 f7g6 c4f1 d6d7 g1h2 d7c7 f1c4 d5f4 e5a5 g6g2 f2g2 f4g2 c4e6 f5f4 h2g1 g2e1 g1f2 e1f3 e6c4 f3d2 c4d3 b7e4 d3e4 d2e4 f2f3",
      "score": {
        "cp": -238
      },
      "depth": 23,
      "nodes": 1500291,
      "time": 1623,
      "nps": 924393
    },
    {
      "pv": "f2g3 c6b7 h2h3 d7d6 g1h2 d6d7 e5e2 d5c7 d4e5 b7d5 g3f2 f7e7 f2b6 d5c4 b3c4 c7e8 c4c5 d7d1 b6b3 d1c1 c5c6 e8f6",
      "score": {
        "cp": 266
      },
      "depth": 25,
      "nodes": 1500085,
      "time": 1754,
      "nps": 855236
    },
    {
      "pv": "c6b7",
      "score": {
        "cp": -232
      },
      "depth": 22,
      "nodes": 1501315,
      "time": 1672,
      "nps": 897915
    }
  ]
}
""".trim
