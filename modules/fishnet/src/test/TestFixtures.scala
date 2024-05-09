package lila.fishnet

import chess.format.pgn.{ PgnStr, SanStr }
object TestFixtures:

  lazy val annotatorTestCases = List(
    AnnotatorTest.TestCase(sans1, PgnStr(pgn1), fish1, PgnStr(expected1)),
    AnnotatorTest.TestCase(sans2, PgnStr(pgn2), fish2, PgnStr(expected2)),
    AnnotatorTest.TestCase(sans3, PgnStr(pgn3), fish3, PgnStr(expected3)),
    AnnotatorTest.TestCase(sans4, PgnStr(pgn4), fish4, PgnStr(expected4))
  )

  lazy val treeBuilderTestCases = List(
    TreeBuilderTest.TestCase(sans1, PgnStr(pgn1), fish1),
    TreeBuilderTest.TestCase(sans2, PgnStr(pgn2), fish2),
    TreeBuilderTest.TestCase(sans3, PgnStr(pgn3), fish3),
    TreeBuilderTest.TestCase(sans4, PgnStr(pgn4), fish4)
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
    """1. e4 { [%eval 0.48] } 1... c5 { [%eval 0.35] } 2. Nf3 { [%eval 0.4] } 2... Nc6 { [%eval 0.42] } 3. Bb5 { [%eval 0.35] } { B30 Sicilian Defense: Nyezhmetdinov-Rossolimo Attack } 3... Qb6 { [%eval 0.73] } 4. Nc3 { [%eval 0.63] } 4... Nd4 { [%eval 0.87] } 5. Bc4 { [%eval 0.66] } 5... e6 { [%eval 0.69] } 6. O-O { [%eval 0.72] } 6... a6 { [%eval 0.59] } 7. d3 { [%eval 0.49] } 7... d6 { [%eval 0.75] } 8. Re1 { [%eval 0.53] } 8... Nf6 { [%eval 0.6] } 9. Rb1 { [%eval 0.11] } 9... Be7 { [%eval 0.37] } 10. Be3 { [%eval 0.04] } 10... Nxf3+ { [%eval 0.08] } 11. Qxf3 { [%eval 0.0] } 11... Qc7 { [%eval 0.1] } 12. a4 { [%eval 0.06] } 12... O-O { [%eval 0.05] } 13. Qg3 { [%eval -0.12] } 13... Kh8 { [%eval 0.03] } 14. f4 { [%eval -0.41] } 14... Qd8?! { (-0.41 → 0.14) Inaccuracy. Bd7 was best. } { [%eval 0.14] } (14... Bd7) 15. e5 { [%eval -0.09] } 15... Nd7?! { (-0.09 → 0.77) Inaccuracy. Ne8 was best. } { [%eval 0.77] } (15... Ne8) 16. exd6 { [%eval 0.73] } 16... Bxd6 { [%eval 0.7] } 17. Ne4 { [%eval 0.82] } 17... Be7 { [%eval 0.8] } 18. Qf2?! { (0.80 → 0.00) Inaccuracy. Bf2 was best. } { [%eval 0.0] } (18. Bf2 b6) 18... Qc7?! { (0.00 → 0.62) Inaccuracy. b6 was best. } { [%eval 0.62] } (18... b6 19. f5 exf5 20. Qxf5 Nf6 21. Qf3 Bb7 22. Qf5) 19. Ra1 { [%eval 0.67] } 19... a5 { [%eval 0.95] } 20. Bb5 { [%eval 0.46] } 20... b6 { [%eval 0.56] } 21. Qg3 { [%eval 0.36] } 21... Bb7 { [%eval 0.9] } 22. Bd2 { [%eval 0.71] } 22... Nf6 { [%eval 0.7] } 23. Ng5?! { (0.70 → 0.04) Inaccuracy. Bc3 was best. } { [%eval 0.04] } (23. Bc3 Nh5 24. Qg4 Nf6 25. Qg5 Ne8 26. Qg3 f6 27. Re3 Rd8 28. Rf1 Bc6) 23... h6 { [%eval 0.0] } 24. Qh3?! { (0.00 → -0.62) Inaccuracy. Nf3 was best. } { [%eval -0.62] } (24. Nf3 Rad8 25. Bc3 Bd6 26. Be5 Kh7 27. c3 Nh5 28. Qg4 g6 29. Bxd6 Qxd6) 24... Nd5 { [%eval -0.3] } 25. Rf1 { [%eval -0.57] } 25... Kg8 { [%eval -0.54] } 26. Ne4 { [%eval -0.39] } 26... f5 { [%eval -0.46] } 27. Ng3 { [%eval -0.85] } 27... Bf6 { [%eval -0.65] } 28. c3 { [%eval -0.87] } 28... Rad8 { [%eval -0.78] } 29. Rae1 { [%eval -0.48] } 29... Qf7 { [%eval -0.78] } 30. Nh5 { [%eval -0.8] } 30... Kh7 { [%eval -0.62] } 31. Nxf6+ { [%eval -0.03] } 31... Qxf6 { [%eval -0.66] } 32. Re5 { [%eval -0.57] } 32... Bc8 { [%eval -0.17] } 33. Rfe1 { [%eval -0.32] } 33... Nc7? { (-0.32 → 0.94) Mistake. Rd6 was best. } { [%eval 0.94] } (33... Rd6 34. Qf3 Qd8 35. Bc4 Bd7 36. Qd1 Rf6 37. Bxd5 Rxd5 38. Rxd5 exd5 39. d4) 34. Bc4?! { (0.94 → 0.32) Inaccuracy. c4 was best. } { [%eval 0.32] } (34. c4 Rd4 35. Bc3 Rxf4 36. Rxe6 Nxe6 37. Bxf6 Rxf6 38. Be8 Rg4 39. Qf3 Ng5) 34... Rde8?! { (0.32 → 1.24) Inaccuracy. Rd6 was best. } { [%eval 1.24] } (34... Rd6) 35. Qf3 { [%eval 0.86] } 35... Re7?? { (0.86 → 3.37) Blunder. Qd8 was best. } { [%eval 3.37] } (35... Qd8) 36. Be3?? { (3.37 → 1.39) Blunder. Qc6 was best. } { [%eval 1.39] } (36. Qc6 Ba6 37. Bxa6 Nxa6 38. Qxb6 Ra8 39. Rxe6 Rxe6 40. Qxe6 Rd8 41. Qxf6 gxf6) 36... Rfe8 { [%eval 1.97] } 37. Bf2 { [%eval 1.65] } 37... Rd8? { (1.65 → 3.21) Mistake. Nd5 was best. } { [%eval 3.21] } (37... Nd5 38. d4) 38. d4?! { (3.21 → 2.39) Inaccuracy. Qc6 was best. } { [%eval 2.39] } (38. Qc6 Red7 39. Qxb6 Nd5 40. Qxe6 Qxe6 41. Rxe6 Nxf4 42. Re8 Bb7 43. Bxc5 Nxd3) 38... Nd5 { [%eval 2.35] } 39. dxc5 { [%eval 2.36] } 39... bxc5 { [%eval 2.37] } 40. Bb5? { (2.37 → 0.95) Mistake. Bxc5 was best. } { [%eval 0.95] } (40. Bxc5 Rc7 41. Qf2 Qg6 42. Bf1 Rc6 43. Bd4 Rcd6 44. Bc4 Bb7 45. R5e2 Ba6) 40... Bb7?? { (0.95 → 3.03) Blunder. Rc7 was best. } { [%eval 3.03] } (40... Rc7) 41. Qg3? { (3.03 → 1.51) Mistake. Bxc5 was best. } { [%eval 1.51] } (41. Bxc5 Rc7 42. Rxe6 Qf7 43. Qf2 Nf6 44. Be7 Rd1 45. Rxd1 Qxe6 46. Bd6 Rc8) 41... Nc7?! { (1.51 → 2.21) Inaccuracy. Rc8 was best. } { [%eval 2.21] } (41... Rc8 42. Bc4 Rcc7 43. b3 Qf7 44. h3 Rc8 45. Kh2 Ree8 46. Bb5 Bc6 47. Ba6) 42. Bc4?! { (2.21 → 1.13) Inaccuracy. Bxc5 was best. } { [%eval 1.13] } (42. Bxc5 Rf7 43. Bd4 Rxd4 44. cxd4 Qd8 45. Qc3 Be4 46. h3 Nxb5 47. axb5 Rc7) 42... Rd2 { [%eval 1.02] } 43. b3 { [%eval 0.47] } 43... Red7 { [%eval 0.35] } 44. Bxc5?? { (0.35 → -3.99) Blunder. Rxc5 was best. } { [%eval -3.99] } (44. Rxc5 Be4) 44... Rd1?? { (-3.99 → 2.63) Blunder. Rxg2+ was best. } { [%eval 2.63] } (44... Rxg2+ 45. Qxg2 Bxg2 46. Kxg2 Rd2+ 47. R1e2 Rxe2+ 48. Bxe2 Nd5 49. Be3 Qh4 50. Bd2) 45. Bd4 { [%eval 2.76] } 45... Rxe1+ { [%eval 2.88] } 46. Qxe1 { [%eval 2.74] } 46... Qf7 { [%eval 3.31] } 47. Qe2?! { (3.31 → 2.40) Inaccuracy. Rxa5 was best. } { [%eval 2.4] } (47. Rxa5 Be4 48. Re5 Qg6 49. Bf1 Nd5 50. Qg3 Qf7 51. h3 Rd6 52. Bc4 Nb4) 47... Nd5 { [%eval 2.45] } 48. Qf2 { [%eval 2.38] } 48... Bc6 { [%eval 2.66] } 49. Re1 { [%eval 2.32] } { White wins. }


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

  val pgn2 =
    """
  1. d4 Nf6 2. Nf3 d5 3. c4 e6 4. Nc3 c5 5. e3 Nc6 6. cxd5 exd5 7. Bb5 cxd4 8. Nxd4 Bd7 9. O-O Bd6 10. Nf3 Be6 11. b3 a6 12. Bd3 Ne5 13. Nxe5 Bxe5 14. Bb2 Rc8 15. Rc1 O-O 16. f4 Bc7 17. Ne2 Bb6 18. Bd4 Bxd4 19. Nxd4 Qa5 20. Qe2 Rfe8 21. h3 Bd7 22. Rxc8 Rxc8 23. Qb2 Qc5 24. a4 a5 25. Qd2 b6 26. Rb1 Qa3 27. Kh2 h6 28. Rb2 Qe7 29. Rc2 Rc5 30. Nf3 Rxc2 31. Bxc2 Qc5 32. Bd3 Kf8 33. Nd4 Kg8 34. Kg1 Kf8 35. Kf2 Ke7 36. Qb2 Kf8 37. Nc2 Ne4+ 38. Bxe4 dxe4 39. Nd4 Bc6 40. Qe2 Qc3 41. Qa6 Qd2+ 42. Ne2 Qb4 43. Qc8+ Be8 44. Qc2 Bd7 45. Nd4 f5 46. Qc7 Qd2+ 47. Ne2 Be6 48. Qxb6 Kf7 49. Qd4 Qa2 50. Qa7+ Kg8 51. Qa8+ Kh7 52. Qxa5 Kh8 53. Qd8+ Kh7 54. Qa5 Kh8 55. b4 Bc4 56. Qd8+ Kh7 57. Qd1 Bb3 58. Qd7 Be6 59. Qb5 Kh8 60. Qc5 g6 61. Ke1 Kg8 62. Qd6 Kf7 63. Qd2 Qxa4 64. Nd4 Bc4 65. Kf2 h5 66. Kg3 Qa3 67. b5 Qd3 68. Qc1 Bxb5 69. Qc7+ Kf8 70. Qc5+ { White wins. } 1-0
  """

  val sans2 =
    """
  d4 Nf6 Nf3 d5 c4 e6 Nc3 c5 e3 Nc6 cxd5 exd5 Bb5 cxd4 Nxd4 Bd7 O-O Bd6 Nf3 Be6 b3 a6 Bd3 Ne5 Nxe5 Bxe5 Bb2 Rc8 Rc1 O-O f4 Bc7 Ne2 Bb6 Bd4 Bxd4 Nxd4 Qa5 Qe2 Rfe8 h3 Bd7 Rxc8 Rxc8 Qb2 Qc5 a4 a5 Qd2 b6 Rb1 Qa3 Kh2 h6 Rb2 Qe7 Rc2 Rc5 Nf3 Rxc2 Bxc2 Qc5 Bd3 Kf8 Nd4 Kg8 Kg1 Kf8 Kf2 Ke7 Qb2 Kf8 Nc2 Ne4+ Bxe4 dxe4 Nd4 Bc6 Qe2 Qc3 Qa6 Qd2+ Ne2 Qb4 Qc8+ Be8 Qc2 Bd7 Nd4 f5 Qc7 Qd2+ Ne2 Be6 Qxb6 Kf7 Qd4 Qa2 Qa7+ Kg8 Qa8+ Kh7 Qxa5 Kh8 Qd8+ Kh7 Qa5 Kh8 b4 Bc4 Qd8+ Kh7 Qd1 Bb3 Qd7 Be6 Qb5 Kh8 Qc5 g6 Ke1 Kg8 Qd6 Kf7 Qd2 Qxa4 Nd4 Bc4 Kf2 h5 Kg3 Qa3 b5 Qd3 Qc1 Bxb5 Qc7+ Kf8 Qc5+
  """.trim
      .split(" ")
      .toList
      .map(SanStr(_))

  val fish2 =
    """
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
        "time": 1634,
        "nps": 918358
      },
      {
        "pv": "d7d5",
        "score": {
          "cp": -29
        },
        "depth": 24,
        "nodes": 1500773,
        "time": 1632,
        "nps": 919591
      },
      {
        "pv": "c2c4",
        "score": {
          "cp": 35
        },
        "depth": 21,
        "nodes": 1500366,
        "time": 1696,
        "nps": 884649
      },
      {
        "pv": "d7d5 c2c4 d5c4 e2e3 a7a6 f1c4 e7e6 e1h1 c7c5 d4c5 f8c5 d1d8 e8d8 c4e2 d8e7 b2b3 b7b6 c1b2 c8b7 b1d2 b8d7 f1c1 a8c8 d2c4",
        "score": {
          "cp": -26
        },
        "depth": 24,
        "nodes": 1500343,
        "time": 1599,
        "nps": 938300
      },
      {
        "pv": "c2c4 c7c6",
        "score": {
          "cp": 30
        },
        "depth": 23,
        "nodes": 1500044,
        "time": 1788,
        "nps": 838950
      },
      {
        "pv": "e7e6 g2g3",
        "score": {
          "cp": -40
        },
        "depth": 22,
        "nodes": 1500892,
        "time": 1826,
        "nps": 821956
      },
      {
        "pv": "g2g3 c7c5",
        "score": {
          "cp": 40
        },
        "depth": 22,
        "nodes": 1500163,
        "time": 1812,
        "nps": 827904
      },
      {
        "pv": "c7c5 c4d5 f6d5 e2e4 d5c3 b2c3 c5d4 c3d4 f8b4 c1d2 b4d2 d1d2 e8h8 f1c4 b8d7 c4d3 b7b6 a2a4 a7a5 e1h1 c8b7",
        "score": {
          "cp": -24
        },
        "depth": 23,
        "nodes": 1500300,
        "time": 1900,
        "nps": 789631
      },
      {
        "pv": "c4d5",
        "score": {
          "cp": 31
        },
        "depth": 23,
        "nodes": 1500431,
        "time": 1832,
        "nps": 819012
      },
      {
        "pv": "a7a6 c4d5",
        "score": {
          "cp": -24
        },
        "depth": 23,
        "nodes": 1500468,
        "time": 1918,
        "nps": 782308
      },
      {
        "pv": "a2a3 d5c4",
        "score": {
          "cp": 9
        },
        "depth": 23,
        "nodes": 1500993,
        "time": 1545,
        "nps": 971516
      },
      {
        "pv": "e6d5",
        "score": {
          "cp": -20
        },
        "depth": 23,
        "nodes": 1500389,
        "time": 1970,
        "nps": 761618
      },
      {
        "pv": "f1b5 c5d4",
        "score": {
          "cp": 13
        },
        "depth": 24,
        "nodes": 1500778,
        "time": 1962,
        "nps": 764922
      },
      {
        "pv": "c5d4",
        "score": {
          "cp": -16
        },
        "depth": 25,
        "nodes": 1500055,
        "time": 1887,
        "nps": 794941
      },
      {
        "pv": "f3d4 c8d7 e1h1 f8d6 b5e2 c6d4 e3d4 h7h6 d1b3 e8h8 b3b7 f8e8",
        "score": {
          "cp": 12
        },
        "depth": 22,
        "nodes": 1500642,
        "time": 1935,
        "nps": 775525
      },
      {
        "pv": "c8d7 e1h1",
        "score": {
          "cp": -15
        },
        "depth": 22,
        "nodes": 1500069,
        "time": 1920,
        "nps": 781285
      },
      {
        "pv": "e1h1 f8d6",
        "score": {
          "cp": 12
        },
        "depth": 22,
        "nodes": 1500138,
        "time": 1993,
        "nps": 752703
      },
      {
        "pv": "f8d6",
        "score": {
          "cp": -13
        },
        "depth": 23,
        "nodes": 1501403,
        "time": 1837,
        "nps": 817312
      },
      {
        "pv": "e3e4 d5e4",
        "score": {
          "cp": 12
        },
        "depth": 23,
        "nodes": 1500171,
        "time": 1863,
        "nps": 805244
      },
      {
        "pv": "d7e6 b2b3 a7a6 b5d3 c6e5 f3e5 d6e5 c1b2 a8c8 d1d2 d8a5 a1c1 e8h8 c1c2 f8d8 c3e2 a5d2 c2d2 e5b2 d2b2 f6e4 f1d1 e4c3",
        "score": {
          "cp": -16
        },
        "depth": 23,
        "nodes": 1500857,
        "time": 1938,
        "nps": 774436
      },
      {
        "pv": "b2b3",
        "score": {
          "cp": 6
        },
        "depth": 21,
        "nodes": 1500117,
        "time": 2023,
        "nps": 741530
      },
      {
        "pv": "a7a6",
        "score": {
          "cp": -3
        },
        "depth": 23,
        "nodes": 1501408,
        "time": 1688,
        "nps": 889459
      },
      {
        "pv": "b5e2 e8h8",
        "score": {
          "cp": 12
        },
        "depth": 22,
        "nodes": 1500158,
        "time": 2016,
        "nps": 744125
      },
      {
        "pv": "e6g4 d3e2",
        "score": {
          "cp": -14
        },
        "depth": 22,
        "nodes": 1500252,
        "time": 1979,
        "nps": 758085
      },
      {
        "pv": "f3e5",
        "score": {
          "cp": 14
        },
        "depth": 23,
        "nodes": 1500474,
        "time": 1936,
        "nps": 775038
      },
      {
        "pv": "d6e5 c1b2",
        "score": {
          "cp": -6
        },
        "depth": 23,
        "nodes": 1501099,
        "time": 2004,
        "nps": 749051
      },
      {
        "pv": "c1b2 d5d4",
        "score": {
          "cp": 11
        },
        "depth": 22,
        "nodes": 1501010,
        "time": 1998,
        "nps": 751256
      },
      {
        "pv": "a8c8 d1d2",
        "score": {
          "cp": -18
        },
        "depth": 20,
        "nodes": 1500578,
        "time": 1972,
        "nps": 760942
      },
      {
        "pv": "d1d2 d8a5 a1c1 f6e4 d3e4 d5e4 c1c2 c8d8 d2c1 d8c8 c3a4 e8h8 b2e5 c8c2 c1c2 a5e5 c2c5 e5c5 a4c5 f8c8 c5b7 c8c2 f1d1 g8f8 h2h3 f8e7 d1d4 c2a2",
        "score": {
          "cp": 25
        },
        "depth": 23,
        "nodes": 1500778,
        "time": 1844,
        "nps": 813870
      },
      {
        "pv": "e8h8 f2f4 e5d6 c3e2 d8e7 h2h3 e6d7 b2d4 f6e4 c1c8 f8c8 g1h2 c8e8 d3b1 d6b4 b1d3 e7e6 e2g3 e4g3 h2g3",
        "score": {
          "cp": 0
        },
        "depth": 24,
        "nodes": 1500151,
        "time": 1888,
        "nps": 794571
      },
      {
        "pv": "f2f4",
        "score": {
          "cp": 4
        },
        "depth": 23,
        "nodes": 1501351,
        "time": 1530,
        "nps": 981275
      },
      {
        "pv": "e5d6",
        "score": {
          "cp": 6
        },
        "depth": 23,
        "nodes": 1500325,
        "time": 1955,
        "nps": 767429
      },
      {
        "pv": "c3a4",
        "score": {
          "cp": 23
        },
        "depth": 23,
        "nodes": 1500531,
        "time": 1949,
        "nps": 769897
      },
      {
        "pv": "c7b6 d1d2 c8c1 f1c1 e6g4 b2d4 g4e2 d3e2 f8e8 e2f3 h7h5 h2h3 f6e4 d2b4 b6d4 b4d4 h5h4 g1h2 e4f6 a2a4 d8e7 c1e1 e8c8 e1d1",
        "score": {
          "cp": -10
        },
        "depth": 26,
        "nodes": 1501248,
        "time": 1761,
        "nps": 852497
      },
      {
        "pv": "d1d2 c8c1 f1c1 e6g4 b2d4 g4e2 d3e2 f8e8 e2f3 h7h5 d4b6 d8b6 d2d4 b6e6 c1c3 h5h4 h2h3 e6f5 c3d3 e8c8 d3d1 c8e8 f3d5 f6d5 d4d5 f5d5 d1d5 e8e3 d5d8 g8h7 d8d7 b7b5 g1h2 f7f6 d7a7 e3e1 a7a6",
        "score": {
          "cp": 9
        },
        "depth": 28,
        "nodes": 1500839,
        "time": 1876,
        "nps": 800020
      },
      {
        "pv": "b6d4 e2d4 d8a5 d1e2 e6d7 h2h3 f8e8 f1d1 h7h6 c1c8 e8c8 e2b2 a5c3 b2c3 c8c3 d4e2 c3c7 g2g4 g8f8",
        "score": {
          "cp": -9
        },
        "depth": 25,
        "nodes": 1500517,
        "time": 1853,
        "nps": 809777
      },
      {
        "pv": "e2d4",
        "score": {
          "cp": 21
        },
        "depth": 24,
        "nodes": 1501255,
        "time": 1842,
        "nps": 815013
      },
      {
        "pv": "d8a5",
        "score": {
          "cp": -13
        },
        "depth": 24,
        "nodes": 1500376,
        "time": 1853,
        "nps": 809701
      },
      {
        "pv": "d4e6 f7e6",
        "score": {
          "cp": -7
        },
        "depth": 23,
        "nodes": 1500626,
        "time": 1947,
        "nps": 770737
      },
      {
        "pv": "e6d7 h2h3 f8e8 f1d1 h7h5 g1f2 b7b5 e2b2 b5b4 c1c8 e8c8 d3b1 c8c3 d4e2 c3c8 b2d2 h5h4 d2d4",
        "score": {
          "cp": -23
        },
        "depth": 22,
        "nodes": 1500453,
        "time": 1990,
        "nps": 753996
      },
      {
        "pv": "h2h3 e6d7",
        "score": {
          "cp": 8
        },
        "depth": 25,
        "nodes": 1500136,
        "time": 1959,
        "nps": 765766
      },
      {
        "pv": "e6d7 f1d1 g7g6 d4f3 d7b5 d3b5 a5b5 e2b5 a6b5 f3e5 b5b4 c1c8 e8c8 g2g4 h7h5",
        "score": {
          "cp": -9
        },
        "depth": 25,
        "nodes": 1500296,
        "time": 1575,
        "nps": 952568
      },
      {
        "pv": "f1d1 g7g6",
        "score": {
          "cp": 8
        },
        "depth": 23,
        "nodes": 1500515,
        "time": 2010,
        "nps": 746524
      },
      {
        "pv": "e8c8 f1d1",
        "score": {
          "cp": -7
        },
        "depth": 23,
        "nodes": 1501048,
        "time": 2042,
        "nps": 735087
      },
      {
        "pv": "g1f2 g8f8",
        "score": {
          "cp": 5
        },
        "depth": 22,
        "nodes": 1500702,
        "time": 2030,
        "nps": 739262
      },
      {
        "pv": "c8e8",
        "score": {
          "cp": -11
        },
        "depth": 22,
        "nodes": 1500026,
        "time": 1990,
        "nps": 753781
      },
      {
        "pv": "g1f2 h7h6 d3b1 a6a5 d4e2 c5b4 f1c1 c8c4 b1d3 c4c1 b2c1 a5a4 e2d4 a4b3 a2b3",
        "score": {
          "cp": 20
        },
        "depth": 23,
        "nodes": 1500350,
        "time": 2020,
        "nps": 742747
      },
      {
        "pv": "c5c3 b2e2 h7h6 f1d1 c3b4 g1f2 g8f8 f2g1 f8g8",
        "score": {
          "cp": 0
        },
        "depth": 24,
        "nodes": 1500973,
        "time": 2009,
        "nps": 747124
      },
      {
        "pv": "g1f2 h7h5 b2d2 b7b6 f1b1 c5c3 d2c3 c8c3 f2e2 f6e4 d3e4 d5e4 e2d2 c3d3 d2e2",
        "score": {
          "cp": 0
        },
        "depth": 28,
        "nodes": 1500945,
        "time": 1818,
        "nps": 825602
      },
      {
        "pv": "c5c3 f1d1 g8f8 g1f2 c3d2 d1d2 c8c3 f2e2 f6e4 d2b2 b7b6 b3b4 f8e7 d3e4 d5e4 b4a5 b6a5 d4b3 c3c4 b3a5 c4a4 a5b7 d7c6 g2g4 c6d5 b7c5",
        "score": {
          "cp": 5
        },
        "depth": 27,
        "nodes": 1500220,
        "time": 1901,
        "nps": 789174
      },
      {
        "pv": "d4e2 c8e8 e2d4 e8c8",
        "score": {
          "cp": 0
        },
        "depth": 31,
        "nodes": 1501176,
        "time": 1575,
        "nps": 953127
      },
      {
        "pv": "c5c3 d2e2 c3b4 e2b2 h7h6 b2e2 c8c5 g1h2 g8f8 b1d1 c5c8 h2g1 b4c3 g1f2 c8c5 d1b1 c3b4 b1d1 f8g8 f2g1 c5c3 g1h2",
        "score": {
          "cp": 0
        },
        "depth": 27,
        "nodes": 1500802,
        "time": 1962,
        "nps": 764934
      },
      {
        "pv": "g1f2 g7g6 b1b2 a3d6 f2g1 d6e7 g1f2",
        "score": {
          "cp": 0
        },
        "depth": 26,
        "nodes": 1500648,
        "time": 2040,
        "nps": 735611
      },
      {
        "pv": "g7g6 h2g1",
        "score": {
          "cp": -2
        },
        "depth": 25,
        "nodes": 1500549,
        "time": 2053,
        "nps": 730905
      },
      {
        "pv": "h2g1 g8f8 g1f2 c8e8 d4f3 e8c8 b1b2 c8e8",
        "score": {
          "cp": 0
        },
        "depth": 27,
        "nodes": 1500825,
        "time": 1968,
        "nps": 762614
      },
      {
        "pv": "g7g6 b2c2 c8c2 d2c2 a3c5 c2d2 f6e8 d3e2 e8d6 e2d3 d6b7 d4f3 c5d6 d3c2 d7c8 d2d4 d6c5 d4d2",
        "score": {
          "cp": 0
        },
        "depth": 29,
        "nodes": 1500566,
        "time": 1894,
        "nps": 792273
      },
      {
        "pv": "d4f3 e7a3 f3d4 a3e7",
        "score": {
          "cp": 0
        },
        "depth": 27,
        "nodes": 1500081,
        "time": 1911,
        "nps": 784971
      },
      {
        "pv": "c8c5 c2c3 g8f8 c3c1 c5c1 d2c1 f6e8 d3f5 d7f5 d4f5 e7e4 c1a3 f8g8 a3e7 e4f5 e7e8 g8h7 e8c6 g7g5 c6d6 g5f4 d6f4 f5f4 e3f4",
        "score": {
          "cp": -1
        },
        "depth": 24,
        "nodes": 1500175,
        "time": 1587,
        "nps": 945289
      },
      {
        "pv": "c2c5 e7c5 h2g1 f6e8 g1f2 e8d6 f2e2 g8f8 e2d1 d7c8 g2g4 c8d7 d2c2 f8e7 c2b2 e7f8 d1d2 f8e7",
        "score": {
          "cp": 4
        },
        "depth": 23,
        "nodes": 1500860,
        "time": 1851,
        "nps": 810837
      },
      {
        "pv": "c5c2 d3c2 e7c5 c2d3 f6e8 f3d4 g7g6 h2g3 e8g7 d3e2 g7f5 d4f5 d7f5 e2f3 f5e6 d2d4 c5d4 e3d4 f7f6 f3g4",
        "score": {
          "cp": 3
        },
        "depth": 24,
        "nodes": 1500823,
        "time": 2025,
        "nps": 741147
      },
      {
        "pv": "d3c2 e7c5 c2b1 g8f8 f3d4 f6e8 b1f5 d7f5 d4f5 e8d6 f5d6 c5d6 d2c3 g7g5 c3d4 g5f4 e3f4 d6c5 d4h8 f8e7 h8e5 e7d7 e5f6 d7e8 f6e5 e8d7",
        "score": {
          "cp": 0
        },
        "depth": 25,
        "nodes": 1500711,
        "time": 2034,
        "nps": 737812
      },
      {
        "pv": "e7c5 f3d4 d7c8 d2e1 c8a6 c2f5 g7g6 f5c2 c5e7 e1d2 e7c5",
        "score": {
          "cp": 0
        },
        "depth": 28,
        "nodes": 1501047,
        "time": 1975,
        "nps": 760023
      },
      {
        "pv": "f3d4 g7g6 d2e1 c5d6 e1c3 d6c5",
        "score": {
          "cp": 0
        },
        "depth": 26,
        "nodes": 1500229,
        "time": 2003,
        "nps": 748991
      },
      {
        "pv": "f6e8h2g3 e8d6 f3d4 c5c7 g3h2 c7c8 d3c2 g7g6 h2g1 h6h5 g1h2 g8f8 d4e2 c8c5 e2d4",
        "score": {
          "cp": 0
        },
        "depth": 25,
        "nodes": 1500197,
        "time": 2107,
        "nps": 712006
      },
      {
        "pv": "f3d4 f6e8 d3f5 d7f5 d4f5 e8f6 d2b2 c5c6 f5d4 c6c5",
        "score": {
          "cp": 0
        },
        "depth": 29,
        "nodes": 1500304,
        "time": 1843,
        "nps": 814055
      },
      {
        "pv": "f6g8 d2b2 g8e7 d3e2 g7g6 e2f3 f8g8 b2d2 d7e6 g2g4 e7c6 d4e2 c5a3 d2c3 a3c5",
        "score": {
          "cp": 0
        },
        "depth": 23,
        "nodes": 1500907,
        "time": 2061,
        "nps": 728242
      },
      {
        "pv": "h2g3 d7c8 g3f3 f6e4 d2b2 c5e7 d3e4 e7e4 f3f2 c8a6 b2c1 e4d3 g2g4 g8h8 f2g2 d3e4 g2g3 e4d3 g3f2 h8h7 h3h4",
        "score": {
          "cp": 5
        },
        "depth": 26,
        "nodes": 1500351,
        "time": 1964,
        "nps": 763926
      },
      {
        "pv": "d7c8 g1f1",
        "score": {
          "cp": -6
        },
        "depth": 24,
        "nodes": 1501077,
        "time": 1706,
        "nps": 879881
      },
      {
        "pv": "g1f2 c5e7 d2c3 e7c5 c3a1 f6h5 a1b2 c5b4 g2g4 h5f6 b2c2 b4c5 f2e2 c5c2 d3c2 f8e7 e2d2 e7d6 g4g5 f6e4 c2e4 d5e4 h3h4 d6d5 d2c3",
        "score": {
          "cp": 4
        },
        "depth": 29,
        "nodes": 1501000,
        "time": 1886,
        "nps": 795864
      },
      {
        "pv": "c5a3 f2e2 f6e8 d2c2 a3c5 c2b2 e8d6 e2d2 h6h5 d2e2 d7e8 b2d2 d6b7 d3b5 b7d6",
        "score": {
          "cp": 0
        },
        "depth": 22,
        "nodes": 1500717,
        "time": 1912,
        "nps": 784893
      },
      {
        "pv": "g2g4 e7f8 f2e2 c5d6 d2c3 d6c5 e2d2 c5c3 d2c3 f8e7 d3f5 g7g6 f5d3 e7d6 d4f3 d7e6 f3e5 d6c5",
        "score": {
          "cp": 10
        },
        "depth": 22,
        "nodes": 1500267,
        "time": 1931,
        "nps": 776937
      },
      {
        "pv": "c5b4 b2c1 b4c5 c1a1 e7f8 g2g4 c5b4 a1c1 b4c5 c1d2 f8g8 d4e2 g8f8 d2c3 c5c3 e2c3 f8e7 g4g5 f6e4 d3e4 d5e4 g5h6 g7h6",
        "score": {
          "cp": -4
        },
        "depth": 25,
        "nodes": 1500661,
        "time": 1884,
        "nps": 796529
      },
      {
        "pv": "f2e2",
        "score": {
          "cp": 4
        },
        "depth": 24,
        "nodes": 1501045,
        "time": 2002,
        "nps": 749772
      },
      {
        "pv": "f6e8 f2e2 e8d6 c2d4 c5c7 b2d2 d6b7 e2d1 b7d6 d2c2 c7c2 d1c2 f8e7 c2c3 d6b7 e3e4 d5e4 d3e4 b7c5 e4c2",
        "score": {
          "cp": -2
        },
        "depth": 24,
        "nodes": 1500190,
        "time": 1870,
        "nps": 802240
      },
      {
        "pv": "f2e1 d7f5 g2g4 f5c8 d3e4 d5e4 b3b4 c5c4 b4a5 b6a5 b2d4 c4d4 c2d4 h6h5 g4h5 c8h3 e1d2 h3d7 d4b3 d7a4 b3a5",
        "score": {
          "cp": 5
        },
        "depth": 24,
        "nodes": 1501210,
        "time": 1871,
        "nps": 802357
      },
      {
        "pv": "d5e4 c2d4 f8g8 b2b1 d7c6 b3b4 a5b4 d4c6 c5c6 b1b4 g8h7 f2g3 f7f6a4a5 b6a5 b4a5 c6d7 a5c3 d7d1 g3f2 d1d7 c3c2 f6f5 c2c3 h7g8 f2g3",
        "score": {
          "cp": -2
        },
        "depth": 25,
        "nodes": 1500341,
        "time": 1754,
        "nps": 855382
      },
      {
        "pv": "c2d4 f8g8 b2b1 d7c6 b3b4 a5b4 d4c6 c5c6 b1b4 g8h7 a4a5 b6a5 b4a5 c6c4 f2g3 c4c6 a5a2 c6c1 g3f2 c1d1 a2c4 d1d2 c4e2",
        "score": {
          "cp": 4
        },
        "depth": 23,
        "nodes": 1501168,
        "time": 2009,
        "nps": 747221
      },
      {
        "pv": "f8g8 b2b1 d7c6 b3b4 a5b4 d4c6 c5c6 b1b4 g8h7 b4d4 c6c2 f2g1 c2c6",
        "score": {
          "cp": 0
        },
        "depth": 26,
        "nodes": 1501415,
        "time": 1774,
        "nps": 846344
      },
      {
        "pv": "b3b4 a5b4 d4c6 c5c6 b2b4 f8g8 f2g3 g8h7 a4a5 b6a5 b4a5 h7g6 h3h4 g6h7 h4h5 c6d7 a5e5 d7d3 e5f5 h7g8 f5c5 g8h7 c5f5",
        "score": {
          "cp": 0
        },
        "depth": 22,
        "nodes": 1500553,
        "time": 1748,
        "nps": 858439
      },
      {
        "pv": "c5c3 f4f5",
        "score": {
          "cp": -6
        },
        "depth": 22,
        "nodes": 1501297,
        "time": 1937,
        "nps": 775062
      },
      {
        "pv": "e2d1 c6d5",
        "score": {
          "cp": 4
        },
        "depth": 25,
        "nodes": 1501240,
        "time": 1789,
        "nps": 839150
      },
      {
        "pv": "c3b2 f2g3",
        "score": {
          "cp": -22
        },
        "depth": 23,
        "nodes": 1500419,
        "time": 1641,
        "nps": 914332
      },
      {
        "pv": "d4e2 d2d8",
        "score": {
          "cp": 20
        },
        "depth": 23,
        "nodes": 1500429,
        "time": 1796,
        "nps": 835428
      },
      {
        "pv": "d2d8 e2d4 c6d5 a6b5 d8d6 b3b4 a5b4 a4a5 b6a5 d4f5 d6d8 b5c5 f8g8 c5d5 d8d5 f5e7 g8f8 e7d5 f8e8 d5b6 e8d8 f2e2 d8c7 b6c4 a5a4",
        "score": {
          "cp": -18
        },
        "depth": 23,
        "nodes": 1500269,
        "time": 1763,
        "nps": 850975
      },
      {
        "pv": "a6c8 c6e8 c8c2 f8g8 e2d4 f7f6 c2e4 e8f7 e4d3 b4a3 f2f3 g8h8 f3g3 a3d6 g3h2 d6d5 e3e4 d5d6 d3d2 g7g5 g2g3 g5f4 g3f4",
        "score": {
          "cp": 34
        },
        "depth": 25,
        "nodes": 1500798,
        "time": 1739,
        "nps": 863023
      },
      {
        "pv": "c6e8 c8c2 f8g8 e2d4 f7f6 c2e4 b4d2 f2g3 e8f7 g3h2 g8h8 d4e6 d2d6 e6d4 d6d7 e4d3 f7d5 g2g4 d7c8 d3b5 d5e4 b5c4 c8c4 b3c4",
        "score": {
          "cp": -34
        },
        "depth": 26,
        "nodes": 1500099,
        "time": 1752,
        "nps": 856220
      },
      {
        "pv": "c8c2",
        "score": {
          "cp": 37
        },
        "depth": 25,
        "nodes": 1500286,
        "time": 1708,
        "nps": 878387
      },
      {
        "pv": "f7f6",
        "score": {
          "cp": -45
        },
        "depth": 23,
        "nodes": 1500574,
        "time": 1876,
        "nps": 799879
      },
      {
        "pv": "e2d4 f8g8 f4f5 b6b5 a4b5 d7b5 f5f6 b5d7 f6g7 d7e6 d4e6 f7e6 c2d1 b4b6 h3h4 b6b8 d1h5 b8b3 h5h6 b3b2 f2g1 b2a1 g1h2 a1e5 g2g3 e5g7 h6e6 g7f7 e6g4 g8f8",
        "score": {
          "cp": 61
        },
        "depth": 25,
        "nodes": 1500913,
        "time": 1695,
        "nps": 885494
      },
      {
        "pv": "f8g8",
        "score": {
          "cp": -67
        },
        "depth": 24,
        "nodes": 1500812,
        "time": 1739,
        "nps": 863031
      },
      {
        "pv": "c2c7 f8e8",
        "score": {
          "cp": 136
        },
        "depth": 26,
        "nodes": 1501018,
        "time": 1602,
        "nps": 936965
      },
      {
        "pv": "f8e8 c7b8",
        "score": {
          "cp": -146
        },
        "depth": 24,
        "nodes": 1501082,
        "time": 1548,
        "nps": 969691
      },
      {
        "pv": "d4e2 d7e6 c7b6 f8f7 b6d4 d2c2 d4a7 f7g6 a7a6 g6f7 a6a5 e6b3 a5c3 c2a2 f2g3 b3a4 c3c7 f7g6 e2c3 a2b3 c7d6 g6h7 c3a4 b3a4 d6d5 a4c2 d5f5 h7h8 f5b5 h8g8 b5d5 g8h7 g3h2 c2d3 d5f5 h7h8 f5c5 h8g8",
        "score": {
          "cp": 134
        },
        "depth": 30,
        "nodes": 1501003,
        "time": 1491,
        "nps": 1006708
      },
      {
        "pv": "d7e6 c7b6 f8f7 b6d4 d2c2 d4a7 f7g6 a7a6 g6f7 a6a5 e6b3 a5c3 c2a2 f2g3 b3a4 c3c7 f7g6 c7d6 g6h7 e2c3 a2c2 c3a4 c2a4 d6d5 h7h8 d5f5 a4b4 f5c8 h8h7 g3h2 b4b5 c8c2 b5d3 c2c5 h7h8 c5d4 h8h7 f4f5 h7g8 h2g3 d3b1 d4c4 g8h8",
        "score": {
          "cp": -142
        },
        "depth": 27,
        "nodes": 1501213,
        "time": 1484,
        "nps": 1011599
      },
      {
        "pv": "c7b6 f8f7 b6d4 d2c2 d4a7 f7g6 a7a6 g6f7 a6a5 e6b3 a5c3 c2a2 f2g3 b3a4 c3c7 f7g6 c7d6 g6h7 e2c3 a2c4 c3a4 c4a4 d6d5 a4b4 d5f5 h7h8 g3h2 b4c4 f5h5 c4d3h5c5 h8g8 c5d4 g8h7 h2g3 d3b1 d4c3",
        "score": {
          "cp": 144
        },
        "depth": 25,
        "nodes": 1500076,
        "time": 1694,
        "nps": 885523
      },
      {
        "pv": "f8f7 b6d4 d2c2 d4a7 f7g6 a7a6 g6f7 a6a5 e6b3 a5c3 c2a2 f2g1 b3a4 e2d4 a4d7 c3c5 g7g6 c5d6 d7e8 g1h1a2c4 h1h2 c4c8 d6d5 f7g7 h3h4 e8f7 d5e5 g7g8 h4h5 c8d8 h2g3 d8f8 h5g6 f7g6 g3h2 f8f7 e5d6 g8h7",
        "score": {
          "cp": -124
        },
        "depth": 25,
        "nodes": 1500553,
        "time": 1388,
        "nps": 1081090
      },
      {
        "pv": "b6d4 d2c2 d4a7 f7g6 a7a6 g6f7 a6a5 e6b3 a5c3 c2a2 f2g3 b3a4 c3c7 f7g6 e2c3 a2c2 c7d6 g6h7 c3a4 c2a4 d6d5 a4c2 d5f5 h7h8 f5b5 h8h7 b5f1 c2c7 f1f2 h7h8 f2b2 c7c5 b2d4 c5c2 d4d6 c2b1 d6d8 h8h7 d8d2 b1b6 d2f2 b6g6 g3h2",
        "score": {
          "cp": 129
        },
        "depth": 28,
        "nodes": 1500801,
        "time": 1361,
        "nps": 1102719
      },
      {
        "pv": "d2c2 d4a7 f7g6 a7a6 g6f7 a6a5 e6b3 a5c3 c2a2 f2g3 b3a4 c3c7 f7g6 c7d6 g6h7 e2c3 a2c4 c3a4 c4a4 d6d5 h7h8 d5f5 a4c4 f5d7 h8h7 d7d4 c4c2 d4d5 c2b1 d5d2 b1b6 d2d4 b6b1 f4f5 h7g8 d4d2 b1b8 g3g4 b8e5 d2f2 e5h2 f2g3",
        "score": {
          "cp": -150
        },
        "depth": 27,
        "nodes": 1500532,
        "time": 1338,
        "nps": 1121473
      },
      {
        "pv": "d4a7 f7f6",
        "score": {
          "cp": 168
        },
        "depth": 23,
        "nodes": 1500399,
        "time": 1248,
        "nps": 1202242
      },
      {
        "pv": "f7g8 a7a8 g8h7 a8a5 h7h8 a5b4 e6b3 f2g1 a2e2 b4b3 e2e1 g1h2 h6h5 b3f7 h5h4 f7f8 h8h7 f8f5 h7g8 f5d5 g8f8 d5d6 f8g8 f4f5 e1e3 d6d8 g8f7 d8h4 e3d4 a4a5 d4e5 h4g3 e5a5",
        "score": {
          "cp": -121
        },
        "depth": 23,
        "nodes": 1500031,
        "time": 1294,
        "nps": 1159220
      },
      {
        "pv": "a7a8 g8h7 a8a5 h7h8 a5d8 h8h7 b3b4 e6c4 d8d1 a2b2 b4b5 h7h8 b5b6 c4e2 d1e2 b2b6 e2d2 b6c6 a4a5 c6a4 d2d8 h8h7 d8b6 a4a2 f2g3",
        "score": {
          "cp": 204
        },
        "depth": 23,
        "nodes": 1500624,
        "time": 1245,
        "nps": 1205320
      },
      {
        "pv": "g8h7 a8a5",
        "score": {
          "cp": -222
        },
        "depth": 23,
        "nodes": 1500022,
        "time": 1523,
        "nps": 984912
      },
      {
        "pv": "a8a5",
        "score": {
          "cp": 225
        },
        "depth": 22,
        "nodes": 1501245,
        "time": 1403,
        "nps": 1070024
      },
      {
        "pv": "h7h8",
        "score": {
          "cp": -161
        },
        "depth": 21,
        "nodes": 1500822,
        "time": 1206,
        "nps": 1244462
      },
      {
        "pv": "a5a8",
        "score": {
          "cp": 191
        },
        "depth": 21,
        "nodes": 1500246,
        "time": 1183,
        "nps": 1268170
      },
      {
        "pv": "h8h7 d8f8 a2b3 e2d4 b3a4 d4f5 e6f5 f8f5 h7h8 f5c8 h8h7 f2g3 a4b5 c8c2 b5d5 c2f2 d5d8 f4f5",
        "score": {
          "cp": -123
        },
        "depth": 23,
        "nodes": 1500219,
        "time": 1067,
        "nps": 1406015
      },
      {
        "pv": "d8a5",
        "score": {
          "cp": 219
        },
        "depth": 22,
        "nodes": 1500819,
        "time": 1267,
        "nps": 1184545
      },
      {
        "pv": "h7h8",
        "score": {
          "cp": -207
        },
        "depth": 22,
        "nodes": 1500047,
        "time": 1619,
        "nps": 926526
      },
      {
        "pv": "a5d8 h8h7 b3b4 e6c4 d8d1 c4d3 b4b5 d3c4 f2e1 c4d3 b5b6 a2b2 a4a5 b2b5 e1f2 b5a5 e2d4 a5b6 d4f5",
        "score": {
          "cp": 190
        },
        "depth": 24,
        "nodes": 1500568,
        "time": 1142,
        "nps": 1313982
      },
      {
        "pv": "e6c4 a5d8 h8h7 d8d1 h6h5 b4b5 h5h4 b5b6 c4a6 d1e1 a2b2 a4a5 h7g6 e1d1 a6d3 b6b7 b2b7 e2d4 b7b2 f2g1 b2c3 g1h2 c3a5 d1b3 a5d8 b3e6 d8f6 e6g8 d3a6 g8e8 g6h6 e8h8 h6g6",
        "score": {
          "cp": -211
        },
        "depth": 22,
        "nodes": 1500825,
        "time": 1504,
        "nps": 997888
      },
      {
        "pv": "a5d8 h8h7 d8d1 c4d3 b4b5 a2b2 b5b6 b2b6 e2d4 b6a5 d1e1 a5a4 d4f5 g7g6 f5d4 a4a3 e1d2 g6g5 f4f5",
        "score": {
          "cp": 220
        },
        "depth": 21,
        "nodes": 1500604,
        "time": 1585,
        "nps": 946753
      },
      {
        "pv": "h8h7 d8d1 a2b2 b4b5 h7h8 f2e1 h8h7 d1d2 b2a1 e1f2 a1a4 e2d4 c4b5 d4f5 b5d3 f5d4 a4a3 f2g3 g7g5 f4g5 h6g5 g3g4 h7g6 h3h4 g5h4 d2f2",
        "score": {
          "cp": -216
        },
        "depth": 22,
        "nodes": 1501252,
        "time": 1486,
        "nps": 1010263
      },
      {
        "pv": "d8d1",
        "score": {
          "cp": 224
        },
        "depth": 21,
        "nodes": 1500939,
        "time": 1658,
        "nps": 905270
      },
      {
        "pv": "c4d3 b4b5",
        "score": {
          "cp": -224
        },
        "depth": 21,
        "nodes": 1500987,
        "time": 1642,
        "nps": 914121
      },
      {
        "pv": "d1d7 h7g6",
        "score": {
          "cp": 258
        },
        "depth": 23,
        "nodes": 1501576,
        "time": 1545,
        "nps": 971893
      },
      {
        "pv": "h7g6 d7e8 g6h7 e8b5 b3c4 b5f5 h7h8 f5c8 h8h7 c8g4 c4d3 b4b5 a2b2 b5b6 b2b6 e2d4 b6b1 d4e6",
        "score": {
          "cp": -290
        },
        "depth": 22,
        "nodes": 1500364,
        "time": 1630,
        "nps": 920468
      },
      {
        "pv": "d7b5 a2c2 f2g3 c2d2 g3h2 d2e3 a4a5 e3d2 a5a6 e6d5 b5d7 e4e3 d7f5 h7g8 f5c8 g8h7 c8g4 d2d3 a6a7 d5a8 e2g3 a8b7 g4e2 d3d4 e2c2 h7h8 a7a8q b7a8 c2c8 h8h7 c8a8 d4f4",
        "score": {
          "cp": 320
        },
        "depth": 24,
        "nodes": 1500842,
        "time": 1480,
        "nps": 1014082
      },
      {
        "pv": "h7h8 b5c5",
        "score": {
          "cp": -326
        },
        "depth": 21,
        "nodes": 1500246,
        "time": 1558,
        "nps": 962930
      },
      {
        "pv": "b5c5 e6c4 c5f8 h8h7 f8f5 h7h8 f5c8 h8h7 c8g4 c4d3 b4b5 a2b2 b5b6 b2b6 e2d4 b6b4 f2g3 b4a4 g4f5 h7g8 f5d5 g8h7 d5f7 a4a6 d4e6 a6e6 f7e6 d3b1 e6f5 h7h8 f5f8 h8h7",
        "score": {
          "cp": 334
        },
        "depth": 23,
        "nodes": 1501108,
        "time": 1370,
        "nps": 1095699
      },
      {
        "pv": "e6c4 c5f8 h8h7 f8f5 h7h8 f5g4 c4e6 g4h5 e6f7 h5e5 f7c4 e5e8 h8h7 e8e4 h7h8 e4a8 h8h7 a8f3 c4d3 b4b5 a2a4 e2d4 a4a1 f2g3 a1b1 g3h2 d3b5 d4b5 b1b5 e3e4 b5b2",
        "score": {
          "cp": -357
        },
        "depth": 21,
        "nodes": 1500340,
        "time": 1199,
        "nps": 1251326
      },
      {
        "pv": "f2e1 h8g8 a4a5 a2b1 e2c1b1a1 c5c6 g8f7 a5a6 g6g5 f4g5 h6g5 e1f2 f5f4 c1e2 a1a2 e3f4 e4e3 f2e3 e6c4",
        "score": {
          "cp": 382
        },
        "depth": 21,
        "nodes": 1501138,
        "time": 1563,
        "nps": 960420
      },
      {
        "pv": "h8g8 a4a5 g8f7 c5c7 f7e8 c7c3 g6g5 c3e5 a2b1 e1f2 b1a2 f2g1a2e2 e5e6 e8d8 e6f6 d8c7 f6e5 c7d8",
        "score": {
          "cp": -406
        },
        "depth": 20,
        "nodes": 1500053,
        "time": 1377,
        "nps": 1089363
      },
      {
        "pv": "a4a5 h6h5 c5d6 g8h7 d6d2 a2b1 e1f2 e6c4 e2d4 h7h6 d2e1 b1b2 f2g1",
        "score": {
          "cp": 421
        },
        "depth": 21,
        "nodes": 1501499,
        "time": 1402,
        "nps": 1070969
      },
      {
        "pv": "g8f7 d6c7",
        "score": {
          "cp": -369
        },
        "depth": 20,
        "nodes": 1500827,
        "time": 1491,
        "nps": 1006590
      },
      {
        "pv": "e2d4 e6c4 b4b5 h6h5 b5b6 f7g7 d6b4 c4a6 b6b7 a2a1 e1f2 a1f1 f2g3 a6b7 b4e7g7h6 e7f8 h6h7",
        "score": {
          "cp": 459
        },
        "depth": 19,
        "nodes": 1501532,
        "time": 1221,
        "nps": 1229755
      },
      {
        "pv": "a2a4",
        "score": {
          "cp": -300
        },
        "depth": 19,
        "nodes": 1500351,
        "time": 1582,
        "nps": 948388
      },
      {
        "pv": "d2b2",
        "score": {
          "cp": 312
        },
        "depth": 20,
        "nodes": 1501134,
        "time": 1649,
        "nps": 910329
      },
      {
        "pv": "g6g5 f4g5 h6g5 b4b5 a4a3 e1f2 a3c5 d2e2 f5f4 e2h5 f7e7 h5h7 e7d6 h7e4 f4e3 e4e3 e6d7 f2f3 d7c8 e3e4 c5c3 f3f2 c8b7 d4f5 d6c7 e4e7 c7b8 e7g5 c3c2 f2g1",
        "score": {
          "cp": -264
        },
        "depth": 22,
        "nodes": 1500183,
        "time": 1549,
        "nps": 968484
      },
      {
        "pv": "e1f2 f7e8 f2g1 a4a2 d2c3 a2a6 g1h2 h6h5 c3b2 c4d5 b4b5 a6b6 b2b4 h5h4 b4e1",
        "score": {
          "cp": 339
        },
        "depth": 22,
        "nodes": 1500914,
        "time": 1569,
        "nps": 956605
      },
      {
        "pv": "a4a6 f2g3 c4e6 b4b5 a6d6 d2a5 g6g5 d4c6 g5f4 e3f4 d6d3 g3h4",
        "score": {
          "cp": -354
        },
        "depth": 20,
        "nodes": 1501181,
        "time": 1599,
        "nps": 938824
      },
      {
        "pv": "d2c1 a4a2 f2g1 h5h4 b4b5 f7e7 b5b6 e7d6 g1h2 a2a6 c1b2 c4d5 b2b4 d6d7 b4c5 a6b7 c5b5 d5c6 b5b4 c6d5 b4c5 d5f7",
        "score": {
          "cp": 431
        },
        "depth": 22,
        "nodes": 1500841,
        "time": 1198,
        "nps": 1252788
      },
      {
        "pv": "a4a3 g3h4 f7g7 b4b5 a3e7 h4g3 h5h4 g3h2 e7a3 b5b6 c4d5 d4e2 d5c6 d2d4 g7h7 d4f6 a3e3 f6h4h7g8 h4d8 g8h7",
        "score": {
          "cp": -387
        },
        "depth": 21,
        "nodes": 1500490,
        "time": 1422,
        "nps": 1055196
      },
      {
        "pv": "d4c6 c4d3 g3h4 a3a1 c6e5 f7g8 b4b5 a1a7 d2c3 a7e7 h4g3 h5h4 g3h2 d3b5 e5g6 g8h7 g6e7 b5f1 e7f5",
        "score": {
          "cp": 438
        },
        "depth": 21,
        "nodes": 1501290,
        "time": 1117,
        "nps": 1344037
      },
      {
        "pv": "c4d5 g3h2",
        "score": {
          "cp": -390
        },
        "depth": 22,
        "nodes": 1500017,
        "time": 1342,
        "nps": 1117747
      },
      {
        "pv": "d2c1 f7e7 b5b6 e7d7 g3h4 d7c8 h4g5 c8b7 g5g6 b7b6 c1b2 b6c5 d4f5d3d5 b2e5 c5b4 g6h5",
        "score": {
          "cp": 496
        },
        "depth": 21,
        "nodes": 1501026,
        "time": 922,
        "nps": 1628010
      },
      {
        "pv": "f7e8 b5b6 e8d7 g3h4 d7c8 h4g5 c8b7 g5g6 b7b6 c1b2 b6c5 d4f5 d3d8 b2e5 c5b4 g6h5 c4f7 h5g4 d8d1 g4g3 d1e1 g3h2 e1g3h2g3 f7g8 e5e7 b4b5",
        "score": {
          "cp": -516
        },
        "depth": 20,
        "nodes": 1500360,
        "time": 994,
        "nps": 1509416
      },
      {
        "pv": "c1c7 f7g8 c7d8 g8f7 d8d5 f7e7 d5e5 e7d8 e5d6 d8c8 d6c5 c8b7 c5b5 b7c7 b5c6 c7b8 c6c1 g6g5 f4g5 f5f4 g3f4 d3f1 c1f1",
        "score": {
          "cp": 548
        },
        "depth": 21,
        "nodes": 1500028,
        "time": 881,
        "nps": 1702642
      },
      {
        "pv": "f7e8 c7c8",
        "score": {
          "cp": -544
        },
        "depth": 20,
        "nodes": 1500815,
        "time": 1019,
        "nps": 1472831
      },
      {
        "pv": "c7d6 f8f7 d6e6 f7f8 e6f6 f8e8 f6g6 e8d8 g6b6 d8c8 b6c5 c8b7 c5b5 d3b5 d4b5 b7b6 b5d4 b6c5 d4f5 c5b4 h3h4 b4c5 f5d4 c5c4 f4f5 c4d3 f5f6",
        "score": {
          "cp": 648
        },
        "depth": 20,
        "nodes": 1501456,
        "time": 1226,
        "nps": 1224678
      },
      {
        "pv": "f8g8 d4b5",
        "score": {
          "cp": -525
        },
        "depth": 20,
        "nodes": 1500184,
        "time": 835,
        "nps": 1796627
      }
    ]
  }
  """

  val expected2 =
    """1. d4 { [%eval 0.29] } 1... Nf6 { [%eval 0.35] } 2. Nf3 { [%eval 0.26] } 2... d5 { [%eval 0.3] } 3. c4 { [%eval 0.4] } 3... e6 { [%eval 0.4] } 4. Nc3 { [%eval 0.24] } 4... c5 { [%eval 0.31] } 5. e3 { [%eval 0.24] } 5... Nc6 { [%eval 0.09] } { D32 Tarrasch Defense: Symmetrical Variation } 6. cxd5 { [%eval 0.2] } 6... exd5 { [%eval 0.13] } 7. Bb5 { [%eval 0.16] } 7... cxd4 { [%eval 0.12] } 8. Nxd4 { [%eval 0.15] } 8... Bd7 { [%eval 0.12] } 9. O-O { [%eval 0.13] } 9... Bd6 { [%eval 0.12] } 10. Nf3 { [%eval 0.16] } 10... Be6 { [%eval 0.06] } 11. b3 { [%eval 0.03] } 11... a6 { [%eval 0.12] } 12. Bd3 { [%eval 0.14] } 12... Ne5 { [%eval 0.14] } 13. Nxe5 { [%eval 0.06] } 13... Bxe5 { [%eval 0.11] } 14. Bb2 { [%eval 0.18] } 14... Rc8 { [%eval 0.25] } 15. Rc1 { [%eval 0.0] } 15... O-O { [%eval 0.04] } 16. f4 { [%eval -0.06] } 16... Bc7 { [%eval 0.23] } 17. Ne2 { [%eval 0.1] } 17... Bb6 { [%eval 0.09] } 18. Bd4 { [%eval 0.09] } 18... Bxd4 { [%eval 0.21] } 19. Nxd4 { [%eval 0.13] } 19... Qa5 { [%eval -0.07] } 20. Qe2 { [%eval 0.23] } 20... Rfe8 { [%eval 0.08] } 21. h3 { [%eval 0.09] } 21... Bd7 { [%eval 0.08] } 22. Rxc8 { [%eval 0.07] } 22... Rxc8 { [%eval 0.05] } 23. Qb2 { [%eval 0.11] } 23... Qc5 { [%eval 0.2] } 24. a4 { [%eval 0.0] } 24... a5 { [%eval 0.0] } 25. Qd2 { [%eval -0.05] } 25... b6 { [%eval 0.0] } 26. Rb1 { [%eval 0.0] } 26... Qa3 { [%eval 0.0] } 27. Kh2 { [%eval 0.02] } 27... h6 { [%eval 0.0] } 28. Rb2 { [%eval 0.0] } 28... Qe7 { [%eval 0.0] } 29. Rc2 { [%eval 0.01] } 29... Rc5 { [%eval 0.04] } 30. Nf3 { [%eval -0.03] } 30... Rxc2 { [%eval 0.0] } 31. Bxc2 { [%eval 0.0] } 31... Qc5 { [%eval 0.0] } 32. Bd3 { [%eval 0.0] } 32... Kf8 { [%eval 0.0] } 33. Nd4 { [%eval 0.0] } 33... Kg8 { [%eval 0.05] } 34. Kg1 { [%eval 0.06] } 34... Kf8 { [%eval 0.04] } 35. Kf2 { [%eval 0.0] } 35... Ke7 { [%eval 0.1] } 36. Qb2 { [%eval 0.04] } 36... Kf8 { [%eval 0.04] } 37. Nc2 { [%eval 0.02] } 37... Ne4+ { [%eval 0.05] } 38. Bxe4 { [%eval 0.02] } 38... dxe4 { [%eval 0.04] } 39. Nd4 { [%eval 0.0] } 39... Bc6 { [%eval 0.0] } 40. Qe2 { [%eval 0.06] } 40... Qc3 { [%eval 0.04] } 41. Qa6 { [%eval 0.22] } 41... Qd2+ { [%eval 0.2] } 42. Ne2 { [%eval 0.18] } 42... Qb4 { [%eval 0.34] } 43. Qc8+ { [%eval 0.34] } 43... Be8 { [%eval 0.37] } 44. Qc2 { [%eval 0.45] } 44... Bd7 { [%eval 0.61] } 45. Nd4 { [%eval 0.67] } 45... f5?! { (0.67 → 1.36) Inaccuracy. Kg8 was best. } { [%eval 1.36] } (45... Kg8) 46. Qc7 { [%eval 1.46] } 46... Qd2+ { [%eval 1.34] } 47. Ne2 { [%eval 1.42] } 47... Be6 { [%eval 1.44] } 48. Qxb6 { [%eval 1.24] } 48... Kf7 { [%eval 1.29] } 49. Qd4 { [%eval 1.5] } 49... Qa2 { [%eval 1.68] } 50. Qa7+ { [%eval 1.21] } 50... Kg8 { [%eval 2.04] } 51. Qa8+ { [%eval 2.22] } 51... Kh7 { [%eval 2.25] } 52. Qxa5 { [%eval 1.61] } 52... Kh8 { [%eval 1.91] } 53. Qd8+?! { (1.91 → 1.23) Inaccuracy. Qa8+ was best. } { [%eval 1.23] } (53. Qa8+) 53... Kh7 { [%eval 2.19] } 54. Qa5 { [%eval 2.07] } 54... Kh8 { [%eval 1.9] } 55. b4 { [%eval 2.11] } 55... Bc4 { [%eval 2.2] } 56. Qd8+ { [%eval 2.16] } 56... Kh7 { [%eval 2.24] } 57. Qd1 { [%eval 2.24] } 57... Bb3 { [%eval 2.58] } 58. Qd7 { [%eval 2.9] } 58... Be6 { [%eval 3.2] } 59. Qb5 { [%eval 3.26] } 59... Kh8 { [%eval 3.34] } 60. Qc5 { [%eval 3.57] } 60... g6 { [%eval 3.82] } 61. Ke1 { [%eval 4.06] } 61... Kg8 { [%eval 4.21] } 62. Qd6 { [%eval 3.69] } 62... Kf7 { [%eval 4.59] } 63. Qd2?! { (4.59 → 3.00) Inaccuracy. Nd4 was best. } { [%eval 3.0] } (63. Nd4 Bc4 64. b5 h5 65. b6 Kg7 66. Qb4 Ba6 67. b7 Qa1+ 68. Kf2 Qf1+) 63... Qxa4 { [%eval 3.12] } 64. Nd4 { [%eval 2.64] } 64... Bc4?! { (2.64 → 3.39) Inaccuracy. g5 was best. } { [%eval 3.39] } (64... g5 65. fxg5 hxg5 66. b5 Qa3 67. Kf2 Qc5 68. Qe2 f4 69. Qh5+ Ke7 70. Qh7+) 65. Kf2 { [%eval 3.54] } 65... h5 { [%eval 4.31] } 66. Kg3 { [%eval 3.87] } 66... Qa3 { [%eval 4.38] } 67. b5 { [%eval 3.9] } 67... Qd3?! { (3.90 → 4.96) Inaccuracy. Bd5 was best. } { [%eval 4.96] } (67... Bd5 68. Kh2) 68. Qc1 { [%eval 5.16] } 68... Bxb5 { [%eval 5.48] } 69. Qc7+ { [%eval 5.44] } 69... Kf8 { [%eval 6.48] } 70. Qc5+ { [%eval 5.25] } { White wins. }


