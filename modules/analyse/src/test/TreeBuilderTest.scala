package lila.analyse

import chess.eval.{ Score, WhiteScore }
import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr, SanStr }
import chess.{ ByColor, Ply }

import lila.core.id.GamePlayerId
import lila.tree.{ Engine, Eval, ExportOptions, TreeBuilder }

class TreeBuilderTest extends munit.FunSuite:

  private def cpEval(cp: Int) = Eval(Some(WhiteScore.fromWhite(Score.cp(cp))), None)

  test("analysis variation stops when the game is over"):
    val fenStr = "7k/8/8/8/8/3p4/8/K3N3 w - - 0 1"
    val fen = Fen.Full(fenStr)
    val parsed = Parser.full(PgnStr(s"[FEN \"$fenStr\"]\n\n1. Ka2 Kg8 2. Kb1")).toOption.get
    val game = lila.core.game
      .newGame(
        parsed.toGame.forward(parsed.mainline).toOption.get,
        ByColor(lila.core.game.Player(GamePlayerId("abcd"), _, aiLevel = none)),
        rated = chess.Rated.No,
        source = lila.core.game.Source.Api,
        pgnImport = none
      )
      .sloppy
    // 2. Kb1 misses Nxd3, which leaves K+N vs K. The stored line goes on after the game is drawn,
    // as it does for analyses stored before fishnet lines were cut.
    val analysis = Analysis(
      Analysis.Id(GameId("abcd")),
      None,
      List(
        Info(Ply(1), cpEval(0), Nil),
        Info(Ply(2), cpEval(0), Nil),
        Info(Ply(3), cpEval(-1000), SanStr.from(List("Nxd3", "Kg7", "Ne5", "Kf6")))
      ),
      Ply.initial,
      nowInstant,
      Engine.unknown
    )
    val root = TreeBuilder(game, analysis.some, fen, ExportOptions.default, _ => ())
    val afterKg8 = root.mainline(1)
    assertEquals(
      afterKg8.children.variationsOnly.map(_.mainline.map(_.move.san)),
      List(SanStr.from(List("Nxd3")))
    )
