package lila.analyse

import lila.common.Maths.isCloseTo
import lila.common.config.{ NetDomain, BaseUrl }
import lila.tree.Eval
import chess.format.pgn.{ Pgn, PgnStr, InitialComments, Tag, Tags, Parser, PgnTree }
import chess.{ Node, Move, ByColor, Color, Ply }
import lila.game.PgnDump

class AnnotatorTest extends munit.FunSuite:

  given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val annotator = Annotator(NetDomain("l.org"))
  def makeGame(g: chess.Game) =
    lila.game.Game
      .make(
        g,
        ByColor(lila.game.Player.make(_, none)),
        mode = chess.Mode.Casual,
        source = lila.game.Source.Api,
        pgnImport = none
      )
      .sloppy
  val emptyPgn                = Pgn(Tags.empty, InitialComments.empty, None)
  def withAnnotator(pgn: Pgn) = pgn.copy(tags = pgn.tags + Tag(name = "Annotator", value = "l.org"))
  val emptyAnalysis           = Analysis(Analysis.Id(GameId("abcd")), Nil, Ply.initial, nowInstant, None)
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

  val dumper = PgnDump(BaseUrl("l.org/"), lila.user.LightUserApi.mock)
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
    assertEquals(
      annotator(dumped, makeGame(playedGame), none).copy(tags = Tags.empty).render,
      PgnStr("""1. a3 { A00 Anderssen's Opening } g6 2. g4""")
    )