"""

  val pgn3 =
    """[Event "Thematic World Chess Championship Arena"]
  [Variant "From Position"]
  [FEN "r2q1rk1/1bp1bppp/p1np1n2/1p2p3/4P3/1BPPBN1P/PP3PP1/RN1Q1RK1 b - - 0 1"]
  [SetUp "1"]
1... Na5 2. Nbd2 c5 3. Bc2 Rc8 4. d4 Qc7 5. d5 Ba8 6. Nh4 Nxd5 7. exd5 Bxh4 8. Qg4 Bf6 9. Qf5 Rfe8 10. Ne4 Bxd5 11. Nxf6+ gxf6 12. Qxf6 Qe7 13. Qf5 f6 14. Be4 Be6 15. Qf3 Nc4 16. b3 Nxe3 17. fxe3 Rf8 18. Bf5 d5 19. Qg4+ Kh8 20. Bxe6 Rcd8 21. Rad1 d4 22. cxd4 cxd4 23. e4 Qg7 24. Qxg7+ Kxg7 25. Kf2 Rd6 26. Bg4 h5 27. Bxh5 Rc8 28. Rd2 Rc3 29. Bg4 a5 30. Be2 Rb6 31. Bd3 a4 32. b4 Kf7 33. h4 Ke7 34. Rh1 Rb8 35. h5 Rh8 36. Bxb5 Kd6 37. Bxa4 { Black wins on time. } Rc4 0-1


  """

  val sans3 =
    """Na5 Nbd2 c5 Bc2 Rc8 d4 Qc7 d5 Ba8 Nh4 Nxd5 exd5 Bxh4 Qg4 Bf6 Qf5 Rfe8 Ne4 Bxd5 Nxf6+ gxf6 Qxf6 Qe7 Qf5 f6 Be4 Be6 Qf3 Nc4 b3 Nxe3 fxe3 Rf8 Bf5 d5 Qg4+ Kh8 Bxe6 Rcd8 Rad1 d4 cxd4 cxd4 e4 Qg7 Qxg7+ Kxg7 Kf2 Rd6 Bg4 h5 Bxh5 Rc8 Rd2 Rc3 Bg4 a5 Be2 Rb6 Bd3 a4 b4 Kf7 h4 Ke7 Rh1 Rb8 h5 Rh8 Bxb5 Kd6 Bxa4 Rc4"""
      .split(" ")
      .toList
      .map(SanStr(_))

  val fish3 =
    """
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
        "pv": "c6a5",
        "score": {
          "cp": -6
        },
        "depth": 25,
        "nodes": 1500699,
        "time": 1542,
        "nps": 973215
      },
      {
        "pv": "b3c2",
        "score": {
          "cp": 13
        },
        "depth": 25,
        "nodes": 1501146,
        "time": 1564,
        "nps": 959812
      },
      {
        "pv": "a5b3 d1b3",
        "score": {
          "cp": 31
        },
        "depth": 25,
        "nodes": 1500713,
        "time": 1560,
        "nps": 961995
      },
      {
        "pv": "b3c2",
        "score": {
          "cp": 9
        },
        "depth": 23,
        "nodes": 1500480,
        "time": 1610,
        "nps": 931975
      },
      {
        "pv": "a8c8 a2a4 b5b4 c3b4 c5b4 d2b3 d6d5 f3e5 e7d6 e3d4 d5e4 b3a5 d6e5 d4e5",
        "score": {
          "cp": -12
        },
        "depth": 22,
        "nodes": 1501391,
        "time": 1684,
        "nps": 891562
      },
      {
        "pv": "f1e1 f8e8 a2a4 b5b4 a1c1 e7f8 e3g5 h7h6 g5h4 f8e7 c3b4 c5b4 d2b3 f6h5 b3a5 d8a5 c2b3 c8c1 d1c1 h5f4 c1e3 e7h4",
        "score": {
          "cp": 11
        },
        "depth": 23,
        "nodes": 1500918,
        "time": 1676,
        "nps": 895535
      },
      {
        "pv": "e5d4 c3d4",
        "score": {
          "cp": 5
        },
        "depth": 23,
        "nodes": 1501320,
        "time": 1692,
        "nps": 887304
      },
      {
        "pv": "d4d5 g7g6",
        "score": {
          "cp": 130
        },
        "depth": 21,
        "nodes": 1500572,
        "time": 1969,
        "nps": 762098
      },
      {
        "pv": "f6d7 g2g4 a5c4 d2c4 b5c4 d1e2 d7b6 g1h2 c8b8 f1g1 a6a5 h3h4 b7c8 h4h5 a5a4 g1g3 c7d8 a1h1 g7g5 h5g6 f7g6 g3h3",
        "score": {
          "cp": -128
        },
        "depth": 23,
        "nodes": 1500094,
        "time": 2070,
        "nps": 724683
      },
      {
        "pv": "b2b3 a8b7 d1e2 c8a8 g2g4 b7c8 g1h1 c8d7 f3h2 h7h6 h3h4 f6h7 g4g5 h6g5 f1g1 f7f6 d2f3 f8f7 g1g3 a5b7 a1g1 c5c4 b3b4 a8f8",
        "score": {
          "cp": 168
        },
        "depth": 23,
        "nodes": 1500192,
        "time": 1999,
        "nps": 750471
      },
      {
        "pv": "g7g6",
        "score": {
          "cp": -127
        },
        "depth": 22,
        "nodes": 1500755,
        "time": 2012,
        "nps": 745902
      },
      {
        "pv": "e4d5 e7h4 d1h5 f7f5 h5h4 a8d5 g2g4 d5e6 g4f5 e6f5 c2f5 f8f5 f2f4 e5e4 d2e4 a5c4 e3c1 c8e8 e4g3 f5f7 h4g4 c7e7 g4f3 c4e5 f3g2 e5d3 c1d2 d3b2",
        "score": {
          "cp": 275
        },
        "depth": 22,
        "nodes": 1500684,
        "time": 1798,
        "nps": 834640
      },
      {
        "pv": "e7h4 d1h5 f7f5 h5h4 a8d5 g2g4 d5e6 g4f5 e6f5 c2f5 f8f5 f2f4 c8e8 f4e5 f5e5 e3f4 e5e6 a1e1 a5c6 e1e6 e8e6 f4g3 h7h6 h4f4 c7e7 f4f3 e7e8 f1f2 c6e5 f3f5 e6g6 g1h2 g6e6",
        "score": {
          "cp": -265
        },
        "depth": 24,
        "nodes": 1500178,
        "time": 1905,
        "nps": 787495
      },
      {
        "pv": "d1h5 f7f5 h5h4 a8d5 g2g4 d5e6 g4f5 e6f5 c2f5 f8f5 f2f4 c7d7 f4e5 f5e5 e3f4 e5e6 a1e1 c8f8 e1e6 d7e6 h4g4 e6g4 h3g4 d6d5 f4c7 f8f1 g1f1 a5c4 d2c4 d5c4 f1e1",
        "score": {
          "cp": 276
        },
        "depth": 23,
        "nodes": 1500708,
        "time": 1849,
        "nps": 811632
      },
      {
        "pv": "h4e7 e3h6",
        "score": {
          "cp": -39
        },
        "depth": 23,
        "nodes": 1500790,
        "time": 1758,
        "nps": 853691
      },
      {
        "pv": "g4f5 f8e8 c2e4 h7h6 f5h7 g8f8 f2f4 f8e7 f4e5 d6e5 d5d6 c7d6 e4a8 c8a8 d2e4 e8h8 h7f5 d6e6 e3c5 e7e8 e4d6 e8d7 f5f3 a8d8 a2a4 f6e7 a4b5 e7d6 c5d6 e5e4 f3g3 e6d6 a1d1",
        "score": {
          "cp": 306
        },
        "depth": 23,
        "nodes": 1500273,
        "time": 1892,
        "nps": 792956
      },
      {
        "pv": "f8e8 c2e4 g8f8 f2f4 h7h6 f4e5 e8e5 f5h7 a8d5 e4d5 e5d5 f1f6 g7f6 d2e4 a5c4 e3h6 f8e7 a1f1 d5e5 f1f6 c8f8 f6f1 c7d7",
        "score": {
          "cp": -294
        },
        "depth": 23,
        "nodes": 1501263,
        "time": 2042,
        "nps": 735192
      },
      {
        "pv": "c2e4 g8f8 f2f4 f8e7 f4e5 d6e5 f5h7 c7d6 a1d1 e8h8 h7f5 c5c4 f5g4 a8d5 e4d5 d6d5 d2e4 d5e6 g4f3 h8d8 e3c5 c8c5 e4c5",
        "score": {
          "cp": 318
        },
        "depth": 21,
        "nodes": 1500645,
        "time": 1895,
        "nps": 791897
      },
      {
        "pv": "f6h4 f5h5",
        "score": {
          "cp": -319
        },
        "depth": 21,
        "nodes": 1500005,
        "time": 1866,
        "nps": 803861
      },
      {
        "pv": "e4f6 g8f8 f6d5 f7f6 d5c7 c8c7 f5h7 e5e4 b2b3 a5b7 a1d1 b7d8 d1d6 d8f7 d6b6 e8c8 h7f5 c8d8 c3c4 b5c4 b3c4",
        "score": {
          "cp": 851
        },
        "depth": 19,
        "nodes": 1501404,
        "time": 928,
        "nps": 1617892
      },
      {
        "pv": "g8f8 f6d5 f7f6 d5c7 c8c7 f5h7 e5e4 a1d1 a5c4 e3f4 c4e5 a2a4 e5f7 d1d6 b5a4",
        "score": {
          "cp": -839
        },
        "depth": 19,
        "nodes": 1501334,
        "time": 946,
        "nps": 1587033
      },
      {
        "pv": "e3h6 d5e4 c2e4 d6d5 f5h7",
        "score": {
          "mate": 3
        },
        "depth": 245,
        "nodes": 1352444,
        "time": 158,
        "nps": 8559772
      },
      {
        "pv": "c7e7 f6h6",
        "score": {
          "cp": -168
        },
        "depth": 23,
        "nodes": 1500052,
        "time": 1903,
        "nps": 788256
      },
      {
        "pv": "f6h6",
        "score": {
          "cp": 159
        },
        "depth": 24,
        "nodes": 1500755,
        "time": 1881,
        "nps": 797849
      },
      {
        "pv": "f7f6 e3h6",
        "score": {
          "cp": -41
        },
        "depth": 22,
        "nodes": 1500482,
        "time": 1759,
        "nps": 853031
      },
      {
        "pv": "e3h6",
        "score": {
          "cp": 57
        },
        "depth": 22,
        "nodes": 1500727,
        "time": 1867,
        "nps": 803817
      },
      {
        "pv": "d5e6 f5f3",
        "score": {
          "cp": 85
        },
        "depth": 22,
        "nodes": 1500258,
        "time": 1840,
        "nps": 815357
      },
      {
        "pv": "f5h5 d6d5",
        "score": {
          "cp": -96
        },
        "depth": 20,
        "nodes": 1500214,
        "time": 1862,
        "nps": 805700
      },
      {
        "pv": "e7f7 b2b4",
        "score": {
          "cp": 63
        },
        "depth": 22,
        "nodes": 1500025,
        "time": 1850,
        "nps": 810824
      },
      {
        "pv": "b2b3 c4e3",
        "score": {
          "cp": -3
        },
        "depth": 24,
        "nodes": 1500962,
        "time": 1807,
        "nps": 830637
      },
      {
        "pv": "c4e3 f2e3",
        "score": {
          "cp": 3
        },
        "depth": 25,
        "nodes": 1500486,
        "time": 1764,
        "nps": 850615
      },
      {
        "pv": "f2e3",
        "score": {
          "cp": -2
        },
        "depth": 24,
        "nodes": 1500896,
        "time": 1839,
        "nps": 816147
      },
      {
        "pv": "e8f8 e4d5",
        "score": {
          "cp": 0
        },
        "depth": 24,
        "nodes": 1500435,
        "time": 1933,
        "nps": 776220
      },
      {
        "pv": "e4d5 f6f5",
        "score": {
          "cp": -5
        },
        "depth": 24,
        "nodes": 1500485,
        "time": 1805,
        "nps": 831293
      },
      {
        "pv": "e6f5 f3f5",
        "score": {
          "cp": 4
        },
        "depth": 24,
        "nodes": 1500886,
        "time": 1688,
        "nps": 889150
      },
      {
        "pv": "f3g4",
        "score": {
          "cp": 51
        },
        "depth": 22,
        "nodes": 1500701,
        "time": 1694,
        "nps": 885891
      },
      {
        "pv": "g8f7 a2a4 b5b4 c3b4 c5b4 f5e6 e7e6 g4b4 f7g7 a1c1 c8c1 f1c1 f8d8 b4e1 e6d6 e1g3 g7h8 a4a5 d6e7 g3e1 e7d6 c1c3 d5d4 c3d3 d6c5 b3b4 c5c4 e1f1",
        "score": {
          "cp": -34
        },
        "depth": 23,
        "nodes": 1500355,
        "time": 1648,
        "nps": 910409
      },
      {
        "pv": "f5e6 c8d8 a1d1 d5d4 c3d4 e5d4 e3d4 d8d4 g4f5 f8e8 d1d4 c5d4 e6d5 e7e5 f5e5 f6e5 d5e4 e8e7 h3h4 h8g7 g2g4 e7c7 g4g5",
        "score": {
          "cp": 440
        },
        "depth": 22,
        "nodes": 1500455,
        "time": 1629,
        "nps": 921089
      },
      {
        "pv": "c8d8 a1d1 d5d4 c3d4 c5d4 e3e4 d4d3 e6d5 e7c5 g1h1 c5d4 g4f3 d3d2 f3g4 d8c8 g4h4",
        "score": {
          "cp": -453
        },
        "depth": 23,
        "nodes": 1500639,
        "time": 1567,
        "nps": 957650
      },
      {
        "pv": "a1d1 d5d4 c3d4 c5d4 e3e4 d8d6 e6d5 e7g7 g4h5 d6d8 d1c1 d8c8 h5h4 c8c3 b3b4 f6f5 e4f5 c3c1 f1c1",
        "score": {
          "cp": 431
        },
        "depth": 23,
        "nodes": 1501163,
        "time": 1626,
        "nps": 923224
      },
      {
        "pv": "d5d4 c3d4 e5d4 e3d4 d8d4 d1d4 c5d4 f1d1 f8e8 e6d5 e7e3 g1h1 e3e2 g4e2 e8e2 a2a4 e2e3 h1h2 b5a4 b3a4 e3e5 d5f3 e5a5 d1d4 a5a4 d4a4 h7h6 h2g3 a6a5 f3e4 h8g7 g3f4 g7f7 g2g3",
        "score": {
          "cp": -462
        },
        "depth": 23,
        "nodes": 1501197,
        "time": 1534,
        "nps": 978616
      },
      {
        "pv": "c3d4 c5d4 e3e4 d4d3 e6d5 e7c5 g1h1 c5e3 h1h2 e3d4 g4f3 d8d5 e4d5 e5e4 f3f4 d4d5",
        "score": {
          "cp": 442
        },
        "depth": 21,
        "nodes": 1500208,
        "time": 1805,
        "nps": 831140
      },
      {
        "pv": "c5d4 e3e4 d8d6 e6d5 e7g7 d1c1 g7g4 h3g4 f8d8 g1f2 h8g7 g4g5 f6g5 f2e2 d8d7 c1c8",
        "score": {
          "cp": -451
        },
        "depth": 22,
        "nodes": 1501388,
        "time": 1778,
        "nps": 844425
      },
      {
        "pv": "e3d4 d8d4 d1d4 e5d4 f1d1 f8e8 e6d5 e7e3 g1h1 e3e5 g4d4 e5d4d1d4 e8e2 d4g4 h7h5 g4g6 e2a2 g6f6 a6a5 h1h2 a2g2 d5g2 h8g7 f6a6 h5h4 a6a5",
        "score": {
          "cp": 470
        },
        "depth": 21,
        "nodes": 1500258,
        "time": 1371,
        "nps": 1094280
      },
      {
        "pv": "d8d6 e6d5 e7g7 g4h5 f8c8 d1c1 d6d8 c1c8 d8c8 d5e6 c8d8 e6f5 b5b4 f1d1 d8g8 h5f3",
        "score": {
          "cp": -451
        },
        "depth": 19,
        "nodes": 1500294,
        "time": 1902,
        "nps": 788798
      },
      {
        "pv": "g4g7 h8g7 d1c1 g7h8 c1c7 d8d6 e6f5 h7h5 f1f3 h5h4 f3d3 h8g8 g2g3 f8f7 c7c8 f7f8 c8c2 g8g7 g3h4 f8h8 c2c7 g7h6 g1f1 a6a5 f1e1 a5a4 b3a4 b5a4 d3g3 d6b6 g3g7 b6b1 e1f2",
        "score": {
          "cp": 479
        },
        "depth": 22,
        "nodes": 1501232,
        "time": 1848,
        "nps": 812354
      },
      {
        "pv": "h8g7 d1c1 g7h8 c1c7 h7h6 f1d1 d8d6 e6f5 d4d3 d1d2 f8d8 g1f2 h6h5 h3h4 d6d4 f2e1 a6a5 c7b7 a5a4 b7b5",
        "score": {
          "cp": -460
        },
        "depth": 23,
        "nodes": 1500297,
        "time": 1936,
        "nps": 774946
      },
      {
        "pv": "d1c1 d8d6 c1c7 g7h8 e6f5 h7h5 f1d1 h8g8 d1d3 h5h4 g2g3 f8f7 c7c8 f7f8 c8c2 g8g7 g3h4 f8h8 d3g3 g7h6 g1f2 h6h5 g3g4 a6a5 g4g3 b5b4",
        "score": {
          "cp": 459
        },
        "depth": 23,
        "nodes": 1501335,
        "time": 1956,
        "nps": 767553
      },
      {
        "pv": "d8d6 e6f5 d6c6 d1c1 c6c3 b3b4 f8d8 c1c3 d4c3 f2e3 d8d2 f1c1 d2g2 c1c3 g2a2 c3c7 g7f8 c7h7 a2b2 h7a7 b2b4 a7a6 f8g7 a6a7 g7f8 e3f3 b4b1 a7b7",
        "score": {
          "cp": -378
        },
        "depth": 23,
        "nodes": 1500517,
        "time": 1997,
        "nps": 751385
      },
      {
        "pv": "e6f5 d6c6 d1c1 c6c3 c1c3 d4c3 b3b4 a6a5 a2a3 a5b4 a3b4 f8a8 f1c1 a8a3 f2e3 h7h5 e3d3 a3b3 c1c3 b3b4 c3c7 g7f8 c7h7 b4b2 h7h5 b2g2 h5h8 f8g7 h8h7 g7f8 h7b7 g2b2 d3c3 b2b1 c3c2 b1a1 c2b2 a1h1 b7b6 f8g7",
        "score": {
          "cp": 399
        },
        "depth": 26,
        "nodes": 1500547,
        "time": 1639,
        "nps": 915525
      },
      {
        "pv": "h7h5 g4f5 d6c6 d1c1 c6c3 b3b4 a6a5 b4a5 f8a8 c1c3 d4c3 f5e6 a8a5 f2e3 h5h4 e3d3 b5b4 f1b1 a5a4 e6b3 a4a6 d3c4 a6b6 b3c2 b6a6 c4b4 a6a2 b4c3 g7h6 b1f1 h6g6 c3b3 a2a7 c2d3 a7b7 b3c3",
        "score": {
          "cp": -354
        },
        "depth": 24,
        "nodes": 1500712,
        "time": 2007,
        "nps": 747738
      },
      {
        "pv": "g4f5 d6c6 d1c1 c6c3 b3b4 a6a5 b4a5 f8a8 c1c3 d4c3 f5e6 a8a5 e6b3 a5a6 h3h4 f6f5 f2e3 f5f4 e3d3 a6g6 d3c3 g6g2 c3b4 g7f6 b4b5 g2a2 b3a2 f6e7",
        "score": {
          "cp": 338
        },
        "depth": 26,
        "nodes": 1500076,
        "time": 1899,
        "nps": 789929
      },
      {
        "pv": "f6f5 f2e2 f5e4 f1f8 g7f8 d1c1 d6h6 h5g4 f8e7 g4f5 e4e3 g2g3 h6f6 g3g4 f6h6 c1h1 h6h4 e2f3 e7d6 f5e4",
        "score": {
          "cp": -335
        },
        "depth": 23,
        "nodes": 1500572,
        "time": 1616,
        "nps": 928571
      },
      {
        "pv": "d1c1 d6c6 c1c6 c8c6 f2e1 c6c1 h5d1 b5b4 f1f2 a6a5 f2c2 c1a1 e1d2 f6f5 d1f3 f5f4 d2d3 a1e1",
        "score": {
          "cp": 390
        },
        "depth": 23,
        "nodes": 1500637,
        "time": 1766,
        "nps": 849737
      },
      {
        "pv": "c8c3 h5g4 d6c6 f2g1 c3e3 b3b4 c6d6 g4f5 d6c6 h3h4 c6c4 d2b2 g7h6 g2g4 h6g7 b2g2 d4d3 g4g5 f6g5 g2g5 g7h6 g5g6 h6h5",
        "score": {
          "cp": -366
        },
        "depth": 23,
        "nodes": 1501183,
        "time": 1772,
        "nps": 847168
      },
      {
        "pv": "h5g4 d6c6 f1e1 c3c2 e1d1 c2c3 f2g1 c3e3 d1f1 g7g6 d2f2 b5b4 g4e2 d4d3 e2f3 g6g5 g2g3 c6c3",
        "score": {
          "cp": 372
        },
        "depth": 24,
        "nodes": 1501217,
        "time": 1754,
        "nps": 855881
      },
      {
        "pv": "b5b4 f2g1 c3e3 d2e2 e3c3 e2f2 c3e3 f1c1 e3c3 c1d1 c3e3 f2e2 e3c3 e2d2 c3e3 g4f3 a6a5 d2d3 e3d3 d1d3 d6c6",
        "score": {
          "cp": -357
        },
        "depth": 22,
        "nodes": 1500053,
        "time": 1762,
        "nps": 851335
      },
      {
        "pv": "f2g1 d6b6 g1h2 a5a4 b3a4 b5a4 f1f3 b6c6 f3g3 g7h6 g4d7 c6c4 d2f2 c3g3 h2g3 h6g7 f2d2 c4c3 g3g4",
        "score": {
          "cp": 380
        },
        "depth": 23,
        "nodes": 1500619,
        "time": 1935,
        "nps": 775513
      },
      {
        "pv": "a5a4 e2b5 a4b3 a2b3 c3b3 b5d3 d6c6 h3h4 c6b6 f2g1 g7g6 g1h2 b3c3 d3e2 c3e3 e2f3 f6f5 e4f5 g6f6 f3d1 d4d3 h2g1 e5e4 g1f2",
        "score": {
          "cp": -390
        },
        "depth": 22,
        "nodes": 1500356,
        "time": 1646,
        "nps": 911516
      },
      {
        "pv": "e2d3 g7h6 f2e2 h6g5 d2c2 c3c2 d3c2 b6c6 c2d3 a5a4 f1f5 g5g6 e2d2 c6c3",
        "score": {
          "cp": 390
        },
        "depth": 22,
        "nodes": 1500778,
        "time": 1761,
        "nps": 852230
      },
      {
        "pv": "a5a4 b3b4 c3c4 f1b1 g7h6 f2e2 h6g5 d3c4 b5c4 d2d1 g5f4 d1c1 f4e4 c1c4 f6f5 b4b5 b6g6 g2g4 e4d5 c4c6",
        "score": {
          "cp": -409
        },
        "depth": 21,
        "nodes": 1500924,
        "time": 1839,
        "nps": 816163
      },
      {
        "pv": "b3b4 b6b7 f2f3 b7c7 f3g4 c3c4 f1b1 c4c3 h3h4 g7f7 b1f1 f7e6 h4h5 c7g7 g4h4 c3d3 d2d3",
        "score": {
          "cp": 382
        },
        "depth": 22,
        "nodes": 1500997,
        "time": 1749,
        "nps": 858202
      },
      {
        "pv": "g7f7 f2e2 b6b8 d2c2 c3c2 d3c2 b8g8 e2f2 f7g6 c2d3 g8c8 f2g1 g6g5 d3b5 c8c2 b5a4 c2a2 a4b3 a2b2 b3c4 g5h4 b4b5",
        "score": {
          "cp": -407
        },
        "depth": 22,
        "nodes": 1500980,
        "time": 1757,
        "nps": 854285
      },
      {
        "pv": "f2e2 f7g6 d2c2 c3a3 f1f3 b6b7 g2g3 g6g5 f3f5 g5g6 h3h4 b7g7 g3g4 g7h7 h4h5 g6h6 e2d2 h7g7 f5f6 h6g5 f6g6 g7g6 h5g6 g5g6",
        "score": {
          "cp": 424
        },
        "depth": 21,
        "nodes": 1500546,
        "time": 1793,
        "nps": 836891
      },
      {
        "pv": "c3c4 d2c2 c4b4 h4h5 a4a3 f2g3 f7g7 g3h4 g7h6 g2g4 b4b2 c2b2 a3b2 f1b1 b5b4 b1b2 b6b7 b2b1 b7a7 b1f1 h6g7",
        "score": {
          "cp": -418
        },
        "depth": 22,
        "nodes": 1500144,
        "time": 1784,
        "nps": 840887
      },
      {
        "pv": "h4h5 b6b8 f2g3 b8g8 g3h4 a4a3 g2g3 g8c8 g3g4 c3c4 f1b1 c4c3 b1g1 c3c6 d2d1 c6c4 g4g5 f6g5 g1g5 c4c1 d1c1 c8c1 g5g7 e7f8 g7g3 c1a1 g3g2 a1d1 d3e2 d1b1",
        "score": {
          "cp": 449
        },
        "depth": 23,
        "nodes": 1500192,
        "time": 1611,
        "nps": 931217
      },
      {
        "pv": "b6b8 h4h5 e7f7 h1f1 b8h8 d3b5 h8h5 b5a4 f7e7 a4b3 h5h4 b4b5 f6f5 e4f5 e5e4 f5f6 e7f8 f2g1 e4e3 d2e2 c3c8",
        "score": {
          "cp": -415
        },
        "depth": 22,
        "nodes": 1500744,
        "time": 1780,
        "nps": 843114
      },
      {
        "pv": "h4h5 a4a3 h5h6 e7f7 h6h7 f7g7 h7h8q b8h8 h1h8 g7h8 d3b5 c3c1 b5d3 h8g8 f2e2 c1g1 b4b5 g1g2 e2d1 g2g7 d1c2 g8f8 c2b3 f8e7",
        "score": {
          "cp": 478
        },
        "depth": 23,
        "nodes": 1500001,
        "time": 1435,
        "nps": 1045296
      },
      {
        "pv": "e7f7 h5h6 f7g8 h6h7 g8h8 f2f3 c3c4 f3g4 c4b4 d2c2 a4a3 g4f5 b4a4 c2c7 a4a8 f5g6 b5b4 d3c4 b8c8 c7c8 a8c8 c4e6 c8c6 e6b3 c6c3 g2g4 c3c6 g4g5 f6g5 g6g5 c6d6",
        "score": {
          "cp": -463
        },
        "depth": 23,
        "nodes": 1500160,
        "time": 1613,
        "nps": 930043
      },
      {
        "pv": "h5h6 a4a3 d3b5 h8b8 h6h7 c3c8 b5d3 b8b4 h7h8r b4b8 h8c8 b8c8 h1h7 e7d6 h7a7 c8c1 a7a6 d6c5 a6f6",
        "score": {
          "cp": 519
        },
        "depth": 21,
        "nodes": 1500359,
        "time": 1637,
        "nps": 916529
      },
      {
        "pv": "a4a3 h5h6 e7f8 b5d7 f8e7 d7f5 e7d6 h6h7 d6c6 h1h3 c6b5 h3c3 d4c3 d2c2 b5b4 f2f3 b4c4 f3e3 c4b4 c2c1",
        "score": {
          "cp": -514
        },
        "depth": 20,
        "nodes": 1501030,
        "time": 1772,
        "nps": 847082
      },
      {
        "pv": "b5a4 c3c4 h5h6 c4b4 a4b3 f6f5 d2e2 d4d3 e2d2 f5e4 f2e3 b4d4 g2g4 d6c5 h1h5 c5b4 h5e5 h8h6 e3d4 h6g6 d4e4",
        "score": {
          "cp": 532
        },
        "depth": 21,
        "nodes": 1501044,
        "time": 1759,
        "nps": 853350
      },
      {
        "pv": "c3c4 a4b3 c4b4 h5h6 b4b7 d2c2 b7h7 b3d5 h7h6 h1b1 h8d8 c2c6 d6e7 a2a4 h6h4 a4a5 h4f4 f2e2 e7f8 a5a6 d4d3 e2e3",
        "score": {
          "cp": -524
        },
        "depth": 20,
        "nodes": 1500377,
        "time": 1583,
        "nps": 947806
      },
      {
        "pv": "a4b3 c4b4 h5h6 f6f5 d2e2 d4d3 e2d2 f5e4 f2e3 b4d4 d2f2 d6c5 g2g4 h8d8 h6h7 d4d7 e3e4 c5b4",
        "score": {
          "cp": 537
        },
        "depth": 20,
        "nodes": 1500801,
        "time": 1613,
        "nps": 930440
      }
    ]
  }
  """.trim

  val expected3 =
    """1... Na5 { [%eval 0.13] } 2. Nbd2 { [%eval -0.31] } 2... c5 { [%eval 0.09] } 3. Bc2 { [%eval 0.12] } 3... Rc8 { [%eval 0.11] } 4. d4 { [%eval -0.05] } 4... Qc7? { (-0.05 → 1.30) Mistake. exd4 was best. } { [%eval 1.3] } (4... exd4 5. cxd4) 5. d5 { [%eval 1.28] } 5... Ba8 { [%eval 1.68] } 6. Nh4 { [%eval 1.27] } 6... Nxd5? { (1.27 → 2.75) Mistake. g6 was best. } { [%eval 2.75] } (6... g6) 7. exd5 { [%eval 2.65] } 7... Bxh4 { [%eval 2.76] } 8. Qg4?? { (2.76 → 0.39) Blunder. Qh5 was best. } { [%eval 0.39] } (8. Qh5 f5 9. Qxh4 Bxd5 10. g4 Be6 11. gxf5 Bxf5 12. Bxf5 Rxf5 13. f4 Qd7) 8... Bf6?? { (0.39 → 3.06) Blunder. Be7 was best. } { [%eval 3.06] } (8... Be7 9. Bh6) 9. Qf5 { [%eval 2.94] } 9... Rfe8 { [%eval 3.18] } 10. Ne4 { [%eval 3.19] } 10... Bxd5?? { (3.19 → 8.51) Blunder. Bh4 was best. } { [%eval 8.51] } (10... Bh4 11. Qh5) 11. Nxf6+ { [%eval 8.39] } 11... gxf6? { (8.39 → Mate in 3) Checkmate is now unavoidable. Kf8 was best. } { [%eval #3] } (11... Kf8 12. Nxd5 f6 13. Nxc7 Rxc7 14. Qxh7 e4 15. Rad1 Nc4 16. Bf4 Ne5 17. a4) 12. Qxf6?? { (Mate in 3 → 1.68) Lost forced checkmate sequence. Bh6 was best. } { [%eval 1.68] } (12. Bh6 Be4 13. Bxe4 d5 14. Qxh7#) 12... Qe7 { [%eval 1.59] } 13. Qf5? { (1.59 → 0.41) Mistake. Qh6 was best. } { [%eval 0.41] } (13. Qh6) 13... f6 { [%eval 0.57] } 14. Be4? { (0.57 → -0.85) Mistake. Bh6 was best. } { [%eval -0.85] } (14. Bh6) 14... Be6 { [%eval -0.96] } 15. Qf3 { [%eval -0.63] } 15... Nc4?! { (-0.63 → -0.03) Inaccuracy. Qf7 was best. } { [%eval -0.03] } (15... Qf7 16. b4) 16. b3 { [%eval -0.03] } 16... Nxe3 { [%eval -0.02] } 17. fxe3 { [%eval 0.0] } 17... Rf8 { [%eval -0.05] } 18. Bf5 { [%eval -0.04] } 18... d5?! { (-0.04 → 0.51) Inaccuracy. Bxf5 was best. } { [%eval 0.51] } (18... Bxf5 19. Qxf5) 19. Qg4+ { [%eval 0.34] } 19... Kh8?? { (0.34 → 4.40) Blunder. Kf7 was best. } { [%eval 4.4] } (19... Kf7 20. a4 b4 21. cxb4 cxb4 22. Bxe6+ Qxe6 23. Qxb4 Kg7 24. Rac1 Rxc1 25. Rxc1) 20. Bxe6 { [%eval 4.53] } 20... Rcd8 { [%eval 4.31] } 21. Rad1 { [%eval 4.62] } 21... d4 { [%eval 4.42] } 22. cxd4 { [%eval 4.51] } 22... cxd4 { [%eval 4.7] } 23. e4 { [%eval 4.51] } 23... Qg7 { [%eval 4.79] } 24. Qxg7+ { [%eval 4.6] } 24... Kxg7 { [%eval 4.59] } 25. Kf2 { [%eval 3.78] } 25... Rd6 { [%eval 3.99] } 26. Bg4 { [%eval 3.54] } 26... h5 { [%eval 3.38] } 27. Bxh5 { [%eval 3.35] } 27... Rc8 { [%eval 3.9] } 28. Rd2 { [%eval 3.66] } 28... Rc3 { [%eval 3.72] } 29. Bg4 { [%eval 3.57] } 29... a5 { [%eval 3.8] } 30. Be2 { [%eval 3.9] } 30... Rb6 { [%eval 3.9] } 31. Bd3 { [%eval 4.09] } 31... a4 { [%eval 3.82] } 32. b4 { [%eval 4.07] } 32... Kf7 { [%eval 4.24] } 33. h4 { [%eval 4.18] } 33... Ke7 { [%eval 4.49] } 34. Rh1 { [%eval 4.15] } 34... Rb8 { [%eval 4.78] } 35. h5 { [%eval 4.63] } 35... Rh8 { [%eval 5.19] } 36. Bxb5 { [%eval 5.14] } 36... Kd6 { [%eval 5.32] } 37. Bxa4 { [%eval 5.24] } { Black wins on time. } 37... Rc4 { [%eval 5.37] }


"""

  val pgn4 =
    """
[Variant "Crazyhouse"]

1. Nf3 c5 2. d4 cxd4 3. Nxd4 Nc6 4. Nxc6 bxc6 5. e4 Nf6 6. Nc3 P@b4 7. Bc4 e6 8. e5 bxc3 9. P@d6 cxb2 10. Bxb2 P@c7 11. dxc7 Qxc7 12. P@d6 Qa5+ 13. Kf1 Rb8 14. exf6 gxf6 15. Bxf6 P@e7 16. N@c7+ Kd8 17. P@g7 Bxg7 18. Bxe7# { White wins by checkmate. }
  """

  val sans4 =
    """Nf3 c5 d4 cxd4 Nxd4 Nc6 Nxc6 bxc6 e4 Nf6 Nc3 P@b4 Bc4 e6 e5 bxc3 P@d6 cxb2 Bxb2 P@c7 dxc7 Qxc7 P@d6 Qa5+ Kf1 Rb8 exf6 gxf6 Bxf6 P@e7 N@c7+ Kd8 P@g7 Bxg7 Bxe7#
  """.trim
      .split(" ")
      .toList
      .map(SanStr(_))

  val fish4 =
    """
  {
    "fishnet": {
      "version": "2.6.11-dev",
      "apikey": ""
    },
    "stockfish": {
      "flavor": "classical"
    },
    "analysis": [
      {
        "pv": "e2e4 b8c6 g1f3 g8f6 e4e5 d7d5 f1b5 c8d7 b5c6 d7c6 e5f6 e7f6 e1h1 P@e4 f3d4 B@e5 N@e2 c6d7 N@f5 d7f5 d4f5",
        "score": {
          "cp": 130
        },
        "depth": 20,
        "nodes": 4200041,
        "time": 3658,
        "nps": 1148179
      },
      {
        "pv": "d7d5 d2d4 c8f5 c1f4 b8c6 e2e3 e7e6 f1d3 f8b4 b1d2 f5d3 c2d3 B@g4 B@e2 g8f6 e1h1 e8h8 f3e5 g4e2 d1e2 b4d2 e5c6 b7c6 e2d2",
        "score": {
          "cp": -92
        },
        "depth": 21,
        "nodes": 4200007,
        "time": 4317,
        "nps": 972899
      },
      {
        "pv": "b1c3",
        "score": {
          "cp": 308
        },
        "depth": 18,
        "nodes": 4200961,
        "time": 4485,
        "nps": 936669
      },
      {
        "pv": "c5d4 f3d4",
        "score": {
          "cp": -131
        },
        "depth": 20,
        "nodes": 4201187,
        "time": 4521,
        "nps": 929260
      },
      {
        "pv": "f3d4 b8c6 e2e4 e7e5 d4c6 b7c6 N@d6 f8d6 d1d6 d8e7 B@b4 e7d6 b4d6 N@e6 Q@d2 Q@a4 b1c3 a4d4 d2d4 e6d4",
        "score": {
          "cp": 284
        },
        "depth": 20,
        "nodes": 4200083,
        "time": 4504,
        "nps": 932522
      },
      {
        "pv": "b8c6 P@d5",
        "score": {
          "cp": -216
        },
        "depth": 19,
        "nodes": 4201476,
        "time": 4610,
        "nps": 911383
      },
      {
        "pv": "b1c3 g8f6 e2e4 d7d6 f1c4 e7e6 P@c7 d8c7 c3b5 c7a5 c1d2 P@b4 d4c6 b7c6 N@c7 e8d7 c7a8 c6b5 R@c7 d7d8",
        "score": {
          "cp": 343
        },
        "depth": 19,
        "nodes": 4200032,
        "time": 4705,
        "nps": 892674
      },
      {
        "pv": "b7c6 b1c3 d8c7 e2e4 a8b8 N@g5 g8h6 P@f5 P@b4 c3a4 e7e6 g5e6 d7e6 c1h6 e6f5 h6g7 f8g7 N@d6 e8f8 d6f5",
        "score": {
          "cp": -138
        },
        "depth": 20,
        "nodes": 4200072,
        "time": 4597,
        "nps": 913654
      },
      {
        "pv": "b1c3 d8c7",
        "score": {
          "cp": 161
        },
        "depth": 19,
        "nodes": 4201240,
        "time": 4758,
        "nps": 882984
      },
      {
        "pv": "e7e5 P@f4 e5f4 c1f4 N@e6 N@d6 f8d6 f4d6 P@e7 d6b4 g8f6 b1c3 e8h8 P@f5 d7d6 f5e6 c8e6",
        "score": {
          "cp": -123
        },
        "depth": 18,
        "nodes": 4200095,
        "time": 4708,
        "nps": 892118
      },
      {
        "pv": "b1c3",
        "score": {
          "cp": 269
        },
        "depth": 19,
        "nodes": 4200681,
        "time": 4918,
        "nps": 854144
      },
      {
        "pv": "P@b4 f1c4",
        "score": {
          "cp": -186
        },
        "depth": 18,
        "nodes": 4202287,
        "time": 4784,
        "nps": 878404
      },
      {
        "pv": "N@d5 f6d5 c3d5 c6d5 d1d5 N@d6 d5a8 N@d4 N@e3 e7e5 R@d5 N@g4 d5c5 g4f2 c5e5 P@e6 P@c7 d4c2 e1f2 d8f6 N@f3 c2e3",
        "score": {
          "cp": 317
        },
        "depth": 20,
        "nodes": 4200396,
        "time": 4963,
        "nps": 846342
      },
      {
        "pv": "N@e5 d1d4 e5c4 d4c4 d8a5 N@b3 a5b6 c1e3 b6b8 P@c7 b8c7 N@d5 c7b8 d5b4 e7e6 b4d3 B@a6 c4d4 a6d3 d4d3",
        "score": {
          "cp": -207
        },
        "depth": 21,
        "nodes": 4200368,
        "time": 5237,
        "nps": 802056
      },
      {
        "pv": "c1f4 d7d6 c3b5 c6b5 c4b5 N@d7 P@c6 N@c7 P@b7 c8b7 c6d7 f6d7 b5d7 d8d7 N@c5 d7c6 c5b7 c6e4 N@e3 e4f4 B@c6 P@d7",
        "score": {
          "cp": 475
        },
        "depth": 20,
        "nodes": 4201292,
        "time": 5166,
        "nps": 813258
      },
      {
        "pv": "N@h4 e1h1 b4c3 e5f6 g7f6 P@g7 N@f3 g2f3 h8g8 N@h5 f8g7 N@d6 e8f8 b2c3",
        "score": {
          "cp": -511
        },
        "depth": 15,
        "nodes": 4200090,
        "time": 5185,
        "nps": 810046
      },
      {
        "pv": "e5f6",
        "score": {
          "cp": 436
        },
        "depth": 18,
        "nodes": 4200382,
        "time": 4987,
        "nps": 842266
      },
      {
        "pv": "c3b2 c1b2",
        "score": {
          "cp": 38
        },
        "depth": 17,
        "nodes": 4200655,
        "time": 4951,
        "nps": 848445
      },
      {
        "pv": "c1b2",
        "score": {
          "cp": 158
        },
        "depth": 21,
        "nodes": 4200565,
        "time": 4778,
        "nps": 879147
      },
      {
        "pv": "f6e4 e1h1",
        "score": {
          "cp": -110
        },
        "depth": 17,
        "nodes": 4200514,
        "time": 5039,
        "nps": 833600
      },
      {
        "pv": "e5f6 N@e3 f2e3 g7f6 N@h5 P@f2 e1f1 N@f5 N@e4 f5e3 f1f2 e3c4 N@g7 f8g7 h5g7 e8f8",
        "score": {
          "cp": 1621
        },
        "depth": 19,
        "nodes": 4200388,
        "time": 5464,
        "nps": 768738
      },
      {
        "pv": "d8c7 e5f6",
        "score": {
          "cp": -723
        },
        "depth": 17,
        "nodes": 4201208,
        "time": 5282,
        "nps": 795382
      },
      {
        "pv": "e5f6",
        "score": {
          "cp": 725
        },
        "depth": 19,
        "nodes": 4202563,
        "time": 5106,
        "nps": 823063
      },
      {
        "pv": "c7a5 P@d2",
        "score": {
          "cp": -255
        },
        "depth": 16,
        "nodes": 4200684,
        "time": 4958,
        "nps": 847253
      },
      {
        "pv": "P@d2 N@h4 N@c7 e8d8 c7e6 d7e6 P@e7 f8e7 d6e7 d8c7 B@d8 h8d8 e7d8b c7d8 R@d4 f6d7 d4h4 B@e7 e1h1 d7e5 b2e5 a5e5",
        "score": {
          "cp": 271
        },
        "depth": 18,
        "nodes": 4202717,
        "time": 4982,
        "nps": 843580
      },
      {
        "pv": "P@h3 N@c7",
        "score": {
          "cp": 674
        },
        "depth": 17,
        "nodes": 4201982,
        "time": 4896,
        "nps": 858247
      },
      {
        "pv": "N@c7 e8d8",
        "score": {
          "cp": 2809
        },
        "depth": 17,
        "nodes": 4201234,
        "time": 5513,
        "nps": 762059
      },
      {
        "pv": "P@f3 g2f3",
        "score": {
          "cp": -1347
        },
        "depth": 16,
        "nodes": 4200322,
        "time": 5351,
        "nps": 784960
      },
      {
        "pv": "N@c7 e8d8 b2f6 P@e7 c7e6 f7e6 P@c7 d8e8 c7b8q N@b6 f6h8 P@f3 N@c7 e8f7 R@f4 N@f5 b8b6 a7b6 d6e7 P@e2 c4e2 f3g2 f1g2 N@h4 g2g1",
        "score": {
          "cp": 4051
        },
        "depth": 18,
        "nodes": 4201425,
        "time": 5845,
        "nps": 718806
      },
      {
        "pv": "c8a6 c4a6",
        "score": {
          "cp": -1379
        },
        "depth": 16,
        "nodes": 4200005,
        "time": 5720,
        "nps": 734266
      },
      {
        "pv": "N@c7",
        "score": {
          "cp": 4001
        },
        "depth": 18,
        "nodes": 4200724,
        "time": 5997,
        "nps": 700470
      },
      {
        "pv": "e8d8 c7e6 f7e6 P@c7 d8e8 c7b8q P@e2 d1e2 N@d2 e2d2 a5d2 b8c8 e8f7 N@e5 f7f6 N@e4 f6e5 e4d2 Q@d4 d2f3 e5f6 B@g5 f6g7",
        "score": {
          "cp": -4439
        },
        "depth": 15,
        "nodes": 4204412,
        "time": 5898,
        "nps": 712853
      },
      {
        "pv": "c7e6 f7e6 P@c7 d8e8 c7b8q N@d8 b8c8 N@d5 c4d5 e7f6 N@c7 a5c7 d6c7",
        "score": {
          "cp": 4223
        },
        "depth": 18,
        "nodes": 4200811,
        "time": 5952,
        "nps": 705781
      },
      {
        "pv": "N@e3 f2e3",
        "score": {
          "cp": -3731
        },
        "depth": 17,
        "nodes": 4201957,
        "time": 5655,
        "nps": 743051
      },
      {
        "pv": "f6e7",
        "score": {
          "mate": 1
        },
        "depth": 245,
        "nodes": 28211,
        "time": 10,
        "nps": 2821100
      },
      {
        "score": {
          "mate": 0
        },
        "depth": 0,
        "nodes": 0,
        "time": 0
      }
    ]
  }
  """

  val expected4 =
    """1. Nf3 { [%eval 0.92] } 1... c5?? { (0.92 → 3.08) Blunder. d5 was best. } { [%eval 3.08] } { A04 Zukertort Opening: Sicilian Invitation } (1... d5 2. d4 Bf5 3. Bf4 Nc6 4. e3 e6 5. Bd3 Bb4+ 6. Nbd2 Bxd3 7. cxd3) 2. d4? { (3.08 → 1.31) Mistake. Nc3 was best. } { [%eval 1.31] } (2. Nc3) 2... cxd4 { [%eval 2.84] } 3. Nxd4 { [%eval 2.16] } 3... Nc6 { [%eval 3.43] } 4. Nxc6?? { (3.43 → 1.38) Blunder. Nc3 was best. } { [%eval 1.38] } (4. Nc3 Nf6 5. e4 d6 6. Bc4 e6 7. P@c7 Qxc7 8. Ncb5 Qa5+ 9. Bd2 P@b4) 4... bxc6 { [%eval 1.61] } 5. e4 { [%eval 1.23] } 5... Nf6? { (1.23 → 2.69) Mistake. e5 was best. } { [%eval 2.69] } (5... e5 6. P@f4 exf4 7. Bxf4 N@e6 8. N@d6+ Bxd6 9. Bxd6 P@e7 10. Bb4 Nf6 11. Nc3) 6. Nc3 { [%eval 1.86] } 6... P@b4 { [%eval 3.17] } 7. Bc4?! { (3.17 → 2.07) Inaccuracy. N@d5 was best. } { [%eval 2.07] } (7. N@d5 Nxd5 8. Nxd5 cxd5 9. Qxd5 N@d6 10. Qxa8 N@d4 11. N@e3 e5 12. R@d5 N@g4) 7... e6?? { (2.07 → 4.75) Blunder. N@e5 was best. } { [%eval 4.75] } (7... N@e5 8. Qd4 Nxc4 9. Qxc4 Qa5 10. N@b3 Qb6 11. Be3 Qb8 12. P@c7 Qxc7 13. N@d5) 8. e5 { [%eval 5.11] } 8... bxc3 { [%eval 4.36] } 9. P@d6?? { (4.36 → -0.38) Blunder. exf6 was best. } { [%eval -0.38] } (9. exf6) 9... cxb2 { [%eval 1.58] } 10. Bxb2 { [%eval 1.1] } 10... P@c7?? { (1.10 → 16.21) Blunder. Ne4 was best. } { [%eval 16.21] } (10... Ne4 11. O-O) 11. dxc7?! { (16.21 → 7.23) Inaccuracy. exf6 was best. } { [%eval 7.23] } (11. exf6 N@e3 12. fxe3 gxf6 13. N@h5 P@f2+ 14. Kf1 N@f5 15. N@e4 Nxe3+ 16. Kxf2 Nxc4) 11... Qxc7 { [%eval 7.25] } 12. P@d6?? { (7.25 → 2.55) Blunder. exf6 was best. } { [%eval 2.55] } (12. exf6) 12... Qa5+ { [%eval 2.71] } 13. Kf1?? { (2.71 → -6.74) Blunder. P@d2 was best. } { [%eval -6.74] } (13. P@d2 N@h4 14. N@c7+ Kd8 15. Nxe6+ dxe6 16. P@e7+ Bxe7 17. dxe7+ Kc7 18. B@d8+ Rxd8) 13... Rb8?? { (-6.74 → 28.09) Blunder. P@h3 was best. } { [%eval 28.09] } (13... P@h3 14. N@c7+) 14. exf6 { [%eval 13.47] } 14... gxf6 { [%eval 40.51] } 15. Bxf6 { [%eval 13.79] } 15... P@e7 { [%eval 40.01] } 16. N@c7+ { [%eval 44.39] } 16... Kd8 { [%eval 42.23] } 17. P@g7 { [%eval 37.31] } 17... Bxg7?! { (37.31 → Mate in 1) Checkmate is now unavoidable. N@e3+ was best. } { [%eval #1] } (17... N@e3+ 18. fxe3) 18. Bxe7# { White wins by checkmate. }


"""
