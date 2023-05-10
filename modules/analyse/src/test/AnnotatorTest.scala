package lila.analyse

import chess.{ ByColor, Color, Ply }
import lila.common.Maths.isCloseTo
import lila.common.config.{ NetDomain, BaseUrl }
import chess.format.pgn.{ Pgn, PgnStr, Initial, Tag, Tags, Parser, PgnTree }
import lila.tree.Eval
import chess.{ Node, Move }
import lila.game.PgnDump

class AnnotatorTest extends munit.FunSuite:

  val annotator = Annotator(NetDomain("l.org"))
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
  val emptyPgn                = Pgn(Tags.empty, Initial.empty, None)
  def withAnnotator(pgn: Pgn) = pgn.copy(tags = pgn.tags + Tag(name = "Annotator", value = "l.org"))
  val emptyAnalysis           = Analysis("abcd", None, Nil, Ply.initial, nowInstant, None)
  val emptyEval               = Eval(none, none, none)

  val pgnStr = PgnStr("""1. a3 g6?! 2. g4""")
  val playedGame: chess.Game =
    chess.format.pgn.Reader
      .fullWithSans(
        Parser.full(pgnStr).toOption.get,
        identity
      )
      .valid
      .toOption
      .get
      .state

  given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val dumper                              = PgnDump(BaseUrl("l.org/"), lila.user.LightUserApi.mock)
  val dumped =
    dumper(makeGame(playedGame), None, PgnDump.WithFlags(tags = false)).await(1.second, "test dump")

  test("empty game"):
    assertEquals(
      annotator(emptyPgn, makeGame(chess.Game(chess.variant.Standard)), none),
      withAnnotator(emptyPgn)
    )

  test("empty analysis"):
    assertEquals(
      annotator(emptyPgn, makeGame(chess.Game(chess.variant.Standard)), emptyAnalysis.some),
      withAnnotator(emptyPgn)
    )

  test("opening comment"):
    // val analysis = emptyAnalysis.copy(
    //   infos = List(Info(Ply(1), emptyEval, Nil))
    // )
    assertEquals(
      annotator(dumped, makeGame(playedGame), none).copy(tags = Tags.empty).render,
      PgnStr("""1. a3 { A00 Anderssen's Opening } 2.g6 2. g4""")
    )

  // test("simple analysis"):
  //   val analysis = emptyAnalysis.copy(
  //     infos = List(
  //       Info(Ply(1), emptyEval.copy(cp = Eval.Cp(300).some), Nil),
  //       Info(Ply(2), emptyEval.copy(cp = Eval.Cp(-300).some), Nil),
  //       Info(Ply(3), emptyEval.copy(cp = Eval.Cp(300).some), Nil),
  //       Info(Ply(4), emptyEval.copy(cp = Eval.Cp(-300).some), Nil)
  //     )
  //   )
  //   assertEquals(
  //     annotator(dumped, makeGame(playedGame), analysis.some),
  //     annotatedPgn
  //   )
