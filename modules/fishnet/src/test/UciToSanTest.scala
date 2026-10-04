package lila.fishnet

import chess.format.pgn.SanStr
import chess.{ Ply, Position }
import chess.eval.{ Score, WhiteScore }

import scala.language.implicitConversions

import lila.analyse.{ Analysis, Info }
import lila.tree.Eval

final class UciToSanTest extends munit.FunSuite:

  private given Conversion[Int, Ply] = Ply(_)

  private def cpEval(cp: Int) = Eval(Some(WhiteScore.fromWhite(Score.cp(cp))), None)
  private def mateEval(mate: Int) = Eval(Some(WhiteScore.fromWhite(Score.mate(mate))), None)

  private val now = nowInstant

  test("convert UCI analysis to PGN"):
    val uciAnalysis = Analysis(
      Analysis.Id(GameId("ke5ssdgj")),
      List(
        Info(1, cpEval(12), Nil),
        Info(2, cpEval(36), Nil),
        Info(
          3,
          cpEval(22),
          SanStr.from(
            List(
              "g1f3",
              "g8f6",
              "e2e3",
              "e7e6",
              "f1e2",
              "b8c6",
              "e1g1",
              "f8d6",
              "b1c3",
              "e8g8",
              "c3b5",
              "f6e4",
              "b5d6",
              "d8d6"
            )
          )
        ),
        Info(4, cpEval(-30), Nil),
        Info(5, cpEval(-48), Nil),
        Info(
          6,
          cpEval(-80),
          SanStr.from(
            List(
              "g8f6",
              "e2e3",
              "c7c5",
              "g1f3",
              "b8c6",
              "b1c3",
              "c5d4",
              "e3d4",
              "f8e7",
              "f1e2",
              "f6e4"
            )
          )
        ),
        Info(7, cpEval(-22), Nil),
        Info(8, cpEval(-64), Nil),
        Info(9, cpEval(-38), Nil),
        Info(10, cpEval(-56), Nil),
        Info(11, cpEval(-48), Nil),
        Info(12, cpEval(-48), Nil),
        Info(13, cpEval(-52), Nil),
        Info(14, cpEval(-98), Nil),
        Info(15, cpEval(-56), Nil),
        Info(16, cpEval(-98), Nil),
        Info(17, cpEval(-54), Nil),
        Info(18, cpEval(-96), Nil),
        Info(19, cpEval(-96), Nil),
        Info(
          20,
          cpEval(-113),
          SanStr.from(
            List(
              "c8d7",
              "b1d2",
              "c6e7",
              "d2f3",
              "c7c5",
              "f3e5",
              "c5d4",
              "e3d4",
              "f6e4",
              "e1g1",
              "a8c8",
              "d1e2",
              "e4g5",
              "f4g5",
              "g7e5",
              "e2e5",
              "c8c2"
            )
          )
        ),
        Info(
          21,
          cpEval(-42),
          SanStr.from(
            List(
              "g5e4",
              "d5e4",
              "e1g1",
              "g6g5",
              "d1h5",
              "g5f4",
              "f1f4",
              "d8d5",
              "h5d5",
              "e6d5",
              "h3c8",
              "a8c8",
              "b1c3",
              "f8d8"
            )
          )
        ),
        Info(
          22,
          cpEval(-535),
          SanStr.from(
            List(
              "c8e6",
              "g5e4",
              "d5e4",
              "h2h4",
              "g7f6",
              "h4h5",
              "f6h4",
              "e1f1",
              "g6g5",
              "b1d2",
              "e6f5",
              "f4g5",
              "d8g5",
              "h1h4",
              "g5h4"
            )
          )
        ),
        Info(
          23,
          cpEval(-296),
          SanStr.from(
            List(
              "g5e4",
              "d5e4",
              "h2h4",
              "f8f5",
              "b1c3",
              "f5h5",
              "d1d2",
              "h5h4",
              "h1g1",
              "h4h6",
              "d2f2",
              "d8h4",
              "f2h4",
              "h6h4",
              "g1g6",
              "h4h1",
              "e1f2",
              "h1h2",
              "g6g2"
            )
          )
        ),
        Info(24, mateEval(3), SanStr.from(List("d8h4", "e1e2", "h4f2", "e2d3", "c6b4"))),
        Info(
          25,
          cpEval(-935),
          SanStr.from(
            List(
              "e1g1",
              "e6h3",
              "b1d2",
              "h3f1",
              "d1f1",
              "d8e8",
              "f1d3",
              "e4f6",
              "d2f3",
              "a7a6",
              "f3e5",
              "f6e4",
              "c1d2",
              "c6e5",
              "d4e5"
            )
          )
        ),
        Info(26, cpEval(-2165), Nil),
        Info(
          27,
          cpEval(-2731),
          SanStr.from(
            List(
              "g1g3",
              "h4h2",
              "d1f3",
              "e4g3",
              "c2c3",
              "h2g1",
              "e1d2",
              "g3e4",
              "d2c2",
              "e6g4",
              "f3g4",
              "g1g4",
              "b1d2",
              "g4e2",
              "a2a4",
              "a8e8",
              "a1a3",
              "e4d2",
              "c1d2",
              "e8e3"
            )
          )
        ),
        Info(28, mateEval(2), SanStr.from(List("h4f2", "e2d3", "c6b4"))),
        Info(29, mateEval(-2), Nil)
      ),
      0,
      now,
      None,
      None
    )

    val pgn =
      "d4 d5 f3 e6 f4 g6 g3 Bg7 Nf3 Nf6 e3 O-O Bh3 Nc6 g4 h6 g5 hxg5 Nxg5 Ne4 Bxe6 fxe6 Nxe6 Bxe6 Rg1 Qh4+ Ke2 Qxh2+ Kd3 Nb4#"
    val andPly = Position.AndFullMoveNumber(chess.variant.Standard, none)
    val positions = andPly.playPositions(SanStr.from(pgn.split(' ').toList)).toOption.get
    UciToSan(positions, andPly.ply, uciAnalysis) match
      case (_, errs) => assertEquals(errs, Nil)
  test("even in KotH"):
    val pgn = SanStr.from(
      List(
        "e4",
        "e5",
        "d4",
        "Nc6",
        "Ke2",
        "Nxd4+",
        "Ke3",
        "Ne6",
        "f4",
        "Bc5+",
        "Kf3",
        "Nf6",
        "fxe5",
        "Qe7",
        "Be3",
        "d6",
        "Bxc5",
        "Nxc5",
        "exf6",
        "Qxf6+",
        "Ke3",
        "Qe5",
        "Qd4",
        "Qg5+",
        "Kf3",
        "f5",
        "exf5",
        "O-O",
        "Qxc5",
        "Bxf5",
        "Qc4+",
        "Kh8",
        "Qf4",
        "Qg6",
        "g4",
        "Be4+",
        "Ke3",
        "c5",
        "Bd3",
        "Bxd3",
        "Qg3",
        "Bxc2",
        "Nf3",
        "Rae8+",
        "Kf2",
        "Qd3",
        "Kg2",
        "Re2+",
        "Kh3",
        "Rxf3",
        "Kh4",
        "Rxg3",
        "hxg3",
        "h6",
        "g5",
        "Qf5",
        "g4",
        "Re4",
        "Kg3",
        "Qf4+",
        "Kh3",
        "Re3+",
        "Kh4",
        "Qxg5#"
      )
    )
    val andPly = Position.AndFullMoveNumber(chess.variant.KingOfTheHill, none)
    val positions = andPly.playPositions(pgn).toOption.get
    val uciAnalysis = Analysis(Analysis.Id(GameId("g5hX8efz")), Nil, 0, now, None, None)
    UciToSan(positions, andPly.ply, uciAnalysis) match
      case (_, errs) => assertEquals(errs, Nil)
