package chess
package format.pgn

// format: off
object Fixtures {

  val simple = "e3 Nc6 d4 Nf6"

  val raws = List(
    "e3 Nc6 d4 Nf6 c3 e5 dxe5 Nxe5 Bb5 a6 Ba4 b5 Bb3 d5 e4 dxe4 f4 Qxd1+ Kxd1 Nd3 Be3 Ng4 Bd4 Ngf2+ Bxf2 Nxf2+ Ke1 Nxh1 Bd5 Ra7 Bc6+ Kd8 Bxe4 Bd6 g3 Re8 Nd2 f5 Ne2 fxe4 Kf1 e3 Kg2 exd2 Rxh1 Bb7+ Kf2 Bc5+ Kf1 d1=Q#",
    "c4 Nc6 e3 Nf6 h3 Ne4 d3 Nc5 a3 Ne5 d4 d6 dxe5 dxe5 b4 Qxd1+ Kxd1 Ne4 f3 Nf2+ Ke2 Nxh1 Nd2 Ng3+ Ke1 Bf5 Bd3 Bxd3 Rb1 Bxb1 Nxb1 Rd8 Bd2 e6 h4 Be7 Nh3 Bxh4 Nf2 Ke7 Bc3 f6 Nd2 h5 c5 g5 Nc4 Rhg8 Na5 Nh1 Ke2 Nxf2 Be1 Nd3 Nxb7 Bxe1 Nxd8 Rxd8 c6 a5 bxa5 Bxa5 a4 f5 Kd1 Nf4+ Kc2 Rd2+ Kc1 Nxg2 Kb1 Nxe3 Kc1 h4 Kb1 h3 Kc1 h2 Kb1 h1=Q#",
    "d4 Nf6 c4 Nc6 Nc3 e5 Nd5 Nxd5 cxd5 Nxd4 e3 Nf5 e4 Nd4 h4 Qf6 Bg5 Qb6 b3 h6 Bc4 hxg5 h5 Bc5 Ne2 Qa5+ Kf1 d6 Nxd4 Bxd4 Rc1 Qxa2 Rc2 Qa5 Qc1 g4 h6 g3 f3 gxh6 Rxh6 Rxh6 Qxh6 Bf2 Qh8+ Kd7 Qf8 Qe1#",
    "Nc3 c6 Nf3 Na6 b4 Nxb4 Rb1 c5 a3 Nxc2+ Qxc2 b6 Nb5 Ba6 Qa4 Bxb5 Rxb5 Nf6 Bb2 Nd5 Qg4 Nc7 Bxg7 Bxg7 Qxg7 Rf8 Rb3 Ne6 Qxh7 Qb8 Re3 f6 Qg6+ Rf7 g3 Nf8 Qg8 e5 d4 d6 dxc5 Qc7 cxd6 Qc1#",
    "d4 Nf6 Nf3 Nc6 Nbd2 e6 e4 d6 c4 Qe7 Bd3 e5 d5 Nd4 Nxd4 exd4 O-O Bg4 f3 Bd7 Nb3 Qe5 Be2 c5 dxc6 bxc6 Qxd4 Qxd4+ Nxd4 Rb8 b3 Be7 Be3 Bd8 Rfd1 Bb6 Kf2 Ke7 Rd2 Ba5 Rd3 Bb6 Rad1 Rhd8 g4 h6 Bf4 g5 Bg3 h5 h3 h4 Bh2 Rb7 e5 dxe5 Bxe5 Ne8 Kg2 Bc7 Bxc7 Rxc7 Nf5+ Kf8 f4 gxf4 Nxh4 Ng7 Bf3 Ne6 Nf5 Nc5 Rd4 Ne6 Rd6 c5 h4 Ng7 Nxg7 Kxg7 g5 a5 Kf2 Kf8 Bc6 Ke7 Ba4 Bxa4 Rxd8 Bc6 h5 Ke6 h6 Be4 Rh8 Re7 Re1 Kf5 h7 Kg6 Rc8 Kxh7 Rxc5 a4 b4 Kg6 b5 f6 gxf6 Kxf6 b6 a3 Rc7 Rxc7 bxc7 Bb7 Re8 Kf5 c5 Ba6 Ra8 Bb7 Rf8+ Ke5 c6 Ba6 Ra8 Kd6 Rxa6 Kxc7 Kf3 Kb8 Kxf4 Kc8 Ke5 Kc7 Ke6 Kd8 Kd6 Ke8 Ra7 Kf8 c7 Kf7 c8=Q+ Kg6 Qg4+ Kf6 Ra8 Kf7 Qf5+ Kg7 Ra7+ Kg8 Qc8#",
    "e3 Nc6 Nf3 Nf6 Nc3 e6 a3 Bd6 d4 Ng4 h3 Nf6 Bb5 a6 Bxc6 dxc6 e4 Nd7 O-O h5 h4 c5 Bg5 f6 Be3 cxd4 Bxd4 c5 Be3 Qe7 g3 Ne5 Nxe5 Bxe5 Qe2 Bxc3 bxc3 Kf7 Rad1 b5 c4 Rb8 Rd2 Bb7 Rfd1 Bc6 Kg2 Bxe4+ Kf1 Bc6 Bf4 e5 Be3 Qe6 Bxc5 Qh3+ Ke1 Qh1+ Qf1 Qe4+ Re2 Qxc4 Bd6 Rbd8 Bb4 Bf3 Rxd8 Rxd8 Re3 Rd1#",
    "e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6 Nxe5 Nxe5 d4 Ng6 Bg5 h6 Bxf6 Qxf6 e5 Qe6 Bd3 d6 Qe2 Nf4 Qe4 dxe5 Bb5+ c6 d5 Nxd5 Bc4 O-O O-O b5 Bb3 Bb7 Bc2 Nf4 Qh7#")

  val shortCastles = List(
    "e3 Nf6 Be2 d5 Nf3 Nbd7 O-O c6",
    "e3 Nf6 Be2 d5 Nf3 Nbd7 o-o c6",
    "e3 Nf6 Be2 d5 Nf3 Nbd7 0-0 c6"
  )

  val longCastles = List(
    "e3 e5 d3 f5 Qe2 d5 Bd2 Nd7 Nc3 Ngf6 O-O-O Bb4",
    "e3 e5 d3 f5 Qe2 d5 Bd2 Nd7 Nc3 Ngf6 o-o-o Bb4",
    "e3 e5 d3 f5 Qe2 d5 Bd2 Nd7 Nc3 Ngf6 0-0-0 Bb4"
  )

  lazy val annotatedCastles = for {
    castle <- List("O-O", "o-o", "0-0", "O-O-O", "o-o-o", "0-0-0")
    annotation <- List("+", "#", "+?", "#!")
  } yield s"$castle$annotation"

  // https://github.com/ornicar/lila/issues/2441
  val enpassantEP = """
1. d4 Nf6 2. c4 e6 3. Nf3 c5 4. d5 exd5 5. cxd5 d6 6. Nc3 g6 7. Nd2 Bg7 8. e4 O-O 9. Be2 Na6 10. O-O Qe7 11. Re1 Nc7 12. a4 b6 13. h3 Nd7 14. Nb5 Nxb5 15. axb5 Qd8 16. Ra4 Nf6 17. Nf3 a5 18. bxa6 e.p. Bd7 1-0
"""
  val enpassantEP2 = """
1. d4 Nf6 2. c4 e6 3. Nf3 c5 4. d5 exd5 5. cxd5 d6 6. Nc3 g6 7. Nd2 Bg7 8. e4 O-O 9. Be2 Na6 10. O-O Qe7 11. Re1 Nc7 12. a4 b6 13. h3 Nd7 14. Nb5 Nxb5 15. axb5 Qd8 16. Ra4 Nf6 17. Nf3 a5 18. bxa6e.p. Bd7 1-0
"""

  val recentChessCom = """[Event "Live Chess"]
[Site "Chess.com"]
[Date "2016.05.10"]
[Round "?"]
[White "LyonBeast"]
[Black "FabianoCaruana"]
[Result "0-1"]
[WhiteElo "2747"]
[BlackElo "2699"]
[ECO "A00"]
[TimeControl "300+2"]
[Termination "FabianoCaruana won by resignation"]
[Annotator "Komodo 9.01 64-bit"]
[CurrentPosition "3r2k1/p3n1p1/1p5p/2q1p3/2P1B1R1/2Q1P3/P5PP/3r1R1K w - - 6 28"]

1.d4 Nf6 2.c4 e6 3.Nf3 d5 4.Nc3 Be7 5.Bg5 h6 6.Bh4 O-O 7.e3 Ne4 8.Bxe7 Qxe7 9.cxd5 Nxc3 10.bxc3 exd5 11.Qb3 Rd8 12.c4 Be6 13.Rc1 c5 14.dxc5 Qxc5 15.Nd4 Nc6 16.Nxe6 fxe6 17.Be2 d4 18.O-O dxe3 19.fxe3 Ne5 20.Kh1 b6 21.Qc3 Rd6 22.Rf4 Rad8 23.Re4 Nc6 24.Rf1 e5 25.Rg4 Rd2 26.Bf3 Ne7 27.Be4 Rd1 28.Kg1? ( 28.Rg1! { Forced, but Black is clearly much, much better. The silicon companion says Kh8 is now best. #shrug } ) 28...b5? ( 28...R8d3! { This classic interference tactic just ends the game. } 29.Bxd3 Qxe3+ 30.Kh1 Rxf1+ 31.Bxf1 Qxc3 ) 29.Bc2? ( 29.Rxd1 Rxd1+ 30.Kf2 { Black clearly still has a big plus, but is not yet winning. } ) 29...Rxf1+ 30.Kxf1 Nd5 31.Rxg7+ Kf8! { Black simply leaves White to deal with the hanging queen, e3 pawn, and rook. } ( 31...Kxg7? 32.Qxe5+ Kf8 33.Qh8+ Ke7 34.Qe5+ Kf7 35.cxd5 { Unclear. } ) ( 31...Kh8? 32.Rh7+ Kg8 33.Rh8+! )
0-1
"""

  val chessComCrazyhouse = """
[Event "Live Chess - Crazyhouse"]
[Site "Chess.com"]
[Date "2016.11.30"]
[White "eekarf"]
[Black "JannLeeCrazyhouse"]
[Result "0-1"]
[ECO "C46"]
[WhiteElo "2071"]
[BlackElo "2593"]
[TimeControl "180"]
[Termination "JannLeeCrazyhouse won by checkmate"]
[Variant "Crazyhouse"]
[CurrentPosition "r1bBk1r1/ppp2p1p/1b1p3p/3Pp3/6N1/2PPn2n/PP2B1pP/R2Q3K w q - 0 22 QNrp"]

1.e4 e5 2.Nf3 Nc6 3.Nc3 Bc5 4.Be2 Nf6 5.O-O d6 6.d3 Nd4 7.Nxd4 Bxd4 8.Nd5 Nxd5 9.exd5 N@f6 10.c3 Bb6 11.Bg5 N@f4 12.N@e4 Rg8 13.N@h6 Nxe4 14.Bxd8 N@h3+ 15.gxh3 Nxh3+ 16.Kh1 Nexf2+ 17.Rxf2 Nxf2+ 18.Kg2 gxh6+ 19.N@g4 N@e3+ 20.Kg1 Nh3+ 21.Kh1 @g2#
0-1
"""

val invisibleChar = """
﻿[Event "Potsdam"]
[Site "?"]
[Date "2018.01.21"]
[Round "5.1"]
[White "XXXXX, XXXXX"]
[Black "YYYYY, YYYYY"]
[Result "1/2-1/2"]
[ECO "A40"]
[WhiteElo "1637"]
[BlackElo "1859"]
[Annotator "ZZZZZ"]
[PlyCount "19"]
[SourceDate "2017.09.30"]
[SourceVersionDate "2017.09.30"]
[WhiteTeam "XXXXX"]
[BlackTeam "YYYYY"]

{A40: Verschiedene (seltene) Antworten auf 1.d4} 1. e4 g6 2. d4 Bg7 3. c4 Nc6
4. Nf3 e5 5. d5 Nd4 6. Nxd4 exd4 7. f4 (7. Bd3 d6 (7... c5 8. O-O d6 9. Nd2 Nh6
10. b4 b6 11. bxc5 bxc5 12. Rb1 O-O 13. Qa4 Ng4 14. Nf3 f5 15. Bg5 Bf6 16. Bxf6
Qxf6 17. Qc6 fxe4 18. Bxe4 Bf5 19. Bxf5 gxf5 20. Rfe1 Rab8 21. Rxb8 Rxb8 22. h3
{Duessel,U (2204)-Schwierskott,M (2232) Baden 2013 1/2-1/2 (43)}) 8. O-O Nf6 9.
Bg5 h6 10. Bh4 O-O 11. Nd2 g5 12. Bg3 Ng4 13. Rc1 Ne5 14. Bb1 h5 15. f3 h4 16.
Bf2 d3 17. Qb3 Qf6 18. Be3 Ng6 19. Bxd3 Qxb2 20. Bxg5 Qd4+ 21. Kh1 f6 {Hansen,
C (2621)-Sebastian,D (2384) Germany 2007 0-1}) 7... d6 $146 (7... c5 8. Bd3 d6
9. O-O Nh6 10. Nd2 O-O 11. Nf3 f6 12. Bd2 b6 13. Qc2 Qd7 14. h3 Ba6 15. a4 Rae8
16. Rae1 Bc8 17. Re2 Re7 18. Rfe1 Rfe8 19. e5 fxe5 20. fxe5 dxe5 21. Nxe5 Qd6
22. Bf4 {Putz,G-Kirschner,C Pocking 2001 1/2-1/2}) 8. Be2 f5 9. exf5 Bxf5 10.
O-O {Viel gibt es nicht zur Partie zu sagen. Obwohl Stockfish 8 die Stellung
mit -0.10 minimal Schwarz vorne sieht, ist doch die Remisbreite sehr hoch.}
1/2-1/2
"""

  val fromPosProdCloseChess = """
[FEN "8/rnbqkbnr/pppppppp/8/8/PPPPPPPP/RNBQKBNR/8 w - - 0 1"]                                                                     [12/1807]
1. d4 d5 2. Nf4 Nf5 3. g4 g5 4. gxf5 exf5
5. Nbd3 gxf4 6. Nxf4 Bxf4 7. exf4 Nd6 8. Bh4 Qd8
9. Bd3 Kd7 10. Kf1 Kc8 11. Rg2 Kb8 12. Qe1 Bh5
13. Qf2 Rh8 14. Re2 Bf7 15. Rg3 a5 16. Rg2 a4
17. b4 Rf8 18. Re3 Nc4 19. Bxc4 dxc4 20. Kg1 Qd6
21. Qd2 Rc7 22. Kh2 b5 23. Rg7 Kb7 24. Re1 Bh5
25. Rxc7+ Kxc7 26. Qe2 Kd7 27. Rf1 Rg8 28. Qd2 Re8
29. Qf2 Qxf4+ 30. Kg2 Bxf3+ 31. Qxf3 Qxh4 32. Qxf5+ Kc7
33. Qf4+ Qxf4 34. Rxf4 Re2+ 35. Rf2 Re3 36. Rf3 Rd3
37. Kf2 Rd2+ 38. Ke1 Ra2 39. Rxf6 h5 40. Rf7+ Kd6
41. Rf5 Rxa3 42. Kd2 Ra2+ 43. Kc1 h4 44. Rf4 a3
45. Kb1 Rb2+ 46. Ka1 Kd5 47. Rxh4 Rc2 48. Rh8 Rxc3
49. Ka2 Rb3 50. Rd8+ Ke4 51. h4 Rxb4 52. Kxa3 Rb3+
53. Ka2 Rh3 54. Rd6 b4 55. Rxc6 Kxd4 56. Rg6 Rh2+
57. Kb1 c3 58. Kc1 Kc4 59. Kd1 Kb3 60. Rc6 c2+
61. Kc1 Rh1+ 62. Kd2 Rd1+ 63. Ke2 c1=Q 64. Rxc1 Rxc1
65. Kf3 Rh1 66. Ke3 Kc4 67. Kf3 Rxh4 68. Kg2 b3
69. Kg3 Rd4 70. Kh3 b2 71. Kg3 b1=R 72. Kf3 Rb3+
73. Ke2 Rdd3 74. Kf2 Rd2+ 75. Ke1 Ra2 76. Kd1 Rb1#
"""

  val fromChessProgrammingWiki = """
1.Nf3 d5 2.d4 c6 3.c4 e6 4.Nbd2 Nf6 5.e3 c5 6.b3 Nc6 7.Bb2 cxd4
8.exd4 Be7 9.Rc1 0-0 10.Bd3 Bd7 11.0-0 Nh5 12.Re1 Nf4 13.Bb1 Bd6
14.g3 Ng6 15.Ne5 Rc8 16.Nxd7 Qxd7 17.Nf3 Bb4 18.Re3 Rfd8 19.h4 Nge7
20.a3 Ba5 21.b4 Bc7 22.c5 Re8 23.Qd3 g6 24.Re2 Nf5 25.Bc3 h5 26.b5
Nce7 27.Bd2 Kg7 28.a4 Ra8 29.a5 a6 30.b6 Bb8 31.Bc2 Nc6 32.Ba4 Re7
33.Bc3 Ne5 34.dxe5 Qxa4 35.Nd4 Nxd4 36.Qxd4 Qd7 37.Bd2 Re8 38.Bg5
Rc8 39.Bf6+ Kh7 40.c6 bxc6 41.Qc5 Kh6 42.Rb2 Qb7 43.Rb4 1–0"""

  val noTagButResult = "1.g4 e5 2.d4 e4 3.c4 Qh4 4.h3 Bb4+ 5.Nc3 Bxc3+ 6.bxc3 Qe7 7.Bf4 d6 8.e3 g5 9.Bg3 Be6 10.Rb1 Bc8 11.Be2 Nf6 12.h4 gxh4 13.Bxh4 Qe6 14.g5 Nfd7 15.Nh3 Rg8 16.Nf4 Qe7 17.Nd5 Qd8 18.g6 f6 19.gxh7 1-0"

  val inlineTags = """
[Event "NII - Lille 2 / Tour Blanche Paris"] [Site "Lille"][Date "2015.03.14"][Round "8"] [White "Blazquez, Denis"] [Black "Soubirou, Oriane"] [Result "0-1"] [ECO "B00"] [WhiteElo "2083"] [BlackElo "2135"] [PlyCount "35"]

1. d4 a6 2. e4 e6 3. c4 b5 4. cxb5 axb5 5. Bxb5 Bb7 6. Nc3 Bb4 7. Qe2 Qh4 8.
Bd3 f5 9. g3 Qf6 10. Nf3 c5 11. O-O Ne7 12. Bg5 Qf8 13. Nb5 Bxe4 14. Bxe4 fxe4
15. Qxe4 Nd5 16. Ne5 cxd4 17. Qxd5 Ra7 18. Qa8 0-1
"""

  val whiteResignsInTags = """
[Event "NII - Lille 2 / Tour Blanche Paris"] [Site "Lille"][Date "2015.03.14"][Round "8"] [White "Blazquez, Denis"] [Black "Soubirou, Oriane"] [Result "0-1"] [ECO "B00"] [WhiteElo "2083"] [BlackElo "2135"] [PlyCount "35"]

1. d4 a6 2. e4 e6 3. c4 b5 4. cxb5 axb5 5. Bxb5 Bb7 6. Nc3 Bb4 7. Qe2 Qh4 8.
Bd3 f5 9. g3 Qf6 10. Nf3 c5 11. O-O Ne7 12. Bg5 Qf8 13. Nb5 Bxe4 14. Bxe4 fxe4
15. Qxe4 Nd5 16. Ne5 cxd4 17. Qxd5 Ra7 18. Qa8
"""

  val whiteResignsInMoves = """
[Event "NII - Lille 2 / Tour Blanche Paris"] [Site "Lille"][Date "2015.03.14"][Round "8"] [White "Blazquez, Denis"] [Black "Soubirou, Oriane"] [ECO "B00"] [WhiteElo "2083"] [BlackElo "2135"] [PlyCount "35"]

1. d4 a6 2. e4 e6 3. c4 b5 4. cxb5 axb5 5. Bxb5 Bb7 6. Nc3 Bb4 7. Qe2 Qh4 8.
Bd3 f5 9. g3 Qf6 10. Nf3 c5 11. O-O Ne7 12. Bg5 Qf8 13. Nb5 Bxe4 14. Bxe4 fxe4
15. Qxe4 Nd5 16. Ne5 cxd4 17. Qxd5 Ra7 18. Qa8 0-1
"""

  val whiteResignsInTagsAndMoves = """
[Event "NII - Lille 2 / Tour Blanche Paris"] [Site "Lille"][Date "2015.03.14"][Round "8"] [White "Blazquez, Denis"] [Black "Soubirou, Oriane"] [Result "0-1"] [ECO "B00"] [WhiteElo "2083"] [BlackElo "2135"] [PlyCount "35"]

1. d4 a6 2. e4 e6 3. c4 b5 4. cxb5 axb5 5. Bxb5 Bb7 6. Nc3 Bb4 7. Qe2 Qh4 8.
Bd3 f5 9. g3 Qf6 10. Nf3 c5 11. O-O Ne7 12. Bg5 Qf8 13. Nb5 Bxe4 14. Bxe4 fxe4
15. Qxe4 Nd5 16. Ne5 cxd4 17. Qxd5 Ra7 18. Qa8 0-1
"""

val festivalFigueira = """
[Event "figueira"]
[Site "?"]
[Date "2017.10.29"]
[Round "1.21"]
[White "Paiva, Henrique M A Albergaria"]
[Black "Morais, Mario Martins Freitas"]
[Result "1-0"]
[BlackElo "1457"]
[WhiteElo "2110"]
[LiveChessVersion "1.4.8"]
[ECO "B35"]

1. e4 {[%clk 1:30:58]} c5 {[%clk 1:30:58]} 2. Nf3 {[%clk 1:31:07]} g6
{[%clk 1:31:18]} 3. d4 {[%clk 1:31:30]} cxd4 {[%clk 1:31:37]} 4. Nxd4
{[%clk 1:31:52]} Nc6 {[%clk 1:31:42]} 5. Nc3 {[%clk 1:32:03]} Nf6 6. Bc4
{[%clk 1:31:26]} Bg7 {[%clk 1:27:34]} 7. Be3 {[%clk 1:31:10]} Qa5
{[%clk 1:24:02]} 8. O-O {[%clk 1:29:40]} O-O {[%clk 1:21:27]} 9. Bb3
{[%clk 1:27:53]} d6 {[%clk 1:17:26]} 10. h3 {[%clk 1:27:38]} Bd7 {[%clk 1:15:56]}
11. f4 Nxd4 12. Bxd4 Bc6 {[%clk 1:11:22]} 13. Re1 {[%clk 1:19:12]} Nd7
{[%clk 1:06:46]} 14. Bxg7 {[%clk 1:13:57]} Kxg7 {[%clk 1:07:08]} 15. Qd4+
{[%clk 1:14:21]} Kg8 {[%clk 1:06:55]} 16. Rad1 {[%clk 1:14:32]} Qc5
{[%clk 1:04:24]} 17. Nd5 {[%clk 1:09:40]} Bxd5 {[%clk 1:01:12]} 18. exd5
{[%clk 1:09:44]} Qxd4+ {[%clk 0:51:57]} 19. Rxd4 {[%clk 1:09:58]} Nc5
{[%clk 0:51:59]} 20. Rxe7 {[%clk 1:09:08]} Rfe8 {[%clk 0:52:17]} 21. Rxe8+
{[%clk 1:07:56]} Rxe8 {[%clk 0:52:37]} 22. Kf2 {[%clk 1:05:57]} f5
{[%clk 0:44:33]} 23. c4 {[%clk 1:00:52]} Nxb3 {[%clk 0:41:17]} 24. axb3
{[%clk 1:01:17]} a5 {[%clk 0:41:40]} 25. g3 {[%clk 1:01:15]} Kf7 {[%clk 0:41:46]}
26. Rd3 {[%clk 1:00:09]} b5 {[%clk 0:36:07]} 27. Rc3 {[%clk 0:55:05]} Rc8
{[%clk 0:35:36]} 28. Ke3 {[%clk 0:47:55]} Ke7 {[%clk 0:35:54]} 29. Kd4
{[%clk 0:48:05]} Kd7 {[%clk 0:36:00]} 30. c5 {[%clk 0:44:27]} dxc5+
{[%clk 0:36:29]} 31. Rxc5 {[%clk 0:44:52]} Rb8 {[%clk 0:36:45]} 32. Rc6
{[%clk 0:45:16]} a4 {[%clk 0:35:24]} 33. b4 {[%clk 0:45:10]} a3 {[%clk 0:34:48]}
34. bxa3 {[%clk 0:45:34]} Ra8 {[%clk 0:35:17]} 35. Rc3 {[%clk 0:44:17]} Kd6
{[%clk 0:35:43]} 36. Re3 {[%clk 0:37:51]} Ra7 {[%clk 0:33:33]} 37. g4
{[%clk 0:37:42]} Rc7 {[%clk 0:31:26]} 38. Re6+ {[%clk 0:37:31]} Kd7
{[%clk 0:31:45]} 39. gxf5 {[%clk 0:37:28]} gxf5 40. Rh6 Rc4+ {[%clk 0:24:34]} 41.
Ke5 {[%clk 0:37:50]} Re4+ {[%clk 0:24:21]} 42. Kxf5 {[%clk 1:08:14]} Re3
{[%clk 0:54:44]} 43. Rxh7+ {[%clk 1:08:19]} Kd6 {[%clk 0:54:59]} 44. Rb7
{[%clk 1:08:30]} Rxa3 45. Rxb5 Rxh3 {[%clk 0:54:36]} 46. Ke4 {[%clk 1:08:56]} Rh1
{[%clk 0:54:18]} 47. Rb6+ {[%clk 1:09:12]} Kd7 {[%clk 0:54:19]} 48. f5
{[%clk 1:07:41]} Re1+ {[%clk 0:52:42]} 49. Kd4 {[%clk 1:08:04]} Rd1+
{[%clk 0:52:45]} 50. Ke5 {[%clk 1:08:26]} Re1+ 51. Kf6 Rd1 {[%clk 0:50:48]} 52.
Rb5 {[%clk 1:07:13]} Kd6 {[%clk 0:50:12]} 53. Kf7 {[%clk 1:07:12]} Re1
{[%clk 0:46:28]} 54. f6 {[%clk 1:07:52]} Re4 {[%clk 0:43:59]} 55. Kf8 Rg4 56. f7
Re4 57. Rb6+ {[%clk 1:08:13]} Re1 {[%clk 0:37:03]} 58. Kg7+ Kd7 1-0
"""

val crazyhouseFromProd = """
[Event "Hourly Crazyhouse Inc Arena"]
[Site "https://lichess.org/vjT6KovO"]
[Date "2016.08.19"]
[White "danieldima"]
[Black "Hesar"]
[Result "1-0"]
[WhiteElo "2319"]
[BlackElo "1943"]
[PlyCount "49"]
[Variant "Crazyhouse"]
[TimeControl "180+1"]
[ECO "?"]
[Opening "?"]
[Termination "Normal"]
[Annotator "lichess.org"]

1. e4 Nf6 2. e5 d5?! { (0.37 → 0.89) Inaccuracy. Best move was Nd5. } (2... Nd5 3. d4 d6 4. c4 Nb4 5. Nf3 Bf5) 3. Nc3?! { (0.89 → 0.23) Inaccuracy. Best move was exf6. } (3. exf6 exf6 4. d4 Nc6 5. N@c5 Bxc5 6. dxc5) 3... Ng4 4. Nf3?! { (0.64 → -0.10) Inaccuracy. Best move was d4. } (4. d4 Nxe5 5. dxe5 d4 6. Bf4 Na6) 4... e6?! { (-0.10 → 0.43) Inaccuracy. Best move was d4. } (4... d4 5. Na4 e6 6. d3 Nc6) 5. d4 Bb4 6. Bd2 Bxc3 7. Bxc3 Nxf2 8. Kxf2 N@e4+ 9. Ke2?! { (0.47 → -0.24) Inaccuracy. Best move was Ke1. } (9. Ke1 P@f2+ 10. Ke2 Bd7) 9... P@f2? { (-0.24 → 1.19) Mistake. Best move was Nxc3+. } (9... Nxc3+ 10. bxc3 P@e4 11. B@b5+ B@c6 12. Ke1 exf3 13. Qxf3) 10. B@d3 Nc6 11. N@h5 O-O 12. Nxg7?! { (1.30 → 0.60) Inaccuracy. Best move was Ng3. } (12. Ng3 Nxc3+ 13. bxc3 f5 14. exf6 B@h6 15. Kxf2 Rxf6) 12... Kxg7 13. P@f6+? { (1.05 → -0.10) Mistake. Best move was Bxe4. } (13. Bxe4 dxe4 14. N@h5+ Kg8 15. Nf6+ Qxf6 16. exf6 N@f4+ 17. Kxf2) 13... Kg8? { (-0.10 → 2.31) Mistake. Best move was Kh8. } (13... Kh8 14. Bxe4 dxe4 15. Ng5 B@b5+ 16. Kxf2 e3+ 17. Kxe3 N@f5+) 14. Bxe4 dxe4 15. Kxf2?? { (2.14 → -1.11) Blunder. Best move was N@h6+. } (15. N@h6+ Kh8 16. Ng5 Nxd4+ 17. Bxd4) 15... N@g4+ 16. Kg3 h5? { (-1.58 → 0.42) Mistake. Best move was exf3. } (16... exf3 17. Qxf3 N@f5+ 18. Kh3 Nge3) 17. P@g7?! { (0.42 → -0.28) Inaccuracy. Best move was Ng5. } (17. Ng5 Nxf6 18. P@f4 B@g4 19. N@h6+ Kg7 20. exf6+ Kxh6) 17... B@f2+ 18. Kh3 exf3? { (-1.84 → -0.26) Mistake. Best move was Nxf6. } (18... Nxf6 19. gxf8=Q+ Kxf8 20. R@h8+ Kg7 21. exf6+ Kxh8 22. N@g6+ fxg6) 19. gxf8=Q+ Qxf8 20. R@g5+ P@g6 21. Qxf3 Ncxe5?? { (0.55 → 5.63) Blunder. Best move was Be3. } (21... Be3 22. P@f4 N@f2+ 23. Kh4 Bxd4 24. Bd3) 22. dxe5? { (5.63 → 3.18) Mistake. Best move was P@g7. } (22. P@g7 Qe8 23. dxe5 N@f5) 22... Qh6? { (3.18 → 4.26) Mistake. Best move was N@h7. } (22... N@h7 23. Rxg4 hxg4+ 24. Qxg4 Qh6+ 25. P@h4 Nxf6 26. N@e7+ Kh8) 23. N@e7+ Kh7 24. P@g7? { (4.21 → 3.04) Mistake. Best move was N@f8+. } (24. N@f8+ Qxf8 25. P@g7 Qd8 26. Rd1) 24... N@g8?? { (3.04 → Mate in 1) Checkmate is now unavoidable. Best move was Bd7. } (24... Bd7 25. N@f8+ Rxf8 26. gxf8=Q Qxf8 27. R@g8 Qxg8 28. a3 Nxe5) 25. N@f8# { Black is checkmated } 1-0
"""

  val unclosedQuote = """
[Event "NII - Lille 2 / Tour Blanche Paris]
[Site "Lille"]
[Date "2015.03.14"]
[Round "8"]
[White "Blazquez, Denis"]
[Black "Soubirou, Oriane"]
[Result "1-0"]
[ECO "B00"]
[WhiteElo "2083"]
[BlackElo "2135"]
[PlyCount "35"]

1. d4 a6 2. e4 e6 3. c4 b5 4. cxb5 axb5 5. Bxb5 Bb7 6. Nc3 Bb4 7. Qe2 Qh4 8.
Bd3 f5 9. g3 Qf6 10. Nf3 c5 11. O-O Ne7 12. Bg5 Qf8 13. Nb5 Bxe4 14. Bxe4 fxe4
15. Qxe4 Nd5 16. Ne5 cxd4 17. Qxd5 Ra7 18. Qa8 0-1
"""

  val complete960 = """[Event "Casual game"]
[Site "http://lichess.org/analyse/---qxr00"]
[Date "2010.10.30"]
[White "Anonymous"]
[Black "Crafty level 1"]
[WhiteElo "?"]
[BlackElo "?"]
[Result "0-1"]
[PlyCount "42"]
[Variant "Chess960"]
[FEN "rbkrnnbq/pppppppp/8/8/8/8/PPPPPPPP/RBKRNNBQ w KQkq - 0 1"]

1. e3 Nf6 2. Ng3 Ne6 3. Nf3 d5 4. Nd4 Nxd4 5. exd4 e6 6. Re1 Ng4 7. Re2 f6 8. c4 dxc4 9. Be4 Rxd4 10. Bf3 Ne5 11. Ne4 Nxf3 12. gxf3 Bf7 13. Nxf6 gxf6 14. Kd1 e5 15. h4 Bg6 16. Bh2 Bf5 17. Rxe5 fxe5 18. Bxe5 Qxe5 19. Qf1 Qf4 20. d3 Rxd3+ 21. Ke1 Qd2# 0-1"""

  val fromWikipedia = """[Event "F/S Return Match"]
[Site "Belgrade, Serbia Yugoslavia|JUG"]
[Date "1992.11.04"]
[Round "29"]
[White "Fischer, Robert J."]
[Black "Spassky, Boris V."]
[Result "1/2-1/2"]

1. e4 e5 2. Nf3 Nc6 3. Bb5 {This opening is called the Ruy Lopez.} 3... a6
4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 8. c3 O-O 9. h3 Nb8  10. d4 Nbd7
11. c4 c6 12. cxb5 axb5 13. Nc3 Bb7 14. Bg5 b4 15. Nb1 h6 16. Bh4 c5 17. dxe5
Nxe4 18. Bxe7 Qxe7 19. exd6 Qf6 20. Nbd2 Nxd6 21. Nc4 Nxc4 22. Bxc4 Nb6
23. Ne5 Rae8 24. Bxf7+ Rxf7 25. Nxf7 Rxe1+ 26. Qxe1 Kxf7 27. Qe3 Qg5 28. Qxg5
hxg5 29. b3 Ke6 30. a3 Kd6 31. axb4 cxb4 32. Ra5 Nd5 33. f3 Bc8 34. Kf2 Bf5
35. Ra7 g6 36. Ra6+ Kc5 37. Ke1 Nf4 38. g3 Nxh3 39. Kd2 Kb5 40. Rd6 Kc5 41. Ra6
Nf2 42. g4 Bd3 43. Re6 1/2-1/2"""

  val stLouisFischerandom = """[ePGN "0.1;DGT LiveChess/2.2.3"]
[Event "Champions Showdown 2018"]
[Site "Saint Louis, United States"]
[Date "2018.09.12"]
[Round "6.1"]
[White "Kasparov, Garry"]
[Black "Topalov, Veselin"]
[Result "*"]
[WhiteClock "00:04:08"]
[BlackClock "00:07:03"]
[ReferenceTime "B/2018-09-12T15:26:56.191-05:00"]
[FEN "rbnnqkbr/pppppppp/8/8/8/8/PPPPPPPP/RBNNQKBR w HAha - 0 1"]
[Variant "Fischerandom"]
[WhiteTitle "GM"]
[BlackTitle "GM"]
[WhiteElo "2734"]
[BlackElo "2722"]

1. d4 {[%clk 00:30:00]} {[%emt 00:00:05]} d5 {[%clk 00:29:57]} {[%emt
00:00:14]} 2. f3 {[%clk 00:30:00]} {[%emt 00:00:04]} f6 {[%clk 00:29:16]}
{[%emt 00:00:51]} 3. Nd3 {[%clk 00:29:58]} {[%emt 00:00:13]} c6 {[%clk
00:28:46]} {[%emt 00:00:40]} 4. e4 {[%clk 00:29:06]} {[%emt 00:01:02]} dxe4
{[%clk 00:28:14]} {[%emt 00:00:43]} 5. fxe4 {[%clk 00:27:57]} {[%emt 00:01:18]}
e5 {[%clk 00:28:03]} {[%emt 00:00:20]} 6. dxe5 {[%clk 00:23:33]} {[%emt
00:04:34]} Bxe5 {[%clk 00:26:44]} {[%emt 00:01:29]} 7. Bc5+ {[%clk 00:23:11]}
{[%emt 00:00:32]} Bd6 {[%clk 00:26:44]} {[%emt 00:00:03]} 8. Bxd6+ {[%clk
00:23:11]} {[%emt 00:00:03]} Nxd6 {[%clk 00:26:44]} {[%emt 00:00:01]} 9. e5
{[%clk 00:23:11]} {[%emt 00:00:01]} fxe5 {[%clk 00:25:53]} {[%emt 00:01:00]} 10.
O-O+ {[%clk 00:22:47]} {[%emt 00:00:34]} Bf7 {[%clk 00:23:55]} {[%emt
00:02:09]} 11. Nxe5 {[%clk 00:22:13]} {[%emt 00:00:44]} O-O {[%clk 00:23:52]}
{[%emt 00:00:12]} 12. c3 {[%clk 00:22:13]} {[%emt 00:00:02]} Qe7 {[%clk
00:18:39]} {[%emt 00:05:21]} 13. Nxf7 {[%clk 00:19:46]} {[%emt 00:02:38]} Rxf7
{[%clk 00:18:19]} {[%emt 00:00:30]} 14. Bc2 {[%clk 00:19:35]} {[%emt 00:00:22]}
Rxf1+ {[%clk 00:18:06]} {[%emt 00:00:22]} 15. Qxf1 {[%clk 00:19:32]} {[%emt
00:00:13]} N8f7 {[%clk 00:18:06]} {[%emt 00:00:08]} 16. Nf2 {[%clk 00:19:26]}
{[%emt 00:00:15]} Re8 {[%clk 00:18:06]} {[%emt 00:00:08]} 17. Rd1 {[%clk
00:15:06]} {[%emt 00:04:30]} Ne5 {[%clk 00:13:44]} {[%emt 00:04:33]} 18. Nd3
{[%clk 00:12:31]} {[%emt 00:02:46]} Nec4 {[%clk 00:07:44]} {[%emt 00:06:10]} 19.
Re1 {[%clk 00:12:14]} {[%emt 00:00:26]} Qf8 {[%clk 00:07:18]} {[%emt 00:00:36]}
20. Bb3 {[%clk 00:04:16]} {[%emt 00:08:08]} Qxf1+ {[%clk 00:07:18]} {[%emt
00:00:06]} 21. Kxf1 {[%clk 00:04:16]} {[%emt 00:00:02]} Kf8 {[%clk 00:07:18]}
{[%emt 00:00:02]} 22. Rxe8+ {[%clk 00:04:16]} {[%emt 00:00:03]} Kxe8 {[%clk
00:07:18]} {[%emt 00:00:01]} 23. Ke2 {[%clk 00:04:16]} {[%emt 00:00:02]} Ke7
{[%clk 00:07:18]} {[%emt 00:00:03]} 24. Bxc4 {[%clk 00:04:08]} {[%emt
00:00:18]} Nxc4 {[%clk 00:07:18]} {[%emt 00:00:02]} 25. b3 {[%clk 00:04:08]}
Nd6 {[%clk 00:07:18]} {[%emt 00:00:10]} 26. Ke3 {[%clk 00:04:08]} {[%emt
00:00:03]} *"""

  val inlineComments = """[Event "F/S Return Match"]
[Site "Belgrade, Serbia Yugoslavia|JUG"]
[Date "1992.11.04"]
[Round "29"]
[White "Fischer, Robert J."]
[Black "Spassky, Boris V."]
[Result "1/2-1/2"]

1. e4 e5 2. Nf3 Nc6 3. Bb5 {This opening is called the Ruy Lopez.} 3... a6 ; this is an inline comment
4. Ba4 Nf6 5. O-O Be7 6. Re1 b5 7. Bb3 d6 8. c3 O-O 9. h3 Nb8  10. d4 Nbd7
11. c4 c6 12. cxb5 axb5 13. Nc3 Bb7 14. Bg5 b4 15. Nb1 h6 16. Bh4 c5 17. dxe5 ; Openning route to ocupying b6 weak square by Na4-Nb6. This square seemed more important than f5 (Ne2-Ng3-Nf5) because its nearer the black's king.
Nxe4 18. Bxe7 Qxe7 19. exd6 Qf6 20. Nbd2 Nxd6 21. Nc4 Nxc4 22. Bxc4 Nb6
23. Ne5 Rae8 24. Bxf7+ Rxf7 25. Nxf7 Rxe1+ 26. Qxe1 Kxf7 27. Qe3 Qg5 28. Qxg5
hxg5 29. b3 Ke6 30. a3 Kd6 31. axb4 cxb4 32. Ra5 Nd5 33. f3 Bc8 34. Kf2 Bf5
35. Ra7 g6 36. Ra6+ Kc5 37. Ke1 Nf4 38. g3 Nxh3 39. Kd2 Kb5 40. Rd6 Kc5 41. Ra6
Nf2 42. g4 Bd3 43. Re6 1/2-1/2"""

val fromChessgames = """[Event "The Match - Braingames World Chess Cham"]
[Site "London"]
[Date "2000.01.04"]
[EventDate "2000.10.12"]
[Round "3"]
[Result "1/2-1/2"]
[White "Garry Kasparov"]
[Black "Vladimir Kramnik"]
[ECO "C67"]
[WhiteElo "2849"]
[BlackElo "2770"]
[PlyCount "106"]

1. e4 e5 2. Nf3 Nc6 3. Bb5 Nf6 4. O-O Nxe4 5. d4 Nd6 6. Bxc6
dxc6 7. dxe5 Nf5 8. Qxd8+ Kxd8 9. Nc3 Bd7 10. b3 h6 11. Bb2
Kc8 12. Rad1 b6 13. Ne2 c5 14. c4 Bc6 15. Nf4 Kb7 16. Nd5 Ne7
17. Rfe1 Rg8 18. Nf4 g5 19. Nh5 Rg6 20. Nf6 Bg7 21. Rd3 Bxf3
22. Rxf3 Bxf6 23. exf6 Nc6 24. Rd3 Rf8 25. Re4 Kc8 26. f4 gxf4
27. Rxf4 Re8 28. Bc3 Re2 29. Rf2 Re4 30. Rh3 a5 31. Rh5 a4
32. bxa4 Rxc4 33. Bd2 Rxa4 34. Rxh6 Rg8 35. Rh7 Rxa2 36. Rxf7
Ne5 37. Rg7 Rf8 38. h3 c4 39. Re7 Nd3 40. f7 Nxf2 41. Re8+ Kd7
42. Rxf8 Ke7 43. Rc8 Kxf7 44. Rxc7+ Ke6 45. Be3 Nd1 46. Bxb6
c3 47. h4 Ra6 48. Bd4 Ra4 49. Bxc3 Nxc3 50. Rxc3 Rxh4 51. Rf3
Rh5 52. Kf2 Rg5 53. Rf8 Ke5 1/2-1/2"""

val fromChessgamesWithEscapeChar = """%youpi
[Event "The Match - Braingames World Chess Cham"]
[Site "London"]
[Date "2000.01.04"]
[EventDate "2000.10.12"]
[Round "3"]
[Result "1/2-1/2"]
[White "Garry Kasparov"]
[Black "Vladimir Kramnik"]
[ECO "C67"]
[WhiteElo "2849"]
[BlackElo "2770"]
[PlyCount "106"]

1. e4 e5 2. Nf3 Nc6 3. Bb5 Nf6 4. O-O Nxe4 5. d4 Nd6 6. Bxc6
% tralala
dxc6 7. dxe5 Nf5 8. Qxd8+ Kxd8 9. Nc3 Bd7 10. b3 h6 11. Bb2
Kc8 12. Rad1 b6 13. Ne2 c5 14. c4 Bc6 15. Nf4 Kb7 16. Nd5 Ne7
17. Rfe1 Rg8 18. Nf4 g5 19. Nh5 Rg6 20. Nf6 Bg7 21. Rd3 Bxf3
22. Rxf3 Bxf6 23. exf6 Nc6 24. Rd3 Rf8 25. Re4 Kc8 26. f4 gxf4
27. Rxf4 Re8 28. Bc3 Re2 29. Rf2 Re4 30. Rh3 a5 31. Rh5 a4
32. bxa4 Rxc4 33. Bd2 Rxa4 34. Rxh6 Rg8 35. Rh7 Rxa2 36. Rxf7
Ne5 37. Rg7 Rf8 38. h3 c4 39. Re7 Nd3 40. f7 Nxf2 41. Re8+ Kd7
42. Rxf8 Ke7 43. Rc8 Kxf7 44. Rxc7+ Ke6 45. Be3 Nd1 46. Bxb6
c3 47. h4 Ra6 48. Bd4 Ra4 49. Bxc3 Nxc3 50. Rxc3 Rxh4 51. Rf3
Rh5 52. Kf2 Rg5 53. Rf8 Ke5 1/2-1/2"""

val chessgamesWeirdComments = """[Event "Hastings"]
[Site "Hastings ENG"]
[Date "1895.08.05"]
[EventDate "1895.08.05"]
[Round "1"]
[Result "1/2-1/2"]
[White "Carl Schlechter"]
[Black "William Henry Krause Pollock"]
[ECO "C77"]
[WhiteElo "?"]
[BlackElo "?"]
[PlyCount "47"]

1. e4 {Notes by E. Schiffers} e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4
Nf6 5. Nc3 Bb4 {The 'Handbuch' considers that Black's best
move here is Be7; White continues 6 O-O b5 7 Bb3 d6 8 a3 0r
a4.} 6. Nd5 Bc5 {After Be7 would follow 7 d3 h6 and an even
game. It would not be good to play ...Nxd5 7 exd5 Ne7 8 c3 Ba5
(?) 9 Nxe5.} 7. d3 {The 'Handbuch' gives the continuation 7 c3
Nxe4 8 d4 exd4 9 cxd4 Bb4+ 10 Kf1 in White's favour.} h6
8. Be3 Bxe3 {Also good would have been Nxe3.} 9. fxe3 d6
10. O-O Be6 {...Bg4 is better.} 11. Nxf6+ {An enterprising
move} Qxf6 12. Nd4 Qg5 13. Nxc6 {After 13 Nxe6 fxe6 (...Qxe3+
14 Kh1 fxe6 15 Qh5+) 14 Bxc6+ bxc6 15 Qf3; White's game is
preferable.} Qxe3+ 14. Kh1 Bd7 15. Nxe5 {And other moves still
do not give White the superiority, e.g. 15 Nb4 Bxa4 16 Nd5
Qc5, etc.} Bxa4 16. Nxf7 O-O {!} $14 17. Qh5 Be8 18. Rf3 Bxf7
19. Qxf7+ Rxf7 20. Rxe3 Rf2 21. Rae1 {21 Rc1 Raf8 followed by
...Rd2 and the other rook to f2 would have been worse as White
would then get into difficulties, whereas now the draw is
secure.} Raf8 22. Kg1 Rxc2 23. R3e2 Rxe2 24. Rxe2 1/2-1/2
"""

val withDelimiters = """[pgn] 1. e4 e6 2. d4 d5 3. Nc3 Nf6 4. e5 Nfd7 5. Nf3 a6 6. Bd3 f6 7. O-O (7. Ng5 {is also good, but I was still in \"bullet opening mode\" at this point}) 7. .. fxe5 8. Ng5 Qe7 9. Qh5+ g6 10. Bxg6+ hxg6 11. Qxg6+ Kd8 12. Nf7+ Ke8 13. Nxh8+ Kd8 14. Bg5 Nc6 15. Nf7+ Ke8 16. Nd6+ Kd8 17. Qe8# [/pgn]"""

val withDelimitersOnNewLines = """
[pgn]
1. e4 e6 2. d4 d5 3. Nc3 Nf6 4. e5 Nfd7 5. Nf3 a6 6. Bd3 f6 7. O-O (7. Ng5 {is also good, but I was still in \"bullet opening mode\" at this point}) 7. .. fxe5 8. Ng5 Qe7 9. Qh5+ g6 10. Bxg6+ hxg6 11. Qxg6+ Kd8 12. Nf7+ Ke8 13. Nxh8+ Kd8 14. Bg5 Nc6 15. Nf7+ Ke8 16. Nd6+ Kd8 17. Qe8#
[/pgn]"""

val fromCrafty = """[Event "Live Chess"]
[Site "Chess.com"]
[Date "2014.01.15"]
[Round "?"]
[White "amarkaur"]
[WhiteElo "1116"]
[Black "ludocode"]
[BlackElo "1220"]
[Result "0-1"]
[Annotator "Crafty v23.4"]
{annotating both black and white moves.}
{using a scoring margin of +0.50 pawns.}
{search time limit is 1.00}

  1.     Nf3      d5
  2.      d4     Nc6
  3.      e3     Nf6
  4.      c4    dxc4
  5.    Bxc4     Qd6
                ({15:+0.88}  5. ... Qd6 6. Nc3 Qb4 7. Nd2 e6 8. a3 Qa5 9. O-O Bd6 10. Nde4 Nxe4 11. Nxe4 O-O 12. Bd2 Qf5 13. Nxd6 cxd6 $16)
                ({15:+0.36}  5. ... e6 6. O-O Bd6 7. Nc3 O-O 8. e4 e5 9. d5 Na5 10. Bd3 Bd7 11. Bg5 Re8 12. Be3 Qe7 $14)
  6.     Nc3      e5
  7.     O-O    exd4
  8.    Nxd4
                ({13:+1.22}  8. Nxd4 Ne5 9. Be2 Neg4 10. f4 c5 11. Ndb5 Qe7 12. e4 c4 13. Qa4 Nxe4 14. Nxe4 Qxe4 15. Bxc4 $16)
                ({13:+2.05}  8. exd4 Bg4 9. Nb5 Qd7 10. Bf4 O-O-O 11. Bxc7 Bxf3 12. gxf3 Re8 13. d5 Nb4 14. Rc1 $18)

  8.     ...    Nxd4
  9.    exd4     Be6
                ({13:+1.32}  9. ... Be6 10. d5 Bd7 11. Re1+ Be7 12. Nb5 Bxb5 13. Bxb5+ Kf8 14. Bc4 Rd8 15. Bg5 Qc5 16. Bxf6 Bxf6 $16)
                ({13:+0.70}  9. ... Bd7 10. Re1+ Be7 11. Nb5 Bxb5 12. Bxb5+ c6 13. Bc4 O-O 14. Qb3 b5 15. Bd3 Rfe8 16. Be3 Nd5 $14)
 10.     Re1
                ({13:+0.52}  10. Re1 O-O-O 11. Bxe6+ fxe6 12. Bg5 Rd7 13. Bxf6 gxf6 14. Qb3 Qxd4 15. Rad1 Qb6 16. Qxb6 axb6 17. Rxe6 Rxd1+ 18. Nxd1 $14)
                ({13:+1.32}  10. d5 Bd7 11. Nb5 Qb6 12. Re1+ Kd8 13. Qd3 Bc5 14. Be3 Bxe3 15. Rxe3 Re8 16. Ree1 Kc8 17. Rxe8+ Bxe8 $16)

 10.     ...   O-O-O
 11.     Be2
                ({15:-0.76}  11. Be2 Qxd4 12. Qxd4 Rxd4 13. Be3 Rb4 14. Rab1 Bf5 15. a3 Bxb1 16. axb4 Bf5 17. b5 Kb8 18. Rd1 $17)
                ({15:+0.49}  11. Bxe6+ fxe6 12. Bg5 Qxd4 13. Qxd4 Rxd4 14. Rxe6 Rd6 15. Re5 h6 16. Bxf6 Rxf6 17. Re8+ Kd7 18. Rae1 Rd6 $14)

 11.     ...    Qxd4
 12.     Be3     Qb4
 13.     Qc1     Bd6
 14.      a3     Qh4
 15.      g3     Qh3
 16.     Bf3
                ({14:-2.01}  16. Bf3 Ng4 17. Bxg4 Bxg4 18. f4 a6 19. Qc2 Rhe8 20. Ne4 Bf5 21. Nxd6+ Rxd6 22. Qg2 Qg4 $19)
                ({14:+0.01}  16. Bxa7 Bd7 17. Bf1 Qf5 18. Qd1 Bc6 19. Bd3 Qh3 20. Bf1 Qf5 $10)

 16.     ...     Ng4
 17.    Bxg4    Bxg4
 18.      f3
                ({15:-5.02}  18. f3 Bxf3 19. Re2 Bxe2 20. Nxe2 Rhe8 21. Nf4 Qg4 22. Ng2 b6 23. Qc2 Be5 24. Rc1 Rd5 25. Qc6 $19)
                ({15:-1.82}  18. f4 f5 19. Nb5 a6 20. Nxd6+ Rxd6 21. Qc2 Bf3 22. Rac1 Bc6 23. Bc5 Rh6 24. Re7 Be4 25. Qd2 $19)

 18.     ...    Bxg3
 19.    hxg3
                ({14:-Mat06}  19. hxg3 Qxg3+ 20. Kf1 Qxf3+ 21. Kg1 Qg3+ 22. Kf1 Bh3+ 23. Ke2 Qg4+ 24. Kf2 Qg2# $19)
                ({14:-6.51}  19. Re2 Bxf3 20. hxg3 Qxg3+ 21. Kf1 Bg4 22. Rd2 Rxd2 23. Qxd2 Bh3+ 24. Ke2 Qg2+ 25. Kd1 Rd8 26. Bd4 Qf1+ 27. Qe1 Rxd4+ 28. Kc2 Bf5+ 29. Kb3 $19)

 19.     ...   Qxg3+
 20.     Kf1    Bh3+
                ({9:-9.33}  20. ... Bh3+ 21. Ke2 Qh2+ 22. Bf2 Rhe8+ 23. Qe3 Rxe3+ 24. Kxe3 Qe5+ 25. Ne4 f5 26. Bh4 f4+ 27. Kf2 Qd4+ 28. Ke2 Qxb2+ $19)
                ({9:-Mat05}  20. ... Qxf3+ 21. Kg1 Qg3+ 22. Kf1 Bh3+ 23. Ke2 Qg4+ 24. Kf2 Qg2# $19)
 21.     Ke2    Qg2+
 22.     Bf2   Rhe8+
 23.     Ne4     Bg4
                ({13:-9.33}  23. ... Bg4 24. Qf4 Bxf3+ 25. Qxf3 Rxe4+ 26. Qxe4 Qxe4+ 27. Kf1 Qh1+ 28. Ke2 Qh5+ 29. Kf1 Rd2 30. Kg1 Qg4+ 31. Kf1 Rxb2 32. Re7 $19)
                ({13:-Mat07}  23. ... Rxe4+ 24. Qe3 Rxe3+ 25. Kxe3 Qg5+ 26. f4 Qc5+ 27. Ke2 Qh5+ 28. Ke3 Bg2 29. Bh4 Qf3# $19)
 24.     Qe3      f5
                ({15:-4.06}  24. ... f5 25. Rg1 Bxf3+ 26. Ke1 Bxe4 27. Rxg2 Bxg2 28. Rd1 Rxe3+ 29. Bxe3 Rxd1+ 30. Kxd1 g6 31. Kd2 a5 $19)
                ({15:-11.88}  24. ... Bxf3+ 25. Qxf3 Rxe4+ 26. Qxe4 Qxe4+ 27. Kf1 Qh1+ 28. Ke2 Qh5+ 29. Kf1 Qh3+ 30. Kg1 Rd6 31. Re8+ Kd7 32. Re3 Rg6+ 33. Bg3 Rxg3+ 34. Rxg3 Qxg3+ 35. Kh1 Qf3+ 36. Kg1 Qe3+ 37. Kh1 Qe4+ 38. Kh2 $19)
 25.     Rg1   Bxf3+
 26.    Qxf3
                ({15:-18.62}  26. Qxf3 Rxe4+ 27. Qxe4 Qxe4+ 28. Kf1 Qd3+ 29. Kg2 Rd6 30. Rge1 Rg6+ 31. Kh2 Qf3 32. Re8+ Kd7 33. Re7+ Kxe7 34. Re1+ Kf7 35. Re7+ Kxe7 36. Bc5+ Ke6 37. Bxa7 $19)
                ({15:-4.05}  26. Ke1 Bxe4 27. Rxg2 Bxg2 28. Qxe8 Rxe8+ 29. Kd2 g6 30. Rg1 Be4 31. Be3 b6 32. Bf4 Rd8+ 33. Ke2 c5 34. Rg5 $19)

 26.     ...   Rxe4+
 27.    Qxe4   Qxe4+
 28.     Kf1    Qd3+
 29.     Kg2     Qb3
                ({13:-9.38}  29. ... Qb3 30. Rge1 Rd2 31. Re8+ Kd7 32. Rae1 Qxb2 33. R8e7+ Kd6 34. R1e6+ Kd5 35. Re5+ Qxe5 36. Rd7+ Qd6 37. Rxd6+ Kxd6 $19)
                ({13:-22.79}  29. ... Rd6 30. Rge1 Rg6+ 31. Kh2 Qf3 32. Re8+ Kd7 33. Rd8+ Kxd8 34. Rd1+ Qxd1 35. Bh4+ Kd7 36. Bg3 Qe2+ 37. Kh3 Rh6+ 38. Bh4 Qe3+ 39. Kg2 Rxh4 $19)
 30.    Rgc1
                ({12:-9.97}  30. Rgc1 Rd2 31. Rf1 Rxb2 32. Kg1 Qf3 33. Rae1 Qg4+ 34. Kh2 c5 35. Re3 Kc7 36. Kh1 Qh5+ 37. Kg2 $19)
                ({12:-9.38}  30. Rae1 Rd2 31. Re8+ Kd7 32. Rge1 Qxb2 33. R8e7+ Kd6 34. R1e6+ Kd5 35. Re5+ Qxe5 36. Rd7+ Qd6 37. Rxd6+ Kxd6 $19)

 30.     ...    Qxb2
                ({13:-9.47}  30. ... Qxb2 31. Rd1 Rxd1 32. Rxd1 Qxa3 33. Bh4 Qa2+ 34. Kg1 c5 35. Re1 Qa4 36. Bg3 Qd4+ 37. Kg2 Kd7 $19)
                ({13:-12.77}  30. ... Rd2 31. Rh1 Qc2 32. Rhf1 Qe4+ 33. Kh2 Qh4+ 34. Kg2 Qg4+ 35. Kh1 Qh3+ 36. Kg1 Rd6 37. Bh4 Rg6+ 38. Kf2 Rg2+ 39. Ke1 Qxh4+ 40. Kd1 $19)
 31.     Kg1
                ({14:-15.11}  31. Kg1 Rd6 32. Re1 Rg6+ 33. Kf1 Qb5+ 34. Re2 Qd5 35. Re8+ Kd7 36. Re3 Qh1+ 37. Ke2 Qxa1 38. Rd3+ Kc6 39. Bxa7 Rg2+ 40. Bf2 Qa2+ 41. Rd2 Qxa3 $19)
                ({14:-9.45}  31. Rd1 Rxd1 32. Rxd1 Qxa3 33. Rh1 Qa6 34. Rh4 c5 35. Kf3 Qd3+ 36. Be3 Qd5+ 37. Kg3 h6 38. Ra4 Qb3 $19)

 31.     ...     Rd2
                ({13:-10.53}  31. ... Rd2 32. Rcb1 Qc2 33. Rf1 Qa4 34. Rae1 Qg4+ 35. Kh2 f4 36. Kh1 Kd7 37. a4 Qh5+ 38. Kg2 Qd5+ 39. Kh2 Qh5+ 40. Kg2 $19)
                ({13:-15.11}  31. ... Rd6 32. Re1 Rg6+ 33. Kf1 Qb5+ 34. Re2 Qd5 35. Re8+ Kd7 36. Re3 Qh1+ 37. Ke2 Qxa1 38. Rd3+ Kc6 39. Bxa7 Rg2+ 40. Bf2 Qa2+ 41. Rd2 Qxa3 $19)
 32.    Rab1     Qf6
                ({15:-12.64}  32. ... Qf6 33. Rb3 Qg5+ 34. Rg3 Qf4 35. Rg2 Rxf2 36. Rxc7+ Kxc7 37. Rxf2 Qg3+ 38. Rg2 Qe1+ 39. Kh2 g5 40. Rg3 Qe2+ 41. Rg2 Qe5+ 42. Rg3 $19)
                ({15:-13.99}  32. ... Qxa3 33. Rd1 Ra2 34. Rd4 Rxf2 35. Kxf2 Qa2+ 36. Ke3 Qxb1 37. Kd2 Qb2+ 38. Kd3 Qb3+ 39. Kd2 g5 40. Rd3 Qb2+ 41. Ke3 g4 $19)
 33.     Bg3
                ({14:-Mat04}  33. Bg3 Qd4+ 34. Kf1 Qd3+ 35. Kg1 Qxg3+ 36. Kh1 Rh2# $19)
                ({14:-12.64}  33. Rb3 Qg5+ 34. Rg3 Qf4 35. Rg2 Rxf2 36. Rxc7+ Kxc7 37. Rxf2 Qg3+ 38. Rg2 Qe1+ 39. Kh2 g5 40. Rg3 Qe2+ 41. Rg2 Qe5+ 42. Rg3 $19)

 33.     ...     Qg5
                ({7:-10.72}  33. ... Qg5 34. Rxc7+ Kd8 35. Rc3 f4 36. Rxb7 fxg3 37. Rxa7 $19)
                ({7:-Mat04}  33. ... Qd4+ 34. Kf1 Qd3+ 35. Kg1 Qxg3+ 36. Kh1 Rh2# $19)
 34.   Rxc7+     Kd8

       0-1"""

val disambiguated = "Be5 g6 Ne7g6+"

val fromProd1 = """d4 Nf6 c4 e6 Nc3 Bb4 Bd2 O-O Nf3 c6 e4 Qa5 Be2 Kh8 O-O h5 Ne5 h4 h3 Be7 Nd5 Qd8 Nxe7 d6 Ne7g6+"""
val fromProd2 = """e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5 Nf5 d5 g4 Bxf5 exf5 d4 Ne2 Nxg4 Bh3 Qd5 Ng3 Bb4+ Bd2 Bxd2+ Qxd2 Qf3 Qe2 Qxe2+ Kxe2 Nh6 Bg2 Kf8 Rhe1 Ke7 Kd3 Nb4+ Kd2 Rhc8 c3 dxc3+ bxc3 Rd8+ Kc1 Nd3+ Kc2 Nxe1+ Rxe1 Kf6 Be4 Rdb8 Rd1 Kg5 Rd7 a6 Bxb7 Ra7 Bc6 Rxd7 Bxd7 Rd8 Bc6 Nxf5 Nxf5 Kxf5 f3 Rb8 Be4+ Kf4 c4 Rh8 c5 a5 c6 f5 Bd5 Rc8 Kc3 g6 Kc4 h5 Kb5 Rb8+ Ka6 a4 c7 Rc8 Kb7 Rh8 c8=Q Rxc8 Kxc8 g5 Kd7 g4 fxg4 hxg4 Ke6 e4 Bc4 Kg5 Ke5 e3 Bd3 f4 Ke4 g3 Kf3 gxh2 Kg2 f3+ Kxh2 e2 Bxe2 fxe2 Kh3 e1=Q Kg2 Qe6"""

val promoteRook = """d4 Nf6 f3 d5 e3 e5 Ne2 Bd6 g3 a5 Bg2 Bf5 e4 Bb4+ Nbc3 Bxc3+ Nxc3 exd4 Qxd4 Be6 O-O Nc6 Qf2 Qd6 Bf4 Ne5 Bxe5 Qxe5 Rfd1 Ra6 Rd2 c6 Rad1 Bd7 exd5 b5 d6 Rg8 f4 Qe6 Qc5 Kf8 Bf3 Ke8 Kg2 Kf8 Re2 Qc4 Qxc4 bxc4 h3 Be8 Bg4 h5 Bf5 c5 Red2 g6 Be4 Rb6 Kf3 Kg7 g4 hxg4+ hxg4 Ra6 f5 Ra7 Nd5 g5 Nxf6 Kxf6 b3 Bb5 bxc4 Bxc4 d7 Rxd7 Rxd7 Bxa2 Rd8 Rxd8 Rxd8 a4 Ra8 Ke7 Rxa4 Bb1 c4 Bxe4+ Kxe4 Kd6 f6 Ke6 Ra6+ Kd7 Kf5 Kc7 Kxg5 Kb8 Kh6 Kc7 g5 Kb8 g6 fxg6 Kxg6 Kc7 f7 Kb7 Rd6 Ka7 f8=R Kb7 Rf7+"""

val castleCheck1 = """e3 d5 Be2 Nc6 Nf3 e5 d4 e4 Ne5 Nxe5 dxe5 Qg5 Kd2 d4 g3 dxe3+ fxe3 Bh3 Bh5 O-O-O+"""
val castleCheck2 = """d4 Nf6 c4 e6 Nc3 Bb4 Bd2 O-O#"""

def prod500standard = List(
"""e4 c5 Nc3 d6 f4 e6 Nf3 d5 e5 d4 Ne2 Ne7 d3 Nd5 Ng3 Be7 Be2 Bh4 O-O Bxg3 hxg3 O-O Ng5 f6 Ne4 fxe5 fxe5 Rxf1+ Bxf1 Qc7 Qf3 Qxe5 Bf4 Nxf4 gxf4 Qc7 Re1 Nd7 Ng5"""
,"""d4 e6 c4 Nf6 Nc3 Bb4 Nf3 Bxc3+ bxc3 b6 e4 Bb7 e5 Ne4 Qd3 f5 exf6 Nxf6 Bg5 O-O Be2 d6"""
,"""d4 d5 Nf3 Nf6 c4 c6 Nc3 dxc4 g3 e6 Bg2 a6 O-O b5 a3 Bb7 e4 Nbd7 e5 Nd5 Ne4 Qc7 Bg5 Be7 Bxe7 Kxe7 Nd6 Rhd8 Ng5 f6 Nxh7 Rh8 Qh5 Raf8 exf6+"""
,"""e4 d6 d4 Bg4 Qxg4 Nf6 Qf3 e5 dxe5 dxe5 Nc3 Bb4 Bg5 Bxc3+ Qxc3 Nc6 Rd1 Qe7 Nf3 O-O Bc4 Rad8 O-O h6 Bxf6 Qxf6 Bd5 Nd4 Nxd4 exd4 Rxd4"""
,"""e4 c5 Nf3 d6 Nc3 Nf6 h3 e6 a3 Be7 Bc4 O-O d3 a6 Bg5 b6 O-O Bb7 Ba2 Nbd7"""
,"""d4 e6 h3 c6 Bf4 d5 Nf3 g6 e3 Bg7 Nbd2 Ne7 c3 Nd7 Bd3 O-O O-O f6 Rc1 e5 dxe5 fxe5"""
,"""d3 Nf6 g3 d5 Bg2 e6 Nf3 Be7 e3 c5 Qe2 Nc6 Bd2 Bd7 Nc3 Rb8 e4 d4 Nb5 a6 Na3 e5 Nc4 Qc7 Ng5 b5 Na3 h6 Nh3 Ng4 f3 Nf6 g4 g5 Qf2 Nd8 Qg3 Ne6"""
,"""e4 c6 d4 Qc7 c4 d6 Nf3 e5 d5 Nf6 Nc3 Bg4 Be2 Nbd7 Be3 Be7 O-O h5 h3 Bxf3 Bxf3 g6 Bg5 Qd8 Qd2 Nxd5"""
,"""e4 e6 Nc3 d5 exd5 exd5 d4 Nf6 Be3 c5 Nf3 cxd4 Bxd4 Nc6 Bb5 Bd7 O-O a6 Bxc6 Bxc6 Re1+ Be7 Ne5 Qd6 Ne2 h6 c3 O-O Qb3 Rfe8 Rad1 Nd7 Nd3 Bf6 Ng3 Bxd4 cxd4 Rxe1+ Rxe1 Nf6 Qd1 Re8 Qd2 Rxe1+ Nxe1 Qd7 Nd3 Bb5 Ne5 Qd6 h3 Bc4 Nf5 Qe6 Qf4 Ne4 g4 g6 Nxh6+ Kg7 Nhxf7 Qxf7 Nxf7 Nxf2 Kxf2"""
,"""e4 c5 Nf3 e6 c3 a6 d4 h5 d5 d6 dxe6 Bxe6 e5 Nc6 exd6 Bxd6 Bg5 Qc7 Nbd2 f6 Be3 O-O-O Qa4 Nge7 O-O-O Nf5"""
,"""Nf3 Nf6 d4 d5 c4 Bf5 cxd5 Nxd5 Nc3 Nxc3 bxc3 e6 Qb3 b6 Bf4 Bd6 Bg3 O-O e3 c5 Be2 cxd4 cxd4 Nc6 O-O Na5 Qa4"""
,"""b3 a5 Bb2 Nc6 g3 h6 a3 Nf6 c3 e6 f3 d5 Kf2 a4 e3 axb3 Qxb3 Bd6 d3 O-O Be2 Re8 Nd2 Na5 Qc2 c5 h3 c4 d4 Nc6 f4 Ne4+ Nxe4 dxe4 Qxe4 Be7 Bxc4 Qc7 Bd3 f5 Qf3 Bf6 g4 fxg4 Qxg4 Ne7 h4 Nf5 Nf3 Qc6 e4 Ne7 Rhg1"""
,"""e4 b6 d4 Bb7 Nc3 g6 Nf3 Bg7 Bd3 c6 Be3 d6 Qd2 Na6 O-O-O Nc7 h4 Qd7 h5 O-O-O hxg6 hxg6 Ng5 e6 Qe2 f6 Nf3 Kb8 Rxh8 Bxh8 Rh1 Ne7 Rh7"""
,"""e4 e6 Nf3 d5 exd5 exd5 d4 Nf6 Bd3 Bd6 Bg5 h6 Bh4 g5 Bg3 Bg4 Be2 Nbd7 Nc3 Qe7 Qd2 O-O-O O-O-O Bb4 a3 Bd6 Bxd6 Qxd6"""
,"""d4 d5 c4 e6 Nc3 Nf6 Nf3 h6 g3 c6 Bg2 Bd6 O-O Nbd7 b3 O-O Bb2 b6 e4 Nxe4 Nxe4 dxe4 Nd2 f5 Qe2 Bb7 f3 e3 Qxe3"""
,"""e4 e6 d3 d5 e5 c5 Nf3 Nc6 Bf4 Nge7 Be2 Ng6 Bg3 Qc7 d4 cxd4 Nxd4 Nxd4 Qxd4 Bc5 Qg4"""
,"""b3 d5 Bb2 c6 g3 Nf6 Bg2 e5 a4 e4 Bxf6 Qxf6 Na3 Bxa3 Rxa3 Qd6 Ra2 Be6 Qc1 Nd7 Qa3 Qxa3 Rxa3 Nc5 d3 Na6 dxe4 dxe4 Bxe4 Nb4 Nf3 f5 Bd3 O-O-O O-O Nxd3 cxd3 g6 Ng5 Bd7 Nf7 Rdf8 Ne5 Be6 Nc4 Rd8 Ne5 Rhe8 Nc4 Bxc4 bxc4 Rxe2 a5 Rd2 a6 R8xd3 Rxd3 Rxd3 axb7+ Kxb7 Rb1+ Kc7 Ra1 Rc3 Rxa7+ Kd6 Rxh7 Rxc4 Rh6 Rc1+ Kg2 Ke5 Rxg6 c5 Rc6 c4 Rc5+ Kd4 Rxf5 c3 Rf4+ Kd3 Rf3+ Ke2"""
,"""d4 g6 Nf3 Bg7 c4 d6 Nc3 b6 e4 Bb7 Bd3 Nc6 Bc2 Nh6 O-O Ng4 h3 Nf6 Qe2 O-O Rd1 Nb4 Bb1 a5"""
,"""c4 g6 d4 Bg7 Nc3 b6 e4 Bb7 f3 e6 Be3 Ne7 Qd2 O-O Bd3 f5 e5 d6 exd6 cxd6 Nge2 e5 d5 Nd7 O-O f4 Bf2 Nf5"""
,"""e4 d6 d4 Nf6 Nc3 e5 dxe5 dxe5 Qxd8+ Kxd8 Bg5 Be6 Bxf6+ gxf6 O-O-O+ Bd6 Nb5 Ke7 Nxd6 cxd6 Nf3 Nd7 Bb5 Nc5 b4 Nxe4 Rhf1 Nc3 Bd3"""
,"""d4 Nf6 c4 g6 Nc3 Bg7 e4 O-O Nf3 d6 Be2 c6 O-O e5 dxe5 dxe5 Qxd8 Rxd8 Bg5 Be6 Nxe5 Na6 Nf3 h6 Bh4 g5 Bg3 Nc5 e5 Nfe4"""
,"""c4 e5 Nc3 Bb4 a3 Bxc3 dxc3 d6 e4 Nf6 Nf3 Nxe4 Bd3 Bf5 O-O c6 Re1 d5 c5 Nd7 Nxe5 Nxe5 Bf4 Qf6"""
,"""c4 c5 b3 Nc6 Bb2 d6 g3 Nf6 Bg2 g6 Nf3 Bg7 O-O O-O e3 Nb4 d4 Bd7 dxc5 dxc5 a3 Nc6 Nc3 Qa5 Nd5 Nxd5 Bxg7 Nxe3 fxe3 Kxg7"""
,"""e4 e5 Qf3 Nc6 Bc4 Nf6 d3 Bc5 Qg3 Rg8 Bh6 g6 Nf3 d5 exd5 Nxd5 Bg5 f6 Bh4 Nd4 Nxd4 exd4 O-O Be6 Qf3 Qd6 Bxd5 Bxd5 Qe2+ Be6 Bxf6"""
,"""d4 Nf6 c4 e5 e3 exd4 exd4 Bb4+ Nc3 O-O Bd2 Re8+ Be2 Bxc3 Bxc3 Ne4 Nf3 Nxc3 bxc3 d6 O-O Qf6 Qd3"""
,"""d4 b6 e3 Bb7 c4 e6 Be2 d6 Nf3 Nd7 Nc3 h6 O-O Be7 e4 c6 b4 g5 a4 g4 Nd2 f5 exf5 exf5 d5 Ndf6 dxc6 Bxc6 b5 Bb7 Nb3 Qc7 Nd5 Nxd5 cxd5 Nf6 Bf4 h5 Rc1 Qd7 Bc4 h4 Re1 Rc8 Bg5 Kf7 Nd4"""
,"""e4 c6 Nc3 d5 d3 d4 Nce2 c5 f4 Nc6 Nf3 Bg4 h3 Bxf3 gxf3 e5 Kf2 Qh4+ Kg2 exf4 Bxf4 Nf6 Bg3 Qh6 Qc1 Qg6 Kh2 Nh5 Bf2 Bd6+ f4 O-O-O Rg1 Qf6 e5 Nxe5 fxe5 Bxe5+ Kh1 Qxf2 Rg2 Qf3 Ng1 Ng3+ Kh2 Nxf1+ Kh1 Ne3 Nxf3 Nxg2 Kxg2 Bc7 Qg5 h6 Qxc5 Kb8 Re1 Rd6 Re7 Rc6 Qxc6 bxc6 Nxd4 g5 Nxc6+ Kb7 Ne5 Rf8 Nd7 Rd8"""
,"""d4 Nf6 Nc3 e6 e4 Bb4 Bd3 d5 Be3 dxe4 Bb5+ c6 Bc4 b5 Bb3 a5 Bd2 a4 a3 Bxc3"""
,"""d4 d5 c4 e6 Nc3 Nf6 cxd5 Nxd5 e4 Nxc3 bxc3 c5 d5 Bd6 Nf3 O-O c4 exd5 exd5 Bg4 Be2 Re8"""
,"""e4 e5 Nf3 Nc6 Bb5 Nd4 Nxd4 exd4 c3 c6 Ba4 b5 Bc2 Bc5 O-O Bb7 Qg4 g6 cxd4 Nf6 Qf3 h5 dxc5 Qc7"""
,"""e4 g6 d4 Bg7 c3 e6 Bd3 Ne7 f3 O-O Ne2 d5 O-O b6 exd5 Nxd5 Nd2 Bb7 Qc2 Ne3 Qd1 Nxd1"""
,"""d4 d5 c4 c6 Nf3 dxc4 g3 b5 Bg2 Bb7 O-O Qc7 a4 a6 Bf4 Qc8 axb5 cxb5 b3 cxb3 Qxb3 Nf6 Ne5 Bxg2 Qxf7+ Kd8 Kxg2 Qb7+ f3 Nbd7 Rd1 Qd5 Qxd5 Nxd5"""
,"""d4 g6 e4 Bg7 Be3 b6 Bd3 Bb7 f4 Nf6 Nd2 O-O Ngf3 Ng4 Bg1 c5 c3 cxd4 cxd4 Nc6 h3 Nf6"""
,"""e4 Nf6 e5 Nd5 d4 e6 c4 Nb6 c5 Nd5 Nc3 Nxc3 bxc3 d5 exd6 cxd6 Bb5+ Bd7 Bxd7+ Nxd7 Be3 d5 Bf4 Nxc5 dxc5 Bxc5 Nf3 O-O O-O Re8 Re1 Qf6 Be5 Qf4 Bxf4"""
,"""e4 e6 Nc3 d5 exd5 exd5 d4 Nf6 Be2 c5 Nf3 c4 Ne5 Nc6 f4 Nxe5 fxe5 Nd7 Bf3 f6 e6 Nb6 Qe2 Qe7 Bg4 g5 O-O Bg7 Bf5"""
,"""e4 b6 d3 Nc6 Nc3 Bb7 Bd2 e5 Ke2 d6 Nf3 Be7 Nd5 Nf6 Ke1 Nxd5 Be2 Nf6 Bg5 O-O Bxf6 Bxf6 Qd2 Ne7 h4 g6 h5 Bg7 h6 Bh8 g4 d5 g5 dxe4 dxe4 Qxd2+ Nxd2 Nc6 Nc4 Rfe8 Bg4 Rad8 Ke2 Nd4+ Ke1 Nxc2+ Kf1 Nxa1 Kg2 Bxe4+"""
,"""e4 c6 d4 d5 e5 Bf5 g4 Be4 f3 Bg6 h4 h5 g5 e6 Bg2 c5 c3 Nc6 Ne2 Nge7 O-O Nf5 Nf4 cxd4 Nxg6 fxg6 Qe1 Bc5 Kh2 O-O f4 Rc8 Bd2 Qb6"""
,"""e4 c5 c4 d6 f4 Nc6 f5 e5 g4 f6 Nc3 Nge7 d3 g5 h4 Bg7 hxg5 fxg5 Bxg5 h6"""
,"""e4 e6 d4 c5 d5 exd5 exd5 d6 Qe2+ Be7 Be3 Nf6 Nc3 Bg4 f3 Bh5 O-O-O O-O g4 Bg6 h4 h6 h5 Bh7 Qg2 b5 Bxb5 a6 Bc4 Nbd7 g5 hxg5 Bxg5 Ne5 Be2 Rb8 h6 g6 f4 Ned7 f5"""
,"""d4 d5 Nf3 Nf6 Bf4 Bf5 e3 c6 Bd3 Bxd3 Qxd3 e6 Nbd2 Nbd7 Rc1 Be7 c4 h6 O-O O-O Ne5 Rc8 Rc2 c5 cxd5 Nxd5 Nxd7 Qxd7 Be5 Nb4"""
,"""e4 b6 Nc3 c6 d4 Bb7 Nf3 d6 Bd3 e6 Be3 f6 O-O g6 Qd2 h6 h3 g5 b3 f5 exf5 exf5 Bxf5 Be7 Be6 Nf6 Bxg5 Rg8 Bxf6 Bxf6 Bxg8 Kd7 Rfe1 Qxg8 Qf4 c5 Qf5+ Kc6 Qxf6 Nd7 Ne5+ Kc7 Nxd7 Kxd7 Re7+ Kc6 Rae1 Rd8 Qf7 Rc8 Qxg8 Rxg8 R1e6 Rc8 Rf6 b5 Ree6 b4 Rxd6+ Kc7 Nb5+ Kb8 Nxa7 Kxa7 Rxh6 Rc6 Rxc6 Bxc6 Rxc6 Kb7 Rd6 cxd4 Re6 d3 cxd3 Kc7 d4 Kd7 Re3 Kd6 h4 Kd5 h5 Kxd4 h6"""
,"""d4 d5 Nf3 c6 Bf4 f5 g3 e6 Bg2 Nf6 O-O Bd6 e3 O-O c4 a5 Nc3 h6 Bxd6 Qxd6 Ne5 Nbd7 Ng6 Re8 Qd2 Kh7 Nf4 g5"""
,"""g3 c6 Bg2 d5 d3 Nf6 Nc3 Bf5 Bg5 e6 Bxf6 Qxf6 e4 Bg6 exd5 cxd5 Nge2 Bd6 Qd2 Nc6 O-O O-O Nf4 Bf5 Nfe2 a6 Nd1 b5 Ne3 Bg6 Ng4 Qe7 Nf4 Bf5 Ne3 Bg6 Nxg6 hxg6 c3 Ne5 d4 Nc4 Nxc4 bxc4 Rfe1 Rab8 Bxd5 Qc7 Be4 Qb6 Re2 f5 Bg2 Rfe8 Qe3 e5 dxe5 Qxe3 Rxe3 Rxe5"""
,"""f4 Nc6 Nf3 e6 d4 d5 e3 a6 Bd3 Nf6 O-O Ne4 Nbd2 f5 Ne5 Nxe5 fxe5 Be7 Bxe4 dxe4 Rf4 Bg5 Rf1 Bxe3+ Kh1 Bxd4 Nc4"""
,"""e4 e6 f4 g6 Nf3 Be7 Bc4 h5 f5 a6 fxg6 fxg6 Ne5 Nc6 Nxg6 Rh6 Nxe7 Qxe7 O-O Rf6 Rxf6 Nxf6 d3 Ng4 Qf1 b5 Bb3 Nd4 Nc3 Bb7 h3 Ne5 Ne2 O-O-O Nxd4 Rg8 Bf4 Rf8 g3 Nf3+ Qxf3"""
,"""e4 e5 Nf3 Nc6 d4 exd4 Bc4 Bc5 c3 dxc3 Bxf7+ Kxf7 Qd5+ Ke8 Qxc5 d6 Qxc3 Nf6 Bg5 Qe7 O-O h6 Bh4 Qxe4"""
,"""e4 c5 Bc4 e6 d3 Qc7 f4 Nc6 Nf3 a6 a4 Nge7 Be3 Ng6 Qd2 Be7 O-O h5 f5 Nge5 fxe6 Nxc4 dxc4 fxe6 Bg5 O-O Bxe7 Nxe7 Nc3 b6 Qg5"""
,"""d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 Nc6 O-O e5 d5 Ne7 b4 Ne8 Nd2 a5 Ba3 f5 c5 Nf6 f3 Nh5 Nc4 axb4 Bxb4 dxc5 Bxc5 b6 Bxe7 Qxe7"""
,"""d4 g6 Nf3 Bg7 Nc3 e6 a3 Ne7 Bf4 O-O h3 a6 Qd3 d6 O-O-O Nbc6 e3 d5 e4 dxe4 Nxe4 e5 Bxe5 Nxe5 dxe5 Qxd3 Rxd3"""
,"""d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 Bg4 f3 Bh5 Bb5+ Nbd7 Bg5 Be7 Bxd7+ Qxd7 Bxf6 Bxf6 Nge2 O-O Nf4 Bg6 Nxg6 hxg6 O-O Rfe8 Qd2 Bd4+ Kh1 f6 Ne2"""
,"""e4 e5 Nf3 Nc6 Bc4 Nf6 d4 d6 d5 Na5 Bd3 c6 c4 cxd5 cxd5 b6 b4 Nb7 a4 Bd7"""
,"""e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be3 e6 f3 Nbd7 Qd2 b5 g4 Nb6 g5 Nfd7 O-O-O Bb7 h4 Rc8 h5 Be7 Kb1 Nc5 Nce2 Nc4 Qc1 d5 Ng3 Qa5 g6 hxg6 hxg6 Rxh1 gxf7+ Kxf7 Nxh1 dxe4 fxe4 Bxe4 Ng3 Bg6 Nb3 Nxb3 axb3 Nxe3 Qxe3 Bxc2+ Kc1 Qa1+ Kd2 Qxd1#"""
,"""e4 c5 Qh5 d6 Bc4 e6 Nf3 Nf6 Qg5 Nxe4 Qxd8+ Kxd8 d3 d5 Bb5 Nf6 Ne5 Ke7 Nc6+ bxc6 Bxc6 Nxc6 c4 dxc4 dxc4 Ba6 O-O Bxc4 Nc3 Bxf1 Kxf1 Rd8 Kg1 Ne4"""
,"""e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be3 Ng4 Bc1 Nc6 Be2 Qb6 Bxg4 Bxg4 Qxg4 Nxd4 Qd1 Nc6 Nd5 Qd8 Be3 Rc8 O-O e6 Nb6 Rb8 c4 Be7 Qd2 Qc7 Rac1 O-O Nd5 exd5 cxd5 Qd7 dxc6 bxc6 Rfd1 Rfc8 Bf4 Rd8 e5"""
,"""b3 d5 Bb2 Bg4 g3 c6 Bg2 Nd7 Nf3 Ngf6 h3 Bxf3 exf3 e5 O-O Bd6 d4 O-O dxe5 Bxe5"""
,"""e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 e6 Nc3 a6 Bd2 Nf6 Qe2 Nxd4 Qd3 e5 f4 d6 fxe5 dxe5 Bg5"""
,"""d4 c5 dxc5 Nc6 Nf3 Nf6 g3 e5 Bg2 Bxc5 O-O d5 Bg5 Be6 a3 h6 Bxf6 gxf6 b4 Bb6 b5 Nd4 Nxd4 Bxd4 c3"""
,"""e4 e5 Bc4 d6 d4 exd4 Bxf7+ Kxf7 Qxd4 Nc6 Qc4+ Be6 Qb5 Rb8 Nf3 h6 Nc3 Nf6 Be3 Be7 O-O-O Rf8 e5 Nd7 exd6 Bxd6 Qh5+ Kg8 Qg6"""
,"""e4 d5 exd5 Qxd5 Nc3 Qe5+ Qe2 Qxe2+ Bxe2 Bf5 d3 Nc6 Nf3 O-O-O O-O e6 Bf4 Bd6 Bxd6 Rxd6 Ng5 Bg6 Nge4 Rd7 Bf3 Nd4 Bd1 Bxe4 Nxe4 Nf6 Nxf6 gxf6 c3 Nf5 d4 e5 dxe5 fxe5 Bb3 f6 Be6 Rd8 Bxf5"""
,"""e4 e5 Nf3 Qf6 Nc3 c6 d3 a5 Be3 Na6 Be2 Bc5 O-O d6 Na4 Ba7 Nb6 Rb8 Nxc8 Bxe3 Nxd6+ Qxd6 fxe3"""
,"""e4 e5 Nf3 Nc6 Nc3 Nf6 d3 Bb4 Bd2 d6 g3 Bg4 Bg2 O-O a3 Ba5 b4 Bb6 O-O Nd4 Be3 Nxf3+ Bxf3 Bxf3 Qxf3 Bxe3 Qxe3 Qd7 Ne2 Ng4 Qf3 f5 exf5 Qxf5 Qxf5 Rxf5 Kg2 Rh5 h3 Nf6 c4 e4 d4 d5 c5 c6"""
,"""Nf3 e6 d4 Bb4+ Nc3 Be7 e4 Nh6 Bb5 O-O e5 d6 Bxh6 gxh6 O-O c6 exd6 Bxd6 Bd3 c5"""
,"""d4 Nf6 Nf3 g6 c4 Bg7 Nc3 d5 cxd5 Nxd5 e4 Nb6 h3 O-O Bd3 Bxd4 Bh6 Bg7 Bxg7 Kxg7 Qe2 Nc6 Qe3 Be6 O-O-O Qd6 a3 Bb3 Nb5 Qe6 Nxc7 Qf6 Nxa8 Bxd1 Rxd1"""
,"""e4 e5 Nf3 d6 h3 Nf6 Nc3 g6 b3 Bg7 Bb2 O-O Qe2 b6 O-O-O Bb7 Kb1 c5 h4 h5 Nh2 a5 g4 hxg4 Nxg4 Nxg4 Qxg4 Nd7 h5 gxh5 Qxh5 Nf6 Qh3 b5 Bxb5 d5 exd5 Bxd5 Nxd5 Qxd5 Bc4"""
,"""d4 e6 c4 d5 Nc3 Nf6 Nf3 Bb4 e3 Nc6 Qb3 O-O a3 dxc4 Bxc4 Bxc3+ bxc3 Na5 Qb4 Nxc4 Qxc4 b6 Qc6 Rb8 Ne5 Bb7 Qb5 Bxg2 Rg1 Be4 f3 Bb7 Ra2 a6 Qd3 Qd6 Rag2"""
,"""d4 c5 d5 d6 c4 Nf6 Bg5 e5 e4 Be7 Bxf6 Bxf6 Nc3 a6 b3 Qa5 Qc2 Bd7 Nf3 b5 cxb5 axb5"""
,"""f4 d5 Nf3 Nc6 e3 Bg4 Be2 e6 O-O h6 h3 Bxf3 Bxf3 Nf6 Nc3 g5 fxg5 hxg5 Bg4 Nxg4 Qxg4 Rh4 Qf3"""
,"""e4 e6 d4 d5 exd5 exd5 Nc3 c6 Nf3 h6 h3 Bd6 Be2 Be6 Ne5 Nd7 f4 Qe7 a3 Nxe5 fxe5 Bc7 O-O f6 exf6 Nxf6 Be3 O-O-O"""
,"""e4 c5 f4 Nc6 Nf3 e6 Be2 d5 e5 f6 O-O fxe5 Nxe5 Nf6 Bh5+ g6 Bg4 Bg7 f5 exf5 Nxc6 bxc6 Be2 O-O d4 cxd4 Qxd4 Ne4 Qe3 c5 Nc3 Bd4 Rf3 Bxe3+ Bxe3 Qb6 Nxd5 Qd6"""
,"""b3 e5 Bb2 d5 d4 Nc6 dxe5 f6 exf6 Nxf6 Nf3 Bf5 e3 Bc5 Bd3 Qd7 Bxf5 Qxf5 O-O O-O Nbd2 Ne4 Nxe4 dxe4 Nd4 Nxd4 exd4 Bd6 c3 Qh5 Qxh5"""
,"""e4 e5 f4 Nc6 Nf3 exf4 d4 d5 exd5 Qxd5 Bxf4 Qe4+ Qe2 Qxe2+ Bxe2 Bd6 Bxd6 cxd6 c3 Nf6 Nbd2 O-O O-O Bg4 Bd3 Rfe8 Rae1 g6 Rxe8+ Rxe8 Ng5 Kg7 h3 Bd7 Nde4 Nxe4 Rxf7+ Kg8 Bxe4"""
,"""d4 Nf6 c4 c6 Nc3 d5 Nf3 dxc4 e4 b5 b3 cxb3 Qxb3 Bg4 a4 Bxf3 gxf3 Qxd4 axb5 e6 Be3 Qe5 b6 Bc5 b7 Bxe3 fxe3 O-O bxa8=Q Qg5 Ne2 Qh4+ Ng3 Nh5"""
,"""e4 e5 Nf3 d6 d4 exd4 Nxd4 a6 Nc3 c5 Nf3 h6 Bc4 Nf6 h3 b5 Bd5 Nxd5 Nxd5 Nc6 O-O"""
,"""e4 e5 f4 exf4 Nf3 Nc6 d4 d5 e5 Nge7 c3 Nf5 Bxf4 Be7 Bb5 Bh4+ g3 Be7 Bxc6+ bxc6 b4 f6 Qe2 fxe5 Bxe5 O-O c4 dxc4 Qxc4+ Kh8 Nc3 Ne3 Qd3 Rxf3"""
,"""e4 e6 Nf3 g6 d4 Bh6 Bxh6 Nxh6 Qd2 Ng4 h3 Nf6 e5 Nd5 c4 Ne7 Nc3 a6 Qh6 b6 Qg7 Rg8 Qxh7 Nbc6 O-O-O Bb7 d5 exd5 cxd5 Na5 d6 Bxf3 gxf3 cxd6 exd6 Qb8 dxe7 Kxe7 Nd5+ Ke6 Bg2"""
,"""e4 e5 Nf3 Nc6 Nc3 f5 Bc4 Nf6 d3 d6 O-O Be7 Re1 fxe4 dxe4 Bg4 h3 Bh5 Be2 O-O Nd2 Qe8 Bxh5 Nxh5 Nf3 Nf4 Bxf4 Rxf4 Nd5 Rf7 c3 Bf6 Qb3 Kh8 Rad1 Ne7 Ne3 Ng6 Kh1 Nf4 Nf5 Rf8 Kh2 Qg6 g3 Qg5 Nxg5 Bxg5 gxf4 Bxf4+ Kh1 g6 Ng3 Rf6 Re2 Raf8 Rg1 Bh6 Rg2 Bg7 Qc2 Rf3 Qd2 R3f6 Re3 R6f7 Qe2 Rf4 Nf1 Bh6 Kh2 R4f6 Reg3 Bg7 Ne3 R6f7 Ng4 Rf6 Nxf6 Bxf6 Rxg6 Bg7"""
,"""e4 c5 Bc4 Nc6 Qf3 e6 c3 g6 Nh3 Bg7 O-O Ne5 Qe2 b6 Bb3 Bb7 Bc2 Nf6 d4 cxd4 cxd4 Neg4 f3 Nh6 d5 O-O d6 e5 Nc3 Rc8 Be3 Rc6 Nb5 a6 Nc3 Rxd6 Bb3 Rc6 Qd2 d6 Bxh6 d5 Bxg7 Kxg7 exd5 Nxd5 Nxd5 Rd6 Rad1 b5 f4 exf4 Nhxf4 Re8 Qd4+ f6 Qa7 Re7 Qd4 Red7 Ne6+"""
,"""e4 c5 Nc3 e6 f4 a6 Nf3 h5 e5 Nc6 Ne4 d5 exd6 Bxd6 c3 Nf6 d3 Nxe4 dxe4 Qc7 Bd3 Bxf4 O-O Bxc1 Rxc1 Ne5 Nxe5 Qxe5 Qf3 f6 Bc4 Bd7 Rcd1 Bc6 Bd3 Rd8 Bc2 Kf7 Rxd8 Rxd8 g3 g6 Rf2 h4 Kg2 Rh8 g4 f5 exf5 Bxf3+ Rxf3 gxf5 gxf5 h3+ Kf2 Qxh2+ Ke3 Qxc2 fxe6+ Kxe6 Kf4 Rf8+ Kg3 Qg6+ Kf2 Qg2+"""
,"""d4 d5 Bf4 Nf6 e3 g6 h3 c5 c3 Bg7 Nd2 cxd4 cxd4 O-O Ngf3 Nc6 Bd3 Nb4 Be2 Bf5 O-O Rc8 a3 Nd3 Ne5 Nxb2 Qb3 Qb6 Qxb6 axb6 g4 Be6 Rfb1 Rc2 Nef3 Rfc8 Kf1"""
,"""e4 e5 Nc3 Nf6 d3 d5 exd5 Nxd5 Bd2 Nc6 Nf3 Be7 Be2 O-O Nxd5 Qxd5 Bc3 Bd6 O-O Bf5 Nd2 Rfe8 Bf3 Qe6 Ne4 Bf8 Ng5 Qd7 Ne4 f6 Qe2 Rad8 Rad1 b6 Rfe1 Bg6 Qd2 Nd4 Bxd4 Qxd4 c3 Qd7 d4 exd4 Qxd4 Qxd4 Rxd4 Rxd4 cxd4 f5 Nf6+ gxf6 Bd5+ Kg7 Rxe8"""
,"""e4 g6 e5 Bg7 d4 e6 Nf3 Ne7 Be3 O-O Nc3 b6 Bd3 Bb7 Qe2 Nd5 Nxd5 Bxd5 c4 Bxf3 Qxf3 Nc6 d5 Nxe5 Qe4 Nxd3+ Qxd3 Bxb2 Rb1 Bg7 Bd4 e5 Bc3 d6 O-O f5 f4 e4 Qe3 Bxc3 Qxc3 Qf6 Qxf6 Rxf6 h4 Re8 Rb3 a5 Re3 Rf7 Rfe1 Kg7 a4 Kh6 g3 Kh5 Kg2 Kg4 R1e2 h6 Rb2 g5 hxg5 hxg5 fxg5 Kxg5 Rbb3 f4 gxf4+ Rxf4 Rg3+ Kf5"""
,"""d4 e6 c4 Nf6 Nc3 Bb4 Bg5 h6 Bh4 O-O Nf3 d5 e3 dxc4 Bxc4 Nc6 O-O Bxc3 bxc3 b6 e4 Bb7 e5 g5 Bxg5 hxg5 exf6 Qxf6"""
,"""g3 d5 Bg2 e6 f3 Nf6 d3 Bd6 e4 Nc6 f4 e5 fxe5 Nxe5 d4 dxe4 c3 Nc6 c4 Qe7 d5 Nd4 Qxd4 e3 Qd1 O-O Qe2 Re8 Nf3 Ne4 Ng5 Nxg5 h3 f6 h4 Ne4 g4 Ng3 Qf3 Nxh1 Bxh1 e2 Qe4 Bg3+"""
,"""e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5 Ndb5 d6 Bg5 a6 Na3 Be7 Nd5 Nxe4 Bxe7 Nxe7 Bd3 Nc5 Nxe7 Qxe7 Bc4 O-O O-O Be6 Qe2 Rfd8 Rad1 Rac8 Bd3 f6 Nc4 d5 Nb6 Rc6 Qh5 e4 Be2 Rxb6 c4 d4"""
,"""d4 Nf6 c4 c5 Nc3 cxd4 Nd5 Nxd5 cxd5 Qa5+ Qd2 Qxd5 Nf3 Nc6 b3 d6 Bb2 Bg4 Nxd4 Nxd4 Qxd4 Qxd4 Bxd4 e5 Be3 Be7 h3 Be6 g4 O-O Bg2 d5 f3 b6 O-O Rac8 Rac1 d4 Bf2 Rc5 f4 f6 Bh4 Rfc8 Rxc5 Rxc5 fxe5 Rxe5 Kf2 Bc5"""
,"""d4 Nf6 c4 e6 Nc3 Bb4 Bg5 c5 d5 Bxc3+ bxc3 Qa5 Bd2 Qc7 Nf3 exd5 cxd5 Nxd5 c4 Nf6 g3 d6 Bf4 Be6 e3 Ke7 Be2 Rd8 Ng5 h6 Nxe6 fxe6 O-O Nc6 Bf3 Rac8 e4 Nh5 Bxh5 e5 Be3 Kf8 Qf3+ Kg8 Qg4 Qe7 Bxh6 Kh8 Bg5 Qd7 Bxd8 Qxg4 Bxg4 Rxd8 Bf5 Nd4 Rad1 Kg8 h4 Kf7 Kg2 g6 Bg4 Kf6"""
,"""e4 e5 Nf3 Nc6 d4 exd4 Nxd4 Nxd4 Qxd4 d6 Nc3 Be7 Bd3 Nf6 O-O O-O Bg5 h6 Bxf6 Bxf6 Qc4 Be6 Qb5 Rb8 Rfe1 Bd7 Qd5 Bxc3 bxc3 Re8 Be2 Bc6 Qd3 Bxe4 Qc4"""
,"""d4 d5 Nf3 Nc6 c4 Bg4 e3 a6 Be2 e6 Nc3 Bxf3 Bxf3 dxc4 Bxc6+ bxc6 Qa4 Qd7 Ne4 g6 Qxc4 Bg7 Nc5"""
,"""e4 c5 d4 cxd4 c3 dxc3 Nxc3 d6 Bc4 e6 Bf4 Be7 Qd2 Nf6 O-O-O e5 Bg5 Nbd7 f4 O-O fxe5 Nxe5 Bb3 Bg4 Nge2 Rc8 Kb1 a6 Bxf6 Bxf6 Qxd6 Qxd6 Rxd6 Bxe2 Nxe2 Rfd8"""
,"""b3 c6 Bb2 d5 c3 Nf6 g3 h5 f3 h4 g4 g6 h3 Rg8 d3 Nh7 Nd2 f5 Qc2 fxg4 fxg4 Bg7 O-O-O Nf6 Ngf3 a5 Ng5 a4 b4 a3 Ba1 c5 b5 Bd7 Nb3 Bxb5 c4 dxc4 dxc4 Qc7 cxb5 b6 Ne6 Qb7 Nxg7+ Kf7 Rg1 Rxg7 Bg2 Qa7 Bxa8 Qxa8 e4 Nbd7 Rge1 e5 g5 Nxe4 Qc4+ Kf8 Rxe4"""
,"""e3 e5 f4 f6 Qh5+ g6 Qh4 Be7 fxe5 fxe5 Qe4 Nc6 Bb5 d5 Qf3 Bd7 e4 Nd4 Bxd7+ Qxd7 Qd3 O-O-O Nc3 Qg4"""
,"""e4 e6 Nf3 d5 exd5 exd5 d3 Nf6 h3 Bd6 Be2 O-O O-O c5 Nc3 Be6 Nh2 Qc7 Ng4 Nxg4 Bxg4 a6 f4 d4 Ne4 Bxg4 Qxg4 Nc6 f5 Rae8"""
,"""c4 e6 Nc3 b6 d3 Bb7 Bd2 Nc6 Nf3 d6 g3 Qd7 Bg2 Be7 O-O Nf6 a3 h5 h4 Nh7 b4 f6 b5 Na5 a4 g5 hxg5 fxg5 Ne4 g4 Nfg5 Bxg5 Nxg5 O-O-O Bxb7+ Kxb7 Nxh7 Rxh7 Bxa5 bxa5 Qd2 Kb6 Qe3+ Kb7 Qd2 Kb6 Qe3+ Kb7 Rab1 d5 Qd2 dxc4 Qxa5 Qd5 Qa6+ Ka8 dxc4 Qd4 e3 Qe4 Rfc1 Rd2 Rd1 Rf7 Rxd2 Qxb1+ Kg2 Qe4+ Kg1"""
,"""e4 e5 Ke2 Nc6 Kd3 Nf6 Kc4 a6 d4 b5+ Kb3 d6 c4 Be6 d5 bxc4+ Bxc4 Na5+ Ka4 Nxc4 dxe6 fxe6 Kb4 d5+ Kc3 Nxe4+ Kd3 Nxf2+ Kc3 Nxd1+ Kb3 Rb8+ Ka4 Qd7#"""
,"""e4 c5 Nf3 e6 b3 Nf6 e5 Nd5 Bb2 g6 d4 cxd4 Nxd4 Bg7 Nb5 Qa5+ Nd2 a6 Nd6+ Ke7 Qf3"""
,"""d4 d5 c3 e5 Nf3 Nc6 h4 e4 Ng1 Nf6 g3 Ng4 Bf4 Qf6 Bh3 h5 e3 Bd6 Bxg4 hxg4 Bg5 Qg6"""
,"""e4 c5 Bc4 Nc6 Bxf7+ Kxf7 Qh5+ g6 Qxc5 e6 Qe3 Bg7 d3 Nge7 Qf3+ Bf6 Nh3 Rf8 Bh6 Rh8 Bg5 Ng8 Bxf6 Qxf6 Qd1 Nge7 Nc3 a6 O-O Kg7 Qd2 Rf8 f4 Qd4+ Kh1 Kg8 Ne2"""
,"""e4 e5 Bc4 Bc5 d3 Nf6 Nf3 d6 Bg5 h6 Bh4 Nc6 Nc3 a6 Bxf6 Qxf6 Nd5 Qd8 a3 Ne7 Nxe7 Qxe7 O-O Bg4 c3 Ba7 d4 O-O dxe5 dxe5 Be2 f5 Qb3+ Kh8 exf5 e4 Nd4 Bxd4 Bxg4 e3 cxd4"""
,"""e4 c6 Nc3 d5 e5 Bf5 d4 e6 Nf3 Bb4 Bd2 a6 Be2 Bxc3 Bxc3 Nh6 h3 f6 O-O fxe5 Nxe5 O-O Bd2 Nf7 Ng4 Nd7 f4"""
,"""e4 e5 Nc3 f5 Bb5 c6 Bd3 Nf6 exf5 d5 f4 e4 Be2 Bxf5 d4 exd3 cxd3 Bc5 Nf3 O-O Na4 Bd6 O-O Qa5 b3 Nbd7 Bd2 Bc5+ Kh1 Qc7 Qe1 b5 Ba5 Bb6 Bxb6 Nxb6 Nc3 Rfe8 Nd4 Bxd3 Qd2 Bxe2 Ncxe2 Ne4 Qd3 Qf7 Ng3 Nxg3+ hxg3 c5 Nf3 d4 Qxb5 Nd5 Qxc5 Qf5 Qxd4 Qh5+ Nh4 Rad8 Qf2 Ne3 Qf3 Ng4 Rad1 Rxd1 Rxd1 h6 Rd7 Rf8 Rd5 Qe8 Qxg4 Qe1+ Kh2 Qe4 Qe6+ Qxe6 Re5 Qxe5 fxe5 Re8 Nf5 Rxe5 Nd6 Re2 a4 Ra2 b4 Rb2 b5 a6 Nf5 axb5 axb5 Rxb5 Ne7+ Kf8 Nc6 Kf7 Nd4 Kg6 Nf3 Rb3 Nh4+"""
,"""e4 e6 d4 d6 Nc3 c6 Nf3 b5 Be3 b4 Nb1 a5 Bd3 Ba6 Bxa6 Nxa6 Nbd2 Qb6 d5 Qb7 dxe6 fxe6 Nd4 e5 Nf5 g6 Ng3 h5 O-O h4 Ne2 Ne7 f4 Bg7 f5 gxf5 exf5 Bf6 Ne4 Rf8 Nxd6+ Kd7 Nxb7+"""
,"""d4 e6 Bf4 d5 e3 Nf6 Nd2 Bd6 Bg3 O-O f4 Nbd7 Ngf3 Ne4 c3 Nxg3 hxg3 c5 Bd3 g6 Rc1 c4 Bb1 f5 b4 cxb3 axb3 Nf6 Qe2 Ng4 Ng5 h5 Ngf3 a6 Nh4 Kg7 Ndf3 b5 O-O Bd7 c4 bxc4 bxc4 dxc4 Ne5 Bxe5 dxe5 Bb5 Rfd1 Qb6 Rc3 Rad8 Rxd8 Rxd8 Kh1 a5 e4 Nf2+ Kh2 fxe4 Bxe4"""
,"""e4 c5 Nf3 Nc6 Nc3 e6 Ne2 Nf6 Ng3 Be7 d3 O-O Be2 d5 Be3 d4 Bd2 e5 h3 Nd7 Nf5 Nf6 Nxg7 Kxg7 Qc1 Kh8"""
,"""d4 e6 e3 c5 c3 Qb6 a3 a6 Nf3 h6 Nbd2 Nf6 Nc4 Qc7 Bd3 Nc6 O-O d6 h3 d5"""
,"""e4 e5 Nf3 d6 Bc4 Nf6 d3 g6 O-O Bg7 Bg5 O-O Nc3 Qe8 Nd5 Nxd5 Bxd5 c6 Bc4 b5 Bb3 a5 a4 bxa4 Bxa4 Na6 c3 Bd7 Qd2 Nc5 Bh6"""
,"""e4 e5 d3 d6 h3 Nc6 Nf3 Nf6 Be2 g6 O-O Bg7 Nh2 Be6 f4 O-O f5 Bd7 c3 Qc8 Ng4 Re8 Nxf6+ Bxf6 Bg4 b6 Bh6 Qd8 fxg6 fxg6 Bxd7 Qxd7 Rxf6 Ne7 Bg5 Kg7 Rf1 h6 Bxe7 Rxe7 Nd2 Rf7 Rxf7+ Qxf7 Qf1 Qe7 Qe2 Qg5 Nf3 Qf4"""
,"""e4 Nc6 d4 d6 d5 Ne5 f4 Bg4 Be2 Bxe2 Qxe2 Ng6 f5 Ne5 Qb5+ Nd7 Bf4 b6 e5 dxe5 Bxe5 f6"""
,"""d4 d5 c4 c6 Nc3 Nf6 Nf3 Bf5 Bf4 e6 e3 dxc4 Bxc4 Bb4 O-O Bxc3 bxc3 Nbd7 Qb3 Nb6 Ne5 Nh5 Nxc6 bxc6"""
,"""e4 d5 Nf3 dxe4 Ng5 Bf5 Nc3 Nf6 Qe2 e3 dxe3 e6 Qb5+ Nbd7 Qxb7 Rb8 Qxa7 Ra8 Qd4 Bc5 Qd2 h6 Nf3 Qe7 Bb5 Rd8 O-O O-O Rd1 Ne5 Qe2 Nxf3+ Qxf3 Rxd1+ Qxd1 Ne4 Nxe4 Bxe4 Be2 Rd8 Bd3 Bc6 Qe2 Qg5 f3 e5 Kf1 f5 e4 f4 Bd2 Qh4 b4 Bb6 Bc3 Qxh2 Ke1 Qg1+ Kd2 Be3+"""
,"""e4 h5 Nf3 e6 h4 b6 g3 Bb7 d4 Bxe4 Bd3 Bxd3 cxd3 Be7 Ne5 g5 f3 d6 Nc4 f5 f4 b5 Ne3 c5 Qb3 c4 Qxb5+ Nd7 Qxc4 d5 Qc6 Rc8 Qxe6 Rxc1+"""
,"""e4 f5 Nf3 fxe4 Nd4 d5 Nc3 c6 d3 Nf6 dxe4 dxe4 Bc4 Bg4 f3 exf3 Nxf3 Qxd1+ Nxd1 Bxf3 gxf3 Nbd7 Ne3 O-O-O Bd2 Ne5 Be2 Nd5 Nxd5 Rxd5 O-O-O e6 Bf4 Rxd1+ Rxd1 Ng6 Bg3 Bc5 Bc4 Re8 Rd7 Kxd7 b3 Be3+ Kb2 Bf4 Bxf4 Nxf4"""
,"""e4 c5 Nf3 Nc6 Bb5 e6 Bxc6 bxc6 e5 Ba6 Nc3 g6 d3 Bg7 Be3 c4 dxc4 Bxc4 Bc5 Ne7 Bd6 Nd5 Nxd5 Bxd5 Qd3 Qa5+ c3 Qb6 a3 Qxb2 O-O Bxf3 Qxf3 Qc2 Rfc1 Qf5 Qxf5 gxf5"""
,"""d4 f5 c4 Nf6 Nc3 e6 Bf4 Be7 Nf3 O-O e3 a6 h4 b6 h5 h6 Nh4 Ne4 Ng6 Nxc3 bxc3 Rf7 Qc2 Bb7 Ne5 Rf8 Ng6 Rf6 Rh2 Rf7 Bd3 d6 g4 fxg4"""
,"""e3 e5 d4 exd4 Qxd4 d5 Bb5+ c6 Be2 Nf6 Nf3 Be7 O-O O-O Bd3 a6 b3 b6 Bb2 c5 Qc3 Nbd7 Nbd2 b5 a3 a5 Nb1 b4 axb4 cxb4 Qd2 Nc5 Bd4 Nxd3 Qxd3 Ba6 c4 dxc4 bxc4 Qc7 Nbd2 Rfc8 Rfc1 h6 Ne5 Rd8 Qb3 Bd6 c5 Bxe5 Bxe5 Qxe5 Nf3 Qe6 Qxe6 fxe6 Ne5 Rac8 c6 Rd6 c7 Nd5 Rc5"""
,"""e4 e5 Bc4 h6 Nf3 Nc6 d4 Nf6 dxe5 Nxe4 Qd5 Qe7 Qxe4 a6 O-O g5 Nc3 d6 Nd5 Qd7 Nf6+ Kd8 Nxd7 Bxd7"""
,"""e4 e6 d4 d5 exd5 exd5 Nf3 Bd6 Bd3 Bg4 O-O c6 Re1+ Ne7 c3 Qc7 h3 Bh5 Be3 Nd7 Nbd2 f6 Qc2 O-O-O b4 g5 Bf5 Nxf5 Qxf5 Bg6"""
,"""e4 d6 d4 Nf6 Nc3 g6 Bc4 c6 e5 dxe5 Nf3 exd4 Nxd4 Bg7 O-O O-O Bg5 Nbd7 Re1 Nb6 Bb3 a5 a4 Nbd5 Qe2 Nxc3 bxc3 c5 Nb5 e6 Rad1 Qb6 Rd6 Qxd6 Nxd6 Bd7 Nxb7 Bc6 Nxc5 Rac8 Nxe6 fxe6 Bxe6+ Kh8 Bxc8"""
,"""g3 c5 Bg2 d6 Nf3 e6 O-O Nf6 d3 Be7 Nbd2 h6 b3 a6 Bb2 Nbd7 Ne4 Nxe4 dxe4 Rb8 e5 d5 Nd2 b6 c4 dxc4 Nxc4 b5 Nd6+ Kf8 Bc6 Bb7 Bxb7 Qc7 Bg2 Bg5 Ne4 Be7 f4 Nb6 Rc1 Rd8 Qc2 Nd5 Nxc5 Ne3 Nxe6+ fxe6 Qxc7 Nxf1 Rxf1"""
,"""e4 b6 d3 Bb7 Nc3 d6 Be3 e5 Nf3 f6 Be2 g5 O-O h5 Qd2 g4 Ne1 Qd7 f3 Bh6 fxg4 Bxe3+ Qxe3 hxg4 Qg3 Qh7 Bxg4 Nh6 h3 Nxg4 Qxg4 Qg8"""
,"""e4 e6 Nf3 c6 Nc3 d5 exd5 cxd5 d4 Nf6 Bd3 Be7 Ne5 O-O O-O Nbd7 f4 a6 Kh1 b5 a3 Bb7 Qf3 h6 Qg3 Nh7 Ng4 f5 Nxh6+ Kh8 Qg6 gxh6 Qxe6 Rf6 Qe2 Rg6 Bxf5 Rg7 Qe6 Ndf6 Re1 Bd6 Qe3"""
,"""c4 c6 Nc3 e6 e4 Nf6 Nf3 Be7 d4 d5 cxd5 exd5 e5 Nfd7 Bd3 Na6 a3 Nc7 O-O Nb6 h3 Be6 Re1 Nd7 Qc2 c5"""
,"""e4 c6 d3 d5 Nc3 dxe4 dxe4 Nd7 Nf3 Ngf6 Bd3 e6 O-O Bd6 Re1 Qc7 Bg5 Ng4 g3 h6 Bd2 Nde5 Nxe5 Nxe5 Bf4 Nxd3 Bxd6 Qxd6 Qxd3 Qxd3 cxd3 O-O Kf1"""
,"""e4 e5 Qf3 Nf6 a4 Nc6 a5 Nxa5 Rxa5 c6 Rxe5+ Be7 Qg3 d6 Rxe7+ Qxe7 Qxg7 Rg8 Qh6 Qxe4+ Kd1 Bg4+ f3"""
,"""e4 e6 Nf3 d5 e5 c5 d3 Nc6 Nbd2 Nge7 b3 Ng6 Bb2 Qc7 Qe2 Be7 O-O-O O-O Ne4 dxe4 Qxe4 b5 d4 c4 bxc4 bxc4 Bxc4 Na5 Bd3 Bb7 Nh4 Bxe4 Bxe4 Rac8"""
,"""e4 e5 Nf3 Bc5 c3 f6 d4 Be7 Be3 Nc6 Nbd2 Nxd4 cxd4 exd4 Nxd4 Bb4 Be2 Qe7 O-O c5 Nf5 Qe5 Nc4 Qxe4 Nxg7+ Kd8 Qd6 Ne7 Ne6+ Ke8 Nc7+ Kf8 Bh6+ Kf7 Bh5+ Ng6 Bxg6+ Kxg6 Bf4 Qxc4 Nxa8 Qf7 Qd3+ f5 Qd6+ Qf6"""
,"""d4 d5 Bg5 Nd7 g3 Ngf6 Bh3 e6 Nc3 Be7 Bxf6 Nxf6 e3 a6 Nge2 b5 O-O O-O Qb1 c5 dxc5 Bxc5 b4 Bb6 a4 Bb7 a5 Bc7 Nd1 e5 c3 e4 f4 Bc8 Bxc8 Rxc8 Nd4 Bb8 Nf2 Qd7 Ra3 Ng4 Nxg4 Qxg4 Rf2 h5 Kg2 h4 Raa2 h3+ Kh1 Ba7 Rac2 Bxd4 cxd4 Rxc2 Qxc2 Rc8 Qb2"""
,"""d4 d5 f3 e6 Nh3 f5 g3 Nf6 e3 Be7 f4 c5 Qd2 cxd4 Qxd4 Nc6 Qd2 Bd7 Nc3 O-O Qe2 d4 Bd2 dxc3 Bxc3 b6 O-O-O Bc5 Kb1 b5 Bd4 Bxd4 exd4 Be8 c4 Bf7 d5 Ne7 d6 Nc6 cxb5 Nb8 d7 a6 Rc1 Nfxd7 Qd2 Nf6 Qxd8 Rxd8 bxa6 Nbd7 Bb5 Rxa6 Bxa6 Nc5 Bc4 Rc8 b4 Nce4 Bb3 Rd8 Rhd1 h6 Rxd8+ Kh7 Rdd1 Kg6 Rg1 Nd7 Rgf1 Nb6 Nf2 Nd6 Nd3 Nb5 Nc5 Nd5 Bxd5 exd5 Nd7 d4 Ne5+ Kf6 Nxf7 Kxf7 Rfd1 Nc3+ Rxc3 dxc3 Rc1 c2+ Rxc2 Kg6 Rc5 Kf6 h3 g6 Rc6+ Kf7 g4 fxg4 hxg4 g5 fxg5"""
,"""e4 e5 Nf3 Nc6 Bb5 d6 Bxc6+ bxc6 d4 Nf6 dxe5 dxe5 Qxd8+ Kxd8 Nxe5 Bd6 Nxf7+ Ke8 Nxh8 Bb7 f4 Ke7 e5 Bb4+ Nc3 Bxc3+ bxc3 Ne4 O-O Nxc3 Bd2 Ne2+ Kh1 Rxh8 f5 Rf8 Rae1 Nd4 Bg5+ Ke8 f6 g6 e6 Nf5 e7 Rf7 g4 Nxe7 Rxe7+ Kf8 Rxf7+ Kxf7 Re1 c5+ Kg1 Bd5"""
,"""e4 e6 c4 d5 exd5 exd5 cxd5 Nf6 b3 Nxd5 Bb2 Nf6 Nf3 Be7 Be2 O-O O-O h6 Re1 Re8 d4 Nbd7 Nc3 Nb6 Qc2 c6 Rad1 Nbd5 Ne5 Be6 Bd3"""
,"""e4 e5 Nc3 d6 Bc4 f5 exf5 Bxf5 d4 e4 d5 Nf6 Nge2 Nbd7 Ng3 Bg6 h4 Ne5 Be2 Bf7 h5 g6 h6 Nfg4 Be3 Nxe3 fxe3 Qf6 Qd2 Qe7 O-O-O O-O-O Rdf1 a6 Rf2 Be8 Rhf1 Bd7 Ngxe4 Ng4 Bxg4 Bxg4 Nf6 Bxh6 Nxg4 Bg5 Qe2 Rhe8"""
,"""d4 d5 Nf3 Nf6 e3 Bg4 h3 Bh5 g4 Bg6 Ne5 Nbd7 Nxg6 hxg6 Bg2 e6 a3 Nb6 g5 Nh5 e4 c6 exd5 Nxd5 Bxd5 cxd5 Nd2 Qxg5 Nf3 Qd8 Bg5 Be7 Bxe7 Qxe7 Qd3 O-O O-O-O Rac8 Ne5 Qc7 Rhg1 Nf4 Qd2 Nxh3 Rg3 Nxf2 Qxf2 Qb6 c3 Qb3 Kb1"""
,"""e4 c6 Nc3 d6 d4 g6 f4 Bg7 Nf3 Nf6 Bd3 O-O O-O Bg4 Qe1 Nbd7 Be3 c5 Rd1 cxd4 Bxd4 Qa5 h3 Bxf3 Rxf3 e5 Be3 exf4 Bxf4 Ne5 Bxe5 dxe5 Nd5 Qxe1+ Rxe1 Nxd5 exd5 f5 Bc4 e4 Rb3 b6 d6+ Kh8 d7 Rad8 Bb5 a6 Bc6 b5 Rd1 Rf6 Kf2 Rxc6 a4 Rc7 axb5 Rdxd7 Rxd7 Rxd7 bxa6 Bd4+ Ke2 Ra7 Rb8+ Kg7 Rb7+ Rxb7 axb7 Ba7 c4 Kf7 b4 Ke7 c5 Kd7 b5 Kc7 c6 Bb8 Ke3 Kb6 Kd4 Kxb5 Kd5 Kb6 Ke6 Kxc6 Kf7 f4 Kg7 e3 Kxh7 e2 Kxg6 e1=Q Kg5 Qg3+ Kf5 Qxg2 h4 f3"""
,"""e3 e6 b3 d5 Bb2 c5 h3 Nf6 g4 Bd6 h4 d4 h5 dxe3 dxe3 Nc6 h6 g6 g5 Rg8 Bxf6 Qb6 Nc3 e5 Nd5 Qa5+ Qd2 Qa3 Nf3 Nb4 Rd1 Be6 Bb5+ Nc6 Bxc6+ bxc6 Nc3 Be7 Ne4 Bxf6 Nxf6+ Ke7 Nxg8+ Rxg8 Qd7+ Kf8 Qd8#"""
,"""Nc3 Nf6 e4 d5 e5 d4 exf6 dxc3 fxg7 cxd2+ Qxd2 Qxd2+ Bxd2 Bxg7 O-O-O O-O Ne2 Nc6 Bc3 Bg4 f3 Bf5 Bxg7 Kxg7 Ng3 Bg6 Bd3 Rad8 Be4 Bxe4 Nxe4 b6 h4 e6 c3 Ne5 Rh3 Rxd1+ Kxd1 Rd8+ Kc2 f6 h5 Kf7 Rg3 Nc4 f4 Nd6 Nxd6+ cxd6 Kd3 f5 Rg5 Rg8 Kd4 e5+ Kd5"""
,"""e4 d5 exd5 Qxd5 Nc3 Qd7 Bc4 e6 Nf3 Bd6 O-O h6 d4 c6 Ne5 Bxe5 dxe5 Qxd1 Rxd1 Ne7 b4 O-O Rd3 b5 Rg3 Nf5 Rg4 bxc4 Ne4 Nd7 Rb1 Ba6 a4"""
,"""d4 d5 Nf3 Nd7 c3 e6 Bf4 Ngf6 e3 c5 Bd3 c4 Bc2 b5 Nbd2 b4 Qe2 a5 e4 a4 O-O dxe4 Nxe4 Ba6 Nd6+ Bxd6 Bxd6 b3 Bb1 Nd5 Re1 Ne7"""
,"""e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Bc4 Bg7 f3 O-O Be3 Nc6 Qd2 Bd7 O-O-O Rc8 Bb3 a6 g4 Ne5 Rdf1 Nc4 Bxc4 Rxc4 Bh6 b5 Bxg7 Kxg7 g5 Nh5 b3 Rc5 f4 Qc7 Nd5 Rxd5 exd5 Rc8 f5 Qc3 Qxc3 Rxc3"""
,"""e3 Nf6 d4 g6 c4 Bg7 Nc3 d6 Nf3 O-O Be2 Bg4 h3 Bxf3 Bxf3 c6 O-O d5 Bd2 dxc4 Be2 b5 b3 cxb3 axb3 Nd5 Nxd5 Qxd5 Qc2 c5 dxc5 Bxa1 Bxb5 Bg7 Bc4 Qc6 Rd1 Qxc5 Bxf7+ Rxf7 Qxc5 Nd7 Qc7 Nf6 Rc1 Raf8 f3 Nd5 Qc5 Rf5 Qd4 Bxd4 exd4 Nf4 Bxf4 Rxf4 Rc4 R4f7 Ra4 Rf6 Rxa7 Rb6 Ra3 Rd8 Ra4 Rxb3 Kf2 Rd3 d5 R3xd5 Re4 e5 Kg3 Kf7 f4 exf4+ Rxf4+ Ke6 Re4+ Re5 Rf4 Rf5 Rg4 Kf6 Kh2 Rd3 Rd4 Rf2 Rd6+ Rxd6 Kg3 Rdd2 Kh4 Rxg2"""
,"""e4 e6 d4 d5 exd5 exd5 Nf3 Nf6 Be2 h6 Nc3 c6 Bd2 Bf5 a3 Bd6 Nh4 Bh7 Be3 Ne4 Nxe4 dxe4 g3 O-O Ng2 f5 Bc4+ Kh8 Nf4 Qb6 Bb3 a5 O-O Nd7 Ne6 Rf6 d5 c5 c4"""
,"""c4 e5 e4 Nc6 d3 a6 Nf3 d6 a3 Bg4 Be2 Qf6 O-O h5 Nc3 Be6 Nd5 Bxd5 cxd5 Nd4 Nxd4 exd4 Bxh5 g6 Bf3 O-O-O Bg4+ Kb8 b4 Bh6 b5 a5 Bd2 b6 Bxh6 Rxh6 a4 Ne7 Qc2 Qe5 h3 f5 Bf3 f4 Bg4"""
,"""e4 c6 Nf3 d5 exd5 cxd5 d4 Nc6 c4 dxc4 Bxc4 e6 O-O Nf6 Re1 Bb4 Nc3 O-O Bd2 a6 a3 Ba5 b4 Bb6 Be3"""
,"""e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 e5 Nxc6 bxc6 Nc3 Bc5 Be3 Bxe3 fxe3 Qh4+ g3 Qd8 Qd2 Nf6 O-O-O O-O h3 a5 g4 h6 Qg2 Qb6 g5 Qxe3+ Kb1 hxg5 Be2 Rb8 h4 Qxc3 b3 a4 Bc4 a3 Kc1 Qa1+ Kd2 Qxa2 hxg5 Nh7 Qh3 Re8 Qxh7+ Kf8 Qh8+ Ke7 Qxg7 Rf8 Qf6+ Ke8 g6 fxg6 Rh8 Rxh8"""
,"""e4 e5 Nf3 Nf6 Bc4 Bc5 O-O O-O Nxe5 Nxe4 Re1 Qe8 d4 Bb4 Rxe4 d6 Bxf7+ Rxf7 Nxf7 Qxf7 Qe2 Qg6 Re8+ Kf7 Qe7#"""
,"""e4 g6 d4 d6 Nf3 Bg7 Be3 b6 c4 Ba6 b3 e6 Nc3 Ne7 Bd3 c5 d5 Bxc3+ Nd2 Bxa1 Qxa1 exd5 Qxh8+ Kd7 Qf6 dxc4 bxc4 Ng8 Qxf7+ Kc6 Qd5+ Kd7 e5 Nc6 Qxd6+ Kc8 Qxc6+ Qc7 Qe6+ Kb8 Be4 Bb7 Qxg8+ Qc8 Qxc8+ Bxc8 Bxa8 Kxa8 O-O Be6 f4 Kb7 g4"""
,"""e4 e6 d4 d5 e5 Nc6 c3 g6 Nf3 Bh6 Bxh6 Nxh6 Bd3 O-O Qd2 Nf5 h4 Qe7 h5 g5 Bxf5 exf5 Nxg5 f6 Nf3 Qg7 g3 f4 Qxf4 fxe5 Qg5 Qxg5 Nxg5 exd4 cxd4 Nxd4 Na3 Nf3+ Kf1 Nxg5 Kg2 Bh3+ Rxh3 Nxh3 Kxh3 Rxf2 b3 c5 Nb5 c4 bxc4 dxc4 Rc1 Rxa2 Rxc4 Ra5 Nd6 Rxh5+ Kg4"""
,"""e4 e6 d4 d6 Nc3 Nd7 Nf3 Ne7 Bd3 Ng6 Be3 Be7 Qd2 c6 O-O-O O-O h4 b5 h5 Nh8 Kb1 a5 e5 d5 Bh6 Re8 Rh3 f5 exf6 Nxf6 Rg3 Bf8 Ne5 Nf7 Nxf7 Kxf7 Bg5 h6 Bg6+ Kg8 Bxe8 Qxe8 Bxf6 Qxh5 Qf4 Qf5 Qxh6 Kf7"""
,"""d4 d6 e4 e5 dxe5 dxe5 Qxd8+ Kxd8 Nf3 Nc6 Bb5 Bd7 Nc3 f6 O-O g5 h3 h5 a3 a6 Bxc6 Bxc6 Rd1+ Ke8 Nd5 Bd6 c4 Ne7 Nxf6+ Kf7 Nd5 g4 Ng5+ Ke8 h4 Ng6 Nf6+ Ke7 Nd5+ Bxd5 cxd5 Nf4 Bxf4 exf4 Ne6 c6 Rac1 f3 g3 Rh6 Ng5 cxd5 exd5 Bc7 Re1+ Kd6 Rcd1 Bd8 Re6+ Rxe6 Nxe6"""
,"""e4 d5 exd5 Qxd5 Nc3 Qe5+ Qe2 Qxe2+ Bxe2 Bf5 Nd5 Kd8 c3 c6 Ne3 e6 Nxf5 exf5 Nf3 g6 Bc4 Ke7 O-O Nf6 Re1+ Kd7 Ne5+ Kc7 Nxf7 Rg8 Ng5 Rh8 Ne6+ Kc8 d3 Nbd7 Bg5 Be7"""
,"""e4 e5 Nf3 d6 d4 exd4 Qxd4 Nf6 Nc3 Nc6 Bb5 Bd7 Bxc6 Bxc6 O-O Be7 Re1 O-O Bf4 Nd7 Qd2 Bf6 Nd4 Ne5 Bxe5 Bxe5 Nxc6 bxc6 Rab1 g6 Ne2 Qh4 f4 Bg7 g3 Qf6 c3 a6 Nd4 c5 Nf3 Rab8 Re2 a5 Rc1 a4 a3 Rb3 Rc2 Rfb8 Qd5 Qe6 Qd3"""
,"""e4 e5 Nc3 Qe7 Bc4 c6 d3 h6 Nge2 a5 a3 g5 Ng3 Bg7 Nf5 Qf6 O-O d6 Bd2 Bxf5 exf5 Qxf5 Ne4 d5 Nd6+ Kd7 Nxf5 Nf6 Nxg7 dxc4 dxc4 Kc8 Nf5 Nbd7 Bc3 Rd8 Nd6+ Kb8 Nxf7 Rf8 Nxe5"""
,"""e4 e6 d4 c5 d5 exd5 Qxd5 d6 Bc4 Be6 Qd3 d5 exd5 Bxd5 Bxd5 Nf6 Bxb7 Qxd3 cxd3 Bd6 Bxa8 O-O Bf3 Nbd7 Nh3 Ne5 Be2 Rb8 Nc3 a6 O-O Nc6 b3 Nb4 Bb2 Nc2 Rac1 Nd4 Ne4 Nxe2+ Kh1 Nxc1 Rxc1 Nxe4 dxe4 Re8 f3 f6 Nf2 Rc8"""
,"""e4 c5 b3 Nc6 Bb2 d6 Bb5 Nf6 Qe2 e5 f4 a6 Bxc6+ bxc6 fxe5 dxe5 Nf3 Bd6 O-O O-O d3 Re8 Nbd2"""
,"""g3 d5 Bg2 e6 e4 Nf6 exd5 exd5 d4 Nc6 c3 Be6 h3 Be7 Bf3 Qd7 g4 O-O-O Ne2 a6 Nf4 Bd6 Be3 Rde8 Nd2 g5 Ng2 h6 h4 gxh4 Nxh4"""
,"""b4 e6 Bb2 d5 a3 Nf6 e3 Be7 Bxf6 O-O Bxe7 Qxe7 d4 b6 Nd2 Bb7 c4 Nd7 cxd5 exd5 Ngf3 c5 bxc5 bxc5 Nb3 c4 Nbd2 Bc6 Nb1 Rab8 Nc3 Rb3 Qc2 Rfb8 Rb1 Qxa3 Rxb3 Rxb3 Be2 Rxc3 Qb1 Rc1+ Qxc1 Qxc1+ Bd1 Qc3+ Nd2 a5 O-O a4 Nb1 Qb2 Bxa4 Bxa4 g3 c3 Kg2 c2 Na3 Qxa3"""
,"""e4 d6 Nf3 Bd7 e5 Qc8 Ng5 Nc6 Nxf7 Kxf7 exd6 cxd6 Bc4+ Ke8 Bxg8 Rxg8 O-O Nd8 Qh5+ g6 Qxh7 Rg7 Qh4 Be6 d3 Bf7 Nc3 g5 Qa4+ Nc6 d4 Be6 d5 Bd7 dxc6 Bxc6 Qd4 g4 Bf4 Rf7 Bg3 Bg7 Qd3 Bd7 Qh7"""
,"""c4 e6 Nf3 Nf6 Nc3 d5 d4 Be7 Bg5 Nbd7 e3 c6 c5 h6 Bh4 a5 a3 O-O Bd3 b6 b4 axb4 axb4 Rxa1 Qxa1 bxc5 bxc5 Nh7 Bxe7 Qxe7 Qb1 Nhf6 O-O e5 dxe5 Nxe5 Nxe5 Qxe5 Ne2"""
,"""d4 d5 c4 Nc6 Nc3 e5 e3 exd4 exd4 dxc4 Bxc4 Nf6 Nge2 Bb4 O-O Bg4 f3 Bh5 a3 Bd6 b4 O-O Qb3 Bg6 Bb2 Nxd4 Nxd4 Bxh2+ Kxh2 Qxd4 Ne4 Qd7 Rad1 Qf5 Ng3 Qf4 Rd4 Qh6+ Kg1 Rfe8 Rg4 Nxg4 fxg4 Qe3+ Kh2 Qxb3 Bxb3 Rad8 Nh5 Rd2 Bc1 Rd3 Bc4 Rc3 Bb5 c6 Ba4 b5 Bb2 Rce3 Bd1 a6 Nxg7 Bd3 Rf3"""
,"""e4 e5 f4 d6 fxe5 dxe5 Nf3 Nc6 Bc4 Be7 O-O Nf6 Ng5 O-O Qf3 Nd4 Qh3 h6 Rxf6 Bxh3 Nxf7 Ne2+ Kh1 Rxf7 Bxf7+ Kh8 gxh3 Bxf6 d3"""
,"""c4 e5 Nc3 Nf6 g3 Nc6 d3 d5 cxd5 Nxd5 Bg2 Be6 h4 Bb4 Bd2 Nxc3 Bxc6+ bxc6 bxc3 Ba3 h5 Qd5 Nf3 e4 dxe4 Qxe4 Rh4 Qf5"""
,"""e4 e5 Nf3 Nc6 Bc4 Bc5 d3 Nf6 Nc3 d6 Bg5 Bg4 Nd5 Nd4 h3 Bxf3 gxf3 c6 Nxf6+ gxf6 Be3 Ne6 Qd2 Qb6 O-O-O Rg8 Rhg1 O-O-O Bxe6+ fxe6 Bxc5 Qxc5 Qe3 Qxe3+ fxe3 Kd7 f4 Ke7 f5 d5 Rxg8 Rxg8 fxe6 Kxe6 exd5+ cxd5 Rd2 Rg3 Rh2 Rxe3 Kd2 Rg3 b3 d4 c3 Kd5 c4+ Ke6 a3 f5 b4 e4 dxe4 fxe4 h4 Rxa3 Rg2 Ra2+"""
,"""e4 e5 Nf3 Nc6 Bb5 Nf6 d3 a6 Ba4 b5 Bb3 Na5 c3 c6 Bc2 Bc5 b4 Bxf2+ Kxf2 Ng4+ Ke2 O-O bxa5"""
,"""e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be3 e5 Nb3 Be6 f3 Be7 Qd2 Nbd7 g4 Qc7 O-O-O Rc8 g5 Nh5 Kb1 b5 Nd5 Bxd5 exd5 O-O Na5 Nb8 Bd3 f5 gxf6 Bxf6 Rhg1 Kh8 Qg2 Qxa5 Qg4 Nf4 Bxf4 exf4 Qh5 h6 Qg6 Kg8 Rde1 Be5 Qh7+ Kf7 Qf5+ Ke7 Rxg7+ Kd8 Rxe5 dxe5 Qxe5 Re8 Qd6+ Nd7 Qxd7#"""
,"""e4 e5 Nf3 Nc6 Bb5 Nf6 O-O Be7 d4 exd4 Nxd4 Nxd4 Qxd4 O-O Nc3 d6 h3 h6 Bf4 b6 e5 c5 Qc4 Be6 Qa4 Nd5 Nxd5 Bxd5 Rad1 Bh4 Rxd5 Qe7 Rxd6 Qe6 Bxh6 Rfd8 Rxd8+ Rxd8 Qxh4 gxh6 Qxd8+ Kg7"""
,"""e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 dxc6 Nxe5 Bd6 Nf3 Nf6 Nc3 Bc5 h3 O-O O-O b5 d4 Bb4 Bg5 Be7 Qd2"""
,"""Nf3 c5 g3 Nc6 Bg2 e5 Nc3 e4 Nh4 d5 b3 Be7 Bb2 Bxh4 gxh4 Qxh4 Nxd5 Qd8 Bxg7 Qxd5 Bxh8 f6 e3 Ne5 Qh5+ Kf8 d4 Bg4 Qh4 Nf3+ Bxf3 Bxf3 Bxf6 Bxh1 O-O-O Bf3 Be5"""
,"""e4 e5 Bc4 c6 Nf3 d5 exd5 cxd5 Bb3 e4 d3 exf3 Qxf3 Be6 Nc3 Nf6 Bg5 Bg4 Bxf6 Bxf3 Bxd8 Kxd8 gxf3 Bc5 Bxd5 Nc6 Bxf7 Rf8 Bb3 Rxf3 O-O-O Rxf2 Ne4 Be3+ Kb1 Nd4 Nxf2 Bxf2 Rhf1 Be3 Rfe1 Bg5 Rf1 Kc7 Rde1 a6 Rf7+ Kb6 c3"""
,"""f4 e6 b3 c6 Bb2 d5 Nf3 Nf6 e3 Nbd7 c4 b6 d3 Bb7 Be2 c5 O-O Qc7 Nbd2 Be7 Qc1 O-O Ne5 Nxe5 fxe5 Nd7 d4 Bg5 Rf3 Rad8 Rg3 Bh6 Nf3 dxc4 Bxc4 cxd4 Bxd4 Nc5 Ng5 Qe7 h4 Kh8 Qc2 g6"""
,"""e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 Bc4 Bg7 Be3 e6 Nc3 Nge7 O-O O-O f4 a6 a4 Nxd4 Bxd4 Nc6 Bxg7 Kxg7 f5 exf5 exf5 Qe7 f6+ Qxf6 Rxf6 Kxf6 Nd5+ Kg7 Qf3 f5 Qf4 b5 axb5 axb5 Rxa8 Bb7 Rxf8 Kxf8 Bxb5 Kg7 Bxc6 Bxc6 Qe5+ Kf7 Qe7+ Kg8 Nf6+ Kh8 Qxh7#"""
,"""e4 e5 Nf3 Nc6 Bb5 d6 Nc3 f5 d4 fxe4 Nxe4 Bg4 d5 a6 Bxc6+ bxc6 dxc6 Nf6 Nxf6+ Qxf6 Bg5 Qg6 Qd2 Qe4+ Qe3 Bxf3 gxf3 Qxc6"""
,"""e4 c5 d3 d6 f4 Nf6 Nf3 g6 Nc3 Bg7 Bd2 O-O Be2 Nc6 O-O a6 h3 b5 g4 Qb6 Qc1 b4 Nd1 a5 Kh2 a4 f5 Bb7 Nf2 a3 b3 e6 Rb1 exf5 gxf5 Ne5 Nxe5 dxe5 Be3 Qc7 Ng4 Nxg4+ Bxg4 Rfd8 Bh6 f6 Bxg7 Qxg7 Rg1 g5 h4 h6 hxg5 hxg5 Kg3 Kf7 Bh5+ Ke7 Bg6 Rh8 Qe3 Rh6 Qxc5+ Kd7 Rh1 Rah8 Qb5+ Kc7 Qc4+ Kb8 Kg2 Rxh1 Rxh1 Rxh1 Kxh1 Qh6+ Kg2"""
,"""e4 d5 exd5 Qxd5 Nc3 Qa5 d4 c6 Nf3 Bf5 Bd3 e6 Bxf5 Qxf5 O-O Qa5 Bd2 Nd7 Qe2 Ngf6 a3 Qc7 Rfe1 Bd6 h3 O-O Ne4 Nxe4 Qxe4 Nf6 Qh4 Rfe8 c4 Be7 Bf4 Qd8 Be5 Nd7 Qe4 Nxe5 dxe5 Qb6 b4 c5 b5 Red8 a4 a5 Red1 Qc7 Qe2 b6 Qc2 h6 Rxd8+ Rxd8 Rd1 Rxd1+ Qxd1 Qd8 Qxd8+ Bxd8 Kf1 f6 Ke2 Bc7 exf6 gxf6 g4 Kf7 Ke3 Kg6 Ke4 Kf7 Nh4 Bd6 f4 Be7 f5"""
,"""d4 Nf6 c4 e6 Nf3 Bb4+ Bd2 Qe7 g3 Bxd2+ Qxd2 Nc6 Bg2 d5 O-O O-O b3 Ne4 Qc2 f5 Nbd2 Bd7 Nxe4 fxe4 Ne5 Nxd4 Qc3 Nxe2+"""
,"""e4 c5 f4 Nc6 Nf3 d6 d3 Bd7 Be3 Qc7 Nbd2 O-O-O a3 e6 g3 Be7 Bg2 f6 O-O h5 b4 h4 bxc5 hxg3 hxg3 dxc5 Rb1 g5 fxg5 f5 Bf4 e5 Be3 f4 Bf2 fxg3 Bxg3 Bxg5 Nc4 Nf6 Nxg5 Rdg8 Qd2 Ng4"""
,"""e4 e6 Nf3 g6 d4 Bh6 Bd3 d6 O-O Bd7 c4 Qe7 Nc3 Nc6 d5 exd5 exd5 Ne5 Re1 f6 Bxh6 Nxh6 Qd2 O-O-O Nd4 f5 Qxh6 Ng4 Rxe7 Rhe8 Qxh7 Rxe7 Qxe7 Re8 Qg5 f4 Qxf4 Nxh2 Kxh2 Rh8+ Kg1 Rh5 Bxg6 Re5 Qf8+"""
,"""d4 d5 Nf3 Nf6 e3 Nc6 Be2 Bf5 a3 e6 O-O Bd6 Ne1 e5 c3 e4 f4 O-O c4 Qe7 Nc3 Be6 cxd5 Bxd5 Bg4 Bc4 Rf2 Kh8"""
,"""e3 e5 d4 Nc6 d5 Nb4 e4 a6 a3 Nxd5 exd5 c6 c4 cxd5 cxd5 d6 Nc3 b6 Nge2 Bb7 g3 Nf6 Bg2 Qc8 Bg5 Be7 Rc1 h6 Bxf6 Bxf6 Ne4 Qd8 Nxf6+ Qxf6 O-O O-O f4 exf4 Nxf4 Qe7 Re1 Qc7 Rxc7 Rae8 Rxe8 Rxe8 Rxb7 Re5 Bh3 Kh7 Qc2+ g6 Rxf7+ Kg8 Qxg6+ Kh8 Qg7#"""
,"""e4 c6 Nf3 Qc7 d4 Nf6 Bd3 d6 O-O Bg4 c4 Nbd7 Nbd2 e5 d5 Be7 Qc2 h5 h3 Bxf3 Nxf3 O-O b3 cxd5 exd5 Nc5 Bf5 e4 Ne1 a5 Be3 b6 Bxc5 Qxc5 Bxe4 Qd4 Bd3 Qxa1 Nf3"""
,"""e4 d5 exd5 Qxd5 Nc3 Qd8 Nf3 Nf6 d4 e6 Bd3 Bb4 O-O O-O Be3 Nd5 a3 Bxc3 bxc3 Nxc3 Qd2 Nd5 Ne5 Nxe3 Qxe3 Nd7 Qh3 g6 Ng4 h5 Ne5 Nxe5 dxe5 Qg5 f4 Qh6 Qg3 Bd7 f5 exf5 Bxf5 Bxf5 Rxf5 Kh7 Rff1 Rae8 Rab1 b6 Rbc1 c5 Rcd1 Qg7 Rd7 Rxe5 h3 Rf5 Rxf5 gxf5 Qf3 Qd4+ Rxd4 cxd4 Qxf5+"""
,"""e4 d5 exd5 Qxd5 Nc3 Qa5 d4 e5 Nf3 Bg4 Be2 Bb4 Bd2 Nc6 Nxe5 Bxe2 Qxe2 Nxe5 dxe5 O-O-O a3 Ne7 O-O Bxc3 Bxc3 Qb6 Rad1 Nd5 Bd4 Nf4 Qg4+ Qe6 Qxf4 Rd5 Bc3 Rhd8 Rxd5 Rxd5 h3 h6 Qg4 Qxg4 hxg4 Kd7 Re1 c5 f4 g6 Kf2 b5 Re2"""
,"""e4 c5 f4 g6 Nf3 Bg7 a3 f6 c3 Qc7 e5 fxe5 fxe5 Bxe5 Nxe5 Qxe5+ Be2 d5 O-O Nf6 d4 Qd6 Bf4 Qb6 Be5 O-O dxc5 Qxc5+ Bd4 Qd6 Nd2 Nc6 Bf2 Bf5 Nf3 Ng4 Nd4 Qxh2#"""
,"""e4 d5 exd5 Qxd5 Nc3 Qd8 Nf3 Nf6 Be2 e6 O-O Be7 d4 O-O Bg5 b6 h3 Ba6 Bxa6 Nxa6 a3 c5 Qd3 cxd4 Qxd4 Qxd4 Nxd4 Rac8 Rad1 h6 Bh4 Nc5 Rfe1 g5 Bg3 Nh5 Bh2 Bd8 Ndb5 a6"""
,"""e4 c5 d3 Nc6 f4 e5 c3 d6 Nf3 Bg4 Be2 Bxf3 Bxf3 Nf6 O-O Be7 a4 O-O Na3 a6 Be3 Rc8 Nc4 b5 axb5 axb5 Nd2 Qd7 Qe2 h6 Ra6 b4 Rfa1 Rc7 Nc4 Rb7 fxe5 dxe5 Rxc6 Qxc6 Nxe5 Qe6 Nc4 Rbb8 e5"""
,"""e4 e5 Nf3 d6 Bc4 Bg4 O-O Qf6 h3 Bxf3 Qxf3 Qxf3 gxf3 Nc6 Nc3 Nd4 Bd3 c6 Ne2 Nxf3+ Kg2 Nh4+ Kg3 Ng6 Bc4 Nh6 d4 Be7 dxe5 Bh4+ Kh2 dxe5 Be3 b5 Bb3 O-O Bxh6 gxh6 Ng3"""
,"""g3 c6 Bg2 d5 e3 e6 Ne2 Nf6 d4 Be7 O-O O-O b3 h6 c4 b6 cxd5 exd5 Nbc3 Bb4 Bb2 Bxc3 Bxc3 Bf5 Nf4 Ne4 Bb4 Re8 f3 Nd6 Bh3 Bxh3 Nxh3 a5 Bxd6 Qxd6 Re1 c5 dxc5 bxc5 Nf4 d4 exd4 Rxe1+ Qxe1"""
,"""d4 d5 c4 c6 Nf3 Bg4 cxd5 cxd5 Qb3 Qd7 Ne5 Qc8 Nxg4 Qxg4 Qxb7 Qd7 Qxa8 Qc7 Nc3 e6 Bd2 Bb4 Rc1 Nf6 a3 Ba5 b4 Bb6 Nxd5 Qxc1+ Bxc1 O-O Nxf6+ gxf6 Bh6 Re8 e3 e5 Bb5 Rc8 O-O exd4 exd4 Bxd4 Qf3 f5 Qxf5 Rd8 Bd3"""
,"""c4 e5 g3 Nf6 Bg2 Nc6 e3 d6 Nc3 Be6 b3 Rb8 Nge2 a6 d4 exd4 exd4 Bf5 O-O Be7 Bg5 O-O Bxf6 Bxf6 d5 Ne5 Nd4 Bg6 f4 Nd7 Nde2 h6 Rc1 Bf5 Nd4 Bxd4+ Qxd4 Qf6 Qd2 Qd4+ Qxd4"""
,"""Nf3 d5 g3 Nf6 Bg2 c5 O-O Nc6 d4 cxd4 Nxd4 e5 Nxc6 bxc6 c4 Bb7 cxd5 cxd5 Nc3 Bc5 Bg5 Bd4 Bxf6 gxf6 Qb3 Bxc3 Qxb7 Bd4 Bxd5 O-O Qxa8 Qxa8 Bxa8 Rxa8 Rab1 Rb8 b4 Kf8 e3 Bc3 b5 Ke7 Rfc1 Ba5 a4 Rd8 Rd1 Rc8 Rbc1 Rb8 Rc6 Bb6 Kg2 Rg8 e4 h5 h4 Rg4 f3 Rg8"""
,"""e4 e6 f4 Ne7 Nf3 d5 exd5 exd5 Be2 Nf5 O-O Be7 d4 O-O Nc3 Nh4 Nxh4 Bxh4 Be3 Bf6 Qd3 c6 Bd2 Nd7 Rab1 Qc7 Nd1 c5 dxc5 Qxc5+ Be3 d4 Bf2 Qc7 Bxd4 Bxd4+ Qxd4 Nf6 Bd3 Bd7 Qf2 Bc6 Nc3 Qb6 Qxb6 axb6 a3 Nd5 Nxd5 Bxd5 c4 Bc6 f5 f6 Rbe1 Rfd8 Be4 Rd2 Bxc6 bxc6 Rf2 Rd3 Rf3 Rd2 Rf2 Rd4 Rf3 Kf7 Rb3 b5 cxb5 cxb5 Rxb5 Ra7 Ra1 Rd2 a4 Rad7 a5 Re7"""
,"""d4 Nf6 c4 g6 Nc3 Bg7 e4 O-O e5 Ne8 f4 d6 Nf3 dxe5 fxe5 f6 Be2 fxe5 dxe5 Qxd1+ Bxd1 Nc6 Bf4 e6 Bg3 Bd7 Bc2 Rxf3 gxf3 Nd4 O-O-O Nxf3 Rhf1 Nxe5 Bxe5 Bxe5 Nb5 Bxb5 cxb5 Nf6 Kb1 Nd5 Bb3 Ne3"""
,"""d4 b6 Nf3 Bb7 c4 Nf6 Nc3 g6 g3 Bg7 Bg2 O-O Nh4 d5 cxd5 Nxd5 Nxd5 Bxd5 e4 Bb7 Be3 e6 O-O Nd7 Rc1 Rc8 Qa4 a5 Qb5 Nf6 f3 Ra8 a4 Ba6 Qc6 Bxf1 Bxf1 Rc8 Ba6 Ra8 Bb7 Ra7 e5 Qxd4 Bxd4 Ne8 Qd7 Rxb7 Bc3 f6 Qe7 c5 Qxe6+ Kh8 exf6 Nxf6"""
,"""d4 f5 Nf3 Nf6 h4 e6 Ng5 d5 e3 c6 c3 Be7 Nd2 b5 Ndf3 a5 Ne5 Ne4 Ngf7 Qc7 Nxh8 Bf6 Qh5+ Kf8 Qxh7 Bxe5 Ng6+ Kf7 Nxe5+ Kf8 Qh8+ Ke7 Qxg7+ Kd8 Qf8#"""
,"""Nc3 Nf6 e4 d6 f4 g6 Nf3 Bg7 Bc4 c5 O-O O-O d3 Nc6 Kh1 a6 a3 b5 Ba2 Bb7 Nd5 Qc7 c4 Rad8 Rb1 b4 Nxc7"""
,"""d4 d5 c4 e6 a3 Nf6 Nc3 Be7 Bf4 Nh5 Be5 f6 Bg3 Nxg3 fxg3 dxc4 e4 c5 d5 O-O Bxc4 a6 Nf3 b5 Be2 c4 O-O Qb6+ Kh1 e5 Nh4 Rd8 Bg4 Bb7 Nf5 Bf8 Ne3 Nd7 Be6+ Kh8 Nf5 Nc5 Qg4 Nxe6 dxe6 Qxe6 Rad1 g6 Ne3 Qxg4 Nxg4 Rxd1 Rxd1 f5 exf5 gxf5 Nxe5 Bg7 Nf7+ Kg8 Rd7 Bc6 Rc7 Be8 Nd6 Rd8 Nxf5 Rd1+ Nxd1"""
,"""e4 e5 Nc3 Bb4 a3 Ba5 b4 Bb6 Na4 Nc6 Nxb6 axb6 Nf3 d6 Bc4 Bg4 h3 Bxf3 Qxf3 Qf6 Qb3 Nd4 Qa2 Qg6 O-O b5 Bd5 c6 Bb3 Nf6 d3 O-O Be3 Nf3+ Kh1 Nh4 Rg1 Qh5 c4 Ng6 Bd1 Qh4"""
,"""d4 g6 e3 Nf6 Bd3 d6 h3 Bg7 f4 b6 c4 Bb7 Nf3 O-O O-O c5 d5 Qc8 Nh4 e6 e4 h6 Nc3 a6 f5 gxf5 exf5 exf5 Nxf5 Qe8"""
,"""e4 e5 Nf3 d6 Bc4 Bg4 Nc3 Bxf3 Qxf3 Nf6 Nd5 Nbd7 Nxf6+ Nxf6 d3 Qd7 Bg5 Be7 Bxf6 O-O-O Bxe7 Qxe7 Bxf7 Rhf8 Qf5+ Qd7 Qxh7 Qxf7 Qh3+ Kb8 O-O-O Qf4+ Kb1"""
,"""e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 bxc6 Nxe5 Qe7 d4 d6 Nf3 Qxe4+ Be3 Nf6 Nc3 Qg6 O-O Bf5 Nh4 Qh5 Qxh5 Nxh5 Nxf5 g6 Ng3 Nxg3 fxg3 Bg7 Rae1 O-O"""
,"""e4 a6 Nf3 b5 Be2 Bb7 e5 Nc6 O-O e6 d4 Bb4 c3 Ba5 Bg5 Nge7 a4 h6 Bxe7 Qxe7 axb5 axb5 Bxb5 O-O Nbd2 Bb6 Qc2 Ra5 Bd3 Rfa8 Rxa5 Rxa5 Nc4 Ra2 Nxb6 cxb6 Qb3 Ra8 Qxb6 d5 Qb3 g5 h3 Na5 Qc2 Ba6 Bxa6 Rxa6 Qa4 Qb7"""
,"""e4 d5 exd5 Qxd5 Qf3 Qe6+ Qe3 Nc6 Qxe6 Bxe6 c3 Bf5 d4 O-O-O Be3 e5 dxe5 Nxe5 f3 b6 Nd2 Nd3+ Bxd3 Bxd3 Kf2 Nf6 Ne2 Bd6 Nb3 Rhe8 Rad1 Bc4 Nbc1 Nd5 Bd4 c5 Bxg7 Bxe2 Nxe2 Kc7 Rxd5 Bg3+ hxg3 Rxd5 Bh6 a5 Bf4+ Kc6 Rh6+ Kb7 Rxh7 Re7 g4 Red7 g3 Rd1 Be3 Ra1 a3 Ra2 Bc1 Ra1 g5 Kc6 Rh6+ Kb5 b3 Rb1 c4+ Ka6"""
,"""d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g6 h3 Bg7 Nf3 O-O Bd3 a6 a4 Re8 O-O Nbd7 Bf4 Qc7 Rc1 Nh5 Bh2 Ne5 Nxe5 Bxe5 Bxe5 Rxe5 f4 Re8 e5 dxe5 f5 c4 Ne4 Bxf5 Rxf5 gxf5 Qxh5 fxe4 Qg5+ Kh8 Qf6+ Kg8 Bxe4 Qe7 Qf5 f6 Rc3 Kh8 d6 Qf7 Rxc4"""
,"""e4 e5 Bc4 Nf6 Nc3 Bb4 Nd5 Nxd5 Bxd5 O-O a3 Ba5 b4 Bb6 Qe2 c6 Bb3 d5 exd5 cxd5 Nf3 Bg4 h3 Bh5 g4 Bg6 d3 Nc6 Be3"""
,"""d4 d5 c4 Nf6 Nc3 Bf5 Nf3 Nc6 e3 e6 Be2 Bb4 O-O O-O cxd5 Nxd5 Bd2 a5 a3 Nxc3 bxc3 Bd6 c4 Qf6 c5 Be7 Rc1 Qg6 Kh1 Rfe8 Qb3 Bf6 Qxb7 Be4 Qxc7 e5 Qd7 exd4 exd4 Nxd4 Rfe1"""
,"""c4 e5 Nc3 Nf6 d3 Ng4 e4 f5 exf5 Bb4 Qxg4 h5 Qe2 d5 Qxe5+ Qe7 Qxe7+ Kxe7 Bd2 Bxf5 Nxd5+ Kd6 Nxb4 b5 g3 bxc4 Bg2 cxd3 Bxa8 Re8+ Kd1 h4 gxh4 c5 Nxd3 c4 Nf4 Ke5 Nge2 c3 bxc3 Nd7 Bc6 Kd6 Bxd7 Kxd7 Rb1 a5 Rb5 a4 Rxf5 a3 Ra5 Rb8 Rg1 Rb1+ Kc2 Rb6 Rxg7+ Kd6 Rg6+ Kc7 Rxb6 Kxb6 Rxa3 Kb5 c4+ Kxc4 Rd3 Kc5 Ne6+ Kc4 Rd8 Kb5 Kc3 Ka4 Kc4 Ka3 Rb8 Kxa2 Kc3 Ka1 Kc2"""
,"""c4 Nf6 Nc3 c5 e3 d6 d4 cxd4 exd4 a6 Nf3 Bg4 Be2 Bxf3 Bxf3 Nc6 O-O e6 Bg5 Be7 a3 O-O b4 e5 b5 axb5 cxb5 Nxd4 Bxb7 Rb8 Ba6 Qa5 Bd2 Nb3 Ra2 Nxd2 Qxd2 Qb6 a4 Nh5 a5 Qc5 b6 Nf4 Rb1 Qc6 f3 d5 Bb5 Qc5+ Kf1 d4 Ne4"""
,"""d3 Nf6 g3 g6 Bg2 Bg7 Nf3 O-O e3 d6 h4 h5 Nh2 c5 Nc3 a6 f3 Nc6 g4 hxg4 fxg4 Qd7 Bd2 Nxg4 Nxg4 Qxg4 Qxg4 Bxg4 Bh3 Bh5 Ne2 e6 Nf4 Bf3 Rg1 e5 Ne2 Nb4 Bxb4 cxb4 Ng3 f5 Rf1 Bg4 Bxg4 fxg4 O-O-O Rxf1 Rxf1 Rf8 Rxf8+ Bxf8 Ne4 Be7 Ng5 Bxg5 hxg5 Kf7 Kd2 Ke6 Ke2 Kf5 Kf2 Kxg5 Kg3 e4 dxe4 Kf6 Kxg4 b5 Kf4 a5 e5+ dxe5+ Ke4 a4"""
,"""e4 e5 Nf3 Nc6 Bc4 d6 Nc3 Nf6 d4 exd4 Nxd4 Bd7 Bf4 Be7 O-O O-O Nf3 Nh5 Be3 Bg4 h3 Bxf3 Qxf3 Ne5 Qxh5 Nxc4 Bc1 Bf6 Re1 Ne5 Bf4 Ng6 Re3 Bd4 Rg3 Nxf4 Qg4 Ng6 Rd1 Bxc3 bxc3 Qe7 Rd5 Qe6 Rg5 Qxg4 R3xg4 f6 Rh5 Ne5 Rf4 g6 Rhh4 g5"""
,"""d4 d5 c4 dxc4 Nc3 Nf6 Nf3 e6 a3 Nc6 g3 Be7 Bg2 O-O O-O Bd6 e4 e5 dxe5 Nxe5 Nxe5 Bxe5 Qxd8 Rxd8 Bg5 Rd6 Nd5 Nxd5 exd5 b5 Be7 Rd7 Bb4 Rxd5 Bxd5 Ba6 Bxa8"""
,"""e4 e5 Nf3 Nc6 c3 Nf6 d3 d6 Bg5 Bg4 h3 Bxf3 Qxf3 Be7 Bxf6 Bxf6 Nd2 Qe7 a3 O-O-O b4 Bg5 Nb3 b6 a4 h5 b5 Na5 Nxa5 bxa5 Be2 Qe6 O-O"""
,"""d4 e6 c4 d5 Nf3 c6 Nc3 Bd6 e3 f5 Bd3 Nf6 Qc2 O-O O-O Bd7 c5 Bc7 b4 Be8 Ng5 Ne4 Nxe6 Qh4 Nxf8 Qxh2#"""
,"""e4 b6 d4 Bb7 Bd3 e6 c4 Bb4+ Nc3 Bxc3+ bxc3 h6 Nf3 Nf6 Qe2 O-O O-O d6 h3 Nbd7 a4 e5 Re1 a5 Nh2 Nh7 d5 Nc5 Bc2 Bc8 f4 Qh4 Rf1 Nf6 fxe5 dxe5 Nf3 Qg3 Kh1"""
,"""e4 d5 exd5 Qxd5 Nc3 Qd8 Bc4 Nf6 d3 Bg4 f3 Bf5 Be3 e6 Nge2 c6 Ng3 Bg6 Qd2 Bd6 Nce4 Nxe4 Nxe4 Bc7 Bb3 Ba5 c3 Bxe4 fxe4 O-O O-O-O b5 h4 Bb6 d4 Nd7 g4 c5 g5 cxd4 Bxd4 Bxd4 Qxd4 Nb8 Qe3 Qc7"""
,"""e4 d5 exd5 Qxd5 Nc3 Qe5+ Be2 Bg4 h3 Bxe2 Qxe2 Qxe2+ Ngxe2 Nc6 O-O Nf6 Nb5 Rc8 d4 a6 Nbc3 e6 Bg5 Be7 f4 O-O Rad1 Rfd8 a3 h6 Bh4 Rd6 Bg3 Rcd8 f5 R6d7 fxe6 fxe6 Be5 Nxe5 dxe5 b5 exf6 a5 fxe7 b4 Rxd7"""
,"""e4 e6 d4 d5 Nd2 c5 exd5 exd5 Ngf3 Nf6 dxc5 Bxc5 Bd3 O-O O-O Nc6 h3 Bb6 c3 Qd6 Nb3 Bd7 Bg5 h6 Bh4 g5 Bg3 Qxg3 Nbd4 Qd6"""
,"""e4 e5 Nf3 Nf6 d3 Nc6 g3 d5 exd5 Nxd5 Bg2 Bd6 O-O O-O c3 f6 Qb3 Be6 Qxb7 Qd7 Nh4 g5 Nf3 Rfb8 Qa6 Rb6 Qa4 h5 Nfd2 Kg7 Nc4 Rb7 Qd1 Kh6 Nbd2 f5 Nf3 f4 Nxd6 Qxd6 Nd2 g4 Ne4 Qd7 Nc5 Qc8 Nxb7 Qxb7 Qb3 Qxb3 axb3 Kg6 b4 f3 Bh1 Nb6 Be3 Ne7 Bxb6 cxb6 Rfe1 Kf5 Re4 Bd5 Re3 Nc6 Rae1 a5 bxa5 Rxa5 d4 Bb3 dxe5 Nxe5 Re4 Bc4 Rf4+ Kg5 Rxe5+ Rxe5"""
,"""e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 dxc6 d4 Bg4 h3 Bh5 d5 cxd5 g4 Bg6 exd5 Be4 Nc3 Qf6 Nxe4 Qb6 Be3 Qxb2 Nxe5 O-O-O Nxf7 Rxd5 Qxd5 Qxa1+ Ke2 Ne7 Qd8#"""
,"""e4 e5 d3 Nf6 Be3 c6 Be2 Be7 Nf3 Qc7 Nc3 O-O O-O d6 Qd2 Nbd7 a4 b6 b4 Bb7 a5 Rad8 a6 Ba8 b5 d5 exd5 cxd5 d4 e4 Ne5 Bb4 Nxd7 Rxd7 Ra3 Bxa3 Bg5 Bb4 Bxf6 gxf6 Qe3 Qxc3 Qf4 Qxd4 Qg4+ Kh8 Qxd7 Rg8 Qxa7 e3 f3 Bc5 Qc7 Qe5 a7 Bd6 Qxb6 Qxh2#"""
,"""e4 g6 Nc3 Bg7 d4 b6 Bd3 Bb7 f3 e6 Nge2 Ne7 Bf4 d5 Bg3 dxe4 fxe4 O-O O-O c5 e5 cxd4 Ne4 Nec6 Nf6+ Kh8 Qd2 Nd7"""
,"""e4 e5 d3 Nc6 Nf3 Nf6 Be2 d5 O-O d4 Bg5 Be7 Bxf6 Bxf6 c3 dxc3 Nxc3 O-O Nd5 Be6 Nxf6+ Qxf6 Qc2 Rac8 Rac1 Rfd8 Nd2 Nd4 Qb1 Nxe2+"""
,"""e4 c5 f4 e5 d3 d6 fxe5 dxe5 Nf3 Nc6 h3 Nf6 Bg5 Be7 Bxf6 Bxf6 g3 O-O Qe2 Nd4 Nxd4 exd4 Nd2 b5 Bg2 Bb7 Bf3 Qc7 O-O-O Rfc8 h4 c4 g4 cxd3 Qxd3 a5 g5 Be5 Bg4 Rd8 Nf3 Bc8 Nxe5 Qxe5"""
,"""d4 d6 e4 Nf6 Nc3 Nbd7 f4 c5 d5 a6 a4 g6 Nf3 Bg7 Be2 O-O O-O Rb8 h3 Ne8 Be3 Ndf6 g4 Bd7 e5 dxe5 fxe5 Nxg4 hxg4 Bxg4 Bg5 f6 Bh4 Bxf3 Bxf3 fxe5 Bg4 Nd6 Be6+ Kh8 Qg4 b5 a5 b4 Ne4 b3 c3 Nc4 Ng5 Rxf1+ Rxf1 Ne3 Nf7+ Kg8 Nxd8+ Kh8 Nf7+ Kg8 Ng5+ Kh8 Qf3 Nxf1 Qxf1 Rf8 Nf7+ Kg8 Qxa6 e4 Qe2"""
,"""e4 d5 exd5 Qxd5 Nf3 Nf6 Be2 Bg4 O-O e5 Nc3 Qe6 Ng5 Bxe2 Qxe2 Qf5 d4 Bd6 dxe5 Bxe5 Re1 Nbd7 f4 O-O fxe5 Nxe5 Qxe5 Qxc2 Nge4 Rae8 Nxf6+ gxf6 Qg3+ Kh8 Rxe8 Rxe8 Bh6 Rg8 Qf3 f5 Nd5 Qxb2 Rf1 Qxa2 Qc3+ f6 Qxf6+"""
,"""d4 d5 c4 Nf6 Nc3 e6 Nf3 c6 Bg5 Be7 e3 Nbd7 Bd3 a6 O-O dxc4 Bxc4 b5 Bb3 Bb7 Rc1 c5 Ne5 c4 Bc2 Nd5 Bxe7 Qxe7 Qg4 g6 e4 N5f6 Qe2 O-O Rcd1 Nxe5 dxe5 Nd7 f4 Nc5 f5 exf5 exf5 Rae8 Rde1 b4 f6 Qe6 Qe3 Nd3 Qh6 Qb6+ Kh1 Bxg2+ Kxg2 Nxe1+ Rxe1"""
,"""b3 c6 Bb2 d5 c3 Nf6 g3 e5 f3 Bf5 e3 Bg6 d3 Nfd7 g4 f6 h4 Bf7 Nd2 Qb6 Qe2 Be7 h5 h6 Bg2 Nf8 a3 Nbd7 b4 Rc8 a4 Qd8 f4 exf4 exf4 Ne6 Qe3 Kf8 g5 fxg5 f5 Nf4 Qf3 Bf6 O-O-O a5 Nh3 Nxh3 Bxh3 axb4 c4 Ne5 c5 Nxf3 Nxf3 Qa5 Bxf6 gxf6 Kd2 Qxa4 Ra1 Qb3 Rab1 Qa2+ Ke3 b3 Rh2 Qxb1 Ne5 fxe5 Re2 d4+ Ke4 Re8 f6 Bd5+ Kf5 Qxd3+ Kg4 Qxe2+ Kg3 Qe3+ Kh2 Qxh3+ Kxh3 b2 Kg4 b1=Q Kh3 Qg1 f7 e4"""
,"""d4 d5 Nc3 c6 a3 b5 e4 dxe4 Nxe4 f5 Nc3 h6 Qh5+ Kd7 Bf4 Nf6 Qxf5+ Ke8 Qg6+ Kd7 O-O-O h5 d5 c5 Bxb5+ Nc6 dxc6+"""
,"""e4 e5 d4 d6 Nf3 f6 dxe5 fxe5 Bc4 Nf6 Nc3 Bg4 h3 Bh5 g4 Bg6 Bg5 h6 Bxf6 gxf6 Bd5 Na6 Bxb7 Rb8 Bxa6 Rxb2 Bb5+ Kf7 Bc4+ Ke8 O-O Qa8 Bd5 c6 Be6 Bf7 Bf5 Rg8 Na4 Rb4 c3 Rc4 Nd2 Rxc3 Nxc3 h5 Nc4 d5 Nd6+ Bxd6 exd5 hxg4"""
,"""c4 e5 Nc3 Nf6 Nf3 Nc6 g3 Bc5 Bg2 O-O e3 e4 Ng5 d5 cxd5 Nxd5 Ngxe4 Nxc3 dxc3 Qxd1+ Kxd1 Rd8+ Ke2 Bb6 Rd1 Rxd1 Kxd1 Ne5 Ke2 Bg4+ f3 Be6 Ng5 Bc4+ Kf2 Rd8 Bf1 h6 Ne4 f5 Nd2 Bxf1 Nxf1 g5 Ke2 Re8 b3 Ng6 Bb2 f4 e4 fxg3 Nxg3 Nf4+ Kd2 Rd8+ Kc2 Be3 Bc1 Bxc1 Rxc1 Rd3 Nf5 Rxf3"""
,"""Nf3 e6 Ne5 b6 e4 Bb7 Qf3 Nh6 d4 Nc6 Bf4 Nxd4 Qe3 Nxc2+ Ke2 Nxe3 fxe3 Bxe4 Bxh6 gxh6 Rg1 Bd6 Ng4 f5 Nxh6 Qg5 Nc3 Qxh6 Nxe4 fxe4 Ke1 Qxh2 Kf2 Bg3+"""
,"""e4 e6 Nf3 d5 exd5 exd5 d4 c6 Bd3 Bd6 Be3 Ne7 Nbd2 O-O c3 Bf5 Qc2 Qd7 Bxf5 Nxf5 O-O-O Nxe3 fxe3 b5 e4 a5 e5 Be7 Rdg1 b4 g4 a4 h4 c5 g5 c4 cxb4 Bxb4 h5 a3 b3 cxb3 Qxb3 Rc8+ Kd1 Bxd2 Kxd2 Na6 g6 Rab8 gxh7+ Kh8 Qd3 Rb2+ Ke3 Rcc2 h6 g6 Rxg6 fxg6 Qxg6 Qxh7 Qf6+ Kg8 Rg1+ Rg2 Ng5 Rbe2+"""
,"""e4 e5 Nf3 Nc6 Bc4 Nf6 d3 h6 Nc3 Bc5 O-O a6 a4 O-O h3 Nd4 Nxe5 d6 Nf3 c6 Nxd4 Bxd4 Be3 Be5 f4 Bxc3 bxc3 d5 exd5 cxd5 Bb3 Bf5 Qf3 Rc8 Bd4 Qd6 g4 Bd7 Rae1 Rfe8 Rxe8+ Rxe8 g5 hxg5 fxg5 Nh7 Qxf7+ Kh8 Qxg7#"""
,"""e4 e5 Nf3 Nc6 Bc4 Nf6 Nc3 h6 O-O a6 d4 exd4 Nxd4 Nxd4 Qxd4 b5 Bb3 c5 Qd2 c4 Bxc4 bxc4 Nd5 Bc5 b3 cxb3 axb3 O-O Bb2 Nxd5 exd5 f6 Qc3 d6 Qg3 Bf5 c4 Qb6"""
,"""e4 e6 d4 d5 Nc3 dxe4 f3 Nf6 fxe4 c5 Nf3 cxd4 Nxd4 Be7 Bb5+ Bd7 O-O O-O Bd3 Nc6 Nf3 a6 b3 Qc7 Bb2 Rfd8 Ng5 h6 Nxf7 Kxf7 Qh5+ Kg8 Rxf6 Bxf6 e5 Be8 Qh3 Bxe5 Re1 Bf7 Bc1 Bxc3 Rf1 Bf6 Bxh6 Qe5 Bf4 Qc5+ Kh1 Rxd3 cxd3 e5 Bd2 Nd4 Qg4 Qe7 Re1 Re8 Bc3 Ne6 Rf1 Nf4 Bd2 Nxd3"""
,"""b3 d5 Bb2 Nf6 g3 e6 Bg2 c6 Nf3 Be7 e3 O-O c4 Nbd7 d4 b6 Nbd2 a5 a3 a4 b4 b5 c5 Re8 O-O Bf8 Qc2 g6 e4 dxe4 Nxe4 Nd5 Ne5 f5 Nxc6 Qc7 Ng5 Qxc6 Rfe1 N7f6 Re5 Bd7 Rae1 Bg7 Bxd5 Nxd5 R5e2 h6 Nf3 Qc7 Ne5 Kh7 c6 Bc8 Qc5 Rb8 f4 Bf8 Qc2 Qd6 Nd7 Bxd7 cxd7 Qxd7 Qd3 Rbc8"""
,"""d4 Nf6 c4 d5 Bg5 dxc4 e3 Ne4 Bh4 Nd6 Nc3 c6 Qa4 b5 Qc2 Qa5 f3 Bf5 Qc1 Bd3 Bxd3 cxd3 Kf2 Nc4 a3 d2 Qc2 b4 Ne4 bxa3 Qxc4 axb2 Rb1 Qa1 Ne2 Qxb1 Rxb1 d1=B Rxd1 b1=N Rxb1"""
,"""e4 e5 Nf3 d6 d3 Be6 h3 Nf6 g4 g6 Bg2 Bg7 O-O Nc6 c3 Qd7 Nbd2 O-O-O b4 a6 a3 h5 g5 Nh7 h4 f6 gxf6 Nxf6 Ng5 Ng4 Nxe6 Qxe6 f3 Ne3 Qe1 Nxf1 Qxf1 Bf6 Qf2 Rdg8 f4 exf4 Qxf4 g5 hxg5 Bxg5 Qf5"""
,"""d4 d5 c4 c6 cxd5 cxd5 Nc3 Nf6 Bf4 e6 Nf3 Bb4 e3 Bxc3+ bxc3 O-O Bd3 Nc6 O-O Bd7 Re1 Ne4 c4 dxc4 Bxe4 Rc8 Qd2 Na5 Bd6 c3 Qc2 Re8 Bg3 Bb5 Bxh7+ Kf8 Bh4 f6 Bg6 Re7 Ne5 fxe5 Bxe7+ Qxe7 dxe5 Qg5 Be4 Rc4 a3 Bc6 Bxc6 bxc6 Rab1 Qxe5 Rb8+ Kf7 Ra8 Rg4 Rxa7+ Kf6 h3 Rg6 Rd1 Qg5 g3 Qc5 Rad7 Qc4 R7d4 Qc5 Qe4 c2 Rc1 Nb3 Rxc2 Qa5 Qf4+ Ke7"""
,"""e4 c5 Nf3 Nc6 Bc4 e6 a3 d5 exd5 exd5 Ba2 Be7 h3 Nf6 d3 O-O O-O Bf5 Bg5 Qd7 Kh2 h6 Bh4 Rae8 Nc3 Qd6+ Bg3 Qd7 Re1 Bd6 Qd2 Bxg3+ fxg3 Qd6 Nh4 Bd7"""
,"""e4 e5 d4 exd4 c3 dxc3 Bc4 cxb2 Bxb2 Bb4+ Nc3 Bxc3+ Bxc3 Qe7 f3 Nf6 Ne2 O-O e5 Ne8 Qd4 Nc6 Qe4 d6 Bd3 g6"""
,"""e3 b6 c3 Bb7 d4 e6 Nd2 Nc6 f3 Qe7 Bd3 h5 Ne2 h4 Nb3 h3 g3 O-O-O Bd2 Nf6 Qc2 Nh5 Rg1 f5 O-O-O g5 g4 fxg4 fxg4 Ng7 Ng3 Nb4 cxb4 Bg2 Be4 Kb8 Bxg2 hxg2 Rxg2 e5 Nf5 Nxf5 gxf5 exd4 Nxd4 Qf7 Kb1 Bd6 Rxg5 Rxh2 Nf3 Rf2 Nd4 Bxb4 Nb3 Be7 Rg6 a5 a3 Rf8 Nd4 Qd5 Qb3 Qe4+ Ka2 Bc5 Bxa5 Bxd4 Rxd4 Qxe3 Qxe3"""
,"""e4 e6 d4 d5 e5 c5 c3 Nc6 Be3 Qb6 b3 cxd4 cxd4 Bb4+ Bd2 Nge7 Bxb4 Qxb4+ Qd2 Qxd2+ Nxd2 Nxd4 Ngf3 Nc2+ Kd1 Nxa1 Bb5+ Nc6 Ng5 O-O Ke2 a6 Rxa1 axb5 Ndf3 f6 exf6 gxf6 Nh3 e5"""
,"""e4 e5 Nc3 Nc6 Bc4 Nf6 Nf3 h6 d3 Nd4 Nxd4 exd4 Nd5 Nxd5 Bxd5 c6 Bc4 d5 exd5 cxd5 Bb5+ Bd7 a4 Bxb5 axb5 Bc5 Qe2+ Be7 c3 dxc3 bxc3 O-O Qg4 Bf6 Bxh6 Bxc3+ Ke2 Bxa1 d4 Bxd4 Qxd4 f6 Qg4 Rf7 Ra1 Qe7+ Kf3 Qe4+ Qxe4 dxe4+ Kxe4 Re8+ Kf5 gxh6 Rxa7 Re5+ Kg6 Rg7+ Kxf6 Rxb5 Ra8+ Kh7 g4 Rb2 f4 Rxg4 Re8 Rxf4+ Ke6 Re4+ Kf7 Rxe8 Kxe8 Rxh2"""
,"""e4 e5 f4 exf4 Nf3 Be7 h4 d6 d4 Bg4 Bxf4 Nc6 Qd3 Qd7 Nc3 O-O-O O-O-O Bxf3 gxf3 Kb8 Bh3 Qe8 a3 Bf6 d5 Ne5 Bg5 Nxd3+"""
,"""d4 d5 c4 dxc4 e3 b5 Nc3 a6 a4 b4 Nb1 Bb7 Bxc4 Bxg2 Nf3 Bxh1 Kd2 Bg2 Ne5 e6 Qg1 Bb7 Qg4 Nf6 Qf4 Ne4+ Ke1 g5 Qxf7#"""
,"""e4 e5 c4 Nc6 Nf3 Nf6 Nc3 Bc5 a3 O-O b4 Bd4 Nxd4 Nxd4 Nd5 Nxd5 exd5 d6 d3 Bf5 f4 exf4 Bxf4 Re8+ Be2 Nxe2 Rf1 Nc3+"""
,"""e4 e5 Nc3 Nc6 Bb5 f5 f3 Bc5 Bxc6 bxc6 exf5 d6 Nge2 Bxf5 b3 Nf6 Bb2 O-O d3 Bb4 Qd2 c5 O-O-O d5 Qg5 d4 Nb5 a6 Qxf5 axb5 c4 Rxa2 Qxe5 Ra1+ Kc2 Ra2 Qe6+ Kh8 cxb5 Ba3 Ra1 Nd5 Rxa2 Ne3+ Kd2 Bxb2 Rxb2 Qg5 Kc1"""
,"""e4 e5 Bc4 Nf6 Nf3 Nc6 Ng5 d5 exd5 Nxd5 Bxd5 Qxd5 Qf3 Be6 Nc3 Qd7 Nxe6 Qxe6 Nb5 O-O-O Qe3 Kb8 O-O f5 Qb3 Qg6 Qc4"""
,"""e4 e5 Bc4 d6 d3 Be6 Bb3 Nf6 Be3 Be7 h3 Nc6 Nf3 O-O O-O a6 c4 Qd7 Ba4 Qc8 Qd2 Nh5 Nc3 f5 Nd5 Bd8 Bg5 f4 Bxd8 Nxd8 b4 Bxh3 gxh3 Qxh3 Nh2 Ne6 Qe2 Ng5 Ne7+"""
,"""c4 Nf6 d4 d5 Nc3 dxc4 e3 b5 a4 c6 Qf3 e6 axb5 cxb5 Nxb5 Nd5 Bxc4 Bb4+ Bd2 Bxd2+ Kxd2 Bb7 Qg4 O-O Ne2 a6 Nbc3 Nxc3 Nxc3 Qd6"""
,"""e4 e5 Nf3 Nc6 Bc4 h6 c3 d6 h3 Nf6 d3 Qe7 O-O Be6 Bb5 Bd7 Re1 a6 Ba4 b5 Bc2 Be6 d4 exd4 cxd4 Nb4 d5 Nxc2 Qxc2 Bc8 Nc3 Qd7 e5 dxe5 Rxe5+ Be7 Qe2 Qd6 Bf4 Nh5 Rxh5 Qxf4 Re5 Qf6 Re1 O-O Rxe7 c6 dxc6 Qxc6 Nd4 Qc5 Qe4 Be6 Nxe6 Qxe7 Nxf8 Qxe4 Rxe4 Kxf8 f3 Rd8 Kf2 Rd2+ Re2 Rd4 Ne4 Rd5 Rd2 Re5"""
,"""e4 c5 Nf3 g6 d4 cxd4 Nxd4 Bg7 Nc3 Nc6 Nb3 Nf6 Be2 O-O O-O a5 a4 d6 Be3 Nb4 f4 Be6 Nd4 Rc8 Nxe6 fxe6 Qd2 d5 e5 Ne4 Nxe4 dxe4 c3 Nd3 Rad1 g5 Bxd3 exd3 Qxd3 gxf4 Qxd8 Rfxd8 Rxd8+ Rxd8 Bxf4 Rd5 Re1 Kf7 g4 Kg6 h3 h5 Kg2 hxg4 hxg4 Rd3 Kf2 Bh6 Bxh6 Kxh6 Rh1+ Kg5 Ke2 Rg3 Rh7 Rg2+ Kd3 Rxb2 Rxe7 Kxg4 Rxe6 Kf5 Re7 Ra2 Kd4 Rxa4+ Kd5 b5 Rf7+ Kg6 e6 Rc4 Rb7 Rxc3 Rxb5 a4 Kd6 a3 Ra5 Re3 Ke7 Kg7 Ra6 Kg6 Ke8 Kf6 e7+ Kg7 Ra8 Kf6 Ra6+ Kg7 Kd8 Kf7 Ra7 a2 e8=Q+ Kf6"""
,"""e4 d6 d4 Nf6 Bd3 e5 dxe5 dxe5 Bg5 Bg4 Bxf6 Bxd1 Bxd8 Kxd8 Kxd1 Bd6 Nc3 f6 Ke2 Nc6 Rd1 Nd4+ Ke1 a6 Nge2 c5 Nxd4 cxd4 Nd5 Kd7 c3 dxc3 Nxc3 Rac8 Bc2 Ke6 Bb3+ Ke7 Ke2 Bc5 Nd5+ Ke8 Rd2 Bd4 Rhd1 h5 Rxd4 exd4 Rxd4 h4 Nf4 g5 Ng6 Rh6 Be6 Rc2+ Kd3 Rxf2 Bf5 Rxg6 Bxg6+ Ke7 Ke3 Rxg2 Rc4 Rxb2 Rc7+ Kd6 Rf7 Ke5 Re7+ Kd6 Rf7 Ke6 Rf8 Rxa2 Bf5+ Ke7 Rb8 Ra3+ Kd4 Rb3 Kc4 Rb1 Rc8 a5 Rc7+ Kd6 Rd7+ Ke5 Re7+ Kf4 Re6 Rc1+ Kb3 b5 Rxf6 a4+ Ka3 Ra1+ Kb4 a3 Ra6 a2 Kb3 Rh1 Kxa2 Rxh2+ Kb3 Ke5 Kb4 Kd4 Kxb5 Rb2+ Kc6 Rc2+ Kd7 Rd2 Ra5 Ke3+ Rd5 Kf4 Be6 Kg3"""
,"""e4 e5 d3 Nf6 Nf3 Nc6 Nbd2 d5 Qe2 d4 a3 Bg4 h3 Bxf3 Nxf3 Be7 b3 O-O Bb2 a5 g3 a4 b4 Na7 Bg2 b5 Nxe5 c5 bxc5 Bxc5 Nf3 Re8 O-O Nc6 c3 dxc3 Bxc3 Nd4 Nxd4 Bxd4 Bxd4 Qxd4 Rab1 Rad8 Rfd1 Re5 Rb4 Qc3 Qb2 Qxb2 Rxb2 Nxe4 Bxe4 Rxe4 dxe4 Rxd1+ Kg2 Rd4 Kf3 b4 axb4 g6 b5 a3 Ra2 Rb4 Rxa3 Rxb5 Kg2 Rb2 Re3 Kg7 e5 f6 exf6+ Kxf6 Rc3 h5 Kf3 Ra2 Rc4 Ra5 Kf4 Rf5+ Ke3 Re5+ Kf3 Rf5+ Kg2 Rd5 g4 hxg4 hxg4 g5 Kg3 Rd3+ f3 Ra3 Rc6+ Kg7 Rc5 Kf6 Rf5+ Kg6 Kf2 Ra2+ Ke3 Ra3+ Ke4 Ra4+ Ke3 Ra3+ Kf2"""
,"""e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 Qe7 Nxf7 Rg8 d3 h6 O-O a6 Nc3 Na5 Bd5 Nxd5 Nxd5 Qc5 b4 Qc6 bxa5 Kxf7 Qh5+ Qg6"""
,"""e4 e5 d4 d6 d5 c6 Nf3 Nf6 Nc3 cxd5 exd5 Na6 Bxa6 bxa6 O-O Bb7 Re1 e4 Nd2 Qa5 Ndxe4 Nxe4 Rxe4+ Be7 Bg5 O-O Bxe7 Rfe8 Qe1 Bxd5 Nxd5 Qxd5 c4"""
,"""d4 d5 c4 e5 Nc3 Bb4 e3 exd4 Qxd4 Nf6 Bd2 c5 Qe5+ Be6 cxd5 Nxd5 Nxd5 Bxd2+ Kxd2 O-O Bc4 Nc6 Qe4 Nb4 Nf3 Nxd5 Ke1 Qa5+ Kf1 Rad8 Ng5 Nf6 Qc2 Rd2"""
,"""e4 c5 Nf3 Nc6 Nc3 e5 Bb5 d6 Nd5 a6 Ba4 b5 Bb3 c4 d3 cxb3 axb3 Be6 Ne3 Nf6 O-O Be7 h3 O-O c3 Qd7 c4 Bxh3 gxh3 Qxh3 Nh2 Nh5 Qf3 Qh4 Nf5 Qf6 Qxh5 g6 Nxe7+ Nxe7 Qg4 Qg7 Bg5 Nc6 Nf3 f5 exf5 gxf5 Qg2 Kh8 Kh1 Rg8 Rg1 Qd7 Qh3 Rg6 Nh4 Kg8 Qxf5 Qxf5 Nxf5 Rf8 Nh6+ Kh8 Rxa6 Nd4 cxb5 Nf3 Rg2 Nxg5 Rxg5 Rxg5 Rxd6 Rg6 Rxg6 hxg6 Kg2 Kg7 Ng4 Re8 b6 Kf7 b7 Ke6 Ne3 Rb8 Nc4 Kd5 Kf3 Rxb7 Ke3 Rxb3 f3 g5 Kd2 Rb8 Ke3 Rf8 b3 Rb8 Nd2 Rb4 Ke2 Kd4 Kd1 Kc3 Ke2 Kc2 Ke3 g4 fxg4 Rxg4 Nc4 Rg3+ Ke4 Rxd3 b4 Rb3 Nxe5 Rxb4+ Kd5 Rb5+ Kd4 Kd2 Nc6 Rh5 Ne5 Rh4+ Kd5 Kc3 Nf3 Rh5+ Ke6 Ra5"""
,"""e4 e6 e5 d6 f4 dxe5 Nf3 exf4 d4 g5 Ne5 f6 Qh5+ Ke7 Qf7+ Kd6 Nc4+ Kc6 Na5+ Kd6 b3"""
,"""e4 Nc6 d4 d6 d5 Ne5 Nc3 g6 f4 Nd7 Be3 Bg7 Qd3 c5 Nf3 a6 g4 Qb6 O-O-O Ngf6 e5 Nxg4 h3 Nxe3 Qxe3 dxe5 fxe5 h5 e6 Bh6 exf7+"""
,"""e4 e5 Nf3 Nc6 d4 exd4 Bc4 Be7 Nxd4 Ne5 Bd3 d6 Nc3 Nf6 Be3 O-O O-O d5 exd5 Nxd3 Qxd3 Nxd5 Nxd5 Qxd5 c4 Qd8 Rad1 Bd7 b3 a6 Nf5 Bxf5 Qxf5 Qc8 Qxc8 Raxc8 Bf4 Rfd8 Rfe1 Kf8 f3 c6 Kf2 Rxd1 Rxd1 Ke8 Ke2 g6 Be5 f6 Bc3 Rd8 Rxd8+ Kxd8 g4 Kd7 Kd3 Ke6 Ke4 f5+ gxf5+ gxf5+ Ke3 Bd6 h3 Be7 a4 Bg5+ Kd3 Bf4 Bg7 Bd6 Bh6 Kf7 Be3 Kg6 Ke2 Kh5 Kf2 Kh4 Kg2 Be5 Bf2+ Kh5 Be3 Kg6 Kf2 Kf7 Ke2 Ke6 Kd3 Bd6 Bg5 Bc7 Bh6 Kf6 b4 Kg6 Be3 Kh5 Bf2 b5 axb5 axb5 cxb5 cxb5 Kd4 Bb6+"""
,"""e4 c6 d4 d5 exd5 cxd5 c4 Nf6 Nf3 e6 a3 Be7 Nc3 O-O b4 Nc6 c5 Ne4 Bb2 Ng5 Be2 Nxf3+ Bxf3 Bf6 Ne2 Bd7 b5 Qa5+ Nc3 Bxd4"""
,"""e4 e5 Nf3 Nc6 Bc4 Nf6 Nc3 Nxe4 Bxf7+ Kxf7 Nxe4 Be7 d4 exd4 Nxd4 Rf8 Nxc6 dxc6 Qxd8 Bxd8 Ng5+ Kg8 O-O h6 Nf3 Bf6 c3 g5 Be3 Bf5 Bd4 Bxd4 Nxd4 Bd7 Rfe1 Rae8 Rad1 c5"""
,"""e4 e5 Nf3 Nc6 Bc4 Nh6 O-O Bc5 c3 d6 g4 Nxg4 h3 Nxf2 Rxf2 Bxh3 Kh2 Bxf2 Kxh3 Qd7+ Kg2 Bb6 Nh4 O-O-O a4 a6 b4 g5 Nf5 h5 Nh6 Rxh6 Qe1 Qg4+ Kh1 Qf3+ Kh2 g4"""
,"""e4 e5 Nf3 d6 Bc4 h6 d4 Bg4 dxe5 Bxf3 Qxf3 Qe7 exd6 cxd6 O-O Nf6 Nc3 g5 Qf5 Nbd7 Nd5"""
,"""e4 e6 d4 d5 Nd2 Nf6 e5 Nfd7 Bd3 c5 c3 Nc6 Ne2 cxd4 cxd4 Be7 O-O O-O f4 f6 Nf3 f5 a3 a6 h3 b5 g4 g6 Kh2 Kh8 Rg1 Na5 b3 Nc6 Bd2 b4 a4 a5 gxf5 gxf5 Ng5 Bxg5 Rxg5 Rg8 Qg1 Rxg5 fxg5 Nf8 Nf4 Nxd4 Qxd4 Qxg5 Rg1 Qh6 Nxd5 Qxd2+ Rg2 Qc1 Nf6 Ng6 Qd8+ Kg7 Ne8+ Kh6 Qf6 Qf4+ Kh1 Qc1+ Kh2 Qf4+ Kg1"""
,"""d4 d5 c4 c6 Nf3 Nf6 e3 a6 Nbd2 Bg4 Qc2 e6 Bd3 Bd6 b3 Nbd7 Bb2 b5 Ne5 Bxe5 dxe5 Ng8 Bxh7 Ne7 Bd3 Bf5 b4 Qc7 cxd5 exd5 Bxf5 Nxe5 Bh3 Nc4 Nxc4 bxc4 Bxg7 Rg8 Bd4 a5 a3 axb4 axb4 Rxa1+ Bxa1 Qa7 Bd4 Qc7 O-O Ng6 Ra1 Ke7 Bc5+ Kf6 Qc3+ Kg5 Qd4 Kh6 b5 cxb5 e4 c3 exd5 c2 Qe3+ Kh7 d6 Qc6 d7 c1=R+ Rxc1 Nf4 Qxf4"""
,"""e4 c5 f4 Nc6 Bb5 Qc7 d3 Qa5+ Nc3 g6 Bxc6 dxc6 Bd2 Qb6 Be3 Qxb2 Na4 Qb4+ Kf2 Qxa4 c4 Bg7 Rb1 Qxa2+ Ne2 Bg4"""
,"""d4 Nf6 e3 d6 Nf3 g6 Be2 Bg7 O-O a6 a4 O-O c4 Nbd7 Qc2 e5 d5 Nc5 b4 Bf5 Qb2 Nd3 Bxd3 Bxd3 Qc3 Bxf1 Kxf1 e4 Nd4 Nd7 Bb2 Qf6 Nd2 Rae8 Qc2 Qh4 g3 Qxh2 Ke2 Qg2"""
,"""e4 e5 f4 d5 exd5 exf4 Nf3 Nf6 Bb5+ c6 dxc6 Nxc6 O-O Bd6 Re1+ Be6 Ng5 Qb6+ d4 Qxb5 Nxe6 fxe6 Rxe6+ Kd7 Qe1 Rae8 Rxe8 Rxe8 Qf2 g5 Nc3 Qf5 a3 Ne4 Nxe4 Qxe4 c3 Qe1+ Qxe1 Rxe1+ Kf2 Re8 Bd2 h6 b4 b5 a4 a6 axb5 axb5 Ra6 Rb8 d5 Ne7 Ra7+ Kc8 Be1 Nxd5 Kf3 Rb7 Ra6 Rb6 Ra7 Be5 Rh7 Bxc3 Bf2 Rg6 Ke4 Nxb4 Kf5 Rc6 Ke4 Nc2 Kd5 Rg6 Kc5 b4 Kc4 Ne3+ Kb3 Nxg2 Bc5 Ne3 Bb6 Ng4 Bc5 Nxh2 Re7 f3 Rf7 g4 Kc4 g3 Kd5 f2 Bd6"""
,"""e4 e5 Bc4 Nc6 Nh3 d6 Qf3 Qf6 Qc3 h6 Na3 Bxh3 Qxh3 a6 c3 Nge7 Be2 Ng6 d3 Be7 Nc4 O-O Ne3 Nf4 Qf3"""
,"""e4 e5 Bc4 Bc5 Nc3 Nf6 Nf3 d6 h3 Nc6 d3 h6 a3 O-O Na4 a6 Nxc5 dxc5 Nh4 Nxe4 Qh5 Nf6 Qf3 Nd4 Qd1 b5 Ba2 Bb7 O-O e4 c3 Ne6 Nf5 Qxd3 Qxd3 exd3 Be3 c4 b3 Nd5 Bd2 Nb6 a4 bxa4 bxc4 Nc5 Rae1 Rfe8 Rxe8+ Rxe8 Nd4 Ne4 Re1 Kf8 Rd1 c5 Nf5 Nxd2 Rxd2 Re1+ Kh2 Be4 Nd6 Bg6"""
,"""e4 e5 Nf3 Nc6 Nc3 Nf6 Bc4 Bc5 Ng5 O-O O-O d6 Qf3 Nd4 Bxf7+ Rxf7 Qd3 Rf8 Qc4+ d5 Qxc5 h6 Nxd5 hxg5 Nxf6+ Qxf6 Qd5+ Be6 Qxb7 Nxc2 f3 Nxa1 Qxc7 Rac8 Qxg7+ Kxg7 d4"""
,"""e4 c5 d4 cxd4 Qxd4 b6 Nc3 Bb7 Bd2 Nc6 Qe3 Nb4 Bd3 Nf6 a3 Nc6 h3 d6 O-O-O e6 Nf3 Be7 e5 Nd7 exd6 Bxd6 Nb5 Bc5 Qg5 Qxg5 Bxg5 O-O Rhe1 Bxf2 Rf1 Bg3 Be4 h6 Be7 Nxe7 Bxb7 Rad8 Nxa7 Nc5"""
,"""e4 e5 Nf3 Qf6 d4 exd4 Nxd4 Bc5 Nf5 g6 Ne3 Bxe3 Bxe3 Qxb2 Nd2 Qa3 Bc4 d6 O-O Ne7 Bh6 Qc3 e5 Qxe5 Re1 Qh5 Bg7 Rg8 Bf6 Nc6 Bxe7 Nxe7 Nf3 Qc5 Qe2 Be6 Bxe6 fxe6 Qxe6 Rg7 Qf6 Rg8 Qxe7#"""
,"""e4 g6 e5 Bg7 Nf3 c5 Nc3 d6 Bb5+ Nc6 exd6 exd6 O-O Ne7 Re1 O-O d4 Nxd4 Nxd4 Bxd4 Ne2 Bg7 Nf4 a6 Be8 Rxe8 Qxd6 Qxd6 Nxg6 hxg6 Rxe7 Rxe7 Bg5 Rd7 Rd1 Qxd1#"""
,"""d4 d5 e3 e6 Nf3 Nf6 Ne5 Nc6 Nc3 Nxe5 dxe5 d4 exd4 Nd5 Nxd5 exd5 Bb5+ c6 Bd3 Bb4+ Bd2 Be7 O-O O-O Re1 b5 b3 a6 f4 h6 f5 f6 exf6 Bxf6"""
,"""e4 e6 f4 d5 e5 Nh6 d4 Nf5 Nf3 c5 c3 cxd4 cxd4 Nc6 Nc3 Bb4 Bb5 a6 Bxc6+ bxc6 O-O c5 a3 cxd4 axb4 dxc3 bxc3 O-O Nd4 Qb6 Ba3 Ne3 Qe2 Nxf1 Rxf1 a5 b5 Bd7 Bxf8 Rxf8 Kh1 Rb8 f5 exf5 Nxf5 Bxf5 Rxf5 Qxb5 Qf3 Qb1+ Qf1 a4 Rxf7 a3"""
,"""e4 e5 Nf3 d6 d4 Bg4 dxe5 Bxf3 Qxf3 dxe5 Qd3 Bd6 Be3 a6 Be2 Nc6 O-O Nb4 Qc3 h6"""
,"""e4 e5 Nf3 Nf6 d3 d5 exd5 Qxd5 Nc3 Qd6 Be3 Bg4 Qd2 Nc6 Be2 Nd4 Nxd4 exd4 Nb5 Qc6 Nxd4 Qxg2 O-O-O Bxe2 Qxe2 Be7 Rdg1 Qd5 Kb1 c5 Nb5 O-O Nc7 Qe5 Nxa8 Rxa8 Rg5 Qe6 Rhg1 g6"""
,"""d4 a6 e3 b5 Bd3 Bb7 Nf3 e6 O-O c5 c3 c4 Bc2 Qc7 Nbd2 d6 Qe2 Nd7 e4 Be7 e5 d5 h3 h6 Nh2 O-O-O f4 Rf8 f5 Nb6 fxe6 fxe6 Rxf8+ Bxf8 Qg4 Qe7 Ndf3 h5 Qg5 Bc6 Qxe7 Bxe7 Bg5 Be8 Bxe7 Nxe7 Ng5 Kd7 Rf1 g6 Nf7 Rg8 Nd6 Nf5 Bxf5 gxf5 Nf3 Nc8 Nxc8 Kxc8 Nh4 Kd7 Rf3 Rg5 Kf2 a5 a3 a4 Rg3"""
,"""e4 c6 d4 d5 e5 Bf5 f4 e6 Nf3 Qa5+ Nc3 Ne7 Bd3 Bxd3 Qxd3 Qa6 Qxa6 Nxa6 a3 c5 Be3 Nc6 O-O Be7 dxc5 Bxc5 Bxc5 Nxc5 Nb5 O-O Nd6 Rab8 c3 g6 Nd4 Ne7 Rac1 a6 h3 Nf5 N4xf5 gxf5 Kh2 Ne4 Nxe4 fxe4 g4 Rbc8 Kg2 b5 Kf2 Rc4 Ke3 Rfc8 Rfd1 a5 Rd4 b4 Rxc4 Rxc4 axb4 axb4 Kd2 bxc3+ Rxc3"""
,"""e4 e5 Bc4 d6 d3 Nf6 Bg5 Be7 Nc3 Nc6 a3 Na5 Ba2 Bd7 Nf3 h6 Bh4 g5 Bg3 Nh5 Nxg5 Bxg5 Qxh5 Qf6 O-O Qg6 Qxg6 fxg6 h3 O-O-O Nd5 c6 Ne3 Rdf8 b4 h5 bxa5 h4 Bh2 Bf4 Bxf4 exf4 Ng4 g5 f3 Bxg4 fxg4 f3 Rxf3 Rxf3 gxf3 Rf8 Kg2 Rf4 Re1 Kc7 d4 b6 e5 d5 e6 Kd8 e7+ Ke8 c3 b5 Bb1 Rf6 Bf5 a6 Bd7+ Kxd7 e8=Q+ Kd6 Qe7#"""
,"""d4 c5 e3 d5 Nf3 Bg4 c3 Nf6 h3 Bxf3 Qxf3 e6 Bb5+ Nc6 Bxc6+ bxc6 O-O Qb6 Nd2 Be7 dxc5 Bxc5 e4 O-O e5 Nd7 Re1 Qc7 Nb3 Bb6 Bf4 c5 Bg3 Rae8 Rac1 Qc6 c4 f6 cxd5 Qxd5 Qxd5 exd5 e6 Ne5 Bxe5 fxe5 Rxe5 c4 Na1 Rxf2 Kh1 Rxb2 Nc2 d4 e7 d3 Na3 d2 Rd1 c3 Nc4 Rxa2 Nd6 c2 Ree1 cxd1=Q Rxd1 Rxe7 Kh2 Re1"""
,"""d4 d5 c4 Nf6 Nf3 dxc4 Nc3 c6 e3 b5 Be2 g6 O-O Bg7 Re1 O-O b3 cxb3 axb3 a5 b4 Na6 Ne5 Bb7 h3 axb4"""
,"""e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be3 e6 f3 b5 Qd2 Nbd7 g4 Nb6 g5 Nfd7 h4 Bb7 h5 Rc8 g6 hxg6 Bg2 Rxh5 Rxh5 gxh5 Bh3 Nc5 O-O-O Nc4 Qe2 Nxe3 Qxe3 b4 Nce2 Qa5 Kb1 Be7 Ng3 g6 f4 h4 Nge2 Nxe4 Bg2 Nc5 Bxb7 Nxb7 Nxe6 fxe6 Qxe6 Qc5 Nd4 Na5 Re1 Rc7 Qg8+ Kd7 Qe6+ Kd8 Qg8+ Kd7 Qe6+ Ke8 Qg8+ Kd7"""
,"""e4 e5 d3 d5 exd5 Qxd5 Nc3 Bb4 Nf3 Bxc3+ bxc3 c5 c4 Qd6 Bb2 Nc6 Be2 Bf5 Nd2 Nf6 Bf3 O-O Bxc6 Qxc6 O-O Rad8 Nf3 Rfe8 Nxe5 Qc7 Nf3 Bg4 Bxf6 gxf6 h3"""
,"""d4 d5 c4 c6 Nc3 dxc4 Nf3 Nf6 Bg5 Bd7 e4 h6 Bh4 g5 Bg3 e6 e5 Nd5 Bxc4 Bg7 Bb3 Qe7 O-O a6 a3 O-O Bc2 f5 Qd3 Bh8 Rae1 f4 Bh4 Nxc3 Qg6+ Qg7 bxc3 Qxg6 Bxg6 gxh4 Nxh4 Kg7 Be4 Be8 Rb1 b5 c4 Ra7 a4 Rc7 cxb5 cxb5 axb5 Bxb5 Ng6 Rff7 Rfd1 Nd7 Nh4 Kg8 d5 Nf8 dxe6 Nxe6 Rd6 Bxe5 Rxe6 Rfd7 Rxe5 Rd2 Re8+ Kf7 Re5 Rcd7 Rf5+"""
,"""e4 e5 Nf3 d6 d4 exd4 Nxd4 Nc6 Nc3 Nf6 Be2 Nxd4 Qxd4 Be7 Nd5 Nxd5 exd5 Bf6 Qe4+ Be7 Bb5+ Bd7 Bxd7+ Qxd7 O-O O-O Bf4 Bf6 c3 Rfe8 Qf3 Qf5 Rfe1 h6 Qg3 Bg5 Bxg5 hxg5 c4 f6 b3 Qc2 Re6 Qf5 Rae1"""
,"""d4 d5 e4 dxe4 Nc3 Nf6 f3 exf3 Nxf3 Bg4 h3 Bxf3 Qxf3 Qxd4 Qxb7 Qh4+ Kd1 e5 Be2 Bb4 Qxa8 O-O Qf3 e4 Qe3 Rd8+ Bd2 Nd5 Nxd5"""
,"""d4 d5 Bf4 e6 e3 Nf6 c4 Be7 Nc3 c6 Qc2 Nbd7 O-O-O h6 Bd3 dxc4 Bxc4 b5 Bd3 b4 Nb1 a5 Nf3 Ba6 Ne5 Bxd3 Rxd3 c5 dxc5 Qc8 Rhd1 Qxc5 Nxd7 Qxc2+ Kxc2 Rc8+ Kb3 Nxd7 Rxd7 Bf6 R7d3 O-O Bd6 Rfe8 a4 Rc6 e4 e5 Nd2 Rec8 Nf3 Rc2 Bxe5 Bxe5 Nxe5 Rxf2 Rd8+ Rxd8 Rxd8+ Kh7 Rd5 f6 Nf3 Rxg2 Rxa5 Rf2 Nd4 Rxh2 Kxb4 Rxb2+ Nb3 Re2 Rh5 Rxe4+ Ka3 g6 Rh1 h5 a5 Re8 Nc5 Ra8 a6 g5 Kb4 h4 Kb5 Kg6 Kb6 Kh5 Nd7 f5 a7 Rxa7 Kxa7 g4 Ne5 h3 Rg1 Kh4 Ng6+ Kg5 Ne5 Kf4 Nd3+ Kf3 Ne5+ Kf2 Rc1 g3 Ng6"""
,"""b3 Nf6 Bb2 Nc6 d4 b6 e3 Bb7 g3 e6 Nf3 Na5 Bg2 h5 O-O h4 Nxh4 Bxg2 Nxg2 a6 h4 Ra7 c4 Qa8 Nc3 d5 cxd5 Nxd5 Nxd5 Qxd5 Qe2 g5 e4 Qd7 d5 exd5 Bxh8 gxh4 exd5+ Kd8 Nxh4 Nb7 Bf6+ Kc8 Rad1 Nd6 Be5 Kb7 Qf3 a5 Bxd6 Bxd6 Qf5 Qe7 Kg2 Ra8 Rh1 Rg8 Rde1 Qf8 Nf3 Qg7 Qd3 Bxg3"""
,"""e4 e6 Nc3 d5 exd5 exd5 d4 Nf6 Be3 c5 Nf3 cxd4 Bxd4 Nc6 Bb5 Bd7 O-O Nxd4 Nxd4 Bxb5 Ncxb5 a6 Re1+ Be7 Nc3 O-O Nf5 h6 Nxe7+ Kh8 Nf5 Re8 Qf3 Rxe1+ Rxe1 Qd7 g4 Re8 Rxe8+ Qxe8 Kg2 Qd7 h3 g6 Ne3 Kg7 Nexd5 Nxd5 Nxd5 Qc6 Nc3 Qxf3+ Kxf3 b5 Ke4 Kf6 Nd5+ Ke6 Nf4+ Kf6 Nd3 Ke6 f4 g5 f5+ Kf6 Nc5 a5 Nb7 a4 Nd6 Ke7 Nxb5 f6 Nd4 Kd6 Ne6 Ke7 b3 Kd6 c4 axb3 axb3 Kc6 b4 Kb6 Nd4 Kc7 Kd5 Kd7 Nb5 Ke7 c5 Kd8 Ke6 Kc8 c6"""
,"""c3 b6 b4 Bb7 Nf3 e6 d4 Nf6 Bg5 h6 Bxf6 Qxf6 Ne5 Nc6 Nxc6 Bxc6 Nd2 Bb7 e4 c5 e5 Qh4 Nf3 Qe4+ Be2 cxb4 O-O bxc3 Bd3 Qg4 h3 Bxf3 Qxf3 Qxf3 gxf3 Bb4 Be4 Rc8 Bb7 Rc7 Be4 d5 Bc2 Rc4 Ba4+ Ke7 Bb5 Rxd4 Rac1 Rc8 a4 Rf4 Kg2 d4 Rfd1 Bc5 Ba6 Rd8 Bb5 Rd5 Bc6 Rxe5 Be4 Rexe4 fxe4 Rxe4 Kf3 f5 Rg1 g5 h4 Kf6 hxg5+ hxg5 Rge1 Rxe1 Rxe1 e5 Ra1 d3 a5 c2 Rc1 d2 Rxc2 d1=Q+"""
,"""e4 b6 Nc3 Bb7 g3 f5 Bg2 Nf6 d3 e6 Nge2 Bb4 f3 O-O Be3 d6 O-O Nbd7 exf5 exf5 f4 Bxg2 Kxg2 Re8 Bg1 Ng4 a3 Bxc3 Nxc3 Ne3+ Bxe3 Rxe3 Re1 Qe7 Rxe3 Qxe3 Qe2 Re8 Re1 Qxe2+ Rxe2 Rxe2+ Nxe2 Kf7 Kf3 c6 Nd4 Nc5"""
,"""e4 e5 Nf3 d6 h3 Be7 Bc4 Bf6 Nc3 c6 O-O Ne7 d4 exd4 Nxd4 Ng6 Re1 O-O b3 b5 Be2 a6 Bb2 c5 Nf5 Bxf5 exf5 Nf4 Bf3 Ra7 Rb1 Re7 Rxe7 Qxe7 Qd2 Be5 Kh1 Qf6 Nd1 Qxf5 Ne3 Qg5 Bxe5 Qxe5 Re1"""
,"""d4 Nf6 d5 e6 c4 Bb4+ Bd2 c5 Bxb4 cxb4 a3 exd5 cxd5 Qa5 Qd2 Na6 a4 Nxd5 b3 O-O e3 Nc3 Bxa6 bxa6 Nxc3 bxc3 Qd3 Bb7 f3 Rfe8 e4 d5 Ne2 dxe4 fxe4 c2+ Qd2 Qxd2+ Kxd2 Bxe4 Rac1 Rac8 Ng3 Bg6 Rhe1 Rcd8+ Kc3 Rxe1 Rxe1 f6 Rc1 Rd1 Rxc2 Bxc2 Kxc2 Rg1 Nf5 Rxg2+ Kc3 Rxh2 Ne7+ Kf7 Nc6 Rh3+ Kb2 Ke6 Nxa7 Kd7 b4"""
,"""e3 e5 Qf3 Nf6 Nc3 Nc6 Nd5 Nxd5 Qxd5 d6 Bb5 Bd7 Nf3 Be7 h4 O-O Qe4 a6 Bd3 f5 Qc4+ Kh8 Ng5 Bxg5 hxg5 Qxg5 g4 e4 Rh5 Qxg4 Be2 Qg1+ Bf1 Ne5 Qe2 f4 b3 Bg4 Rxh7+ Kxh7 f3 Nxf3+ Kd1 Ne5 Ke1 Bxe2 Kxe2 fxe3"""
,"""e4 c5 Nc3 Nc6 f4 e6 d3 Nf6 Nf3 d5 g3 dxe4 dxe4 Qxd1+ Kxd1 Bd7 Bg2 a6 Ke2 O-O-O Be3 b5 h3 Bd6 e5 Nd5 Nxd5 exd5 exd6 Bf5 Bxc5 Bxc2 Ne5 Nxe5 fxe5 f6 e6 Be4 Bxe4 dxe4 Rac1 Kb7 Rhd1 Kc6 Bb4+ Kb6 d7 a5 Bd6 g5 Bc7+ Ka6 Bxd8 Rxd8 Rc8"""
,"""e4 e5 c3 d6 d4 exd4 Qxd4 Nc6 Qd1 Qe7 Bd3 Nf6 Bg5 Bd7 Qb3 O-O-O a4 h6 Be3 Nxe4 Bxe4 Qxe4 Nf3 Be6 Qd1 Kb8 O-O Be7 Re1"""
,"""e4 e5 Nc3 d6 Bc4 Be6 Bxe6 fxe6 f4 exf4 d4 e5 dxe5 dxe5 Qxd8+ Kxd8 Nf3 Nc6 Bd2 Bd6 O-O-O Ke7 Nd5+ Kd7 Bc3 Rd8 Nxf4 exf4 e5 Nxe5 Nxe5+ Kc8 Nf7 Ne7 Nxh8 Rxh8 Bxg7 Rg8 Bf6 Nc6 Rhg1 Rg6 Bc3 Ne7 Bd2 Nf5 Rgf1 Rxg2 Bxf4 Bxf4+ Rxf4 Ne3 Rf8#"""
,"""e4 e6 Nf3 d5 exd5 exd5 Nc3 d4 Ne2 d3 cxd3 Qxd3 Nc3 Qg6 Qe2+ Be7 Qe4 Qd6 d4 Nf6 Bf4 Qb4 Qe2 Nd5"""
,"""e4 d5 exd5 Qxd5 Nf3 Bg4 Be2 Nc6 O-O O-O-O Nc3 Qh5 d4 e6 Be3 Nf6 a3 Bd6 g3 e5 dxe5 Nxe5 Nxe5 Bxe5 Bxg4+ Nxg4 Qf3 Qxh2#"""
,"""b3 e6 Bb2 b6 c4 Bb7 d4 Nc6 Nc3 d6 e4 Nf6 d5 exd5 exd5 Ne5 Be2 Be7 Nf3 Qd7 Nxe5 dxe5 Qd3 O-O-O Ne4 Nxe4 Qxe4 f5 Qxe5 Bd6 Qc3 Rde8 Kd2 Bf4+ Kd1 Re7 Bf3 Rhe8 Re1 Rxe1+ Qxe1 Rxe1+ Kxe1 Qe7+ Kd1 g6 Kc2 Bg5 Rd1 Bf6 Bc1 Ba1 Bf4 Qf6 Be3 Qc3+ Kb1 Qb2#"""
,"""e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nc3 Bb4 Be3 Bxc3+ bxc3 Nf6 f3 Qa5 Qd2 Ne5 Nb3 Qd8 Bf4 Ng6 Bg3 O-O Bd3 d5 e5 Nh5 Bxg6 Nxg3 hxg3 fxg6 O-O-O a5 Rh3 Qc7 f4 Bd7 Rdh1 h6 Qd3 Rf5 Nd4 Rh5 Rxh5 gxh5 Rxh5 Rc8 Ne2 Qc5 Kd2 Bb5"""
,"""d4 d5 Nc3 Nc6 Nf3 Nxd4 Nxd4 c5 Nf3 d4 Ne4 Bf5 e3 Bxe4 Bd3 Bxf3 gxf3 dxe3 fxe3 Nf6 e4 e5 c3 Bd6 Bg5 O-O Qb3 Rb8 O-O-O b5 Bxb5 Qb6 c4 a6 Bxf6 gxf6 Rhg1+ Kh8 f4 axb5 Qg3 exf4 Qg7#"""
,"""d4 d5 c4 c6 Nc3 Nf6 e3 Bf5 Nf3 e6 Bd3 Bxd3 Qxd3 Bb4 O-O Bxc3 bxc3 a6 Ba3 b5 cxb5 axb5 Bb4 Na6 Ba3 Nc7 Bc5 Ne4 Bb4 Na6 Nd2 Nxd2 Qxd2 Nxb4 cxb4 Qd6 a3 Ke7 Rfc1 Rhc8 Rc5 g6 Qc3 Ra6 Rxb5 Rca8 Rc5 Kd7 g3 Qb8 Qb3 Qb6 a4 Rb8 Rb1 Rba8 b5 Rxa4 bxc6+"""
,"""e4 e6 f4 c5 Nf3 Nc6 c3 Nf6 d3 Be7 Be2 d5 e5 Nd7 O-O O-O d4 cxd4 cxd4 Nb6 Nc3 Nc4 Bd3 h6 Bb1 f6 exf6 Bxf6 Qd3 Kf7 b3 Nd6 Ne5+ Ke7 Ng6+ Kd7 Nxf8+ Qxf8 Be3 Ne4 Nxe4 dxe4 Qxe4 Kc7 Bd3 Bd7 Rac1 Rd8 b4 Kb8 b5 Ne7 a4 Nd5 Bc4 Re8 Bd2 Nb6 Bb3 Qd6 Be3 Nd5 Kh1"""
,"""d4 d6 c4 e6 Nc3 Nf6 h3 Bd7 a3 Be7 Nf3 c6 g4 d5 g5 Ne4 Nxe4 dxe4 Qc2 f5 h4 exf3 exf3 g6 h5 Kf7 hxg6+ hxg6 Rxh8 Qxh8 Be3 Na6 O-O-O Nc7 Bg2 Rc8 Rh1 Qg8 Rh6 Nd5 Bd2 Nb6 Bf1 Qg7 f4 Bf8 Bc3 Qg8 d5 Bxh6 gxh6 cxd5 f3 Rxc4 Qh2 Qh7 Bxc4 Nxc4 Kc2 Bc6 b3 Nd6 a4 Ne8 Be5 Nf6 Bc3 Nh5 Qh4 Qxh6 Qf6+ Nxf6 Bxf6 Kxf6"""
,"""f4 e6 e4 d5 e5 f6 d4 Nc6 c3 fxe5 fxe5 Be7 Qh5+ g6 Qf3 Bg5 Bxg5 Qxg5 Na3 b6 Nh3 Qe7 Bd3 Bb7 O-O Nh6 Qf6 O-O-O Qxe7 Nxe7 Ng5 Nef5 Nxe6 Rde8 Ng5 Kb8 Rae1 Ne7 h3 Ref8 Ne6 Rxf1+ Rxf1 Nhf5 g4 Nh6 Kg2 Bc8 Ng5 Nc6 Nc2 Nd8 b4 Ne6 Nxe6 Bxe6 Rf6 Re8 Bb5 Re7 Bc6 Bf7 a4 Ng8 Rf2 h5 a5 hxg4 hxg4 Nh6 Kg3 Be6 Rf8+ Bc8 a6"""
,"""d4 b6 e4 Bb7 Nc3 e6 Nf3 Bb4 Bd3 Ne7 a3 Bxc3+ bxc3 f5 exf5 Nxf5 O-O O-O Re1 Nc6 Bg5 Qe8 Bc4 h6 Bh4 Nce7 Bg3 Ng6 Ne5 d5 Bd3 c5 Qg4 Nxe5 Bxe5 Qf7 a4 c4 Be2"""
,"""d4 d5 c4 Nf6 cxd5 Nxd5 e4 Nb6 Nc3 e6 Nf3 Nc6 a3 g6 Bg5 f6 Bh4 Be7 Bb5 Bd7 Bxc6 Bxc6 Qd2 Qd7 Rc1 Nc4 Qe2 Bb5"""
,"""d4 c5 dxc5 Qa5+ Nc3 Qxc5 Bf4 g6 e3 Bg7 Nge2 e6 Qd3 Ne7 Nd4 a6 Be2 e5 Ne4 Qa5+ Nc3 exd4 exd4 Nbc6 d5 Nb4 Qe3"""
,"""d4 Nf6 Nc3 g6 e4 d6 Bd3 Bg7 Be3 O-O Nf3 Bg4 h3 Bxf3 Qxf3 Nbd7 O-O-O c5 dxc5 Nxc5 Bc4 Qc7 Nb5 Qc8 e5 dxe5 g4 a6 Nc3 e4 Qg3 Na4 Bb3 Nxc3 bxc3 Qxc3 g5 Nh5 Qg2 Rfd8 Kb1 Qb2#"""
,"""e4 c5 d3 d6 Nf3 Nf6 Nc3 Nc6 Nd5 Nxd5 exd5 Ne5 Nxe5 dxe5 c4 e6 Be2 exd5 cxd5 Qxd5 O-O e4 Be3 Bd6 d4 cxd4 Bxd4 Be5 Qa4+ Bd7 Bc4 Bxa4 Bxd5 Bxd4 Bxb7 Rb8 Bxe4 Bxb2 Rab1 O-O h3 Rfc8 Bf5"""
,"""e4 e6 Nf3 Qf6 d4 Nc6 e5 Qf5 Bd3 Qg4 O-O g5 c3 Qh5 Be2 g4 Ng5 f6 Bxg4 Qg6 Bh5 Nce7 Bxg6+ Nxg6 Nf3 Bh6 Bxh6 Nxh6 Qd2 Ng4 h3 fxe5 hxg4 exd4 Nxd4 Rf8 Qh6 Nh4 Qxh4 Rf4 Qh5+ Rf7 Nd2 c5 Nb5 a6 Nd6+ Ke7 Qxf7+ Kxd6 Ne4+ Kd5 f3 a5 Rad1+ Ke5 Qg7+ Kf4 g3+ Ke3 Rfe1+ Kxf3 Qf6+ Kxg4 Qg5+ Kf3 Rd3#"""
,"""d4 e6 c4 d5 Nc3 Nf6 e3 a6 Nf3 c5 cxd5 exd5 a3 c4 Be2 Bf5 O-O b5 Nh4 Bg6 Nxg6 hxg6 f3 Bd6 e4 Bxh2+ Kf2 dxe4 fxe4 Nc6 Be3 Qc7 Bf3 Bg3+ Ke2 O-O-O e5 Nxd4+ Bxd4 Bxe5"""
,"""e4 d6 Nc3 g6 d4 Bg7 h3 Nf6 Bg5 O-O Bd3 Nbd7 Qd2 c5 dxc5 Nxc5 Bh6 Qb6 Bxg7 Kxg7 b3 Bd7 h4 Bc6 f3 Rac8 h5 Nxd3+ cxd3 Qd4 hxg6 fxg6 Nge2 Qe5 f4 Qe6 f5 gxf5 Qg5+ Kh8"""
,"""c4 Nc6 Qa4 Nf6 b4 Ne5 e3 e6 c5 b6 Bb2 Neg4 cxb6 cxb6 Nf3 Ne4 b5 Bb7 h3 Nh6 d3 Nf6 g4 Nhg8 Bg2 Qe7 Rg1 Qb4+ Qxb4 Bxb4+ Nc3 Nd5 Rc1 Ngf6 Kf1 O-O Nxd5 Nxd5 Ne5 Rad8 e4 Nf4 Rd1 f6 Nc4 d5 Ne3 dxe4 Bxe4 Bxe4 dxe4 Rxd1+ Nxd1 Nxh3 Rg3 Nf4 g5 fxg5 Rxg5 Rd8 Rxg7+ Kf8 Ne3 Bd2 Rxh7 Kg8 Rh8+"""
,"""e4 e5 Nc3 Nf6 Qf3 b6 Qg3 Nc6 d3 Nd4 Qxe5+ Ne6 Nd5 d6 Nxf6+ Qxf6 Qb5+ Bd7 Qc4 b5 e5 Qxe5+ Qe4 Qxe4+ dxe4 Nd4 Bd3 c5 c3 Ne6"""
,"""d4 d5 e3 Nc6 Bb5 a6 Bxc6+ bxc6 Nf3 e6 Ne5 c5 c3 f6 Qh5+ g6 Nxg6 Bg7 Nxh8+ Kd7"""
,"""e4 c5 f4 Nc6 Nf3 g6 Be2 Bg7 O-O Nf6 c3 d6 d3 O-O Ng5 Bd7 Be3 b5 Nd2 Rb8 f5 b4 fxg6 fxg6 c4 e5 Rb1 a5 b3 Nd4 Bxd4 cxd4 Ngf3 Qe8 Rc1 a4 c5 axb3 Qxb3+ Kh8 c6 Be6 Qb2 Ng4 c7 Rc8 Nc4 Bxc4 Rxc4 Ne3 Rcc1 Nxf1 Bxf1 Qe7 Qxb4 Rf7 a4 Rxc7 Rxc7 Qxc7 Ng5 Rf8 Ne6 Rxf1+ Kxf1 Qf7+ Kg1 Qxe6 Qb8+ Qg8 Qxg8+ Kxg8 a5"""
,"""e4 c5 f4 e6 Nf3 a6 c4 d5 cxd5 exd5 exd5 Qxd5 Nc3 Qe6+ Qe2 Nf6 d4 cxd4 Nxd4 Qxe2+ Bxe2 Bd6 Nf5 Bc5 Nxg7+ Kf8 Nh5 Nxh5 Bxh5 Bf5 g4 Bd7 f5 Nc6 Bh6+ Ke7 Kd2 f6 Rhe1+ Kd6 Bf4+ Ne5 Ne4+ Kd5 Nxf6+ Kd4 Bxe5+ Kc4 Bf7+ Kb5 a4+ Kc6 Nxd7 Kxd7 Bxh8 Rxh8 Kc3 a5 Red1+ Kc7 Kc4 b6 Be6"""
,"""d4 d5 Nc3 Bd7 Nf3 e6 Bf4 Bb4 Qd2 Nc6 a3 Bxc3 bxc3 Na5 Rb1 Nc4 Qd3 Nxa3 Rxb7 Rb8 Rb3 Bb5 c4"""
,"""e4 e5 Nf3 Bc5 Be2 Nc6 O-O Nf6 d3 h6 c3 d5 exd5 Nxd5 Nxe5 Nf6 Nxc6 bxc6 d4 Bd6 Re1 Kf8 Bf3 Bb7 Nd2 Rb8 Nc4 g5 Ne5 Nd5 g3 Qf6 Bxd5 Bxe5 dxe5 Qe7 Bg2 h5 Be3 h4 Qg4 hxg3 fxg3 Re8 Bxg5 Qc5+ Qd4 Qb5 Qb4+ Qxb4 cxb4 Rg8 Bf6 Bc8 Bxc6 Rxg3+ hxg3"""
,"""d4 d5 c4 dxc4 e4 b5 a4 c6 axb5 cxb5 b3 Qc7 bxc4 bxc4 Qa4+ Bd7 Qxc4 Qxc4 Bxc4 e6 Be3 a5 d5 Bb4+ Nd2 Nf6 dxe6 Bxe6 Bxe6 fxe6 f3 O-O Ke2 Nc6 Nh3 Rfd8 Rhd1 e5 Nc4 h6 Bb6 Nd4+ Bxd4 Rxd4 Rxd4 exd4 Kd3 Bc3 Ra4 Nd7 Ra2 Nc5+ Kc2 Bb4 Nf4 g5 Nd3 Nxd3 Kxd3 Bc3 Ra4 Kg7 Nd6 Rb8 Nf5+ Kg6 Nxd4 Bxd4 Kxd4 Rb2 Rxa5 Rxg2 Ra6+ Kg7 e5 Rxh2 Ra7+"""
,"""d3 Nc6 e3 Nf6 Nf3 e6 Be2 Ne5 O-O Nxf3+ Bxf3 c6 d4 d5 e4 Nxe4 Bxe4 dxe4 Re1 f5 c4 Bd7 Nc3 Qb8 Nxe4 Bd6 Ng5 Bxh2+ Kh1 h6 Nxe6 Bxe6 Rxe6+ Kf7 Qh5+ Kxe6 Qxh2 Qxh2+ Kxh2 f4 Bxf4 Kf5 g3 Rae8 Rf1 Re2 d5 cxd5 cxd5 Rxb2 d6 Rd8 Re1 Rxa2 Re5+ Kf6 Kh3 Rxf2 Re7 Kg6 Rxb7 Rxf4 gxf4 Rxd6 Rxa7 Rd3+ Kg4 Rd4 Ra6+ Kh7 Kf5 Rd5+ Kg4 h5+ Kh4 g6 f5 Rd4+ Kg5"""
,"""e4 d6 d4 Nf6 Nc3 g6 Nf3 Bg7 Be3 c6 Bd3 O-O a3 b5 h4 Bg4 Be2 a5 Qd2 Nbd7 O-O b4 axb4 axb4 Rxa8 Qxa8 Nd1 Nxe4 Qd3 d5 Nh2 Bxe2 Qxe2 Nb6 c3 Nc4 cxb4 Rb8 h5 Rxb4 hxg6 fxg6 Qf3 Qf8 Qxf8+ Kxf8 Nf3 Nxb2 Nxb2 Rxb2 Ra1 Rb8 Ra3 h6 Ra6 Rc8 Nh4 Kf7 Ra7 Nd6 Nf3 Ke6 g3 Nf5 Kg2 g5 g4 Nd6 Kg3 Ne4+ Kg2 Bf6 Nh2 Nd6 Kf3 Nb5"""
,"""d4 g6 c4 d6 e4 Nf6 Nc3 Bg7 Bg5 O-O e5 dxe5 dxe5 Qxd1+ Rxd1 Nfd7 Bxe7 Re8 Nd5 Bxe5 Nxc7 Bxc7 Nf3 Rxe7+ Be2 Nc6 Rd2 Nde5 Nxe5 Rxe5 O-O Bf5 f4 Re4 Bd3 Rd4 Rfd1 Nb4 Bxf5 Rxd2 Rxd2 gxf5 Rd7 Bxf4 Rxb7 Nxa2 b3 Kg7 Kh1 Rb8 Rxa7 Nc3 g3 Bd6 Ra6 Be5 Ra5 Bd4 Rxf5 Rxb3 Rf4 Be3 Rf3 Bc5 Rf5 Ne4 Re5 Nf2+ Kg2 Ng4 Rxc5 Rb2+ Kh3 Nf2+ Kg2 Nd3+ Kf3 Nxc5 Ke3 Rxh2 Kd4 Rg2 Kxc5 Rxg3 Kb4 Rg4 Kb5 Rxc4 Kxc4 h5 Kd5 Kf6 Ke4 Kg5 Kf3 Kh4 Kf4 f6 Kf5 Kg3 Kxf6 h4 Kg5 h3 Kh5 h2 Kg5 h1=Q Kf5 Qh4 Ke5 Qg4 Kd5 Qf4 Ke6 Qg5 Kd6 Qf5 Kc6 Qe5 Kb6 Qd5 Kc7 Qe6 Kb7 Qd6 Ka7 Qc6 Kb8 Qd7 Ka8 Kf4 Kb8 Ke5 Ka8 Kd6 Kb8 Kc6 Ka8 Kb6 Kb8 Qb7#"""
,"""e4 e5 Nf3 d6 Bb5+ c6 Ba4 Qf6 O-O Bg4 d3 Bxf3 Qxf3 Qxf3 gxf3 Nd7 f4 O-O-O f5 g6 Bg5 f6 Bh4 gxf5 exf5 Nh6 Nd2 Be7 Ne4 Nxf5 Bg3 Rdg8 Kh1 b5 Bb3 Nd4 c3 Ne2 a4 f5 Bxg8 Rxg8 Nd2 f4 axb5 fxg3 fxg3 Kb8 bxc6 Nc5 Rf7 Re8 Re1 Nxg3+ hxg3 Nxd3 Nc4 Nxe1 Rxh7 Nf3 Kg2 Ng5 Rh6 Nf7 Re6 Kc7 Na5 Ng5 Rg6 Kb6 b4 Ne4 c4 Nd2 Rg4 Rf8 Nb3 Nxb3"""
,"""e4 e6 Nf3 d5 exd5 Qxd5 Nc3 Qd6 d4 Nc6 Nb5 Qb4+ c3 Qa5 b4 Qb6 Be3 Nxd4 Bxd4 Qc6 Ne5 Qe4+ Be2 Qxg2 Bf3 Qg5 Nxc7+ Kd8 Nxa8 Ne7 Be3+ Ke8 Bxg5 f6 Bxf6 gxf6 Nc7#"""
,"""e4 d6 d4 Nf6 f3 g6 Bd3 Nc6 Ne2 Nb4 Bc4 Bg7 c3 Nc6 O-O O-O Be3 Na5 Bd3 b6 b4 Nb7 a4 c5 Ra3 Nd7 Nd2 e5 d5 Nf6 b5 Na5 c4 Qe7 g4 h6 h4 Nxg4 fxg4 Qxh4 Nf3 Qxg4+ Kf2 f5 Rg1 Qh5 Ng3 Qh3 Bf1 Qg4 Nxf5 gxf5 Rxg4 fxg4"""
,"""e4 e5 c3 Nc6 Qa4 Nf6 d4 exd4 cxd4 Nxe4 h4 Qf6 f3 Ng3 Bg5 Qe6+ Be2 h6 Rh3 Bd6 Qd1 hxg5 Nd2 Qe3 Nf1 Qxg1 Rxg3 Bb4+"""
,"""e4 e5 Nf3 Nc6 Bc4 Bc5 d3 Nf6 Nc3 d6 h3 Be6 Bb3 h6 Qe2 Nd4 Nxd4 Bxd4 Be3 c5 O-O-O Qa5 Kb1 Bxc3 bxc3 O-O-O Qd2 c4 dxc4 Nxe4 Qd3 Nxc3+ Kb2 Nxd1+ Rxd1 Kb8 f4 f5 g4 fxg4 hxg4 Bxg4 f5 Bxd1 Qxd1 e4 Qd4 Qc5 Qc3 Qe5 Bd4 Qxf5 Bxg7 Rhe8 c5 dxc5 Bxh6 e3 Bg7 e2"""
,"""e4 e5 Nf3 Nc6 d4 d6 d5 Nd4 Nxd4 exd4 Qxd4 Nf6 Bg5 Be7 e5 dxe5 Qxe5 h6 Bxf6 gxf6 Qd4 Bf5 Bd3 Bxd3 Qxd3 Qd6 Nc3 O-O-O O-O-O Qf4+ Qe3"""
,"""e4 e6 d4 d5 e5 c5 c4 cxd4 Qxd4 Nc6 Qc3 dxc4 Qxc4 Bb4+ Bd2 Bxd2+ Nxd2 Nxe5 Qb5+ Nd7 Ne4 Nf6 Nd6+ Kf8 Nf3 h5 Ne5 Nxe5 Rd1 Qe7 Be2 Nc6 O-O Rh6 Nxc8 Rxc8 Bf3 Qb4 Qe2 Ng4 h3 Nf6 a3 Qb3 Rd3 Qa4 Ra1 Rd8 b3 Qa6 Rad1 Rxd3 Rxd3 Nd5 Bxd5 exd5 Rxd5 Qxe2 Rd7 Re6 Rxb7 Qe1+ Kh2 Qxf2 Rc7 Re2 Rc8+ Ke7 Rc7+ Kd6 Rxf7 Qxg2#"""
,"""e4 e5 Nf3 Nc6 Bb5 f5 exf5 e4 Qe2 Nf6 d3 d5 dxe4 dxe4 Bg5 Bxf5 Nc3 Bb4 O-O O-O Nh4 Bg4 Qc4+ Kh8 Bxf6 Qxf6 Bxc6 bxc6 Qxb4 Qxh4 Qxe4 Rf6 g3 Qh3 Qg2 Qh5 Rfe1 Bf3 Qf1 Qxh2+ Kxh2"""
,"""d4 d5 c4 Nf6 Bg5 a6 Nc3 c6 Qb3 h6 Bxf6 exf6 cxd5 cxd5 Qxd5 Nc6 e3 Be6 Qxd8+ Rxd8 b3 Bb4 Rc1 Ba3 Rc2 O-O Bd3 Nb4 Rd2 b5 Nge2 g6 O-O Kg7 f4 Nd5 Nxd5 Bxd5 Rc2 f5 Rc7 Rfe8 Ra7 Rxe3 Rxa6 b4 Bb5 Be4 Ng3 Rc3 Nxe4 fxe4 Rd1 Rc1 Rxc1 Bxc1 Rc6 Bxf4 Rc4 Be3+"""
,"""d4 Nf6 Nf3 d5 e3 e6 Bd3 c5 c3 Nc6 O-O b6 Nbd2 Bb7 e4 c4 Bc2 Qc7 e5 Nd7 Re1 O-O-O Nf1 f5 exf6 gxf6 Rxe6 Bd6 Qe2 h5 Bf5 Kb8 Bd2 h4 h3 Bc8 Re1 Rh5 Bg4 Rhh8 Ne3"""
,"""e4 e5 d4 exd4 c3 dxc3 Bc4 cxb2 Bxb2 d6 Nf3 Be6 Bxe6 fxe6 O-O Nd7 Nbd2 e5 Nb3 Ngf6 Ng5 h6 Ne6 Qe7 Nxc7+ Kf7 Nxa8 g5 Nc7 Nc5 Nxc5 Qxc7 Qb3+ Kg6 Ne6 Qc6 Nxf8+ Rxf8 f3 Nh5 Rac1 Qa6 Qe6+ Rf6 Qg8+ Ng7 Rc7 Qb6+ Kh1 Qxc7 Rc1 Qb6 h4 Qxb2 Rc7 Kh5 Rxg7 Kxh4 Qc8 Rxf3 gxf3 Qb1+ Kg2 Qxa2+ Kf1 Qa1+ Ke2 Qb2+ Kd1 Qd4+ Kc2 Qf2+ Kb1 Qxf3 Qc2 Qf1+ Kb2 Qb5+ Ka3 Qa5+ Kb2 Qb5+ Kc1 Qf1+ Kb2 Qb5+ Kc1 Qf1+ Kb2"""
,"""e4 e5 Nf3 Nc6 Nc3 f6 Bc4 a6 Bd5 b5 a4 b4 Ne2 Bb7 c3 bxc3 bxc3 Na5 Bxb7 Nxb7 Ba3 Bxa3 Rxa3 Ne7 Qb3 Rb8 Qa2 Na5 O-O d5 exd5 Nxd5 d4 Rb6 dxe5 Qe7 Ng3 Nf4 exf6 Qxf6 Re1+ Kd7 Qd2+ Kc6 Nd4+ Kb7 Ne4 Qf8 Ra2 Nc4 Qc2 Qf6 Nc5+ Ka7 Nd7 Qd6 Nxb6 cxb6 Qe4 Nd5 Qe6 Qxe6 Rxe6 Nxc3 Rc2 Nd5 Rxc4 a5 Rc7+ Ka6 Nb5 Nxc7 Nxc7+ Kb7 Re7 Kc6 Nb5 Rd8 g3 Rd3 Rxg7 Ra3 Rg4 Rb3 Rc4+ Kd5 Rd4+ Kc5 Kg2 Rxb5 axb5 Kxb5 Rh4 a4 Rxh7 a3 Ra7 Kb4 h4 Kb3 h5 a2 h6 Kb2 h7 a1=Q Rxa1 Kxa1 h8=Q+ Ka2"""
,"""e4 g6 d4 Bg7 c3 b6 Bd3 Bb7 f3 e6 Ne2 Ne7 Be3 O-O O-O d5 e5 Nd7 Nd2 c5 Rc1 Nf5 Bf2 cxd4 Bxf5 exf5 cxd4 Qe7 Qb3 Rac8 Rc3 Bh6 f4 f6 exf6 Qxe2 Nf3 Qe6 Re1 Qxf6 Ne5 Nxe5 fxe5 Qe6 Be3 f4 Bd2 g5 Rec1 g4 Rc7 Rxc7 Rxc7 Ba6 Qd3 Bxd3"""
,"""e4 e6 d4 d5 Nd2 dxe4 Nxe4 Nf6 Nxf6+ Qxf6 Nf3 Be7 Bg5 Qg6 Bxe7 Kxe7 Qd2 Rd8 Qb4+ Ke8 O-O-O Nc6 Qa3 a6 g3 b5 c3 Bb7 Bg2 a5 Ne5 Qh6+ Kb1 Nxe5 Bxb7 b4 cxb4 Nc4 Qb3"""
,"""d4 e6 Nf3 d5 Bf4 Nf6 e3 c5 dxc5 Bxc5 Nbd2 Nc6 Bb5 Bd7 O-O O-O c4 dxc4 Nxc4 Re8 Nd6 Bxd6 Bxd6 a6 Be2 e5 Ba3 e4 Nd4 Nxd4 Qxd4 Bc6 Rfd1 Qxd4 Rxd4 h5 Rad1 b5 b3 Bb7 h3 g6 Rd6 Kg7 Bb2 Re6 Rxe6 fxe6 Rd7+ Kh6 Bxf6 Bc6 Rc7 Bd5 h4 Rf8 Bg7+ Kh7 Bxf8+ Kg8"""
,"""e4 e5 Nf3 Nf6 Nc3 Nc6 d4 exd4 Nxd4 Bb4 Nxc6 Bxc3+ bxc3 bxc6 Bd3 Qe7 O-O O-O Re1 h6 Ba3 c5 Bb2 Bb7 e5 Nd5 c4 Nb4 Be4 Nc6 Bd5 Rab8 Bc3 Kh8 Qg4 Nb4 Bxb7 Rxb7 e6 f5 Qf3 c6 exd7 Qxd7 Rad1 Qf7 Bxb4 Rxb4 Qxc6 Rxc4 Rd7 Qf6 Qxf6 Rxf6 Rxa7 Rxc2 Ra8+ Kh7 a4 Re6 a5 Rxe1#"""
,"""e4 c5 Nf3 e6 d4 cxd4 Nxd4 a6 Nc3 Qb6 Nb3 d6 Be3 Qc7 g3 Nf6 Bg2 Be7 O-O Nc6 f4 O-O g4 Rb8 g5 Nd7 f5 Re8 Qh5 Nde5 f6 g6 Qh4 Bf8 Rae1 b5 a3 a5 Nd4 Nxd4 Bxd4 b4 axb4 axb4 Ne2 Qxc2 Nf4 Ba6 Rf2 Qc4 Bxe5 dxe5 Nh5 Qd4 Ng3 Bc5 Nh1 b3 Bf3 Rbc8 Kg2 Bf8 Ng3 Rc2 Ne2 Qxb2 Qg3 Rd8 h4 Rdd2 h5 Bxe2 hxg6 Bxf3+ Kg1 Rxf2 gxf7+ Kh8 Rf1 Rg2+ Kh1 Rh2+"""
,"""e4 e5 Nf3 d6 d4 f6 dxe5 fxe5 Bc4 h6 Nxe5 Nf6 Nf7 Qe7 Nxh8 Bg4 f3 Be6 Qe2 Nbd7 O-O O-O-O Nc3 g5 Ng6 Qf7 Nxf8 Bxc4 Qf2 Rxf8 Re1 Ne5 Qxa7 Ba6 Qd4 g4 f4 Nc6 Qf2 h5 Bd2 Rg8 Rad1 g3 hxg3 h4 gxh4 Ng4 Qf3 Nd4 Qh3 Kb8 Nd5 Nxc2 Rc1 Bd3 Nb4 Nxb4 Bxb4 Bb5 b3 Bd7 Qf3 Ne5 Red1 Nxf3+ Kf2 Qg6 gxf3"""
,"""e4 e5 Nf3 d6 d4 exd4 Qxd4 Nc6 Bb5 Ne7 Nc3 Bd7 Bxc6 Nxc6 Qe3 Be7 O-O O-O b3 Bf6 Bb2 Ne5 Nd5 Nxf3+ gxf3 Bxb2 Rad1 c6 Nf4 Be5 Kh1 Qg5 Ng2 Qh5 f4 Bf6 Rg1 Bh3 Rxd6 Bxg2+ Rxg2 Rfd8 Rxf6 Rd1+ Rg1 Rxg1+ Kxg1 gxf6 f5 Qg5+ Kf1 Qxe3 fxe3 Rd8"""
,"""d4 Nf6 c4 e6 Nc3 Bb4 Nf3 d5 e3 c5 Bd2 cxd4 exd4 Bxc3 bxc3 Ne4 cxd5 exd5 Ne5 Nc6 Bb5 O-O Nxf7 Rxf7 f3 Nxd2 Qxd2 a6 Bd3 Re7+ Be2 Qe8 O-O Rxe2 Qd3 Bd7 Rae1 Rxe1 Rxe1 Qxe1+ Qf1 Re8 Qxe1 Rxe1+ Kf2 Ra1 g4 Rxa2+ Kg3 Ra3 h4 Rxc3 h5 Nxd4 Kh4 Rxf3 g5 Rf4+ Kg3 Re4 g6 h6 Kf2 Bg4"""
,"""e4 e5 Nf3 Nf6 d4 Nxe4 dxe5 Bc5 Bd3 Nxf2 Qe2 Nxh1 Bg5 f6 exf6+ Kf7 fxg7 Re8 Bc4+ Kxg7 Bxd8 Rxe2+ Bxe2 Bf2+ Kd2 Nc6 Nc3 Nb4 Rxh1 b6 Rf1 Bd4 Bxc7"""
,"""Nh3 d5 Ng5 e6 d4 h6 e4 hxg5 e5 c5 c3 Nc6 Bb5 cxd4 Qxd4 Be7 Qg4 Nh6 Qh5 g6 Qd1 Nf5 g4 Nh4 f4 a6 f5 gxf5 Rf1 axb5 Qe2 Qb6 gxf5 Nxf5 Rxf5 exf5 e6 Bxe6 b4 Qg1+ Kd2 Rxh2 Kc2 Rxe2+ Nd2 f4 Kb3 Qd1+ Kb2 d4"""
,"""e4 g6 Nf3 Bg7 d4 d6 Bd3 Nc6 c3 Bg4 O-O e5 d5 Nce7 h3 Bxf3 Qxf3 Nh6 Be3 f6 Nd2 Nf7 Qe2 O-O c4 a6 c5 Qe8 b4 Rc8 b5 axb5 Bxb5 c6 dxc6 bxc6 Ba6 Rb8 Rab1 d5 exd5 cxd5 f4 e4 Rb7 Rxb7 Bxb7 Nc6 Nb3 f5 Qb5 Nfd8 Rd1 Nxb7 Qxb7 Rf7 Qb5 Qc8 Rxd5 Rb7 Qc4 Rb4 Qc2 Qe6 Rd6 Qxb3 axb3 Rb7 Rxc6 Bf6 Rc8+ Kg7 Qc4 Kh6 b4 Bd4 Bxd4 Rd7 Be3 Rd1+ Kf2 Rd4 Ke1 Rxc4 g4 Rxc5 g5+"""
,"""d3 f5 e4 Nf6 f3 e5 Nc3 Bc5 h3 O-O Bd2 d5 exd5 c6 dxc6 Nxc6 Nge2 f4 Ne4 Nxe4 dxe4 Qh4+ g3 fxg3 Bg2 Rxf3 Rf1 Rxf1+ Bxf1 g2+ Ng3 Qxg3+ Ke2 Qf2+ Kd3 Qd4+ Ke2 g1=Q Bc3 Qgf2#"""
,"""d4 d5 Nf3 Nf6 e3 Bg4 h3 Bh5 g4 Bg6 Ne5 Nbd7 Nxg6 hxg6 Nc3 e6 Bd2 Be7 Bg2 c6 Qe2 Nb6 O-O-O Nc4 f3 Qb6 b3 Nxd2 Rxd2 Qa5 Kb2 Ba3+ Kb1 Bb4 Nxd5 Nxd5 Rdd1 Nc3+ Kb2 Nxe2"""
,"""e4 e5 Nc3 Nc6 f4 exf4 d3 g5 g3 Bg7 gxf4 d5 fxg5 dxe4 Nxe4 f5 Ng3 f4 Qe2+ Qe7 Qxe7+ Ngxe7 c3 Ng6 Ne4 O-O Nf3 Nce5 Be2 Bf5 Kf2 Bxe4 dxe4 Rae8 Nxe5 Nxe5 Bf3 Nd3+ Kg2 Nxc1 Raxc1 Be5 Rhd1 Rf7 h4 Rfe7 Kh3 Kg7 Kg4 Bd6 Rd5 Re6 Rcd1 Rd8 Kh3 c6 R5d4 Kf7 Bh5+ Ke7 Bg4 Re5 Bf3 Rc5 b3 Bc7 c4 Rxd4 Rxd4 Ra5 Rd2"""
,"""b3 Nf6 Bb2 e6 Nf3 Be7 g3 Nh5 Bg2 Bf6 c3 g6 d4 Bg7 O-O Nf6 Nbd2 O-O e4 d5 e5 Nfd7 Ne1 c5 Nd3 cxd4 cxd4 Nc6 Rc1 Ne7 f4 Nf5 Nf3 Ne3 Qe2 Nxf1 Kxf1 b6 Qd2 Ba6 Kg1 Nb8 Nb4 Qd7 Nxa6 Nxa6 Qe2 Nc7 Qd3 Nb5 Nd2 Rac8 a4 Rxc1+ Bxc1 Rc8 Bb2 Nc7 Bf1 Ne8 Be2 f6 Bd1 fxe5 dxe5 Nc7 Bf3 Qc6 Bd1 Ne8 Nf3 Qc5+ Bd4 Qc1 Kg2 Nc7 Be3 Na6 Bxc1 Rxc1 Qxa6"""
,"""e4 d6 d4 Nf6 Nc3 g6 Nf3 Bg7 Bc4 c6 a4 d5 exd5 cxd5 Bd3 O-O O-O Nc6 Re1 Bg4 Be3 Nd7 h3 Bxf3 Qxf3 Nxd4 Bxd4 Bxd4 Qxd5 Bxc3 bxc3 e6 Qxb7 Nf6 Rad1 Qb6 Qxb6 axb6 Bb5 Rfc8 c4 Rd8 f3 Kf8 Kf2 Ke7 Ke2 Rxd1 Rxd1 Rd8 Rxd8 Kxd8 Ke3 Kc7 Kf4 Kd6 Kg5 Ng8 f4 h6+ Kg4 Kc5 Be8 f5+ Kf3 Ne7 Bf7 Kd6 g4 e5 gxf5 gxf5 fxe5+ Kxe5 h4 Nc6 h5 Nd4+ Ke3 Nxc2+ Kd3 Ne1+ Ke2 Nc2 Kd3 Nd4 Kc3 Ne2+ Kb4 Kd6 Kb5 Kc7 Bd5 f4 c5 Nd4+ Kc4 bxc5 Kxc5 Nf5 Kc4 Ng3 Bf3 Nf5 Kd3 Kb6 Ke4 Ng3+ Kxf4 Nf1 Kf5 Nd2 Kg6 Nxf3 Kxh6 Nd4 Kg7 Nf5+ Kg6 Ng3 h6 Ne4 h7 Nd6 h8=Q Ne8 Qxe8 Ka5 Qb8 Kxa4 Kf5 Ka3 Ke4 Ka4 Kd4 Ka5 Kc5 Ka6 Qb6#"""
,"""e4 e5 f4 exf4 Nf3 Qe7 Nc3 c6 d4 g5 h3 d6 Bc4 Be6 d5 cxd5 Nxd5 Bxd5 Bxd5 Nc6 O-O O-O-O Nd4 Nb4 Bb3 Bg7 c3 Bxd4+ Qxd4 Nc6 Qxh8 Nf6 Qg7 Rg8 Qxf7 Qxf7 Bxf7 Rf8 Be6+ Kc7 Bd2 Nxe4 Rad1 Ng3 Rf3 Re8 Bb3 Ne2+ Kh2 Ng3 Rxg3 fxg3+ Kxg3 Re2 Bc4 Re4 Bd3 Re5 Bxh7 Ne7 Re1 Rxe1 Bxe1 Nd5 Kg4 Ne3+ Kxg5 Nxg2 Bg3 Ne3 Be4 Nd1 h4 Nxb2 h5 Nc4 h6 Ne5 Bxe5 dxe5 h7"""
,"""e4 e6 d4 c5 d5 e5 c4 d6 b3 Nf6 Bd3 Bg4 f3 Bh5 g4 Bg6 h4 h6 Nd2 Be7 Nf1 Nfd7 Bd2 Bxh4+ Ke2 Bg3 Nxg3 Nf6 Be3 Qd7 Kd2 a6 N1e2 Qe7 Qg1 Nbd7 Nf5 Qf8 g5 Nh5 gxh6 gxh6 Neg3 Nxg3 Qxg3 Bxf5 exf5 Rg8 Qh3 O-O-O Rag1 Nf6 Rxg8 Qxg8 Bxh6 Re8 Bg5 Qxg5+ f4 Qxf4+ Kc3 Qd4+ Kc2 e4 Be2 e3 Rd1 Qe4+ Rd3 Qxf5 Qxf5+ Nd7 Qxf7 Rf8 Qg7 Re8 Qg5 Ne5 Rxe3 Kb8 Qh5 Rf8 Rxe5 dxe5 Qxe5+ Ka8 d6 Kb8 d7+ Ka7 Qe7 Rb8"""
,"""e4 e5 f4 exf4 Nf3 f5 d4 d6 Nc3 fxe4 Nxe4 Qe7 Bd3 d5 O-O Bg4 Nc5 Qd6 Nxb7 Kd7 Bf5+ Bxf5 Nh4 Be4 Qg4+ Kc6 Na5+ Kb5 Nb7 Qb6 Qe2+ Kc6 Nc5 Bxc5 dxc5 Qxc5+ Kh1 Bxc2 Qe6+ Qd6 Qxd5+ Qxd5"""
,"""e4 e5 Bc4 Nc6 Bxf7+ Kxf7 Nf3 Nd4 Nxe5+ Ke6 Nd3 d6 O-O Ke7 f4 Nf3+ Qxf3 Bg4 Qxg4 h5 Qxg7+ Ke8 Qxh8 Nf6 e5 Kf7 exf6 Qxf6 Qxh5+ Ke6 f5+ Qxf5 Qxf5+"""
,"""Nf3 c6 b3 d6 Bb2 Nd7 c4 e5 e3 Ngf6 d3 Be7 Nbd2 a5 Be2 Qb6 a3 a4 b4 c5 b5 Nf8 O-O Ne6 Ne4 Ng4 Nc3 Nc7 Nxa4 Qa5 Nc3 Bf6 Nd5 Nxd5 cxd5 Qxb5 Rb1 Qd7 Nd2 h5 Nc4 b5 Nb6 Qf5 Nxa8 Bb7 Nc7+ Kd7 Nxb5 Qg5 f4 Qh4 h3 Nxe3 Qe1 Nxf1 Qxh4 Bxh4 Bxf1 Bxd5 Nc3 Bc6 Ne4 exf4 Bxg7 Ra8 Bf6 Bxf6 Nxf6+ Ke6 Nxh5 Rxa3 Nxf4+ Kf5 Ne2 Rxd3 Nc1 Rd1 Ra1 c4 Nb3 Rd5 Na5 Bb5 Nxc4 Kf4 Nb6 Rg5 Bxb5 Rxb5 Nc4 d5 Rf1+ Kg3"""
,"""e4 e5 d4 d6 Nf3 Qf6 Nc3 Be6 Bg5 Qg6 d5 Bg4 Bb5+ Nc6 O-O Be7 Bxc6+ bxc6 Ne2 Qxe4 Ng3 Bxg5 Nxe4 Bxf3 Nxg5 cxd5 Nxf3 Nf6 Qd3 Rb8 c4 Rxb2 Rab1 Rxa2 Rb8+ Ke7 Rb7 Rd8 Rxc7+ Nd7 cxd5"""
,"""e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 g3 Bg7 Bg2 Nc6 Nxc6 bxc6 e5 Nd5 Nxd5 cxd5 Qxd5 Rb8 exd6 Bb7 Qb5+ Kf8 Bxb7 exd6 O-O Qe7 Qd5 Rxb7 Bg5 Qc7 Bf4 Be5 Bxe5 dxe5 b3 Kg7 Rfe1 Re8 c4 Rb6 f4 Rd6 Qe4 Rd4 Qe3 f6 Rad1"""
,"""d4 d5 c4 e6 a3 Nf6 Nf3 b6 Bf4 Bb7 e3 Nbd7 Nbd2 Bd6 Bg5 h6 Bh4 g5 Bg3 Bxg3 hxg3 c5 Nxg5 cxd4 e4 dxc4 Nxc4 Qe7 Qxd4 Rg8 Nd6+ Kf8 Nxb7 hxg5 Rd1 e5 Qd6 Qxd6 Nxd6 Ke7 Nf5+ Kf8 Bb5 Nc5 Bc6 Rc8 Rd6 Nfxe4 Rdh6 Nd3+ Ke2 Nexf2 Rf1 Ng4 Rd6 Nc5 b4 Nb3 Kf3 Nh2+ Kf2 Nxf1 Kxf1 Nd4 Nxd4 exd4 b5 Ke7 Rxd4 Rgd8 Re4+ Kf6 g4 a6 a4 axb5 axb5 Rd2 Ke1 Rxg2 Rd4 Rxg4 Rxg4 Kg6 Kf2 f5 Rd4 Rh8 Rd6+ Kh5 Bd7 Rf8 Rxb6 Kg4 Rc6 Kf4 b6 g4 b7 g3+ Kg2 Rh8 Rc8 Rh2+ Kg1 Rb2 b8=Q+ Rxb8 Rxb8"""
,"""e4 c5 Nf3 g6 Bc4 Bg7 c3 e6 d4 cxd4 cxd4 Nc6 O-O Nge7 d5 exd5 exd5 Ne5 Nxe5 Bxe5 f4 Bf6 d6 Nc6 Re1+ Kf8 Nc3 Kg7 Nb5 Qb6+ Be3 Qa5 Nc7 Rb8 Rb1 b5 Bb3 Qb4 a3 Qa5 Nd5 Bb7 Nxf6 Kxf6 Bd4+ Nxd4 Qxd4+ Kf5 Bc2+ Kg4 Bd1+"""
,"""d4 e5 dxe5 Nc6 Nf3 d6 Bf4 Be6 exd6 Bxd6 Bxd6 cxd6 e3 Qa5+ Nc3 O-O-O Be2 Qb4 Qc1 Nf6 O-O h5 a3 Qb6 b4 h4 h3 Ne5 Nd4 Qc7 Ncb5 Qb6 a4 a6 a5 Qxb5 Nxb5 axb5 Bxb5 Kc7 a6 b6 a7 g5 Ra3 g4 Qa1 Ra8 Rc3+ Kd8 hxg4 h3 f4 h2+ Kh1 Ne4 Rc6 Ng3#"""
,"""e4 d5 e5 d4 d3 c5 Bd2 Nc6 Nf3 Qc7 Qe2 e6 g3 Be7 Bg2 f6 exf6 Bxf6 O-O Nge7 c3 O-O Qxe6+ Bxe6 cxd4 Nxd4 Nxd4 Bxd4 Bc3 Bxc3 Nxc3 Nf5 Rae1 Nd4 Ne4 Bd5 Ng5 Bxg2 Kxg2 Qc6+ Kg1 Nf3+ Nxf3 Qxf3 Re3 Qxe3 fxe3 Rxf1+ Kxf1 Kf7 Ke2 Ke6 Kf3 Rd8 Ke4 g6 h4 b6 b3 Rf8 a3 Rf2 b4 cxb4 axb4 Rb2 d4 Rxb4 g4 Rc4 Kf4 b5 Kg5 Kf7 Kh6 Kg8 Kg5 b4"""
,"""d4 d5 c4 dxc4 Nc3 Nc6 Nf3 Bf5 e4 Bg4 d5 Nb4 Bxc4 Bxf3 gxf3 e6 a3 Na6 Bxa6 bxa6 Bf4 Nf6 dxe6 fxe6 Ke2 Bd6 Qd2 O-O Rad1 Qe7 Bxd6 cxd6 Qxd6 Qb7 Qxe6+ Kh8 Rd2 Rae8 Qd6 Rd8 Qxd8 Rxd8 Rxd8+ Ng8 b4 Qc7 Rd4 Qxc3 Rd3 Qc2+ Ke3 Qa4 Rg1 h6 f4 a5 bxa5 Qxa5 f5 Qb6+ Ke2 Nf6 e5 Ne4 Rg2 Qb2+ Kf3 Nd2+ Ke3 Nf1+ Kf3 Qxe5 Rd8+ Kh7 Rh8+ Kxh8 Kg4 Qe4+ f4 Qxg2+ Kh4 Qxh2+ Kg4 Qe2+"""
,"""d3 Nf6 Bg5 d6 Bc1 e6 Bd2 Be7 Ba5 O-O Bc3 b6 Bd2 c5 f3 d5 e4 Bb7 e5 Nfd7 f4 Nc6 Nf3 d4 c3 f6 c4 fxe5 fxe5 Ndxe5 Be2 Nxf3+ Kf1 Nxd2+ Ke1 Nb3 Qxb3 Na5 Qc2 Bxg2 Rg1 Bf6 Qd2 Bh3 Qh6 Bf5"""
,"""e4 e6 e5 c5 b4 Nc6 bxc5 Nxe5 d4 Nc6 c3 Nf6 Bg5 Ke7 Bb5 Ke8 Nf3 Be7 a4 h6 Bxc6 dxc6 Be3 Nd5 c4 Nxe3 fxe3 Bf6 O-O Ke7 Nc3 Re8 Ne4 Kf8 a5 Be7 Ne5 f6 Ng6+ Kg8 Qg4 f5 Nxe7+ Qxe7 Qg6 fxe4 Qxe4 Qg5 Ra2 e5 Raf2 Be6 d5 cxd5 cxd5 Bh3 d6 Rf8 Qd5+ Kh8 Rxf8+ Rxf8 Rxf8+ Kh7 Qe4+ g6 Rf7+ Kg8 Rxb7 Bf5 Qd5+ Kh8 Rb8+ Kg7 Rb7+ Kh8 Qxe5+"""
,"""e4 e5 Nf3 Nc6 Bc4 Nf6 d3 Bc5 b4 Bb6 c3 Ng4 O-O Nxf2 Rxf2 Bxf2+ Kxf2 O-O Bb2 Ne7 Kg1 c6 Bb3 d5 exd5 e4 dxe4 Qb6+ Kh1 cxd5 exd5 Nf5 Nbd2 Qf2 Qf1 Qb6 Qd3 Ne3 c4 Bf5 Qe2 Rae8 c5 Qh6 Re1 Bd7 Qf2 Ng4 Qf1 Rxe1 Qxe1 Re8 Qf1 Ne3 Qd3 Bf5 Qe2 Bd7 Bd4 Nxd5 Qc4 Be6 Qd3 Nf4 Qc2 Ne2 Bxe6 Rxe6 Qc3 Nxc3"""
,"""e4 e5 Bc4 d6 Nf3 Nf6 Ng5 Be7 Nxf7 Bd7 Nxd8 Bxd8 d4 c6 d5 c5 Nc3 O-O Nb5 Bg4 f3 Bd7 Nxd6 Be7 Nxb7 Na6 d6+ Kh8 dxe7 Rfe8 Qxd7 Rxe7 Qxe7 Nb4 O-O Ng8 Qf7 Nxc2 Nd6 Nf6 Bg5 Nxa1 Bxf6 Nc2 Qg8+"""
,"""e4 d5 exd5 Qxd5 d4 Bf5 c4 Qe4+ Be2 Qxg2 Bf3 Qg6 Bxb7 Be4 Bxa8 Bxh1 Nf3 Bxf3 Bxf3 Qg1+ Ke2 Qxh2 Qa4+ Nd7 Qxa7 c5 dxc5 Qe5+ Be3 Qxb2+ Nd2 Ngf6 c6 Qb8 Qxb8+ Nxb8 c7"""
,"""e4 e5 Bc4 Nf6 Nc3 c6 Qe2 b5 Bb3 a5 a3 Ba6 d3 b4 axb4 Bxb4 Bd2 d5 exd5 Bxc3 Bxc3 e4 Nh3 cxd5 Bxf6 Qxf6 Bxd5 Ra7 O-O O-O Qe3 Re7 Bxe4 Nd7 Nf4 Bb7 Rxa5 Bxe4 dxe4 Rfe8 Nd5 Qxb2 Nxe7+ Rxe7 Qd3 g6 Ra8+ Kg7 Rfa1 Nc5 Qa3 Qxc2 Rc8 Rxe4 Rxc5 Qe2 g3 h5 Qc3+ Kh6 Qh8#"""
,"""g3 d5 Bg2 c6 Nf3 e6 O-O g6 d4 Bg7 c3 Ne7 b4 O-O a4 Bd7 a5 b6 axb6 Qxb6 Nbd2 a5 bxa5 Rxa5 Rxa5 Qxa5 Bb2 c5 Qc2 cxd4 cxd4 Rc8 Qd3 Nbc6 Ra1 Qb6 Ba3 Nb4 Bxb4 Qxb4 Rb1 Qa4 Nb3 Nf5 Nc5 Qc6 Nxd7 Qxd7 e3 Nd6 Nd2 Qc7 Nb3 Bf8 Rc1 Nc4 Nd2 Bd6 Nxc4 dxc4 Qc3 Qb6 Bf1 Bb4 Qb2 c3 Qb3 Ba5 Qxb6 Bxb6 Rc2 Kf8 Bd3 Ke7 f4 f5 Kg2 Kd6 Kf3 Ba5 h3 Kc6 g4 Bb4 gxf5 gxf5 Bc4 Kd6 Bd3"""
,"""d4 c5 d5 e6 c4 Qf6 Nc3 exd5 cxd5 a6 Nf3 b5 e4 b4 Ne2 Bd6 Ng3 Bb7 Bc4 Ne7 O-O Be5 Rb1 d6 Nxe5 dxe5 f4 Nd7 f5 O-O-O Qd3 Nb6 Bxa6 c4 Bxb7+ Kxb7 Qd2 Nexd5 exd5 Nxd5 Qe2 Rc8 Kh1 c3 bxc3 Nxc3 Rxb4+"""
,"""e4 e5 Nf3 Nc6 Bc4 Bc5 O-O Nh6 d3 O-O Bxh6 gxh6 c3 Qf6 b4 Bb6 Nbd2 Kh8 Bd5 Rg8 Nc4 Qg6 Nh4 Qg5 Bxf7 Rg7 Qf3 Qxh4 Nxb6 cxb6 Bd5 d6 Qf8+ Rg8 Qxg8#"""
,"""e4 e5 Nf3 f6 Bc4 a6 O-O b5 Bd5 Nh6 Bxa8 c6 d3 Qb6 Bxh6 gxh6 Nh4 d6 Qh5+ Kd8 Nf5 Bxf5 Qxf5 Nd7 c4 Rg8 cxb5 axb5 Nd2 Nc5 Qxf6+ Be7 Qxh6 Rf8 Nf3 Nxd3"""
,"""d4 e6 Nf3 d5 c4 dxc4 e3 b5 a4 b4 Bxc4 f6 Bd2 Ba6 Bxa6 Nxa6 O-O e5 b3 e4 Ne1 f5 Qh5+ g6 Qe2 Bd6 Qxa6"""
,"""Nf3 f5 Nc3 g6 d4 Nf6 e3 d5 Bd2 c6 Bd3 b6 O-O Bg7 Ng5 h6 Nf3 a6 Ne5 b5 Nxg6 Rg8 Nf4 Qd6 Nh5 Be6 Nxg7+ Rxg7 f4 Nbd7 Be2 O-O-O Be1 Rdg8 Bf3 Bf7 Bh4 e6 g3 Ne4 Nxe4 fxe4 Bg2 Bg6 Bh3 h5 f5 exf5 Bxf5 Kb7 Bxg6 Qxg6 Rf4 c5 Qe2 c4 c3 Qh6 Raf1 Rg4 Rxg4 Rxg4 Rf7 Kc7 h3 Qg6 Rxd7+ Kxd7 hxg4 hxg4 Qf2 Ke6 Qf4 a5 Qc7 Qf7 Qc6+ Kf5 Qxb5 Kg6 Qc6+ Kh7 Qf6 Qb7 Qe7+ Qxe7 Bxe7 a4 Kf2 Kg6 Ke2 Kf7 Bb4 Ke6 Kd2"""
,"""e4 c5 Nf3 e6 e5 d6 Qe2 Qc7 exd6 Bxd6 d3 Nd7 Ng5 Ne5 f4 Ng6 Nc3 Nf6 g3 b6 Nb5 Qc6 Nxd6+ Qxd6 Bd2 Bb7 Rg1 O-O O-O-O a5 h4 Rfe8 h5 Nf8 g4 Nd5 Nh3 Nb4 a3 Nd5 Bg2 b5 c4 bxc4 dxc4 Rab8"""
,"""e4 c5 Nf3 d6 Nc3 Nf6 d4 cxd4 Nxd4 g6 Nb3 Bg7 Bd2 O-O Bd3 Nc6 O-O Be6 Kh1 a5 f4 Bxb3 axb3 Nb4 Qe2 Rc8 Rfc1 Qd7 Nd1 Qg4 Bxb4 axb4 Qxg4 Nxg4 h3 Nf6 Ra4 d5 e5 Ne4 Bxe4 dxe4 Rxb4 e3 Nxe3 Bh6 g3 g5 Nf5 Bg7 Nxe7+"""
,"""e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nc3 Bc5 Be3 Bxd4 Bxd4 e5 Be3 Nf6 Be2 d6 O-O O-O h3 Be6 a3 Qd7 f4 exf4 Bxf4 Rad8 Qd2 d5 e5 Ne4 Nxe4 dxe4 Qxd7 Rxd7 Rad1 Rfd8 Rxd7 Rxd7 Rd1 Rxd1+ Bxd1 h6 c3 Na5 b4 Nc4 a4 g5 Bg3 Kg7 Kf1 Kg6 Kf2 h5 Bh2 g4 hxg4 hxg4 Bc2 Kf5 Ke2 Nxe5 Bxe5 Kxe5 Ke3 f5 g3 Bc4 Bd1 a6 Bc2 Be6 Bd1 Ba2 Bc2 b6 Bd1 Bd5 Bc2 Bb7 Bb3 f4+ gxf4+ Kf5 Bd1 g3 Be2 g2 Kf2 Kxf4 Kxg2 Ke3 Kf1 Kd2 c4 e3 b5 a5 Bg4 Be4 Be2 Kc3"""
,"""e3 d5 d4 Bf5 Nc3 g6 Bd3 Bxd3 cxd3 Bg7 Nf3 Nd7 Ne5 c5 Nxd7 Qxd7 dxc5 Nf6 d4 e5 O-O e4 Bd2 Ng4 h3 Nf6 f4 exf3 Qxf3 O-O Ne2 Rfe8 Nf4 Ne4 Qf2 Qe7 Qe2 Nxd2 Qxd2 Qxe3+ Qxe3 Rxe3 Nxd5 Re2 b4 Bxd4+ Kh1 Bxa1 Rxa1 Rd2 Ne7+ Kg7 c6 Re8 cxb7 Rd7 Nc6 Rxb7 Ne5 Rxb4 Nc6 Rc4 Na5 Rce4 Nb3 Re1+ Rxe1 Rxe1+ Kh2 Rb1 Nd4 Rb2 Nb5 Rxb5 a3 Ra5 h4 Rxa3 h5 Rh3+ Kxh3 gxh5 Kh4 Kg6 g4 a5 gxh5+ Kh6 Kg4 a4 Kf5 a3 Kf6 a2 Ke7 f6 Kf8 f5 Kg8 Kxh5 Kh8 a1=N Kxh7 f4 Kg7 f3 Kf6 f2 Ke5 f1=Q Kd4 Qe1 Kd5 Qb1 Kd4 Qb4+ Ke5 Qb5+ Kf4 Qb4+ Ke5 Qc3+ Kd6 Qd3+ Ke5 Qf3 Ke6 Qc3 Kd5 Qc5+ Kxc5"""
,"""d3 Nf6 g3 d5 Bg2 e6 Nf3 c6 e3 Nbd7 Qe2 Bd6 Bd2 Ne5 Nc3 Nxf3+ Bxf3 Nd7 O-O-O Ne5 e4 Nxf3 Qxf3 d4 Ne2 e5 Qg2 c5 f4 f6 fxe5 Bxe5 Nf4 O-O h4 Bxf4 gxf4 Qc7 Rhg1 Rf7 Rde1 Qb6 Ref1 Qa6 Kb1 b5 Rf3 b4 Rg3 g6 h5 f5 hxg6 hxg6 Rxg6+ Qxg6 Qh2 Kf8 Rxg6"""
,"""e4 e5 f4 exf4 Bc4 c6 Nc3 b5 Bb3 b4 Nce2 Be7 Nxf4 Ba6 Nf3 g5 Ne2 g4 Ne5 d5 O-O f6 Nxg4 h5 Nf2 Qb6 exd5 cxd5 Bxd5 Nc6 d3 O-O-O Bxc6 Qxc6 Nd4 Qd5 Bf4 Qxd4 Qf3 Bb7 Qh3+"""
,"""e4 e5 Qh5 Nf6 Qxe5+ Qe7 Qxe7+ Bxe7 e5 Nd5 c4 Nb6 b3 d6 exd6 Bxd6 Bb2 O-O Nf3 Re8+ Kd1 Nc6 d4 Bf4 d5 Ne5 Nbd2 Bxd2 Nxd2 Bf5 f4 Ng4 Bd4 Nd7 h3 Ngf6 g4 Be4 Nxe4 Nxe4 Bd3 Ng3 Rh2 c5 Bc3 Rad8 Kd2 Re7 Re1 Rde8 Rxe7 Rxe7 Rg2 Nf1+ Bxf1 Nf6 Bxf6 gxf6 f5 Re4 Bd3 Rd4 Ke3 a6 a3 b5 Rb2 bxc4 bxc4 Kg7 Rb8 h6 Rc8"""
,"""g3 e5 Bg2 d5 e3 Be6 Ne2 c5 O-O Bd6 c4 d4 Bxb7 Bxc4 Bxa8 Nf6 exd4 cxd4 d3 Ba6 Qa4+ Nbd7 Bc6 Bc8 Nd2 O-O Nc4 Nc5 Qd1 Bb7 Bxb7 Nxb7 b3 e4 Nf4 e3 fxe3 dxe3 Nxd6 Qxd6 Bxe3 Re8 Qf3 Nc5 Bxc5 Qxc5+ Kg2 h6 Rae1 Rc8 Nh5 Nd5 Qg4 Qc2+ Rf2 Rc3 Qxg7#"""
,"""e4 c6 d4 d5 e5 Bf5 Na3 e6 g4 Bg6 Nh3 a6 Qf3 c5 dxc5 Bxc5 Bg5 Qc7 Bf4 Nc6 O-O-O Nxe5 Bxe5 Qxe5 c3 Bxa3 bxa3 Qd6 c4 Qc5"""
,"""e4 e5 Nf3 Nc6 c3 d6 d4 exd4 Nxd4 Ne5 Bd3 Qf6 O-O Qg6 f4 Bh3 Qc2 Nxd3 f5 Qg4 Kh1 Qxe4 Nd2 Qxg2#"""
,"""e4 c5 Nf3 d6 Bb5+ Bd7 Bxd7+ Nxd7 O-O e5 c4 Ngf6 Nc3 a6 d3 h6 h3 Be7 Be3 O-O Qd2 Rb8 a4 Nh7 a5 Ng5 Nxg5 Bxg5 f4 Bxf4 Bxf4 exf4 Qxf4 Nf6 g4 Qd7 Na4 b5 axb6 Qc6 Qd2 Nd7 Nc3 Nxb6 Rxa6 Qb7 Rfa1 Rfd8 Ra7 Qc8 Qf2 f6 Nb5 Qe6 Qf5 Qxf5 gxf5 Nd7 Nxd6 Ne5 Nb5 Nxd3 Rc7 Re8 Raa7 Ne5 Rxg7+ Kh8 Rh7+ Kg8 Rag7+ Kf8 Nc7 Nf3+ Kf2 Ng5 Ne6+ Rxe6 fxe6 Nxh7 Rxh7 Kg8 Rxh6 Kg7 Rh4 Re8"""
,"""e4 e5 d4 g6 d5 Nf6 c4 Nxe4 Nc3 Nxc3 bxc3 d6 Ba3 Bf5 Nf3 Na6 Rb1 b6 Bd3 Qf6 Qd2 O-O-O Ng5 h6 g4 Bxd3 Nxf7 Qxf7 Qxd3 h5 c5 dxc5 Qxa6+ Kb8 c4 hxg4 O-O Qf4"""
,"""d4 e6 a3 d5 e3 c5 c4 Nf6 Nf3 Nc6 Nc3 a6 dxc5 Bxc5 b4 Be7 b5 axb5 cxb5 Na5 Bb2 b6 Be2 Bd7 O-O O-O a4 Rc8 Ne5 Bd6 Nxd7 Nxd7 e4 Bb4 Rc1 Nc5 exd5 Ncb3 Rc2 exd5 Nxd5 Rxc2 Nxb4 Rxb2 Qxd8 Rxd8 Nc6 Nxc6 bxc6 Rxe2"""
,"""e4 c5 Nf3 e6 Bc4 Nf6 d3 Nc6 Bg5 Be7 O-O Nd4 Nxd4 cxd4 Nd2 e5 Nf3 d6 c3 Qb6 cxd4 exd4 e5 dxe5 Nxe5 O-O b3 Be6 Bxe6 Qxe6 Re1 Qf5 Bh4 Bd6 Nf3 Rae8 Nxd4 Rxe1+ Qxe1 Qf4 Nf3 Ng4 Bg3 Qf6 Bxd6 Qxd6 h3 Nf6 d4 Re8 Qf1 h6 Rd1"""
,"""c4 e5 e3 Nc6 Nc3 f6 d4 exd4 exd4 Qe7+ Be3 g6 Nf3 d6 h3 Bf5 a3 Bg7 Bd3 Bxd3 Qxd3 Nh6 O-O Nf5 Rad1 O-O Rfe1 Nxe3 Rxe3 Qf7 Rde1 Bh6 R3e2 Na5 d5 Rfe8 Nd4 Rxe2 Rxe2 Re8 b4 Rxe2 Ncxe2 c5 Ne6 cxb4 axb4 Nxc4 Qxc4 Qd7 b5 f5 f4 Bg7 Nxg7"""
,"""e4 c5 Nf3 e6 d4 cxd4 Nxd4 a6 Nc3 Qc7 Bg5 f6 Bh4 Bb4 Qf3 Qe5 a3 Qxd4 axb4 Qxb4 Ra4 Qxb2 Be2 Qc1+ Nd1 b5 Rd4 Bb7 O-O Nc6 Ne3 Qa3 Rd3 Qe7 Rdd1 Ne5 Qh3 Nh6 f4 Ng6 f5 Nxh4 Qxh4 e5 Ng4 Bxe4 Nxh6 gxh6 Bd3 Qc5+ Kh1 Bxg2+ Kxg2 Rg8+ Kh3 Qe3+ Qg3 Rxg3+ Kh4 Rh3+ Kg4 Qg5+ Kxh3 Qh5+ Kg2 Qg5+ Kf2 O-O-O Rde1 Qh4+ Kg1 Rg8+ Kh1 Rg5 Re4 Rh5 Rxh4 Rxh4 Rb1 Rh5 Kg2 Rg5+ Kh3 h5 c4 bxc4 Bxc4 Kd8 Bxa6 Ke7 Rb7 Rxf5 Bb5 Rf3+ Kg2 Rb3 Rxd7+ Ke6 Rxh7 Rxb5 Rxh5 Rb2+ Kf3 Rb3+ Kg4 Rb4+ Kf3 Rf4+ Ke3 Kd5 Rh6 Re4+ Kf3 Rf4+ Ke3 Re4+ Kf3 Rg4 Ke3 Re4+"""
,"""g4 d5 b4 Bxg4 f4 Qd6 Bg2 Nf6 Nc3 Qxf4 d4 Ne4 Qd3 e6 Qb5+ Nd7 a4 Qf2+ Kd1 Qxg2 Ke1 Qxh1 Ra3 Qxg1#"""
,"""e4 e5 Bc4 Nc6 Nf3 Nd4 O-O Nxf3+ Qxf3 Qf6 Qb3 Bc5 Nc3 Nh6 Nd5 Qd6 c3 c6 Ne3 b5 Nf5 Qg6 Nxh6 gxh6 Be2 Rg8 Bf3 d6 a4 Bh3 c4 Bxg2"""
,"""e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Bd3 Bg7 Be3 O-O O-O Nc6 f4 Ng4 Nxc6 bxc6 Qf3 c5 h3 Nxe3 Qxe3 Bd4 Rf3 Bxe3+ Rxe3 Qb6 Rae1 Be6 b3 c4 bxc4 Rac8 Nd5 Bxd5 cxd5 e6 Kh2 exd5 exd5 Qd4 f5 Qf4+ Kh1 Rb8 Rf3 Qh4 Ref1 g5 f6 Rbe8"""
,"""Nf3 Nf6 Nc3 d5 e4 dxe4 Ng5 e5 Bc4 Be6 Nxe6 fxe6 Bxe6 Bd6 O-O c6 Qe2 Qe7 Bb3 Nbd7 Nxe4 Nxe4 Qxe4 O-O-O Qg4 Kb8 Qe6 Qxe6 Bxe6 Nf6 d3 Rhe8 Bh3 Kc7 Bg5 h6 Bxf6 gxf6 Rae1 Rf8 Bf5 Rg8 g4 Rdf8 h3 Rg5 Kg2 Bc5 f3 Rg7 Re4 Bd4 c3 Bc5 b4 Bb6 a4 a5 b5 Rd8 Rd1 Rgg8 Rc4 Rd6 bxc6 Rxc6 Rxc6+ bxc6 Be4 Rd8 c4 Bc5 Rb1 Bb4 Kf2 Kb6 Ke2 Kc5 Ke3 Rd7 h4 Rg7 h5 Rd7 Bf5 Rd8 Be4 Rb8 Ke2 Kd4 Bxc6 Rd8 Be4 Rb8 c5 Rc8 c6 Rc7 Rc1 Bc5 Rc4#"""
,"""d4 d5 c4 dxc4 Nc3 c5 d5 e6 e4 exd5 exd5 Bd6 Bxc4 Nf6 Be3 O-O Nf3 Re8 O-O b6 a3 Bb7 Qd2 Nbd7 Rad1 Ne5 Nxe5 Bxe5 Rfe1 Qd6 g3 Ng4 Kg2 Nxe3+ Rxe3 Re6 f4 Bd4 Rxe6 fxe6 Ne2 exd5 Ba2 Re8 Nxd4 cxd4 Qxd4 Re4 Qd3 Kh8 Bxd5 Bxd5 Qxd5 Qxd5 Rxd5 h6 Kf2 Rc4 Rd2 Kg8 Kf3 Rc7 Ke4 Kf7 Kf5 g6+ Kg4 Ke6 Re2+ Kf6 Rd2 Ke6 Kf3 Kf5 Rd5+ Ke6 Rd2 Rc4 Rd3 Rc2 Re3+ Kf5 Re5+ Kf6 h4 Rxb2 Re3 Ra2 Rb3 Ke7 Kg4 Kf6 Re3 Kf7 Rc3 Rb2 Rc7+ Ke6 Rxa7 Rb3 Ra8 Rb5 Ra6 Kf6 a4 Ke6 axb5"""
,"""e4 c5 Nf3 Nc6 Bc4 d6 Nc3 e6 d4 cxd4 Nxd4 Nxd4 Qxd4 Nf6 e5 dxe5 Qxd8+ Kxd8 Bg5 Be7 O-O-O+ Bd7 Bb5 Re8 Bxf6 Bxf6 Rxd7+ Kc8 Rc7+ Kd8 Bxe8 Kxe8 Rc4 Kd8 Ne4 Rc8 Rxc8+ Kxc8 Nxf6"""
,"""Nc3 c6 e4 Qc7 b3 d6 Bb2 e5 f4 Nf6 Qe2 Be7 O-O-O h5 g3 Nbd7 Nf3 Nc5 Bg2 Bd7 d4 exf4 gxf4 Na6 e5 dxe5 fxe5 Ng4 h3 Nh6 d5 cxd5 Nxd5 Qd8 Nd4 O-O Kb1 Nc5 Rdg1 Ne6 Nf3 Bc6 Nxe7+ Qxe7 Bf1 Rad8 Rg2 Nf4 Qd2"""
,"""d4 d5 Nf3 e6 Bf4 Nc6 Nc3 Nf6 a3 h6 Ne5 Nxe5 Bxe5 Nd7 Bf4 Nf6 g3 Bd6 e3 Bxf4 exf4 O-O Bd3 a5 Nb5 c6 Nc3 b5 Ne2 a4 O-O Qd6 Rc1 Ba6 b3 axb3 cxb3 Rac8 b4 Ra8 Qc2 Bb7 Rfe1 Rxa3 Qb2 Rxd3 Red1 Rxd1+ Rxd1 Ne4 Nc3 Nxc3 Qxc3 Qe7 Ra1 Ra8 Rxa8+ Bxa8 h4 g6 Qa3"""
,"""e4 e5 f4 exf4 Nf3 Nf6 Nc3 Bd6 e5 Bxe5 Nxe5 O-O d4 d6 Nf3 Bf5 Bxf4 Re8+ Be2 Nbd7 O-O c6 Bd3 Bxd3 Qxd3 Qb6 Ng5 c5 d5 c4+ Be3"""
,"""e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 Qe7 Bxf7+ Kd8 Nc3 d6 Bb3 Bg4 Nf3 g6 h3 Bxf3 Qxf3 Bh6 O-O g5 d3 Qe6 Qg3 Qd7 Bxg5 Rg8 Bxf6+ Ke8 Qxg8+ Bf8 Bg5 Ne7 Bf7+"""
,"""e4 c5 d4 cxd4 c3 dxc3 Nxc3 e6 Bc4 d6 f4 Be7 Nf3 Nf6 O-O O-O Ng5 a6 f5 b5 Bb3 b4 fxe6 bxc3 exf7+ Kh8 Rxf6 Bxf6 Nxh7 Bd4+ Kh1 Qh4 Nxf8 Bb7 Ng6+"""
,"""d4 d5 e3 Nf6 Nf3 e6 Bd3 Nc6 O-O Bb4 c3 Bd6 Nbd2 O-O e4 dxe4 Nxe4 Nxe4 Bxe4 Bd7 Qc2 b5 Bxh7+ Kh8 Be4 g6 Bh6 Kh7 Bxf8 Qxf8 d5 Qh6 g3 Ne5 Nxe5 Bxe5 dxe6 Bxe6 Bxa8 Qh3 Bg2 Qh5 Qe4 Bh3 Bxh3 Bxg3 fxg3 Qxh3 Rxf7+ Kh6 Qf4+ g5 Qf6+ Kh5 Rh7+ Kg4 Rxh3 Kxh3"""
,"""e4 e6 d4 Qh4 Nc3 Qe7 Nf3 Nc6 Bb5 a6 Bxc6 bxc6 O-O Rb8 b3 h5 d5 exd5 exd5 cxd5 Re1 Qxe1+ Qxe1+ Kd8 Nxd5 c6 Nf4 Nf6 Be3 Ng4 h3 Nxe3 Qxe3 d5 Ne5 Ke8 Nxc6+ Kd7 Nxb8+ Kc7 Qe5+ Kb7 Qxd5+ Kxb8 Qxf7 Bd6 Nd5 Rf8 Qxg7 Be6 Rd1 Bc5 Ne3 Rc8 Qe5+ Kb7"""
,"""c3 d5 f3 Nc6 Qb3 Nf6 c4 dxc4 Qxc4 Be6 Qb5 a6 Qxb7 Nd4 Qb4 Nc2+ Kd1 Nxa1 Qa4+ Nd7 Qd4 g5 Qxh8 Nf6 e4 Qd4 Nc3 h6 b3 Kd7 Bb2 Qa7 e5 Nd5 Bxa1 f6 exf6 exf6 Qh7+"""
,"""e4 e5 Nf3 d6 Bc4 Be6 Bxe6 fxe6 d4 exd4 Qxd4 Nc6 Qd1 Be7 Nc3 Nf6 Be3 O-O h3 d5 e5 Ne8 Ne2 Rxf3 gxf3 Nxe5 Ng1 c5 f4 Nc6 Qg4 Qd6 Nf3 Nf6 Qg2 d4 Rg1 Nh5 Bd2 Bf6 Qg4 Qd5 Ne5 Nxe5 fxe5 Qxe5+ Kf1 Rf8 Bh6 Rf7 Re1 Qf5 Qxf5 exf5 b3 Kf8 Bd2"""
,"""e4 e5 Nc3 Nf6 g3 Nc6 Bg2 Bc5 Nge2 d6 d3 Bg4 Na4 Bxf2+ Kxf2 Qd7 Bg5 Nd4 Bxf6 gxf6 Bf1 Qxa4 h3 Bxe2 Bxe2 Nxc2 b3 Qd4+ Kf1 Ne3+ Ke1 Nxd1 Kxd1 Qxa1+"""
,"""e4 e6 d4 d6 Nf3 b6 Bd3 c5 c3 Ba6 O-O Bxd3 Qxd3 Ne7 Bg5 h6 Be3 g6 Nbd2 Bg7 Rac1 O-O Nb3 Nd7 Qd2 Rc8 Bxh6 c4 Bxg7 Kxg7 Na1 Rh8 h3 Rh6 Nc2 Nf6 e5 dxe5 dxe5 Nfd5 Rcd1 Qh8 Ne3 Nf4 Ng4 g5 Nxh6 Qxh6 Qd7 Rh8 Qxe7 Nd5 Qxg5+ Qxg5 Nxg5 Rh5 Nf3 Nf4 Rd7 Ne2+ Kh2 Nf4 Rxa7 Nd3 b3 cxb3 axb3 Nxe5 Nxe5 Rxe5 Rb7 Rc5 Rc1 b5 c4 bxc4 Rxc4 Re5 Rcc7 Rf5 b4 e5 b5 Rxf2 b6 e4 Re7 Re2 Re5 e3 Rbe7 Rb2 b7 e2 Rxe2 Kf6 Rxb2 Kxe7 b8=Q Kf6 Qb7 Ke5 g4 Kf6 Rf2+ Ke6 Qxf7+ Ke5 Qf5+ Kd6 Rf4 Ke7 Qg5+ Kd6 Rf6+ Ke7 Qg7+ Ke8 Rf8#"""
,"""e4 b6 Nc3 Bb7 Nf3 Nc6 d4 e6 d5 exd5 exd5 Nce7 Bg5 f6 Bh4 Ng6 Bc4 Bb4 Bg3 N8e7 Qd4 d6 O-O Bxc3 Qxc3 O-O Rad1 Nf5 Rfe1 Nxg3 hxg3 Re8 Nd4 Rxe1+ Rxe1 Qd7 Ne6 Re8 Re3 Qe7 f4 Nf8 Qe1 Nxe6 Rxe6 Kf7"""
,"""Nf3 b6 g3 Bb7 Bg2 Bxf3 Bxf3 c6 e3 d5 d4 e6 c3 f6 Bd2 Bd6 O-O Qc7 Na3 g5 Nc2 h5 Bg2 g4 f4 f5 e4 fxe4 Ne3 Nf6 f5 exf5 Nxf5 Qd7 Bg5 Kf7 Bxf6 Kxf6 Nxd6+ Ke6 Nf5 Rf8 Ng7+ Ke7 Rxf8 Kxf8 Nxh5 Qf5 Nf4 Nd7 Qb3 Re8 Rf1 Qg5 Nxd5+"""
,"""e4 e5 Nc3 Nc6 g3 g6 Bg2 Bg7 d3 Nge7 Nge2 O-O f4 d6 O-O f5 h3 Be6 Be3 Qd7 Kh2 Kh8 Qd2 Rae8 Rae1 d5 Nd1 d4 Bg1 b6 Nf2 h6 g4 Kh7 gxf5 gxf5 Kh1 fxe4 Nxe4 Nd5 fxe5 Bxe5 N2g3 Qg7 Rxf8 Rxf8 Rf1 Rxf1 Nxf1 Qg6 Bh2 Bxh2 Nxh2 Ne5 Nc5 bxc5 Be4 Bf5 Bxd5 Kg7 Qg2 Qg5 h4 Qxg2+ Kxg2 Kf6 Nf3 Ng6 Kg3 Bd7 Bc4 Ne7 Kf4 Nf5 Ne5 Bc8 Ng4+ Kg6 Ne5+ Kg7 h5 Nd6 Nc6 Kf6 Bd5 a6 Bf3 Bd7 a4 Be8 a5 Bf7 Nb8 c4 Nxa6 cxd3 cxd3 c6 Nc5 Nc8 a6 Na7 Nb7 Be8 Na5 Bd7 Nc4 Ke7 Nb6 Be8 Ke5 Kd8 Nc4 Kc7 Nd6 c5 Nf5 Kb6 Nxh6 Nb5 Nf5 Bxh5 Be4 Bg6 Nd6 Bf5 Nxb5 Bc8 a7 Bb7 Bxb7 Kxb7 Kd6 Kb6 Kd5 c4 Kxc4 Ka5 Kxd4 Kb6 Kd5 Kb7 a8=Q+ Kxa8 Kc6 Kb8 d4 Kc8 d5 Kd8 d6 Ke8 d7+ Kf7 Kd6 Kf6"""
,"""e4 g6 Nf3 Bg7 Nc3 c5 d4 cxd4 Nxd4 Nc6 Be3 Qa5 Bc4 Nf6 f3 O-O Qd2 d5 exd5 Nb4 Nb3 Qd8 O-O-O a5 a3 a4 Nc5 Na6 Nxa6 bxa6 Nxa4 Bd7 Nb6 Rb8 Nxd7 Nxd7 Bb3 a5 a4 Qc7 Kb1 Rb4 h4 Rfb8 h5 Nc5 Bxc5 Qxc5 hxg6 hxg6 d6 exd6 Qxd6 Qxd6 Rxd6 Rd4 Ra6 Rd2 Rxa5 Rxg2 Ra7 Bf6 Bxf7+ Kf8 Bb3 Rxb3 cxb3 Rxb2+ Kc1 Rxb3 Rf1 Rc3+ Kd2 Rc8 a5 Bc3+ Kd3 Bf6 a6 Rc3+ Ke4 Ra3 Rg1 Ra4+ Kd3 g5 Rg4 Ra3+ Ke4 Ra4+ Kf5 Bd4 Ra8+ Ke7 Rxg5 Ra5+ Kg4 Ra4 f4 Be3 Rf5 Bf2 Re5+ Kf6 a7 Bd4 Rd5 Be3 Rf5+ Kg6 Rg8+"""
,"""e4 d5 exd5 Nf6 c4 a5 d3 a4 Bg5 Bd7 Bxf6 exf6 Be2 b5 Nd2 Bb4 a3 Bxd2+ Qxd2 O-O Nf3 Bg4 O-O bxc4 dxc4 c6 h3 Bf5 Nd4 cxd5 Nxf5 dxc4 Qxd8 Rxd8 Bxc4 Nc6 Rad1 g6 Ne7+ Nxe7 Rxd8+ Rxd8 Re1 Nf5 Bb5 Ra8 Re4 Rb8 Rb4 Nd6 Bxa4 Rxb4 axb4 Nc4 b3 Nb6 Kf1 Kf8 Ke2 Ke7 Kd3 Kd6 Kd4 Nd5 Kc4 f5 g4 fxg4 hxg4 h5 gxh5 gxh5 Be8 h4 Bxf7 Nf4 b5 h3 b6 h2 b7 h1=Q b8=Q+ Ke7 Qc7+ Kf6 Qxf4+ Ke7 Bd5 Qf1+ Kc5 Qd3 Bc4 Qc3 Qe4+ Kf6 f4 Kg7 Qd4+ Qxd4+ Kxd4 Kf6 Ke4 Ke7 f5"""
,"""e4 c5 d3 e6 Be2 d5 h3 Nc6 Nf3 dxe4 Kd2 exf3 gxf3 Bd7 Ke1 Nf6 Bg5 Be7 Bxf6 Bxf6 Nd2 e5 b3 Be6 a3 a6 c3 O-O d4 Re8 dxe5 Bxe5 Rc1 b6 Bd3 g6 Kf1 Bf6 Be2 Bd7 Ne4 Be6 Nxf6+ Qxf6 Qd6 Rac8 Rd1 Bf5 Qf4 Kg7 Qg3 Be6 f4 Rcd8 Bf3 Rxd1+ Kg2 Rxh1 Kxh1 h6 Kg2 Bf5 a4 b5 Bg4 Bxg4 Qxg4 bxa4 bxa4 c4 Qg3 Qe6 Kf3 f5 Kg2 Qe4+ Qf3 Qxf3+ Kxf3 Kf6 Kg2 Re2 Kf1 Rc2 Ke1 Rxc3 Kd2 Rf3 Ke2 Rxh3 Kd2"""
,"""e4 c5 Nc3 Nc6 Bc4 e6 d3 a6 a3 b5 Ba2 g6 Nge2 Bg7 Bd2 Nge7 O-O O-O f4 h6 Ng3 Kh7 Rb1 f5 Nce2 Nd4 c3 Nxe2+ Qxe2 Bb7 e5 Qc7 Rbc1 Rad8 Be3 Qc6 Rf2 Nd5 d4 c4 Bb1 Rf7 Bd2 Bf8 Rcf1 a5 Qe1 a4 Qe2 Be7 Be3 Bh4 Qf3 Rdf8 Bd2 Kg7 Ne2 Bxf2+ Rxf2 Qc7 Qg3 Qd8 h4 Kh7 Kh2 Rg7 Ng1 Rfg8 Nh3 Ne7 Kg1 Bd5 Bc2 d6 Bd1 dxe5 dxe5 Be4 Be3 Nd5 Bd4 Qc7 Bf3 Bxf3 gxf3 Rd7 Rg2 Rdg7 Qh2 Qc6 Ng5+ hxg5 hxg5#"""
,"""d4 d5 Nf3 Nf6 c4 e6 a3 Be7 Nc3 a6 cxd5 Nxd5 Ne4 Nc6 Bg5 O-O Bxe7 Qxe7 e3 Bd7 Bd3 Rfe8 O-O b6 Nfg5 h6 Qh5 Nf6 Nxf6+ Qxf6 h4 hxg5 Qh7+ Kf8 hxg5 Qxg5 f4 Qf6 g4 g6 f5 Qg7 fxg6 Qxh7 gxh7 Kg7 g5 Ne7 g6 Nxg6"""
,"""e4 c5 d4 cxd4 Qxd4 Nc6 Qd1 d6 Bd3 Nf6 Nc3 a6 Bg5 e6 Nge2 Be7 Qd2 h6 Bxf6 Bxf6 O-O Ne5 a3 Nxd3 cxd3 O-O Rab1 b5 Kh1 Bb7 f4 Rc8 Ng3 g6 f5 g5 fxe6 fxe6 Nh5 Rf7 d4 d5 e5 Bh8 Qd3 Rcc7 Qg6+ Bg7 Nxg7 Rxg7 Qxe6+ Kh7 Rf6 Qxf6 exf6 Rgf7 Rf1 Kg8 Nxd5 Bxd5 Qxd5 Rd7 Qa8+ Kh7 Qxa6 Kg6 Qxb5 Rxd4 Qf5+ Kh5 h3 Rf4 g4+ Kh4 Rxf4 gxf4 Qxf4 Kxh3 Qxh6+ Kg3 Qg7"""
,"""e4 d5 d3 dxe4 dxe4 Qxd1+ Kxd1 f5 exf5 Bxf5 Bd3 Be6 Nf3 Nc6 Be3 O-O-O Nc3 Nf6 Re1 Bg4 Ke2 Nd4+ Bxd4 Rxd4 h3 Bxf3+ Kxf3 g5 Rad1 Kb8 Nb5 c6"""
,"""e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 a4 g6 Bc4 Bg7 O-O O-O Qe2 Re8 Rd1 Bg4 f3 Bd7 Nb3 Nc6 Bf4 Nh5 Qe3 Nxf4 Qxf4 Ne5 Nd2 Qb6+ Kh1 Rac8 Bb3 Be6 Nd5 Bxd5 exd5 Nd7 Nc4 Qb4 Ne3 Qxf4"""
,"""e3 e6 d4 c5 c4 d5 Nc3 Nf6 Nf3 cxd4 exd4 Bb4 Bd2 O-O h3 Nc6 Be2 dxc4 Bxc4 b5 Be2 Qb6 a3 Bxc3 Bxc3 a5 b4 Nd5 Bd2 axb4 axb4 Rxa1 Qxa1 Ncxb4 Bxb4 Nxb4 Qb2 Nd5 Qxb5 Ba6 Qxb6 Nxb6 Bxa6 Ra8 Bb5 Ra1+ Ke2 Rxh1 d5 h5 d6 Kf8 d7 Ke7 Ne5 f6 Ng6+ Kd8 Nf8 Rb1 Nxe6+ Ke7 d8=Q+ Kxe6 Qxb6+ Kf5"""
,"""d4 b6 f3 Bb7 e4 g6 Be3 Bg7 Nc3 c6 Bd3 d6 Nge2 Na6 a3 Nc7 O-O e6 Rb1 Ne7 Qd2 h6 b4 Qd7 Nd1 O-O-O c4 Kb8 a4 g5 b5 f5 bxc6 Nxc6 d5 Na5 Nd4 f4 Bf2 Bxd4 Bxd4 e5 Bc3 Qxa4 Ra1 Qd7 Bxa5 bxa5 Qxa5 a6 Nc3 Rc8 c5 dxc5 Rfb1 Ka8 Qb6 Nxd5 exd5 c4 Be4 Rh7 d6 Rg7 Rxa6+"""
,"""d4 Nf6 Bf4 g6 Nc3 Bg7 Nb5 c6 Bc7 Na6 Bxd8 cxb5 Ba5 b6 Bd2 Bb7 e3 O-O Be2 d5 Bf3 Nc7 c3 Nd7 Qe2 f6 Nh3 e5 dxe5 fxe5 O-O-O e4 Bg4 Ne5 f4 Nd3+ Kb1 b4 Nf2 Ba6 cxb4 Bxb2 Nxd3 Bxd3+ Kxb2 Bxe2 Bxe2 Ne6 Bc3 Rac8 Bd4 Nxd4 Rxd4 Rfd8 Rhd1 a5 Rxd5 Rxd5 Rxd5 axb4 Kb3 Rc5 Bc4 Rxd5 Bxd5+ Kg7 Kxb4 Kf6 Kb5 Kf5 Kxb6 Kg4 a4 h5 a5 g5 fxg5 Kxg5 Bxe4 Kg4 Bf3+ Kh4 a6"""
,"""d4 e6 c4 d5 Nf3 Nf6 g3 dxc4 Nbd2 Be7 Nxc4 O-O Bg2 a6 O-O Nbd7 a3 Rb8 b4 b5 Nce5 Bb7 Bb2 Nd5 Rc1 f6 Nd3 e5 dxe5 fxe5 Ndxe5 Nxe5 Nxe5 Nb6 Qb3+ Nc4 Bxb7 Rxb7 Nxc4 bxc4 Qxc4+ Kh8 Qc3 Bf6 Qb3 Bxb2 Qxb2 Qe7 e3 Re8 Qd4 Rb5 Rfd1 Rh5 Qd7 Qe4 Rxc7 Rg5 Rd4 Qb1+ Kg2 Qg6 Rc8 Rg8 Rxg8+ Kxg8 Qc8+ Kf7 Rd7+ Ke6 Qc6+ Ke5 Rd5+ Ke4 Rxg5+ Kd3 Qxg6+ hxg6 Rxg6 Kc4 Rxa6 Kb5 Rg6 Ka4 Rxg7 Kxa3 Rg4 Kb3 h4 Kc3 Rf4 Kb3 h5 Ka4 h6 Kb5 h7 Ka4 h8=Q Kb5 Rf5+ Ka4 Qe5 Kb3 b5 Kc4 b6 Kd3 b7 Ke2 b8=Q Kd3 Qd4+ Ke2 Qbb2+ Ke1 Qdd2#"""
,"""e4 e5 Nf3 Nc6 Bc4 h6 c3 Nf6 d3 a6 h3 Bc5 Be3 Bxe3 fxe3 O-O d4 Nxe4 Bd5 Ne7 Bxe4 d5 Bc2 e4 Ne5 a5 O-O b5 b3 Nf5 Na3 Nxe3 Qe2 Nxf1 Rxf1 f6 Nc6 Qd6 Qxb5 Ba6 Qa4 Bxf1 Kxf1 Qf4+ Kg1 e3 Ne7+"""
,"""e4 d6 Nf3 f5 exf5 Bxf5 Bc4 Nf6 Ng5 d5 Qf3 Bg6 Bd3 Bxd3 Qxd3 h6 Qg6+ Kd7 Nf7 Qe8 Ne5+ Kc8 Qf5+"""
,"""e4 d5 exd5 Qxd5 Nc3 Qa5 d4 c6 Bc4 Bf5 Ne2 e6 O-O Nf6 Bb3 Nbd7 Ng3 Bg6 Bf4 Bb4 Nce2 O-O c3 Be7 Nc1 Rfe8 Nd3 Qd8 Ne5 Nxe5 Bxe5 Nd7 Qe2 Bf6 f4 a5 Ne4 a4 Bc2 b5 a3 Qb6 Nd6 Red8 f5 exf5 Nxf5 Re8 Nd6 Re6 Bxg6 hxg6 Qg4 Nxe5 Qg3 Nc4 Nxc4 bxc4 Rae1 Rae8 Rxe6 Rxe6 Qg4 Qxb2 h4 Qxc3 Kh1 Qxd4"""
,"""d4 d5 Bf4 Nf6 e3 g6 Nf3 Bg7 Bd3 O-O Ne5 c6 c3 Nbd7 Nd2 Nxe5 Bxe5 Nd7 Bxg7 Kxg7 f4 c5 g4 cxd4 exd4 e5 fxe5 Nxe5 dxe5 Qh4+ Kf1 Bxg4 Nf3 Qh3+ Kf2 Rae8 Be2 Bxf3 Bxf3 Rxe5 Qd4 f6 Qg4 Qh6 Rae1 Rg5 Re7+ Kh8 Qd7"""
,"""e4 e5 Bc4 Nc6 a3 Nf6 Nc3 a6 d3 b5 Ba2 Bd6 Nge2 O-O O-O Bc5 Ng3 Ba7 Bg5 d6 Nd5 Bg4 Qc1 Nd4 c3 Ne2+ Nxe2 Bxe2 Re1 Bxd3 Nxf6+ gxf6 Bh6 Re8 Qd1 Bc4 Qg4+ Kh8 Qg7#"""
,"""e4 e6 d4 Nf6 e5 Ne4 Nf3 Nxf2 Kxf2 Nc6 Bg5 Be7 Qd2 d6 Nc3 O-O h4 dxe5 Bxe7 Qxe7 dxe5 Qc5+ Ke1 Nxe5 Ng5 Nc4 Qd3 Nxb2 Qxh7#"""
,"""e4 e5 d3 d6 Nc3 f5 Nf3 fxe4 dxe4 Nf6 Bg5 Nc6 Nd5 Be7 Nxe7 Qxe7 Bc4 Be6 Bxf6 gxf6 Bxe6 Qxe6 Qd5 Ke7 Qxe6+ Kxe6 O-O-O Raf8 Nh4 Ne7 g3 f5 exf5+ Nxf5 Nxf5 Rxf5 Rd2 Rhf8 Re1 c6 f4 d5 fxe5 Rxe5 Rde2 Rxe2 Rxe2+ Kd7 Kd2 Rf3 Re3 Rxe3 Kxe3 Ke6 Kf4 c5 Kg5 d4 Kf4 Kd5 Kf3 c4 Ke2 b5 Kd2 a5 Ke2 b4 Kd2 Ke4 Ke2 h5 h3 Ke5 Kf3 Kf5 Kf2 Ke4 Ke2 a4 Kf2 d3 cxd3+ Kxd3 Kf3 c3 g4 c2 gxh5 c1=Q Kg4 Qh6 h4 Kc2 Kf5 Kxb2 Ke4 Qxh5"""
,"""e4 c5 Nc3 Nc6 g3 e5 Bg2 Nf6 d3 Nd4 f4 d6 h3 Be7 Nf3 Nxf3+ Qxf3 exf4 Bxf4 Qb6 Nd1 Qb4+ Bd2 Qb6 Bc3 O-O O-O c4+ d4 h6 Kh2 a5 Ne3 Qc7 Rf2 b5 a3 Bb7 d5 Bc8 Nf5 Bxf5 Qxf5 Rab8 Raf1 b4 axb4 axb4 Bxf6 Bxf6 Qg4 Bxb2 Rf5 Be5 Rh5 Kh7 Rhf5 b3 cxb3 Rxb3 R1f3 c3"""
,"""c4 e5 Nc3 f5 g3 Nf6 Bg2 Be7 Nf3 O-O O-O e4 Ne1 c6 d3 d5 cxd5 cxd5 dxe4 dxe4 Qb3+ Kh8 e3 Nc6 Bd2 Qe8 Nb5 Qd8 Rd1 Qb6 Bc3 a6 Nd6 Qxb3 axb3 Bxd6 Rxd6 Ng4 Nc2 Nge5 Nd4 Nf3+ Nxf3 exf3 Bxf3 f4 Bxc6 bxc6 Rxc6 Bb7 Rc7 f3 Bxg7+ Kg8 Bxf8 Rxf8 Rxb7"""
,"""e4 e5 Bc4 Nc6 Nf3 h6 d4 exd4 Nxd4 Bc5 Bxf7+ Kxf7 Qh5+ g6 Qxc5 Nxd4 Qxd4 Qf6 Be3 c5 Qxc5 Qxb2 Bd4 Qc1+ Ke2 Qxh1 Bxh8 Qxg2 Nd2 d6 Qd4 Bg4+ Kd3 Qxh2 Qg7+ Ke6 Qxg6+ Kd7 Qxg4+ Ke7 Qg7+ Ke6 Ke2 Qh5+ Nf3 Qb5+ Ke1 Qb4+ Nd2 Rc8 Qg6+ Kd7 Qf5+ Ke7 Qxc8"""
,"""e4 e6 d3 b6 Bd2 Bb7 Be2 f5 Bc3 Nh6 exf5 Nxf5 Nf3 Bd6 g4 Nh4 Bxg7 Rg8 Be5 Bxe5 Nxe5 Bxh1 f3 Qg5 d4 d6 Nd3 Qe3 Qd2 Nxf3+ Kd1 Nxd2 Nxd2 Nc6 c3 O-O-O Nf1 Qh3 Nf4 Qh6 Nh5 Rdf8 Kc2 Be4+ Kb3 Na5+ Ka3 Qg5 Ba6+ Kd7 Nfg3 Qd5 Nxe4 Qxe4 Bb5+ c6 Ba6 b5 Kb4 Nc4 Kb3 Qd5 Kc2 Ne3+ Kd3 Nxg4 Ng3 Qc4+ Kc2 Rf2+ Kd1 Rf1+ Nxf1 Qxf1+ Kc2"""
,"""e4 e5 Nf3 d6 d4 exd4 Qxd4 c5 Qa4+ Nc6 Bb5 Bd7 Qb3 Be7 Bc4 Be6 Bxe6 fxe6 Qxe6 Qd7 Qxd7+ Kxd7 O-O Nf6 Nbd2 b5 b3 a6 Bb2 h6 Rfe1 Rhg8 e5 dxe5 Nxe5+ Nxe5 Bxe5 Kc6 Nf3 Nd5 Rad1 Rad8 a3 g5 h3 h5 Nh2 Bd6 Bxd6 Rxd6 Rd3 a5 c4"""
,"""d4 Nf6 Bg5 d5 e3 Nbd7 f4 g6 Nf3 Bg7 Bd3 c6 O-O h6 Bh4 O-O c3 Qb6 Qc2 e6 Nbd2 a5 Ne5 a4 b4 Qd8 a3 Nxe5 fxe5 g5 exf6 gxh4 fxg7 Kxg7 Bh7 h3 Nf3 hxg2 Kxg2 f5 Rg1 Kxh7 Kf2 Rg8 Ne5 Bd7 Qe2 Rg5 Nf7 Qf6 Nxg5+ hxg5 Qh5+ Qh6 Qxg5 Qxg5 Rxg5 Rf8 Rag1 f4 Rg7+ Kh8 Rxd7"""
,"""d4 d5 a3 Nf6 Bg5 e6 Bxf6 Qxf6 Nc3 c6 e4 Nd7 e5 Qh4 g3 Qg5 f4 Qd8 Bd3 c5 Nb5 cxd4 Nd6+ Bxd6 exd6 Nf6 Bb5+ Bd7 Bxd7+ Qxd7 Qxd4 Qxd6 O-O-O O-O Nf3 Rac8 c4 Ne4 b3 Rfd8 Qxa7 Nc3 Kb2 Nxd1+ Rxd1 Qa6"""
,"""d4 d5 c4 e5 dxe5 d4 Nf3 Nc6 Bf4 Bb4+ Nbd2 Nge7 a3 Bxd2+ Qxd2 Ng6 Bg3 Be6 e3 dxe3 Qxe3 O-O Bd3 Qe7 O-O Bg4 Rfe1 Rad8 Bxg6 fxg6 e6 Bxf3 gxf3 Nd4 f4 g5 Rad1 c5 fxg5 Nf3+ Kh1 Nxe1 Rxe1 b6 f4 Qb7+ Kg1 Qe7 h4 Rd4 b3 h6 Kg2 hxg5 hxg5 Qb7+ Qf3 Rd2+ Bf2 Qe7 Kg3 Rd4 Bxd4 cxd4 Qe4 Qxa3 Qd3 Qd6 Re4 Re8 Qxd4 Qxd4 Rxd4 Rxe6 Rd8+ Kh7 Rd5 Re3+ Kg4 Rxb3 g6+ Kxg6 Rd6+ Kh7 Rd7 a5 Ra7 a4 c5 a3 c6 a2 c7 Rc3 c8=Q Rxc8 Rxa2 Rc7 Rh2+"""
,"""e4 g6 d4 c5 d5 Bg7 c3 d6 Bb5+ Nd7 Qg4 Nf6 Qh3 Nxe4 Bh6 O-O Bxg7 Kxg7 Nf3 Ndf6 O-O Bxh3"""
,"""e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Na5 Bb5+ c6 dxc6 bxc6 Bd3 h6 Ne4 Nxe4 Bxe4 Qd4 Qf3 Bd7 d3 Be7 O-O O-O Qe3 Qd6 Bd2 f5 Bf3 Nb7 a3 f4 Qe2 g5 Bb4 c5 Bc3 Bc6 Qxe5 Bxf3 gxf3 Qxe5 Bxe5 Bd6 Bxd6 Nxd6 Nd2 Nf5 Rfe1 Rfe8 b3 Nd4 Rxe8+"""
,"""d4 Nf6 c4 e6 Nc3 Bb4 Qc2 d5 a3 Be7 e3 O-O Nf3 Nc6 Be2 a6 O-O dxc4 Bxc4 b5 Bd3 h6 b3 Bb7 Bb2 Nd5 Rad1 Nxc3 Bxc3 Bf6 Ne5 Nxe5 dxe5 Be7 Bh7+ Kh8 Rxd8 Raxd8 Be4 c6 b4 Rd7 Rd1 Rfd8 Rxd7 Rxd7 Bd4 f5 Bf3 a5 Bc5 Bd8 Bd6"""
,"""e4 e6 Nf3 d5 exd5 exd5 Bb5+ c6 Ba4 Bd6 Nc3 Ne7 d4 O-O O-O Bf5 Re1 Nd7 Nh4 Nf6 Nxf5 Nxf5 Bg5 h6 Bh4 Nxh4 Bb3 Re8 Ne2 Qc7 g3 Nf3+ Kg2 Nxe1+ Qxe1 Ne4 Qd2 Nxd2"""
,"""Nc3 Nf6 e4 d5 e5 d4 exf6 dxc3 fxg7 cxd2+ Qxd2 Qxd2+ Bxd2 Bxg7 O-O-O O-O Ne2 Nc6 Bc3 Bg4 f3 Bf5 Bxg7 Kxg7 Ng3 Bg6 Bd3 Rad8 Be4 Bxe4 Nxe4 b6 Ng3 e6 c3 f5 Rhe1 Rxd1+ Kxd1 Kf6 Kc2 e5 Nh5+ Kg5 Ng7 Rf7 h4+ Kxh4 Rh1+ Kg3 Rxh7 Kxg2 Kd3 Kxf3 Rh3+ Kg4 Rh1 e4+ Ke3 f4+ Kxe4 Re7+ Kd3 Ne5+ Kc2 f3 Rg1+ Kh3 Nf5 Rf7 Ne3 f2 Rf1 Kg3 Kd2 Kf3 Nd1 Rd7+ Kc2 Ng4 Nxf2 Nxf2 Re1 Ne4 Rf1+ Ke2 Rf3 Kxf3"""
,"""e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nxc6 bxc6 Bc4 d5 exd5 cxd5 Bb3 Nf6 Bg5 Be7 Nc3 O-O Bxf6 Bxf6 O-O Bb7 Ne2 Re8 Ng3 Bxb2 Rb1 Bf6 Nh5 Bg5 Qg4 Bh6 Rbe1 Qg5 Qxg5 Bxg5 Re5 Bh6 Rfe1 Re7 R1e3 Bxe3 fxe3 f6"""
,"""g3 d6 Bg2 Nf6 Bxb7 Bxb7 f4 Bxh1 Nf3 Ne4 d3 Bxf3 exf3 Nf6 g4 Qc8 g5 Nh5 f5 Qxf5 Be3 Qh3 Qe2"""
,"""c4 Nf6 Nc3 e5 e4 Bb4 Nge2 d6 g3 a6 Bg2 Nc6 O-O Bg4 h3 Be6 b3 O-O Bb2 Bc5 Kh2 h6 Na4 Ba7 d4 exd4 Nxd4 Nxd4 Bxd4 Bxd4 Qxd4 Nd7 Rad1 b5 Nb2 Ne5 f4 Ng6 c5 Ne7 cxd6 cxd6 Qxd6 Qa5 Qxe7 Qxa2 Rf2 Bxb3 Nd3 Qa4 Nc5 Qb4 Rd7 Qe1 Rfd2 Bc4 e5 Rac8 e6 Kh8 Rd8 Rcxd8 Rxd8 Rxd8 Qxd8+ Kh7 e7 Bf1 e8=Q Bxg2 Kxg2"""
,"""e4 d5 exd5 Qxd5 Nc3 Qe5+ Qe2 Qxe2+ Bxe2 e6 Nf3 Nf6 Nb5 Nd5 c4 Nb4 Bd1 N8a6 O-O Bc5 a3 Nd3 Be2 Bxf2+ Rxf2 Nxf2 Kxf2 O-O d3 c6 Nc3 g6 Bh6 Rd8 Ne4 f5 Nf6+ Kf7 Nxh7 Rh8 Nfg5+ Kg8 Nf6#"""
,"""c4 c5 Nc3 Nc6 e4 e6 Nf3 Be7 Bd3 Nf6 Bb1 O-O Qc2 e5 d3 d6 Nd2 Nd4 Qd1 b6 Ne2 Bg4 f3 Bh5 g4 Bg6 h4 h6 g5 hxg5 hxg5 Nd7 Nxd4 cxd4 f4 Nc5 f5 Bh7 Qh5 f6 Qxh7+ Kf7 g6+ Ke8 Qxg7 Qd7 Rh8"""
,"""e3 e6 Nf3 c6 Nc3 d5 e4 b5 d3 Nd7 Bg5 f6 Bf4 Ba6 exd5 e5 Be3 cxd5 Nxd5 Qa5+ c3 O-O-O b4 Qa3 Bc1 Qa4 Qd2 Bd6 c4 Bxb4 Nxb4 bxc4 dxc4 Nc5"""
,"""e4 e5 Nc3 Nf6 d3 Nc6 Bg5 Be7 Nf3 O-O Be2 d6 d4 exd4 Nxd4 Nxd4 Qxd4 Be6 O-O c6 e5 dxe5 Qxe5 Bd6 Qe3 h6 Bh4 g5 Bxg5 hxg5 Qxg5+ Kh8 Rad1 Nh7 Qh6 Qe7 Bd3 f5 Rfe1 Rf6 Qh4 Qf7 Re2 Rg8 Rde1 Rfg6 g3 Be7 Qf4 Ng5 h4 Nh3+"""
,"""e4 e6 Nf3 d5 Nc3 c5 exd5 Nc6 Bb5 exd5 O-O Nf6 Ne5 Bd7 Re1 Be7 Nxd7 Qxd7 Qe2 d4 Na4 Nd5 Nxc5 Qc7 c4 Nb4 d3 a6 Bxc6+ Nxc6 Nb3 O-O Qf3 Bf6 Bf4 Qd8 Nc5 Qc8 Ne4 Be7 Nd6 Qd7 c5 b6 Nc4 Bxc5 a3 Qb7 b4 Be7 Bd6 Bxd6 Nxd6 Qd7 Nc4 Rab8 Rac1 f6 Qf4 Kh8 Nd6 Ne5 Nf5 Nxd3 Qg4 Nxc1 Re7 Qxe7 Nxe7 Rfe8 Qd7"""
,"""e4 e5 f4 exf4 Nf3 Be7 Bc4 Bh4+ Kf1 c6 d4 d5 exd5 cxd5 Bd3 Qf6 Qe2+ Be6 Nc3 Ne7 Bd2 Nbc6 Bb5 O-O Qd3 Nb4 Qe2 Nxc2 Rd1 a6 Bd3 Nb4 Bb1 Bg4 a3 Nbc6 Qd3 g6 Nxh4 Bxd1 Nf3 Bxf3 Qxf3 Nxd4 Nxd5 Nxd5 Qxd5 Rfd8 Qc4 f3 g3 b5 Qc5 Rac8 Qa7 Nb3 Bc3 Qe6 Qf2 Rd1+ Be1 Rxb1"""
,"""e4 e5 Nf3 Nf6 Nc3 Nc6 Bc4 Bc5 d3 O-O Bg5 d6 Nd5 Bxf2+ Kxf2 Ng4+ Kg1 Qd7 h3 h6 hxg4 hxg5 Nxg5 Qxg4 Qxg4 Bxg4 Kf2 Be6 Rh3 Bxh3"""
,"""b4 c6 Bb2 d5 e3 Nf6 Nf3 g6 a3 Bg7 Be2 O-O O-O a5 d3 axb4 axb4 Rxa1 Bxa1 Qb6 Qe1 Bg4 Nbd2 Qxb4 c4 Qb6 cxd5 Nxd5 Bxg7 Kxg7 Qc1 Nd7 Nc4 Qc7 Qb2+ Kg8 Rb1 b5 Ncd2 Rc8 Rc1 Qd6 Nb3 e6 Nbd4 Bxf3 Bxf3 e5 Nxb5 Qb4 Qxb4 Nxb4 Nd6 Ra8 Ne4 Nxd3 Rb1 c5 Nf6+ Nxf6 Bxa8 Kg7 Rb7 e4 Re7 c4 Rxe4 Nxe4 Bxe4 Nc5 Bc2 f5 Kf1 Kf6 Ke1"""
,"""d4 d5 Nc3 Nf6 Bf4 Bf5 e3 Nc6 Nb5 e6 Nxc7+ Kd7 Nxa8 Qxa8 Bb5 Bg4 Nf3 a6 Bxc6+ bxc6 O-O Bf5 Ne5+ Ke7 c3 Qb8 b4 Nh5 Nxc6+"""
,"""e4 g6 d3 Bg7 Nf3 b6 Be2 Bb7 Kf1 e6 g3 Ne7 Kg2 O-O c3 d5 exd5 Bxd5 d4 c6 Bg5 f6 Be3 g5 Re1 g4 c4 gxf3+ Bxf3 Bxf3+ Kxf3 e5 Kg2 exd4 Bxd4 Nf5 Bc3 Qxd1 Rxd1 Na6 Na3 Rad8 Re1 Rde8 Red1 Rd8 Re1 Rde8 Red1 Rd8"""
,"""d4 b6 c4 c5 Nc3 cxd4 Qxd4 d6 Qd1 Nc6 Nf3 Bb7 e4 Qc7 Be2 O-O-O O-O d5 exd5 Ne5 Nxe5 Qxe5 Bf3 e6 Qe2 Qxe2 Nxe2 exd5 Rd1 dxc4 Rxd8+ Kxd8 Bxb7 Bc5 Bf3 Nf6 Be3"""
,"""e4 d6 Qh5 g6 Qd5 e6 Qb3 f6 d4 c6 Nf3 Bd7 g4 h6 h4 b6 Nc3 a6 a4 b5 a5 e5 d5 cxd5 Qxd5 Qxa5 Rxa5 Ke7 Qxa8 Nc6 Qxa6 Nb8 Qb6 f5 gxf5 gxf5 Bh3 fxe4 Nd5+ Kf7 Qxb8 Ne7 Bxd7 exf3 Qe8+ Kg7 Rg1+ Kh7 Nf6#"""
,"""e3 d5 g3 e5 Bh3 Nf6 Bxc8 Qxc8 b3 Be7 Ba3 Bxa3 Nxa3 O-O h3 c5 Nb5 Qd7 a4 a6 Nc3 d4 exd4 exd4 Nce2 Re8 Kf1 Nc6 Nf3 Re7 Kg2 Rae8 Re1 Qd5 c4 dxc3 dxc3 Ne5 Qxd5 Nxd5 Nxe5 Rxe5 Kf1 b5 c4 Nb4 Nf4 g5 Rxe5 Rxe5 Nh5 g4 Nf6+ Kg7 Nxg4 Re6 Re1 Rxe1+ Kxe1 Nc2+ Kd2 Na3 Ne3 Kf6 Nd5+ Ke5 Nc7 bxc4 bxc4 Nxc4+ Kc3 Nd6 f4+ Ke4 Nxa6 Kd5 Nc7+ Kc6 Na6 Kb6 Nb8 Ka5 Nd7 Ne4+ Kc4 Kxa4 Nxc5+ Ka3 Nxe4"""
,"""e4 e5 g3 Nc6 c3 Nf6 d4 d6 dxe5 Nxe5 h4 Bd7 Bh3 Bxh3 Nxh3 Nfg4 Bg5 f6 f4 fxg5 fxe5 Nxe5 Nxg5 Qe7 Rf1 O-O-O a4 h6 Nf3 Ng4 Nfd2 Ne3 Qf3 Nxf1 Qxf1 Qe5 Qf3 Be7 a5 Rhf8 Qg4+ Kb8 a6 b6 Nc4 Qc5 Nbd2 Qf2+ Kd1"""
,"""e4 e5 Nf3 Nc6 Bc4 h6 h3 Nf6 d3 Bb4+ Bd2 Qe7 a3 Bc5 b4 Bd4 Bc3 a6 Bxd4 Nxd4 Nxd4 exd4 O-O d6 Qf3 Qe5 Nd2 Bd7 a4 g5 b5 g4 hxg4 Bxg4 Qg3 Ke7 Qxe5+ dxe5 f3 Bh5 g4 Bg6 bxa6 bxa6 Rab1 Rhg8 Rb7 Kd6 Rf2 Nd7 f4 exf4 Rxf4 h5 g5 Ne5 Rf6+ Kd7 Bd5 h4"""
,"""h3 e5 a3 d5 d3 c5 e3 d4 e4 Nc6 g3 Be6 Bg2 Qd7 Ne2 O-O-O Bd2 Be7 O-O Bxh3 b4 cxb4 axb4 Bxg2 Kxg2 Bxb4 Bxb4 Nxb4 Rxa7 Kb8 Ra5 Qc7 Ra4 Qc6 Rxb4 f5 Nd2"""
,"""d4 g6 c4 Bg7 Nf3 Nf6 Nc3 O-O Bf4 d6 e4 Nbd7 Bd3 e5 dxe5 dxe5 Bg3 Nh5 O-O Nxg3 hxg3 Nc5 Nd5 c6 Nb4 a5 Nxc6 bxc6 Bc2 Ba6 Qxd8 Rfxd8 b3 Bc8 Rfd1 Be6 Rxd8+ Rxd8 Rd1 Rxd1+ Bxd1 Nxe4 Bc2 Nd6 Ng5 Bf5 Bd1 Ne4 Nxe4 Bxe4 f3 Bb1 a3 c5 Kf2 e4 f4 Bd4+ Ke2 Bd3+ Kd2 Bf1 Be2 Bxg2 Bg4 Bf3 Bd7 e3+ Kd3 e2 Kd2 Bc3+ Kxc3 e1=Q+ Kb2 Qd2+ Ka1 Qxd7"""
,"""e4 d6 d4 c6 c4 Qa5+ Nd2 Qd5 Qa4 Qe6 Nb3 Qxe4+ Kd2 g6 c5 Bh6+ Kc3 Bg7 Bb5 Nf6 Bg5 Qd5 Re1 Qxg5 Na5 e6 Nxc6 bxc6 Bxc6+ Ke7 Bxa8 Bd7 Qxa7 Nc6 Bxc6 h5 Bxd7 Nxd7 cxd6+ Kxd6 Nh3 Rc8+ Kb3 Qxg2 Qa6+ Rc6 Qb5"""
,"""e4 e5 Nf3 d6 Bc4 Bg4 h3 Bh5 d3 Nf6 Bg5 Be7 Be3 O-O c3 Nc6 Nbd2 Na5 Bb3 Nxb3 Qxb3 b6 Nf1 Nd7 Ng3 Bg6 O-O Kh8 Kh1 f5 exf5 Bxf5 Nxf5 Rxf5 Rae1 Qe8 Qe6 Rf6 Qd5 c6 Qxc6 Rc8 Qd5 Nc5 Bg5 Rf8 Bxe7 Qxe7 Re2 Qd7 Rfe1 Qb5 c4 Qc6 Qxc6 Rxc6 Rd2 Ne6 b4 Nf4 Kh2 Rf6 g3 Ne6 a3 Rf7 a4 Rc8 Ra1 Rxf3 Kg2 Rcf8 Rf1 Nd4 h4 R3f5 a5 Nb3 Rb2 Nd4 Rb3 Nxb3"""
,"""e4 e6 Nf3 c5 e5 a6 Nc3 h6 d4 Qb6 b3 Nc6 Na4 Qa7 c4 cxd4 Nxd4 Nxd4 g3 Bb4+ Bd2 Bxd2+ Qxd2 Nf3+ Ke2 Nxd2 Kxd2 Qd4+ Kc2 Qxa1 Bg2 Qxe5 Nc3 Nf6 f4 Qa5 Ne4 Qxa2+ Kd3"""
,"""e4 e6 d4 c5 Nf3 cxd4 c3 d5 Bd3 dxe4 Bxe4 dxc3 Qxd8+ Kxd8 Nxc3 Bb4 Ne5 Bxc3+ bxc3 Ke8 O-O f6 Nc4 Nd7 Nd6+ Ke7 Ba3 Nh6 Nxc8+ Kd8"""
,"""e4 c5 Nf3 d6 Nc3 Nf6 Bc4 e6 d3 d5 Bb5+ Nbd7 Bg5 a6 Bxd7+ Qxd7 e5 Ng8 O-O Be7 Bxe7 Nxe7 Ne2 Ng6 c3 O-O d4 cxd4 cxd4 b5 Rc1 Bb7 h3 Rac8 Nh2 Rxc1 Qxc1 Rc8 Qg5 Qe7 Qxe7 Nxe7 Rc1 Rxc1+ Nxc1 Nf5 Nb3 f6 Nf3 Kf7 Nc5 Bc8 Kf1 h6 Ke2 a5 a3 a4 g4 g5 gxf5 exf5 e6+ Bxe6 Nxe6 Kxe6 Ke3 f4+ Kd3 Kf5 Nh2 h5 f3 Kg6 Kc3 f5 Kb4 g4 fxg4 fxg4 hxg4 h4 Kxb5 Kg5 Kc5 f3 Nxf3+ Kxg4 Nxh4 Kxh4 Kxd5 Kg5 Ke5 Kg6 Ke6 Kg7 Ke7 Kg6 d5 Kf5 d6 Ke5 d7 Kd4 d8=Q+ Ke3 Qd5 Kf4 Kf6 Kg4 Qd4+ Kf3 Kf5 Ke2 Kf4"""
,"""e4 c6 d3 Qa5+ Bd2 Qb6 b3 e5 Be3 Bc5 Bxc5 Qxc5 Qd2 Na6 a3 Nf6 f3 O-O Ne2 d5 b4 Qb6 d4 exd4 Qxd4 Qd8 e5 Ne8 Nf4 Bf5 Bd3 Bxd3 cxd3 Qg5 O-O Nac7 Nc3 Ne6 Qe3 Nxf4"""
,"""e4 e6 d4 d5 e5 c5 c3 Nc6 f4 Qa5 Nf3 Be7 Bd2 Qb6 Be2 Qxb2 Na3 Qxa3 O-O cxd4 cxd4 Bd8 Bb5 Qe7 Ng5 h6 Qh5 g6 Qh3 Qd7 Nxf7 Qxf7 f5 Bd7 Be2 Nf6 fxg6 Qxg6"""
,"""d4 d5 c4 Nf6 Nc3 Bf5 e3 e6 Nf3 a6 Bd3 Nc6 Bxf5 exf5 O-O dxc4 Ne5 h6 Nxc6 bxc6 Qa4 Qd7 Re1 Ne4 Nxe4 fxe4 Qxc4 Bd6 g3 O-O b3"""
,"""d4 Nf6 Bg5 g6 Bxf6 exf6 e4 Bg7 c4 O-O Bd3 d6 Ne2 Re8 O-O b6 Nbc3 c5 dxc5 dxc5 Bc2 Be6 Ba4 Rf8 Qxd8 Rxd8 Nd5 Na6 Nef4 f5 Bb5 Bxd5 Nxd5 Nb4 Nxb4 cxb4 exf5 Bxb2 Rab1 Bc3 fxg6 hxg6 Bc6 Rac8 Bb5 Rd2 Rbd1 Rcd8 Rde1 Kf8 Re3 Rxa2 Bc6 Rdd2 Bd5 Bd4 Rf3 f6 c5 Rac2 cxb6 axb6 g3 f5 Bb3 Rb2 g4 Kg7 gxf5 gxf5 Be6 f4 Rxf4 b3 Rf3 b5 Bxb3 b4 Be6 Re2 Bd5 Kg6 Rf4 Bc5 h4 Kh5 Bf7+ Kh6 Rf3 Kg7 Bd5 Red2 Bc4 Bd4 Kg2 Bf6 h5 Rd4 Be6 Rh4 Rh3 Rxh3 Kxh3 Bd4 f3 Kh6 Kg4 Bf6 Bf7 Rg2+ Kf4 Bg5+ Kf5 Rd2 Rb1 Rd4 Re1 Rf4+ Ke6 Rf6+ Ke7 Rxf3+ Ke8 Re3+ Rxe3 Bxe3 Kd7 Bd2 Bb3 Bc3 Kc6 Bd2 Kb5 Be1 Ka4 Bc3 Bd1"""
,"""e4 e5 Bc4 Qe7 Qf3 d6 Nh3 Bxh3 Qxh3 Nc6 Bd5 Nd4 Na3 Nf6 c3 Nxe4 cxd4 Ng5 Qe3 exd4 Qxe7+ Bxe7 O-O O-O Bxb7 Rae8 Bc6 Rd8 Nb5 a6 Nxc7 Bf6 Nd5 Ne4 Nxf6+ Nxf6 d3 Rc8 Bb7 Rce8 Bxa6 Re2 b3 Rfe8 Bb5 Re1 Bd2 R1e2 Bxe8 Rxe8 Rac1 d5 a4 h5 Bg5 Ng4 h3 Ne5 Rc5 Nxd3 Rxd5 Nb4 Rxd4 Nc2 Rd2 Nd4 Rxd4 f6 Bd2 g5 a5 Ra8 b4 f5 Rd5 Kf7 Rxf5+ Ke6 Rxg5 Kf6 Rxh5 Kg6 Rc5 Kf6 b5 Rxa5 Bxa5 Ke6 b6 Kd6 Rcc1 Kd7 b7 Kd6 b8=Q+ Kd5 Qd8+ Ke6 Rfe1+ Kf5 Rc4 Kg6 Re5 Kf7 Qe8+ Kf6 Rc6+ Kg7 Re7#"""
,"""e4 c5 d3 Nc6 Nc3 e5 Nf3 d6 Bg5 Nf6 Be2 Be7 Bxf6 Bxf6 O-O O-O Nd5 Nd4 Nxd4 cxd4 Nxf6+ Qxf6 Qd2 b5 f4 a5 fxe5 Qxe5 Rf5 Qe8 Raf1 Bxf5 exf5 f6 Re1 Qf7 Bf3 Ra6 b3 a4 c4 bxc4 dxc4 axb3 Bd5 Rxa2 Bxf7+ Rxf7 Qxd4 h6 Qd5 Ra7 Re8+ Kh7 Qxd6 b2 Rb8 Ra1+ Kf2 b1=Q Rxb1 Rxb1 Qg3 Rb2+ Kg1 Re7 Qg6+ Kh8 Qg3 Rb1+ Kf2 Rb2+ Kf1 Rd7 Qe1"""
,"""d4 f5 c4 Nf6 Nc3 e6 f3 Bb4 e3 O-O Bd3 b6 Ne2 Bb7 O-O Nc6 a3 Be7 e4 fxe4 fxe4 a5 Bg5 Nh5 Rxf8+ Qxf8 Bxe7 Qxe7 d5 Ne5 Qc2 Nxd3 Qxd3 Rf8 Nb5 Qc5+ Qd4 Qxd4+ Nbxd4 exd5 exd5 c6 dxc6 dxc6 Ne6 Re8 N6f4 Nf6 Ng3 c5 Kf2 Ne4+ Nxe4 Rxe4 g3 Rxc4 Re1 Rc2+ Re2 Rxe2+ Kxe2 Kf7 Kd3 g5 Nh5 Kg6 g4 Bf3 h3 b5 Ng3 h6 Ke3 Ba8 b3 b4 axb4 axb4 Kd3 Bc6 Kc4 Kf6 Kxc5"""
,"""e4 e6 c4 d5 cxd5 exd5 exd5 Qxd5 Nf3 Nf6 Nc3 Qe6+ Be2 Bb4 O-O O-O a3 Bd6 d4 Qg4 Ne5 Qh4 g3 Qh5 Bxh5 Bh3 Re1 Re8 Bxf7+ Kf8 Bxe8 Kxe8 Nc4+ Kf7 Nxd6+ cxd6 Nd5 Nbd7 Nxf6 Nxf6 Qb3+ Kg6 Qxb7 Rf8 Qxa7 Rf7 Qa6 Ne4 f4 Nf6 Qxd6 Rd7 Qc5 Rd5 Qc4 Rd7 b4 Ng4 Qd3+ Kh5 Bb2 Rd6 Qd1 Rg6 Re5+ Kh6 Rc1 Nf6 d5 Bg4 Qd2 Nd7 f5+ Rg5 Re6+ Kh5 h3 Bxf5 g4+ Bxg4 hxg4+ Rxg4+ Kh2 Nf6 Qe2 h6 Rg1 g6 Rxg4 Nxg4+ Kg3 g5"""
,"""d4 Nf6 c4 c5 d5 b5 cxb5 d6 Nc3 g6 g3 Bg7 Bg2 a6 bxa6 O-O a7 Nbd7 Nf3 Qa5 O-O Ba6 Re1 Ng4 Bd2 Rxa7 h3 Nge5 Ne4 Qc7 Qc2 Rb8 Bc3 Rab7 Nfg5 h6 f4 hxg5 fxe5 Nxe5 Nxg5 Bf6 Ne4 Bg7 Rf1 Rxb2 Bxb2 Rxb2 Qa4 Bxe2 Qe8+ Bf8"""
,"""e3 d5 Nf3 Nf6 d4 Nc6 Nbd2 Bd7 Bd3 e5 O-O Bd6 c4 O-O c5 Be7 Nxe5 b6 Nxc6 Bxc6 cxb6 axb6 Nf3 Ba4 b3 Bd7 Bb2 Ng4 h3 Nh6 e4 dxe4 Bxe4 c6 d5 cxd5 Bxd5 Ra6 Qd4 Bb5 Qxg7#"""
,"""e4 Nf6 e5 Nd5 d4 d6 c4 Nb6 Nf3 Bg4 Nbd2 dxe5 dxe5 e6 h3 Bxf3 Qxf3 Nc6 Qe3 Be7 Be2 Qd4 Qxd4 Nxd4 Bd3 O-O-O b3 Bb4 a3 Bc3 Rb1 Bxd2+ Kxd2 Rd7 Ke3 g6 Be4 Rhd8 b4 Nf5+ Kf4 Rd4 f3 h6 h4 Nxc4 g4 Ng7 Kg3 Nd2 Rh2 Nxb1 Bxh6 Ne8 Bg5 R8d7 Bxb1 R4d5 Bf4 c6 Bc2 Nc7 h5 gxh5 Rxh5 Nb5 a4 Nd4 Be4 Ne2+ Kh4 Nxf4 Kg5 Nxh5 gxh5 Rxe5+ Kf6 Rxh5 f4 Rh6+ Ke5 Rh5+ Kf6 Rd4 Bc2 Rxf4+ Ke7 Rh8 b5 Rd8 bxc6 bxc6 Bd3 Rd7+ Kf8 c5 Ke8 Rxd3"""
,"""e4 d6 Nf3 Nf6 e5 dxe5 Nxe5 Nbd7 Nc4 a6 Be2 b5 Nca3 Bb7 f3 e6 d4 c5 c3 c4 Qd2 Nd5 b3 cxb3 axb3 Be7 O-O O-O Bd3 N7f6 c4 Bb4 cxb5 Bxd2 Bxd2 axb5 Bxb5 Qb6 Nc3 Qxd4+ Rf2 Nf4 Ne2 Nxe2+ Kf1 Ba6 Rxe2 Bxb5 Nxb5 Qxa1+ Kf2 Qa6 Nc7 Qc6 Nxa8 Rxa8 Bb4 Qb6+ Re3 Ra2+ Kg3 Qxe3 Bc3 Ne4+ Kg4 h6 Kh3 g5 Kg4 Rxg2+ Kh3 Nf2+ Kxg2"""
,"""e4 e5 Nf3 Nc6 Bc4 h6 c3 Nf6 Qe2 Bc5 b4 Bb6 b5 Na5 Nxe5 Nxc4 Qxc4 d5 exd5 Qxd5 Qxd5 Nxd5 O-O Bf5 c4 Nb4 Nc3 Nc2 Rb1 Nd4 Rb2 Bd3 Re1 O-O Nd5 c6 Ne7+ Kh8 Nd7 Rfe8 Rb4 Nc2 Ng6+ fxg6 Rxe8+ Rxe8 h3 Re1+ Kh2 Rxc1 Rb2 Bxf2 bxc6 bxc6 Rb8+ Kh7 Nf8+ Kg8 Nxg6+ Kf7 Ne5+ Ke6 Nxd3 Nd4 Nxc1"""
,"""d4 g6 Nc3 Bg7 Nf3 e6 Bf4 d5 e3 Nd7 Bd3 Ngf6 O-O Nh5 Bg3 Nxg3 hxg3 O-O Re1 f5 b3 Nf6 Ne2 Ne4 c4 c6 Nf4 Qe7 Ne5 Bxe5 dxe5 Bd7 f3 Nxg3 Kf2 Nh5 Rh1 Nxf4 exf4 Rf7 g4 fxg4 fxg4 Rxf4+ Ke3 Qg5 Ke2 Qxg4+ Kd2 Rf2+ Be2 Qd4+ Ke1 Qxd1+ Rxd1 Raf8 Kd2 Rg2 Kd3 Rf4 Bf1 Rg3+ Ke2 Re4+ Kd2 Rxe5 Kc1 Kg7 Bd3 Ree3 Bc2 b6 Rd2 c5 cxd5 exd5 Rxd5 Bc6 Rdh5 Bxh1 Rxh1 h5 Rd1 Rg2 Rd7+ Kh6 Rd6 Re1+ Bd1 Rxa2 Kb1 Rf2 Kc1 Rf3 Kc2 Rf2+ Kc1 h4 Rd3 Rg1 Rh3 g5 b4 Rg3 Rh1 h3 bxc5 bxc5 Bg4 Rxg4 Rxh3+ Rh4 Rxh4+ gxh4"""
,"""d4 Nf6 Bg5 Ne4 Bf4 g5 f3 Nf2 Kxf2 gxf4 Nh3 Bh6 Nc3 d6 Qd3 Bxh3 gxh3 e6 Ne4 Nc6 Qb5 Rb8 Qh5 Bg7 c3 Qe7 Rg1 Bf6 Nxf6+ Qxf6 e3 fxe3+ Kxe3 Kd7 Bb5 a6 Bd3 e5 Bf5+ Ke7 Rg7 exd4+ cxd4 Qxg7 Rg1 Qxd4+"""
,"""Nf3 e6 c4 Bd6 d4 c5 e3 Qc7 Nc3 Nc6 dxc5 Bxc5 Ne4 b6 Nxc5 bxc5 Be2 Bb7 b3 O-O-O a4 d5 Bd2 dxc4 Bxc4 Nf6 Qc2 Ne5 Nxe5 Qxe5 O-O Ne4 Ba5 Rd2 Qc1 Rd7 b4 Nd2 bxc5 Nxf1 Bb5 Rd5 c6 Kb8 cxb7 Kxb7 Qc6+ Kb8 Ba6 Qb2 Bc7#"""
,"""e4 e6 f4 d5 e5 c5 c3 Nc6 d4 Nge7 b3 Nf5 Nf3 cxd4 cxd4 b6 Bd3 Ncxd4 O-O Bc5 Bb2 Nxf3+ Kh1 N3d4 Bxf5 Nxf5 g4 Ne3"""
,"""d4 d5 Nf3 e6 Bf4 Bb4+ c3 Bxc3+ bxc3 Nc6 e3 Qe7 Bb5 Nf6 Nbd2 O-O Rb1 Nxd4 cxd4 c6 Ba4 Bd7 O-O b5 Bb3 a6 Ne5 Ne4 Nxe4 dxe4 a4 f6 Nxd7 Qxd7 axb5 axb5 Qc2 Rac8 Qa2 Rfd8 Bxe6+ Kh8 Bxd7 Rxd7 Qe6 Rcd8 Rbc1 Rxd4"""
,"""d4 Nf6 c4 c6 Nc3 d5 Nf3 dxc4 e4 b5 a4 b4 e5 Nd5 Nxd5 cxd5 a5 e6 Bd2 Na6 Be2 Be7 O-O O-O g3 Bd7 h4 h6 Kg2 Rc8 Rh1 Nc7 g4 a6 Qc1 c3 bxc3 bxc3 Qxc3 Nb5 Qd3 f6 g5 fxe5 dxe5 Rc4 gxh6 Rg4+ Kf1 Nc7 hxg7 Rf5 Qe3 Bb5 Bxb5 Nxb5 h5 Rxg7 h6 Rh7 Rg1+ Kh8 Rg7 Rf7 Rxf7 Rxf7 Ng5 Bxg5 Qxg5 Qxg5 Bxg5 Kh7 f4 Nd4 Kf2 Nc6 Ke3 Kg6 Kd3 Kf5 Rh1 Rh7 Ra1 Rb7 Rh1 Rh7 Ra1 Kg6 Kc3 Rb7 Rh1 Kh7 Rg1 Rb5"""
,"""e4 e6 f4 b6 Nf3 Bb7 e5 Nc6 c3 d5 d4 Na5 Bd3 Nc4 b3 Na5 Nbd2 Nc6 b4 Qd7 a4 f6 O-O fxe5 Nxe5 Nxe5 fxe5 Ne7 Qf3 Nf5 Bxf5 exf5 Qxf5 Qxf5 Rxf5 O-O-O Nf3 g6 Rf7 h5 Ng5 Re8 h4 Be7 Bf4 Bxg5 Bxg5 Ref8 Raf1 Rxf7 Rxf7 Ba6 b5 Bb7 Rf6 Rg8 Kf2 a6 g3 axb5 axb5 Kd7 Rf7+ Ke8 Rxc7 Bc8 Rxc8+ Kf7 Rxg8 Kxg8 Kf3 Kf7 Kf4 Kg7 g4 Kf7 gxh5 Kg7 hxg6 Kxg6 e6 Kh5 Ke5 Kg4 Kxd5 Kf3 Kc6 Kf2 Kxb6"""
,"""d4 e5 d5 Bb4+ c3 Ba5 e4 Qg5 Nf3 Qh5 Bc4 f5 Bg5 Nf6 Nxe5 Qxg5 O-O Nxe4 Nd2 d6 Nxe4 dxe5 f4 Qg4 Ng3 Rf8 d6 cxd6 Be6 Bxe6 Qxd6 Nd7 Rad1 Qh5 Qxd7+ Bxd7 Rxd7 Kxd7 Nxf5 g6 Ng7 Qh6 Nf5 gxf5 fxe5 Qe3+"""
,"""d4 d5 e3 Bf5 c4 Nf6 cxd5 Nxd5 Nc3 e6 Nf3 Bb4 Bd2 c5 a3 Bxc3 bxc3 cxd4 cxd4 O-O Be2 Nd7 O-O Rc8 Rb1 b6 Rb2 Qf6 Be1 Qg6 Nh4 Qf6 f3 Nxe3 Qd2 Nxf1 Kxf1 Rfd8 Rb5 Nc5 f4 Rxd4 Qe3"""
,"""c4 g6 Nc3 Bg7 d4 Nf6 e4 O-O f3 d6 Be3 c5 d5 e6 Qd2 Nbd7 Bh6 Ne5 Bxg7 Kxg7 h4 h5 Be2 exd5 cxd5 a6 O-O-O b5 Nh3 Qa5 Nf2 b4 Nb1 Qxa2 g4 hxg4 fxg4 Nexg4 Nxg4 Bxg4 Bxg4 Nxg4 h5 Qc4+ Qc2 Qxc2+ Kxc2 Nf2 h6+ Kh7 Nd2 Nxd1 Kxd1 f5 Nf3 fxe4 Ng5+ Kh8 Nxe4 Rad8 h7 Rf5 Rh6 Re5 Nf6 Rh5 Nxh5 gxh5 Rxh5 Rd7 Kd2 Rxh7 Rf5 Rd7 Rf8+ Kg7 Ra8 Kf6 Rxa6 Ke5 b3 Kxd5 Ra1 Rh7 Rg1 Rh2+ Kc1 Kd4 Rg4+ Kc3 Rg3+ Kd4 Rg4+ Kd5 Rg5+ Kc6 Rg6 Kb5 Rg5 Rh6 Kd2 Kc6 Kd3 d5 Rg8 Rd6 Rc8+ Kb5 Rb8+ Rb6 Rd8 d4 Ra8 Ra6 Rb8+ Rb6 Ra8 Kc6 Kc4 Rb7 Rc8+ Rc7 Rh8 Kd6 Rh6+ Kd7 Rh7+ Kc6 Rh6+ Kb7 Rd6 Rc6 Rd7+ Kb6 Rd8 Re6 Rb8+ Kc7 Rh8 Rc6 Rh7+ Kb6 Rh8 Rd6 Rb8+ Kc7 Rh8 d3 Rh1 d2 Rd1 Kc6 Rxd2 Rd4+ Rxd4 cxd4 Kxd4 Kb5 Kd5 Kb6 Kc4 Ka5 Kc5 Ka6 Kxb4 Kb6 Kc4 Kc6 b4 Kb6 b5 Kb7 Kc5 Kc7 b6+ Kb7 Kb5 Kb8 Ka6 Ka8 Ka5 Kb7 Kb5 Kb8 Kc6 Kc8 b7+ Kb8 Kb6"""
,"""d4 d5 Nf3 Bf5 Bf4 e6 a3 Nf6 Nc3 Bd6 Bg3 O-O e3 Ne4 Nxe4 dxe4 Ne5 Bxe5 Bxe5 Nc6 Bc4 Nxe5 dxe5 Qg5 Rg1 Bg4 f3 exf3 gxf3 Qh4+ Rg3 Qxh2 Qe2 Qxg3+ Qf2 Qxf3 Be2 Qh1+"""
,"""e4 c5 Nf3 Nc6 Bb5 Nd4 Nxd4 cxd4 d3 a6 Ba4 b5 Bb3 e5 Bd5 Rb8 Qf3 Nf6 Bg5 Be7 Bxf6 Bxf6 Nd2 Bb7 Bb3 O-O O-O Bg5 Rfd1 Bxd2 Rxd2 d6 a3 Qd7 Rc1 g6 c4 bxc4 Bxc4 Kg7 Rdc2 Rbc8 Qe2 f5 f3 fxe4 fxe4 a5 b4 axb4 axb4 Qa4 b5 Rc5 Ra2 Qb4 Rca1 Rc7 Ra4 Qc5 Qb2 Qb6 Qb4 Rf6 Qa5"""
,"""d4 d5 f4 Nf6 Nf3 e6 e3 Nc6 Bd3 Bd7 c3 Be7 O-O O-O Ne5 Bc8 Nd2 h6 Ndf3 Ne4 Qe1 Bf6 a4 a6 b3 Ne7 Bxe4 dxe4 Nd2 Nf5 Nxe4 Nd6 Ng3 Bh4 Ba3 Qf6 Rf3 Qd8 Qe2"""
,"""e4 c5 Nc3 e6 f4 a6 e5 h5 Nf3 Nh6 Ne4 Nc6 d3 d5 exd6 Bxd6 c3 Be7 Be3 b6 h3 Bb7 Qd2 Nf5 O-O-O Qc7 Bf2 h4 Rg1 Ng3 Nxg3 hxg3 Bxg3 O-O-O Be2 f5 Ne5 Nxe5 fxe5 g5 Qe3 f4 Bxf4 gxf4 Qxf4 Rdg8 g4 Bg5 Qxg5 Rxg5 Kc2 Rxe5 Rde1 Re3 g5 Rhxh3 g6 Rh8 Bg4 Rxe1 Rxe1 Bd5 c4 Qh2+ Re2 Qg3 cxd5 Qxg4 dxe6 Qxg6 e7 Re8 Kc3 Qg7+ Kc2 Rxe7 Rxe7 Qxe7"""
,"""e4 e5 Nf3 Bd6 Nc3 Nf6 d3 Nc6 b3 b6 Nb5 a6 Nxd6+ cxd6 Ba3 Qc7 d4 b5 dxe5 dxe5 c4 Qa5+ Nd2 Qxa3 g3 Qa5 Bh3 Nxe4 b4 Qxb4 Rb1 Qa5 O-O Nxd2 Qg4 Nxb1 Rxb1 Qxa2 Qxg7 Qxb1+ Kg2 Qe4+ Kg1 Rf8 c5 Bb7 Bg2 Qe1+ Bf1 O-O-O Qxh7"""
,"""d4 Nf6 c4 d6 e3 g6 Nf3 Bg7 Be2 O-O O-O c5 Nc3 cxd4 exd4 b6 Bg5 Bb7 d5 Nbd7 Qd2 Nc5 Rac1 Nce4 Nxe4 Nxe4 Qe3 Nxg5 Qxg5 Bxb2 Rc2 Bg7 Re1 e5 Qd2 Rc8 Rb1 Qc7 a4 f5 Ng5 Bxd5 Qxd5+ Kh8 Ne6""")
def prod5threecheck = List(
"Nc3 e6 Nb5 Na6 Nxc7 Nxc7 e3 Nf6 Bc4 Bb4 c3 Be7 Nf3 O-O O-O d5 Bd3 Ne4 Qc2 f5 Nd4 Kh8 b4 a5 f3 Nd6 e4 dxe4 fxe4 e5 exf5 exd4 cxd4 Bf6 Bb2 axb4 d5 Nxd5 Bxf6 Qb6 Kh1 Nxf6 Bc4 Ng4 Qb2 Rf6 Rae1 Nf2 Rxf2 Qxf2 Re8 Nxe8 Qxf6 Qg1",
"e3 e6 Nf3 Nf6 c4 g6 b3 Bg7 Bb2 O-O Nc3 d5 Be2 c5 cxd5 exd5 O-O Nc6 d4 cxd4 Nxd4 Nxd4 exd4 Bf5 Bf3 Ne4 Ne2 Rc8 Ng3 Nxg3 hxg3 Re8 Rc1 Rxc1 Qxc1 Be4 Qf4 Bxf3 gxf3 Re2 Ba3 Rxa2 Bd6 g5 Qf5 Qxd6 Qc8 Bf8 Re1 Qb4 Qe8 Rxf2 Qe3 Rf1 Rxf1 h6 Re1 Qd6 Qe5 Qg6 g4 Qc2 Qe3 Qh2 Kxh2 Bd6",
"e4 e6 Nc3 Bb4 Qg4 Qf6 Nf3 Nh6 Qg5 Bxc3 bxc3 Qxg5 Nxg5 f6 Nxe6 dxe6 Bb5 Kf7 f4 Nc6 f5 Re8 fxe6 Bxe6 O-O Kg8 d4 a6 Bxh6 axb5 d5 Ne5 dxe6 Rxe6 Rad1 Rae8 Bxg7 Nf3 gxf3 f5 Kh1 Rg6 Rd7 Rg2 Be5 Rxh2 Bxh2 Re7 Rg1",
"e4 e6 c3 c6 Nf3 Nf6 Qc2 Na6 d4 Nc7 Bd3 g6 Bg5 Bg7 e5 Ncd5 exf6 Bxf6 Bxf6 Qxf6 Nbd2 b6 Ne4 Qe7 Qd2 Bb7 Qg5 Qxg5 Nd6 Ke7 Nxg5 Kxd6 Nxf7 Kc7 Nxh8 Rxh8 O-O-O Nf4 Bf1 d5 g3 Nh5 Re1 Re8 Bh3 Ng7 f4 b5 Re3 Kb6 Rhe1 Bc8 b3 Nf5 Bxf5 gxf5 c4 Kb7 cxd5 cxd5 Rc3",
"e3 e6 Nf3 Nc6 a3 g6 d4 Bg7 Be2 Nf6 O-O O-O c4 d6 Nc3 e5 dxe5 dxe5 Qxd8 Rxd8 Rd1 Rxd1 Bxd1 Be6 b3 Rd8 Bc2 e4 Ng5 Ng4 Nxe6 fxe6 Bb2 Be5 g3 Nxf2 Rb1 Nh3 Kg2 Rd2")
def prod50crazyhouse = List(
"e4 d6 Nc3 Nf6 d4 Bg4 Be2 Bxe2 Ngxe2 B@h5 f3 Nbd7 d5 e5 dxe6 fxe6 Nf4 P@f7 B@b3 Nc5 Be3 Nxb3 axb3 Be7 O-O O-O Ra5 e5 Nxh5 Nxh5 N@f5 B@g5 Bf2 N@f4 P@g3 Nxg2 Kxg2 P@e6 Nxe7 Qxe7 Kh1 N@h3 B@g2 Nxf2 Rxf2 B@b6 Qg1 Bge3 N@d5 exd5 Nxd5 Bxf2 Nxe7 Kh8 Qf1 Bxg3 N@g4 R@h4 Nf5 Bxh2 B@e1 P@f2 Bxf2 Bxf2 Qxf2 N@g3 Nxg3 Bxg3 B@h3 Rxg4 Qd2 N@f2 Kg1 Nxh3 Kf1 Bf4 P@e3 Rxg2 Qxg2 N@g3 Ke1 P@f2 Kd2 B@e1 Kc1 Bxe3 Kb1 Bxa5 Ka2 f1=Q Qxf1 Nxf1 R@b1 B@c3 P@f6 Nxf6 Q@f5 Bxb2 N@c4 P@c3 Qxh3",
"e4 e5 Nc3 Nc6 Nf3 Bc5 Bc4 Nf6 O-O O-O d3 d6 Ng5 Nd4 Be3 Ne6 Nxf7 Rxf7 Bxe6 Bxe6 N@g5 Re7 Nxe6 Rxe6 B@c4 B@f7 Bxc5 dxc5 Bxe6 Bxe6 P@f5 Bf7 B@e6 B@e8 Nd5 B@h5 Nxf6 Qxf6 Qxh5 Bxe6 N@g4 Bxh5 Nxf6 gxf6 R@g3 B@g5 B@e2 N@f4 Bxh5 N@e2 Kh1 Nxg3 hxg3 R@h3 N@h2 Rxh5 B@f3 Rxh2 Kxh2 N@h6 Kh1 N@d4 fxe6 Nxf3 B@f7 Kh8 R@h2 Nxh2 gxf4 N2g4 N@g6 hxg6 Q@h3 Q@h4 fxg5 Qxh3 gxh3 Q@h2",
"e4 e6 Nc3 Nc6 d4 Bb4 Nf3 Nce7 Bd3 Nf6 O-O Bxc3 bxc3 h6 e5 Nfd5 c4 Nb4 B@h4 Nxd3 cxd3 N@f5 N@h5 B@c3 Ba3 g5 Nf6 Kf8 Bxe7 Qxe7 Nxg5 hxg5 Bxg5 B@g6 Nh7 Rxh7 Bxe7 Kxe7 P@f6 Ke8 P@e7 Nxe7 fxe7 Kxe7 N@g8 Kf8 Nf6 N@e7 Q@e8 Kg7 Qxe7 B@b4 c5 N@f5 N@e8 Kh6 Qf8 P@g7 N@g8 Kg5 Qg4",
"f4 e5 fxe5 d6 exd6 Bxd6 P@f2 Nf6 d4 Nc6 Nc3 O-O P@h6 gxh6 Bxh6 Ng4 Bxf8 Qxf8 e3 B@h4 P@g3 Bg5 Nf3 Bh6 Bd3 Bd7 R@h4 P@g7 Bxh7 Kxh7 P@g5 B@e7 Qd3 P@g6 gxh6 gxh6 Rxg4 Bxg4 B@e4 P@f5 N@d7 Qg7 Bxc6 bxc6 N@e5 Bxf3 Nxf3 N@b4 Qd2 B@e6 B@e5 f6 Bxf6 Bxf6 P@b7 Rd8 b8=Q Bxd7 Qxd8 Bxd8 Ne5 Bxe5 dxe5 N@c4 R@d4 Nxd2 Rxd2 Nxc2 Rxc2 N@d3 Kf1 Nxe5 N@f4 B@a6 Kg1 P@h3 Nxh3 P@f3 N@f4 fxg2 Kxg2 P@f3 Kg1 P@g2 P@d4 gxh1=Q Kxh1",
"Nf3 d5 d4 Bf5 Bf4 e6 e3 Bd6 Bb5 Nc6 Nc3 Ne7 O-O O-O Bxd6 cxd6 B@h4 f6 Bd3 B@g6 Bxf5 Nxf5 e4 Nxh4 Nxh4 Bxe4 Nxe4 dxe4 d5 B@g5 Qh5 Bxh4 dxe6 B@g6 B@f7 Rxf7 exf7 Bxf7 Qxh4 N@f4 R@g3 P@g5 Qg4 B@h5 Qf5 N@e2 Kh1 Nxg3 fxg3 Nxg2 P@e6 Ne3 exf7 Bxf7 Qxe4 P@h3 Qxe3 P@g2 Kg1 R@h1 Kf2 gxf1=Q",
"e4 d6 Nc3 Nf6 d4 Bg4 Be2 Bxe2 Ngxe2 B@h5 f3 Nbd7 d5 e5 dxe6 fxe6 Nf4 P@f7 B@b3 Nc5 Be3 Nxb3 axb3 Be7 O-O O-O Ra5 e5 Nxh5 Nxh5 N@f5 B@g5 Bf2 N@f4 P@g3 Nxg2 Kxg2 P@e6 Nxe7 Qxe7 Kh1 N@h3 B@g2 Nxf2 Rxf2 B@b6 Qg1 Bge3 N@d5 exd5 Nxd5 Bxf2 Nxe7 Kh8 Qf1 Bxg3 N@g4 R@h4 Nf5 Bxh2 B@e1 P@f2 Bxf2 Bxf2 Qxf2 N@g3 Nxg3 Bxg3 B@h3 Rxg4 Qd2 N@f2 Kg1 Nxh3 Kf1 Bf4 P@e3 Rxg2 Qxg2 N@g3 Ke1 P@f2 Kd2 B@e1 Kc1 Bxe3 Kb1 Bxa5 Ka2 f1=Q Qxf1 Nxf1 R@b1 B@c3 P@f6 Nxf6 Q@f5 Bxb2 N@c4 P@c3 Qxh3",
"e4 e5 Nc3 Nc6 Nf3 Bc5 Bc4 Nf6 O-O O-O d3 d6 Ng5 Nd4 Be3 Ne6 Nxf7 Rxf7 Bxe6 Bxe6 N@g5 Re7 Nxe6 Rxe6 B@c4 B@f7 Bxc5 dxc5 Bxe6 Bxe6 P@f5 Bf7 B@e6 B@e8 Nd5 B@h5 Nxf6 Qxf6 Qxh5 Bxe6 N@g4 Bxh5 Nxf6 gxf6 R@g3 B@g5 B@e2 N@f4 Bxh5 N@e2 Kh1 Nxg3 hxg3 R@h3 N@h2 Rxh5 B@f3 Rxh2 Kxh2 N@h6 Kh1 N@d4 fxe6 Nxf3 B@f7 Kh8 R@h2 Nxh2 gxf4 N2g4 N@g6 hxg6 Q@h3 Q@h4 fxg5 Qxh3 gxh3 Q@h2",
"e4 e6 Nc3 Nc6 d4 Bb4 Nf3 Nce7 Bd3 Nf6 O-O Bxc3 bxc3 h6 e5 Nfd5 c4 Nb4 B@h4 Nxd3 cxd3 N@f5 N@h5 B@c3 Ba3 g5 Nf6 Kf8 Bxe7 Qxe7 Nxg5 hxg5 Bxg5 B@g6 Nh7 Rxh7 Bxe7 Kxe7 P@f6 Ke8 P@e7 Nxe7 fxe7 Kxe7 N@g8 Kf8 Nf6 N@e7 Q@e8 Kg7 Qxe7 B@b4 c5 N@f5 N@e8 Kh6 Qf8 P@g7 N@g8 Kg5 Qg4",
"f4 e5 fxe5 d6 exd6 Bxd6 P@f2 Nf6 d4 Nc6 Nc3 O-O P@h6 gxh6 Bxh6 Ng4 Bxf8 Qxf8 e3 B@h4 P@g3 Bg5 Nf3 Bh6 Bd3 Bd7 R@h4 P@g7 Bxh7 Kxh7 P@g5 B@e7 Qd3 P@g6 gxh6 gxh6 Rxg4 Bxg4 B@e4 P@f5 N@d7 Qg7 Bxc6 bxc6 N@e5 Bxf3 Nxf3 N@b4 Qd2 B@e6 B@e5 f6 Bxf6 Bxf6 P@b7 Rd8 b8=Q Bxd7 Qxd8 Bxd8 Ne5 Bxe5 dxe5 N@c4 R@d4 Nxd2 Rxd2 Nxc2 Rxc2 N@d3 Kf1 Nxe5 N@f4 B@a6 Kg1 P@h3 Nxh3 P@f3 N@f4 fxg2 Kxg2 P@f3 Kg1 P@g2 P@d4 gxh1=Q Kxh1",
"Nf3 d5 d4 Bf5 Bf4 e6 e3 Bd6 Bb5 Nc6 Nc3 Ne7 O-O O-O Bxd6 cxd6 B@h4 f6 Bd3 B@g6 Bxf5 Nxf5 e4 Nxh4 Nxh4 Bxe4 Nxe4 dxe4 d5 B@g5 Qh5 Bxh4 dxe6 B@g6 B@f7 Rxf7 exf7 Bxf7 Qxh4 N@f4 R@g3 P@g5 Qg4 B@h5 Qf5 N@e2 Kh1 Nxg3 fxg3 Nxg2 P@e6 Ne3 exf7 Bxf7 Qxe4 P@h3 Qxe3 P@g2 Kg1 R@h1 Kf2 gxf1=Q",
"e4 d6 Bc4 Nf6 Nc3 g6 d3 Bg7 h3 O-O Nf3 a6 Bg5 h6 Be3 b5 Bb3 Bb7 Qd2 Kh7 g4 Nfd7 h4 Ne5 Nxe5 dxe5 N@g1 N@g2 Kf1 Nxe3 fxe3 Qd7 Qe2 b4 Nd1 Nc6 Nf2 Rad8 Qf3 B@e6 Bxe6 fxe6 B@f7 B@e8 N@g5 Kh8 h5 Bxf7 Nxf7 Rxf7 Qxf7 g5 B@g6 N@f6 Nf3 B@g8 Nxg5 hxg5 h6 Bxf7 hxg7 Kxg7 B@h6 Kxg6 P@f5 Kh7 Bxg5 P@h5 P@g6 Kg7 Bxf6 Kxf6 g5 Kxg5 R@g1 Kf6 gxf7 P@e2 Kxe2 N@d4 Kf1 Q@e2 Kg2 Qf3 Kh2 Qxf2 Rg2 Nf3 Kh3 Qh4",
"e4 Nc6 Nc3 Nf6 d4 d5 e5 Ne4 Bd3 Nxf2 Kxf2 Nxd4 Nge2 Nxe2 Qxe2 N@g4 Ke1 P@e4 Bb5 P@c6 Bf4 cxb5 Qxb5 c6 Qe2 B@f2 Kf1 Bb6 Rd1 Nf2 Qxf2 Bxf2 Kxf2 d4 Nxe4 Q@f5 N@h3 Qxe4 B@d3 N@g4 Kf1 Qed5 P@e4 Qxa2 N@g5 P@h6 Nf3 Qxb2 P@f5 e6 fxe6 Bxe6 P@f5 P@d7 fxe6 dxe6 B@h5 Ne3 Bxe3 dxe3 N@c4 Qb4 Nxe3 P@g6 Bxg6 hxg6 P@d6 B@b6 N@c7 Bxc7 dxc7 Qxc7 B@d6 Bxd6 exd6 Qbxd6 B@g3 Qxg3 hxg3 N@c3 Q@e5 Nxd1 Nxd1 B@d6 Qxg7 O-O-O P@a6 bxa6 e5 Bc5 P@d6 Qb6 Qxf7 R@b1 P@b7 Kb8 Qc7 Qxc7 dxc7 Kxc7 N@a8 Kxb7 Bxa6 Kxa6 Q@a2 B@a5 Qxb1 Rxd1 Qxd1 N@e3 Ke2 B@b5 R@d3 Nxd1 Nc7 Bxc7 R@a1 B@a5 Kxd1 N@b2 Kc1 Nxd3 cxd3 P@b2",
"e4 e5 Bc4 Nc6 Nf3 Bc5 O-O Nf6 d3 O-O Bg5 h6 Bh4 d6 h3 Na5 Bxf6 Qxf6 N@d5 Qd8 b4 Bxb4 Nxb4 P@b2 Nbd2 B@c3 Rb1 Bxb4 Rxb2 Nxc4 Nxc4 Bc3 Rb3 Bd4 N@d5 B@e7 Nxe7 Qxe7 B@h4 f6 Ne3 c6 B@c4 Kh8 Bg3 b5 Nh4 Kh7 Nef5 Qc7 P@e7 Bxf5 Nxf5 N@g6 exf8=Q Rxf8 B@h5 N@e7 Qg4 P@g5 Nxe7 Nxe7 N@e6 Qb8 Nxf8 Qxf8 R@f7 Qd8 R@d7 Qxd7 Qxd7 bxc4 Qxe7",
"e4 Nf6 Nc3 Nc6 d4 d5 e5 Ne4 Bd3 Bf5 Nge2 e6 O-O Be7 f3 Nxc3 bxc3 Bxd3 cxd3 N@f5 N@h5 f6 B@h3 O-O Bxf5 exf5 e6 B@e8 Nef4 B@g5 Nxg7 Kxg7 P@f7 Bxf4 N@h5 Kh8 Bxf4 Bxf7 B@g7 Kg8 exf7 Rxf7 B@e6 P@g6 Bxf7 Kxf7 Nxf6 Kxg7 R@e8 Bxf6 Rxd8 Rxd8 P@g5 Be7 Qe2 R@f7 Qxe7 Nxe7 Be5 Kf8 Q@h8 B@g8 B@h6 Ke8 Qxg8 Nxg8 Rae1 P@d2 Re2 N@c1 Rxd2 N@e2 Kf2 B@e3 Kxe3 f4 Bxf4 Rxf4 Rxe2 Nxe2 N@g7 Ke7 B@f6 Nxf6 gxf6 Rxf6 B@b4 B@d6 Kxe2 P@c4 P@c2 Bxb4 P@e5 Rc6 Bg5 Kd7 N@e7 Bxe7 Bxe7 N@f4 Ke3 B@d2 Kxd2 Q@e2",
"e4 e5 Nf3 Nc6 Bc4 Bc5 O-O Nf6 Nc3 O-O d3 d6 Bg5 h6 Bxh6 gxh6 P@g6 B@e6 gxf7 Bxf7 Bxf7 Rxf7 B@d5 P@e6 Bxc6 bxc6 P@g5 Ng4 gxh6 Nxf2 Rxf2 Bxf2 Kh1 B@h5 N@g5 Bxf3 h7 Kg7 P@h6 Kg6 Qxf3 Rxf3 Nxf3 B@h3 B@f7 Kxh6 R@g6 Kxh7 N@g5 Kh8 Nxh3 P@g7 Nxf2 P@h7 B@h4 P@e7 Rg3 N@h6 Bh5 R@f8 B@g5 Ng8 Rg1 Rb8 Ng4 Bd7 Nfxe5 dxe5 Nxe5 N@d6 Bg6 hxg6 P@h6 Q@h7 hxg7 Qxg7 P@h6 Qxe5 P@g7 Kh7 gxf8=N Qxf8 Rf3 P@f6 R@g7 Kh8 Rxg6 Qf7 Rg7 N@g4 Rxf7 Qxh2",
"e4 Nc6 d4 Nf6 Nc3 e5 dxe5 Nxe5 P@d4 Neg4 e5 Nxf2 Kxf2 P@b4 exf6 bxc3 Qe2 P@e6 fxg7 Bxg7 bxc3 Qh4 g3 N@e4 Kg2 Nxc3 gxh4 Nxe2 Bxe2 Q@e4 N@f3 Bxd4 N@b5 P@h3 Kxh3 P@g4 Kg3 gxf3 N@d6 cxd6 Nxd6 Ke7 Nxe4 N@f5 Kxf3 N@e1 Kf4 e5 Kg4 h5 Kg5 P@h6 Kxh5 Ng7 Kg4 d5 Kg3 Nf5 Kh3 Ne3 P@g4 dxe4 P@d6 Ke6 Q@e7 Kd5 P@c4 Nxc4 Bxc4",
"b4 e5 a3 d5 Bb2 Bd6 Nf3 f6 e3 Ne7 d3 Be6 Nbd2 O-O Be2 Nd7 O-O c5 b5 a6 bxa6 Rxa6 c4 dxc4 Nxc4 P@h3 gxh3 Bxh3 P@g2 Bxg2 Kxg2 Kh8 Rg1 b5 Nxd6 Rxd6 P@h6 gxh6 P@g7 Kxg7 Kh1 P@g6 B@h4 P@g5 Bg3 P@d4 exd4 cxd4 Bxd4 Rxd4 Nxd4 exd4 P@d6 P@h3 B@f3 B@a8 P@b7 Bxb7 Bxb7 Nf5 Bg4 P@f7 Bxf5 P@g2 Rxg2 hxg2 Bxg2 N@c3 Qg1 N@e2 P@e7 Nxg1 exd8=Q Rxd8 P@e7 P@f3 N@e6 fxe6 R@f7 Kxf7 exd8=N",
"e4 e5 Nf3 Nc6 Bc4 Bc5 O-O Nf6 d3 O-O Bg5 h6 Bxf6 Qxf6 N@d5 Qd8 c3 d6 Nbd2 B@h5 h3 Kh8 b4 Bb6 Qb3 Qe8 a4 a5 bxa5 Nxa5 Qxb6 cxb6 P@f6 Rg8 fxg7 Rxg7 B@f6 P@e7 Bxg7 Kxg7 P@g4 Bcxg4 hxg4 Bxg4 R@g3 P@h5 B@h3 B@f4 Rxg4 hxg4 Bxg4 P@e2 Rfe1 P@g3 fxg3 Bxg3 P@f2 Bxf2 Kxf2 P@g3 Kxg3 Nxc4 Nxc4 P@f4 Kh2 Q@g3 Kh1 Qxg4 B@h4 B@g5 B@f2 Bxh4 Nxh4 B@g5 N@f5 Kh7 B@h3 Qxh4 Nxh4 Bxh4 Bxh4 B@g3 B@f5 R@g6 Bxg3 fxg3 Rxe2 B@g7 B@e3 N@f4 Nxf4 exf4 P@h5 N@f2 Bxf2 N@f8 P@h2 f6 hxg6 Nxg6 P@h5 P@f7 N@h4 Kh8 hxg6 fxg6 Bxg6 Qxg6 Nxg6 Kh7 P@f5 B@f7 Nxe7 gxf2 Q@g6 Bxg6 fxg6 Kh8 R@h7",
"e4 g6 d4 Bg7 Nc3 d6 Be3 Nf6 f3 O-O Qd2 Nbd7 Bc4 a6 Nge2 b5 Bb3 e6 O-O Bb7 Kh1 Qe7 Rg1 h5 Raf1 Kh7 Nf4 Rh8 Nh3 Kg8 Ng5 h4 Ne2 Nh5 Nh3 Rf8 c3 c5 dxc5 Nxc5 P@h6 Bf6 Bxc5 dxc5 N@d7 B@c7 Nxf6 Nxf6 B@g7 Rh7 Bxf6 Qxf6 N@g4 Qe7 Ng5 Rh8 e5 N@h5 h7 Kg7 Nxe6 fxe6 Qh6 Kf7 P@f5 Ke8 Bxe6 N@g7 Qxg6 P@f7 P@d7 Kd8 Qf6 Nxf6 exf6 Nxf5 fxe7 Nxe7 P@f6 fxe6 fxe7 Kxe7 N@g6 Kxd7 N4e5 Kc8 Ne7 Kb8 N@c6 Bxc6 N5xc6 Kb7 Q@a7",
"e4 e5 Nf3 Nc6 Bc4 Bc5 O-O Nf6 d3 O-O Ng5 Nd4 Nxf7 Rxf7 Bxf7 Kxf7 Be3 d6 Bxd4 Bxd4 N@g5 Ke8 Nc3 B@g8 Nd5 Nxd5 P@f7 Bxf7 Nxf7 Kxf7 exd5 B@f5 B@e4 N@f4 Bxf5 Bxf5 B@g4 B@g6 Bxf5 N@e2 Qxe2 Nxe2 Kh1 Bxf5 B@e6 Bxe6 dxe6 Kxe6 B@g4 P@f5 Bxe2 B@c6 N@f3 N@f4 N@g5 Kd7 R@f7 Q@e7 Rxe7 Qxe7 c3 Qxg5 Q@g3 Qxg3 hxg3 Q@h3 gxh3 B@g2 Kh2 N@g4 hxg4 R@h3",
"e4 d6 Nc3 Nf6 Nf3 g6 d4 Bg7 Bc4 O-O h3 a6 e5 Ne8 Bxf7 Rxf7 Ng5 Rf8 e6 Nf6 P@d7 Nbxd7 exd7 Bxd7 N@e6 Qc8 Nxg7 Kxg7 B@h6 Kg8 Bxf8 Qxf8 d5 B@f5 Be3 P@h6 Nf3 B@g7 Nd4 P@f7 Nxf5 Bxf5 B@f4 N@c4 Qc1 Nxe3 Bxe3 N@c4 N@d4 Nxe3 Qxe3 B@c5 O-O-O B@e5 N@b3 Bexd4 Nxd4 Bxd4 Qxd4 N@b5 Nxb5 axb5 Kb1 Ne4 Qb4 N@c3 Ka1 Rxa2",
"e4 e5 Nf3 Nc6 Nc3 Bc5 Be2 Nf6 O-O d5 exd5 Nxd5 Ne4 Be7 d3 P@h3 gxh3 Bxh3 P@g2 Bxg2 Kxg2 P@g4 Kh1 gxf3 Bxf3 Nd4 P@g2 Nxf3 Qxf3 N@d4 Qxf7 Kxf7 c3 P@e2 Rg1 Q@f1 N@g5 Bxg5 Bxg5 Qxa1 Rxa1 R@d1 Q@g1 Rxa1 Qxa1 R@f1 R@g1 Rxa1 Rxa1 B@d1 B@h5 g6 Bxe2 Q@e1 R@f1 Qxf1 Bxf1 R@e1 Q@g1 Ne6 Bxd8 Raxd8 Rxd1 Rxd1 B@g4 B@f5 Bxd1 R@a1 B@g4 N@h6 c4 Nxg4 cxd5 Rxd5 Bxg4 B@e7 N@h6 Ke8 Bxf5 gxf5 B@f7 Kd7 Bxe6 Kxe6 N@g5 Kd7 Q@e6 Kd8 R@c8",
"e4 Nc6 Nc3 Nf6 d4 d5 exd5 Nxd5 Nxd5 Qxd5 Nf3 e5 N@e3 Qd8 d5 Nb4 Nxe5 Bd6 Nxf7 Kxf7 P@e6 Kf8 Nf5 Qf6 Nxd6 cxd6 B@g5 Qe5 Be2 P@f6 P@f4 Qe4 O-O fxg5 Bf3 Qg6 fxg5 N@e5 Bh5 Qf5 P@f4 Nc4 P@d3 Nb6 Bg4 Qg6 f5 Qe8 f6 B@e5 d4 N4xd5 dxe5 Bxe6 fxg7 Kxg7 B@f6 Kf7 Bh5 P@g6 Qxd5 Bxd5 N@h6 Ke6 Bg4 P@f5 P@f3 fxg4 Nxg4 N@e2 Kh1 P@h3 P@f5 gxf5 Ne3 hxg2 Nxg2 Q@h3 Bf4 N@g3 Bxg3 Qxg2 Kxg2 N@f4 Bxf4 Nxf4 Kg1 Ne2 Kh1",
"b4 Nf6 Bb2 e6 a3 a5 b5 d5 Nf3 Bd7 e3 Be7 Be2 O-O O-O Qe8 a4 Bc8 d3 Nbd7 Nbd2 Bd6 c4 e5 cxd5 Nxd5 Ne4 N7f6 P@g5 Nxe4 dxe4 N@c3 exd5 Nxd1 Raxd1 P@h3 gxh3 Bxh3 P@g2 Bxg2 Kxg2 P@g4 N@e1 P@h3 Kh1 c6 N@h6 Kh8 Nh4 cxb5 Bxg4 P@g2 Nexg2 hxg2 Nxg2 f6 P@f7 Qd8 N@e6 Q@e7 Nxd8 Raxd8 gxf6 Qxf6 P@g5 Qxg5 Q@g8 Rxg8 fxg8=Q Rxg8 Nf7",
"d4 Nf6 Nf3 d6 h3 Nc6 g3 Bf5 Bg2 e5 d5 Ne7 Nc3 Ng6 O-O Be7 Bg5 O-O Bxf6 Bxf6 e4 Bd7 N@h5 B@c8 Kh2 Bxh3 Bxh3 Bxh3 Kxh3 B@d7 Kh2 Bg4 Nxf6 Qxf6 B@h1 N@d4 B@e2 Nxe2 Qxe2 Bxf3 Bxf3 N@d4 B@d1 B@d7 B@g2 Nxe2 Bdxe2 P@h3 Bxh3 Bxh3 Kxh3 B@d7 P@f5 Bxf5 exf5 Q@h4 Kg2 Nf4 gxf4 Qxf5 B@g4 P@h3 Kg1 Qxf4 B@h2 P@g3 fxg3 Qfxg3 Bxg3 Qxg3 P@g2 B@d4 Kh1 Bxc3 N@h6 gxh6 N@e7 Kh8 Q@f6 P@g7 Qxg7 Kxg7 N@h5 Kh8 P@g7",
"e4 Nc6 Nf3 Nf6 Nc3 d5 exd5 Nxd5 Nxd5 Qxd5 N@e3 Qa5 Bc4 e6 P@d5 exd5 Bxd5 N@f4 Bxc6 bxc6 N@d4 Bc5 Nxc6 Qa6 Ncd4 P@e2 Nxe2 Nxe2 P@b5 Qxb5 P@c4 Nxc1 cxb5 N@d3 cxd3 Nxd3 Kf1 O-O Qc2 B@g6 P@h6 gxh6 Qxc5 B@g7 B@d4 P@f6 Qc6 Be6 N@e7 Kh8 Qxc7 P@h3 gxh3 P@e4 P@e2 exf3 exf3 Bxh3 P@g2 Bxg2 Nxg2 P@h3 Rg1 hxg2 Rxg2 N@f4 P@e2 P@h3 Nxg6 hxg6 B@h1 hxg2 Bxg2 P@h3 B@h1 hxg2 Bxg2 N@c1 Q@d1 B@h3 Bxh3 Nxh3 B@g1 R@h1 Qg3 Rxg1 Qxg1 Nxg1 R@g2 B@h3 exd3 Bxg2 Kxg2 N@h4 Kf1 Q@g2",
"e4 e5 Nf3 Nc6 Nc3 Nf6 Be2 Be7 O-O O-O d3 d6 Nd5 h6 Nxe7 Qxe7 c3 Bg4 B@h4 N@g6 Bxf6 Qxf6 N@d5 Qd8 h3 Be6 d4 exd4 cxd4 Bxd5 exd5 Nce7 Bxh6 gxh6 B@f6 P@g7 Bxe7 Nxe7 P@e4 B@h7 P@d3 B@b6 N@f4 N@b5 Qa4 B@d7 Qc4 Ng6 Nh5 a5 Rac1 a4 Qxb5 Bxb5 Nxg7 Kxg7 N@f5 Kg8 Nxh6 Kh8 Nf5 Rg8 P@h6 N@e8 P@g3 Nf8 N3h4 Qf6 Rxc7 Bxc7 P@g7 Nxg7 hxg7 Rxg7 N@h5 R@g6 Nxf6 Rxf6 Nxg7 Kxg7 R@g4 P@g6 Nf5 Rxf5 exf5 N@f6 Rxg6 fxg6 P@g5 Ng8 f6 Nxf6 gxf6 Kxf6 R@f4 P@f5 N@g4 Ke7 Rxf5 gxf5 P@f6 Kd7 Q@e7 Kc8 Qxh7 R@d8 Qxf5 R@d7 B@e6 Kb8 Bxd7 Nxd7 P@e7 N@f3 Bxf3 Rc8 N@c5 N@b6 Nxd7 Bxd7 Qxd7 Nxd7 N@c6 bxc6 dxc6 B@a6 R@b7 Bxb7 cxb7 Ra7 B@c6 Nc5 bxc8=Q Kxc8 P@d7 Nxd7 B@b7 Rxb7 Bxb7 Kb8 R@a8",
"e4 e5 Nf3 Nc6 Bc4 Bc5 d3 Nf6 O-O O-O Bg5 h6 Bh4 d6 c3 Be6 Bxe6 fxe6 B@b3 Qe8 Nbd2 Na5 Bc2 B@h5 b4 Bxb4 cxb4 P@c3 Nb1 Nxe4 dxe4 P@b2 Nxc3 bxa1=Q Qxa1 Bxf3 gxf3 Rxf3 P@g2 Rxc3 B@g6 P@f7 P@h7 Kh8 Qxc3 N@e2 Kh1 Nxc3 R@g8 Qxg8 hxg8=Q Kxg8 Q@h7 Kf8 Qh8 R@g8 Qxg8 Kxg8 N@e7 Kf8 R@g8",
"e4 d6 Nc3 Nf6 d4 g6 Nf3 Bg7 h3 O-O Bc4 h6 Be3 Nxe4 Nxe4 d5 Bxd5 Qxd5 N@c3 Qa5 P@f6 exf6 Bxh6 Bxh6 Nxf6 Kh8 P@e7 B@g8 P@g5 Bxg5 Nxg5 Qxg5 exf8=Q Qxf6 B@e5 Qxe5 dxe5 N@f5 R@h6 P@h7 Ne4 B@g7 Qxg8 Kxg8 Qd8 B@f8 Nf6 Bxf6 exf6 P@d2 Kf1 P@e2 Kxe2 N@d4 Kxd2 N@e4 Ke1 P@d2 Kf1 Nxf6 B@b4 P@e2 Kg1 d1=Q",
"d4 d5 Nf3 Bf5 Bf4 e6 e3 Bd6 Bd3 Ne7 O-O Nbc6 Nbd2 O-O Bxd6 cxd6 Nh4 B@e4 Qe2 Nb4 Bxe4 dxe4 c3 Nbd5 B@c2 B@d3 Bxd3 exd3 Qf3 B@e2 Qg3 Bxf1 Rxf1 Ng6 Nxf5 exf5 B@f3 f4 exf4 Ndxf4 B@g5 N@e2 Kh1 Nxg3 hxg3 Qxg5 Ne4 Qh6 B@h2 P@e2 Rg1 R@f1 P@h5 Rxg1 Bxg1 e1=Q B@h2 B@e2 Bxe2 Nxe2 N@g4 Qhc1",
"e4 d6 Nc3 Nf6 d4 Bg4 Be2 Bxe2 Ngxe2 e6 d5 Be7 dxe6 fxe6 B@b3 P@f7 Nf4 O-O Nxe6 fxe6 Bxe6 B@f7 Bxf7 Rxf7 B@b3 Nc6 P@e6 Rf8 P@f7 Kh8 P@h6 gxh6 Bxh6 P@g7 Bxg7 Kxg7 Qd2 Ng4 h3 Nh6 Nd5 B@g5 P@f4 Bh4 Nxe7 Bxe7 P@g5 N@h4 gxh6 Kxh6 B@g5 Kg7 Bxh4 Bxh4 N@f5 Kh8 Nxh4 B@g7 N@g6 hxg6 Nxg6 Kh7 Nxf8 Qxf8 P@g6 Kxg6 f5 Kf6 B@g5 Ke5 Qf4 Kd4 R@d5",
"e4 e6 d4 d5 e5 Be7 Bd3 Bd7 Nf3 Nc6 O-O h5 c3 h4 h3 a6 Qb3 Na5 Qc2 Bb5 Nbd2 Bxd3 Qxd3 B@b5 Qc2 Bxf1 Nxf1 Qd7 Bf4 O-O-O a4 Kb8 b4 Nc4 b5 axb5 axb5 P@b2 B@a7 Ka8 Qxb2 Nxb2 Bc5 Kb8 P@c6 bxc6 bxc6 Qxc6 Bxe7 Nxe7 B@a3 P@e2 Bxe7 exf1=Q Rxf1 P@e2 N@h2 exf1=Q Nxf1 N@e2 Kh1 R@a1 Nd2 Q@g1",
"e4 e5 Nf3 Nc6 d4 exd4 Nxd4 Nxd4 Qxd4 N@c6 Qd1 Nf6 Nc3 Bb4 Bd3 d5 exd5 Qxd5 O-O Bxc3 bxc3 P@h3 B@f3 hxg2 Re1 P@e4 Bxg2 Ne5 N@f4 Qd6 Bb5 P@c6 Qxd6 N@f3 Kh1 cxd6 P@e7 Kxe7 Bxf3 Nxf3 Ba3 B@e5 N@d3 Bxf4 Nxf4 Q@e5 B@g3 P@h3 P@g2 hxg2 Nxg2 P@h3 Bxe5 hxg2 Kxg2 Nxe1 Rxe1 N@h4 Kf1 Bh3 Ke2 Bg4 Kd2 Nf3 Kc1 R@a1 Kb2 Nxe5 Bxd6 Kxd6 P@c5 Kd7 Q@d6 Ke8 N@c7",
"e4 Nf6 Nc3 e5 Nf3 Nc6 Bc4 Bc5 O-O d6 d3 Qe7 Bg5 Be6 Bxf6 gxf6 N@d5 Qd8 Nb5 B@b6 Nxb6 Bxb6 Bxe6 fxe6 B@g4 B@f7 B@h5 Bxh5 Bxh5 B@g6 Bg4 Qd7 B@h3 f5 exf5 exf5 Bxf5 Bxf5 Bxf5 Qxf5 B@e4 Qd7 P@e6 Qxe6 Bxc6 bxc6 N@g7 Kd7 Nxe6 cxb5 P@c6 Kxc6 Nxe5 dxe5 Qf3 B@d5 Qxd5 Kxd5 Q@d7 N@d6 P@e4",
"e4 e6 Nc3 d5 d4 dxe4 Nxe4 Bd7 Nf3 Bc6 Neg5 P@f6 Nxf7 Kxf7 Bc4 Bd5 Bxd5 Qxd5 B@b3 Qe4 Be3 N@g4 O-O Bd6 d5 e5 P@d3 Qg6 P@c5 Kf8 cxd6 cxd6 B@e6 P@h3 Nh4 N4h6 Nxg6 hxg6 Bxh6 Nxh6 Q@d8 N@e8 Bxh3 B@e7 N@e6 Kf7 Qc8 B@d7 Qxb7 Nf5 P@h5 Rxh5 Qxh5 gxh5 Bxf5 P@g6 N@h8 Kg8 Nxg6 Bxe6 R@h8 Kf7 Qxe7",
"d4 Nf6 Nf3 g6 Nc3 d5 Ne5 Bg7 g3 O-O Bg2 h6 O-O c6 h3 Be6 Kh2 Nbd7 f4 Nh5 e3 Nxe5 fxe5 Nxg3 Kxg3 P@f6 exf6 exf6 Kh2 Qc7 P@g3 N@h5 N@f4 Nxg3 Kxg3 P@g5 Kh2 Rae8 Kg1 gxf4 exf4 N@h4 P@e5 Nxg2 Kxg2 B@d7 N@g1 Qc8 N@f2 P@e7 N@e3 fxe5 fxe5 P@f4 Neg4 h5 Nh2 Bxh3 Nfxh3 Bxh3 Kf2 P@g3 Ke2 gxh2 Nxh3 Qxh3 Bxf4 N@f5 B@f2 N@e6 B@e3 Nxf4 Bxf4 N@c4 P@f3 B@g2 Qd3 h1=Q Rxh1 Bxh1 Rxh1 Qxh1 B@f1 Nxb2 P@h7 Kxh7 N@g5 Kh8 Qd2 P@c4 P@g2 P@d3 Ke1 dxc2 Qxc2 R@a1 Bc1 Rxc1 Qxc1 Nd3 Kd2 Nxc1 R@h7 Kg8 Rxg7 Kxg7 P@f6 Kh6 Nxf7 Rxf7 B@f4 Kh7 Kxc1 R@b2 Kxb2 Q@b4 R@b3 R@e2 Bxe2 N@d3 Kc2 B@a1 N@g5 Kg8 R@h8 Kxh8 Nxf7 Kg8 Nh6 Kf8 R@g8",
"d4 d5 Nf3 Bf5 Bf4 e6 e3 Bd6 Bd3 Ne7 O-O O-O Bg3 Nbc6 Nbd2 Bg6 Bxg6 Nxg6 c4 B@h5 B@e2 Bxg3 hxg3 B@g4 Rc1 dxc4 Rxc4 P@h3 gxh3 Bxh3 P@g2 Bxg2 Kxg2 P@g4 Kg1 gxf3 Nxf3 P@e4 B@g2 exf3 Bexf3 Bxf3 Bxf3 N@g5 B@g2 B@d5 B@e2 Nxf3 Bgxf3 Nh4 P@g2 Nxf3 gxf3 N@h3 Kh2 Nxf2 Rxf2 P@h4 Rg2 B@f2 Rxf2 hxg3 Kxg3 B@h4 Kg2 Bxc4 B@f4 Bxe2 Qxe2 P@g3 Bxg3 Bxg3 Kxg3 B@h4 Kg2 B@g3 B@g1 Bxf2 Bxf2 Bxf2 Qxf2 B@h4 P@g3 R@g6 B@g4 Bxg3 Qxg3 P@h4 Qh3 R@g3 Qxg3 Nxd4 B@g1 hxg3 exd4 Q@e2 P@f2 gxf2 Bxf2 Rxg4 fxg4 Qxg4 N@g3 B@e4 Kf1 Qd1 N@e1 P@g2 Kg1 Qxe1 Bxe1 N@h3 Kh2 g1=Q Kxh3 Qg2 Kg4 P@f5 Kf4 Qf3 Ke5 Qd6",
"b3 e5 Bb2 d6 g3 Nf6 Bg2 Be7 Nf3 O-O O-O c5 d3 Nc6 Nbd2 h6 h3 Be6 Kh2 Qd7 c4 Nd4 Bxd4 cxd4 Ne4 B@f5 Nxf6 Bxf6 N@g1 N@c3 Qd2 e4 N@h5 exf3 exf3 Bd8 P@f6 Bxf6 Nxf6 gxf6 Qxh6 P@g7 Qh4 Bxd3 Qxd4 Bxf1 Rxf1 N@e2 Nxe2 Nxe2 Qe4 R@e5 B@h7 Kh8 B@d3 N@g5 Bxe2 Rxe4 Bxe4 P@h7 R@h4 f5 P@f4 Nxe4 fxe4 N@g6 Rxh7 Kxh7 N@g5 Kh8 Nxe6 Qxe6 N@g5 Qe7 B@h5 fxe4 P@h6 B@c3 N@f5 Qxg5 fxg5 P@f3 hxg7 Bxg7 P@f6 N@e8 fxg7 Nxg7 Q@h6 R@h7 Bxg6 Rxh6 gxh6 Nxf5 R@h7 Kg8 N@f6",
"e4 Nc6 Nc3 e6 Nf3 Nf6 d4 d6 Bb5 a6 Ba4 b5 Nxb5 axb5 Bxb5 N@a7 Ba4 Bd7 O-O Nxe4 Re1 d5 c3 P@g4 Nd2 Nxd2 Bxd2 N@e4 Bf4 Bd6 Qxg4 Bxf4 Qxf4 O-O P@e5 f6 B@h5 B@g6 Bxg6 hxg6 B@h4 fxe5 Qxe4 dxe4 Bxd8 Raxd8 P@h7 Kf7 h8=Q B@f6 Qxf8 Rxf8 R@h7 B@h6 N@h3 Q@f5 N@g3 Qxh3 gxh3 N@f3 Kh1 Nxe1 Rxe1 P@f3 N@g4 P@g2 Kg1 R@h1 Nxh1 gxh1=Q Kxh1 exd4 Nxh6 Ke7 B@a3 P@d6 R@g3 dxc3 Rxg6 P@g2 Kg1 cxb2 Rgxg7 Bxg7 Rxg7 Ke8 B@h5 P@f7 Q@a8",
"e4 e5 Nf3 Nc6 Nc3 Bc5 Bc4 Nf6 d3 O-O O-O d6 Na4 Bb6 c3 Bg4 Nxb6 axb6 h3 Bh5 B@h4 N@d7 Bb5 Kh8 Bxc6 bxc6 N@f5 Rg8 Ng5 B@g6 g4 Bxg4 hxg4 P@h6 Nh3 Bxf5 gxf5 d5 B@f3 dxe4 dxe4 Nc5 B@g2 Qxd1 Rxd1 P@g4 Bxh6 gxf3 Bxf6 N@e2 Kh2 Q@g4 Bhxg7 Rxg7 N@e3 B@g3 Kh1 fxg2 Kxg2 B@f3 Kf1 Qxh3 Ke1 Bxf2 Kd2 Bxe3 Kxe3 B@f4 Kf2 Qg2 Ke1 N@c2",
"e4 Nc6 d4 d5 exd5 Qxd5 Nc3 Qxd4 Be3 Qb4 Bb5 Qxb2 Nge2 P@h3 gxh3 Nf6 P@g2 Bd7 Rb1 Qa3 Rb3 Qd6 P@d5 Rd8 dxc6 Qxd1 Nxd1 Bxc6 Bxc6 bxc6 N@d4 B@d5 Q@g3 P@d6 O-O e5 Nxc6 Bxc6 P@b5 Bd5 B@c6 Bxc6 bxc6 B@e6 Rb7 N@f5 Rxc7 Nxg3 fxg3 Be7 Rxe7 Kxe7 B@h4 R@g6 Ndc3 Q@c4 B@g5 P@f4 P@d3 Qb4 Bxf6 gxf6 N@d5 Bxd5 Nxd5 Kf8 Nxb4 fxe3 B@e7 Kg8 Bexf6 P@f2 Kh1 Q@g1 Nxg1 fxg1=Q Kxg1 N@e2 Kh1 B@f8 P@g7 Rxg7 N@h6",
"e4 d6 Nc3 Nf6 d4 Bg4 f3 Bh5 Be3 e6 Qd2 Be7 O-O-O a5 Nge2 Nbd7 Nf4 Bg6 h4 e5 Nxg6 hxg6 Bc4 Nb6 dxe5 O-O Bxb6 cxb6 B@b3 dxe5 Bxf7 Kh8 h5 Qxd2 Rxd2 P@a3 hxg6 B@h7 gxh7 axb2 Kxb2 Ba3 Kxa3 P@b4 Kb2 bxc3 Kxc3 Rac8 Kb2 Q@c3 Kc1 Qxd2 Kxd2 Nxe4 fxe4 P@c3 Ke1 R@c1 B@d1 Rxd1 Kxd1 N@b2 Ke2 B@b5 P@c4 N@d4 Ke1 Rxf7 N@g6",
"Nf3 d5 d4 Bf5 Bf4 e6 e3 Bd6 Ne5 Ne7 Bd3 O-O O-O f6 Ng4 Bxf4 exf4 Nbc6 Bxf5 Nxf5 c3 B@e4 B@h3 B@c2 Qd2 Bxb1 Raxb1 N@c4 Qe1 Ncxd4 cxd4 P@d2 Qxe4 dxe4 B@c5 Nxd4 Bxd4 Qxd4 N@h6 gxh6 Nxh6 Kh8 P@g7 Kxg7 N@f5 exf5 Nxf5 Kg8 B@e6 B@f7 Nxd4 Q@g6 Bxc4 Rae8 B@f5 Qg7 N@h5 Bxc4 Nxg7 Bxf1 Nxe8 R@g7 Q@h6 N@f3 Nxf3 Rxg2 Kh1 P@g7 Q@e6 Rf7 Qxh7 Kf8 Qh8",
"e4 e5 Nf3 Nc6 Nc3 Bc5 Bc4 Nf6 d3 O-O O-O h6 Nd5 Ng4 Qe2 d6 c3 Bxf2 Rxf2 Nxf2 Qxf2 R@d1 B@f1 Be6 Ne3 Rxf1 Qxf1 B@b6 Kh1 Bxe3 Bxe3 Bxc4 dxc4 P@f4 Bf2 B@g4 B@h4 N@g5 Bxg5 hxg5 B@f5 B@h5 Bxg4 Bxg4 B@h3 Bxh3 gxh3 B@g6 B@f5 Bxf5 exf5 B@e4 B@g4 B@h7 R@h5 Bhxf5 Bxf5 Bxf3 B@g2 Bxh5 N@g4 P@g6 Bfe4 Bxg4 hxg4 N@f6 Bxc6 bxc6 B@f3 B@d7 N@h3 N@h7 Bxc6 Bxg4 Bxa8 Bxh3 Bxh3 Qxa8 B@g2 e4 N@e7 Kh8",
"d4 d5 Bf4 Bf5 e3 e6 Bd3 Bg6 Ne2 Nf6 O-O Be7 Nd2 O-O Be5 Nbd7 Nf4 Nxe5 dxe5 Nd7 Nxg6 hxg6 B@g3 N@h5 f4 B@b6 Qf3 Bxe3 Qxe3 Bc5 Qxc5 Nxc5 B@h4 Qxh4 Bxh4 B@e3 Kh1 Q@g4 B@f6 Nxf6 Bxf6 B@h6 Bxg7 Kxg7 N@f6 Rh8 Nxg4 Bxd2 P@f6 Kf8 Nxh6 N@g3 hxg3 Rxh6 N@h3 Rxh3 gxh3 P@g2 Kxg2 N@e3 Kh2 N@g4 hxg4 Nxg4 Kh3 Nxf6 Q@h8 Ke7 Qxf6 Kd7 R@e7 Kc6 Q@b5",
"e4 e5 Nf3 Nc6 Bc4 Be7 Nc3 Nf6 d4 exd4 Nxd4 Ne5 Be2 d5 f4 Ng6 e5 Ne4 O-O P@h3 gxh3 Bxh3 P@g2 Bxg2 Kxg2 Nh4 Kh3 Nxc3 bxc3 h5 P@g5 N@f5 Bxh5 Rxh5 Qxh5 B@g2 Kg4 Nxd4 R@h8 Kd7 e6 Kc6 N@e5 Kb6 Rb1 P@b5 Rxd8 Rxd8 P@a5 Ka6 cxd4 Bxf1 Qxh4 Be2 Kg3 N@f5 Kf2 R@g2 Kxg2 Nxh4 Kf2 R@g2 Ke3 Nf5 Kd2 Bb4 c3 Bc4 Ke1 Q@e2",
"e4 e6 d4 d5 Nc3 dxe4 Nxe4 Bd7 Nf3 Bc6 Neg5 P@f6 Nh3 Ne7 Nf4 Nf5 c3 Bd6 P@g4 Bxf3 gxf3 Nh4 Nh5 Rg8 B@g3 Nxf3 Qxf3 N@c2 Kd2 Nxa1 Bb5 P@c6 Bd3 R@h6 Kd1 Rxh5 gxh5 f5 Re1 Bxg3 Qxg3 B@g4 f3 Bxh5 Bxf5 P@c2 Bxc2 Nxc2 Kxc2 Bg6 P@e4 Na6 B@g5 B@e7 Bxe7 Qxe7 B@h4 f6 N@f4 N@b4 Kd2 B@h6 Ke2 Rd8 Nxg6 hxg6 Qxg6 Kd7 B@h3 Bxc1 P@g3 B@c4 Kd1 Bxb2 cxb4 Nxb4 N@c5 Qxc5 dxc5 P@c2 Kd2 c1=Q Rxc1 Bxc1 Kxc1 R@c2 Kd1 N@c3 Ke1 P@f2",
"Nf3 Nf6 b3 d6 Bb2 e5 g3 Be7 Bg2 O-O d3 c5 O-O Nc6 Nbd2 Be6 h3 Qc8 Kh2 Bxh3 Bxh3 P@g4 Bg2 gxf3 Nxf3 Nd4 B@h3 N@g4 Kg1 Qc7 Bxd4 cxd4 Bxg4 Nxg4 N@d5 Qd7 N@f5 B@d8 Nxg7 Kxg7 P@h3 Nf6 Nxe7 Bxe7 P@g5 Kh8 gxf6 Bxf6 N@h5 B@g7 B@g5 Bxg5 Nxg7 B@f6 Nh5 Rg8 Nxg5 Bxg5 B@f6 Bxf6 Nxf6 Qe6 Nxg8 Rxg8 B@d5 Qf6 R@c7 P@e6 B@e7 Qg6 Bde4 B@f5 Bxf5 exf5 B@f6 B@g7 Bxg7 Rxg7 B@f6 N@e8 Rc8 B@d7 Rxe8 Bxe8 N@h4 Qh6 Nxf5 N@g4 Nxh6",
"e4 d6 d4 g6 Nc3 Bg7 Nf3 Nc6 Bc4 Nf6 Bxf7 Kxf7 P@h6 Bf8 d5 Ne5 Nxe5 dxe5 N@e6 Qd6 O-O B@g4 Ng5 Ke8 f3 Bgd7 f4 Qc5 Kh1 N@f2 Rxf2 Qxf2 Bd2 Bxh6 fxe5 P@h3 P@f7 Kd8 f8=Q Rxf8 N@f7 Ke8 Qg1 hxg2 Qxg2 Qxg2 Kxg2 P@h3 Kf2 Q@g2 Ke3 Bxg5 Kd3 Qxd2 Kc4 R@d4 Kb3 N@c5 Ka3 B@b4",
"e4 e5 Nf3 Nc6 Bc4 Be7 Bxf7 Kxf7 P@d5 Nf6 dxc6 Rf8 Nxe5 Kg8 Nc3 dxc6 d4 P@b4 P@f3 bxc3 bxc3 N@h4 Rg1 Bd6 P@h6 gxh6 Bxh6 P@g7 Bxg7 Kxg7 P@g5 Bxe5 dxe5 B@d2 Kf1 Bxg5 Qxd8 B@c4 B@d3 Rxd8 Q@e7 Q@f7 Qxd8 N@d2 Ke2 Nfxe4 Qxg5 Nxg5 B@f6 Kh6 R@h5 Kxh5 N@f4 Kh6 Bxg5 Kxg5 N@h3 Kh6 P@g5 Kg7 Bxc4 Qxc4 B@d3 Ndxf3 gxf3 R@d2 Kf1 Rxf2 Nxf2 P@e2 Nxe2 B@h3 Nxh3 Q@c1 Nxc1")

val withNag = """
[Event "Casual Game"]
[Site "?"]
[Date "1851.??.??"]
[Round "?"]
[White "Anderssen, Adolph"]
[Black "Lionel, Kieseritsky"]
[Result "1-0"]
[ECO "C33"]
[WhiteElo "unknown"]
[BlackElo "unknown"]
[Annotator "Hayes, David"]
[PlyCount "45"]

1. e4 {C33: King's Gambit Accepted: 3 Nc3 and 3 Bc4} 1... e5 2. f4 {White offers a pawn to gain better development and control of the center.} 2... exf4 3. Bc4 Qh4+ 4. Kf1 b5?! {Bryan's Counter Gambit. A dubious gambit in modern times, but typical of the attacking style of that time. Here black lures the Bishop from it attacking diagonal against the sensitive f7-pawn, and provides a diagonal for development of his own Bishop to b7 where it will bear down on white's King side. All this value for the price of a pawn.} 5. Bxb5 Nf6 6. Nf3 Qh6 7. d3 Nh5 {The immediate, cheap, and shallow threat of ... Ng3+ is easily defended.} 8. Nh4 {The position is sharp and getting sharper.} 8... Qg5 {Again, playing for cheap threats. In this case, black attacks two pieces at once.} 9. Nf5 c6 {9... g6 10. h4 Qf6 is another complicated position for another day.} 10. g4 {A brilliant move made with a steady hand. Note that white cares little for defensive moves, and is always alert for attack. Now black plays to win the g4-pawn.} 10... Nf6 {Black should have played 10... cxb5 11. gxh5 with a better game.} 11. Rg1 {Now Anderssen sacrifices his Bishop, the first of many sacrifices in this game. White cares little for defensive moves, and plays always for the initiative.} 11... cxb5 12. h4 Qg6 13. h5 {White gets more space.} 13... Qg5 14. Qf3 {White now has the ghastly threat of Bxf4 winning black's Queen next.} 14... Ng8 {Black is forces to clear a path of retreat for his Queen by also retreating one of his only developed pieces.} 15. Bxf4 Qf6 {Black should quickly develop his pieces.} 16. Nc3 Bc5 17. Nd5 {Inviting black to indulge his greed. Also good is 17. d4 Bf8 (17... Bxd4? 18. Nd5 when the Knights savage the board.) 18. Be5.} 17... Qxb2 18. Bd6 Qxa1+ {And why not capture with check!} 19. Ke2 {Now who can resist the tender morsel on g1, but resist he must.} 19... Bxg1 {Black is just too greedy. He has too few pieces developed, and what is developed is sent to the far corners of the board. Now it is white's turn to play. Black may have won after 19... Qb2 (to guard against Nxg7+) 20. Rc1 g6 21. Bxc5 gxf5 (not 21... Qxc1 22. Nd6+ Kd8 23. Nxf7+ Ke8 24. Nc7#).} 20. e5 {Slipping the noose around the neck of the black King.} 20... Na6 {Perhaps 20... Ba6 would have put up more resistance by giving black's King more room to run.} 21. Nxg7+ Kd8 22. Qf6+! {A final pretty sacrifice that ends the game.} 22... Nxf6 {A deflection.} 23. Be7# 1-0
"""

val fromTcec = """[Event "nTCEC - Stage 2 - Season 2"]
[Site "http://www.tcec-chess.net"]
[Date "2013.09.24"]
[Round "5.2"]
[White "Stockfish 160913"]
[Black "Spike 1.4"]
[Result "1-0"]
[Variant "normal"]

1. d4 Nf6 2. c4 e6 3. Nc3 Bb4 4. Qc2 Nc6 5. e3 O-O 6. f4 Bxc3+ 7. bxc3 d6 8. Nf3
e5 9. fxe5 dxe5 10. Be2 Re8 11. O-O Qe7 12. Rb1 b6 13. Rb5 a6 14. Rb2 h6
15. Nh4 Qd6 16. Qd1 Rb8 17. Qd3 b5 18. cxb5 axb5 19. Nf5 Bxf5 20. Qxf5 b4
21. cxb4 Nxb4 22. a3 Nc6 23. dxe5 Nxe5 24. Rd1 Qc5 25. Qc2 Qa7 26. Rxb8 Qxb8
27. h3 c6 28. a4 Qa7 29. Rd4 Nd5 30. Bd2 Rd8 31. a5 c5 32. Re4 Nc6 33. a6 Qb6
34. Be1 Kf8 35. Qa2 Qc7 36. Qa4 Kg8 37. a7 Ra8 38. Bg3 Qc8 39. Bb8 Rxb8
40. axb8=R Qxb8 41. Qb5 Nf6 42. Ra4 Qxb5 43. Bxb5 Ne5 44. Ra8+ Kh7 45. Rc8 Kg6
46. Rxc5 Kf5 47. Ba4 Ke6 48. Bb3+ Kd6 49. Ra5 Nfd7 50. Rd5+ Ke6 51. Kf2 Kf6
52. Rd6+ Ke7 53. Rd4 Nc5 54. Bd5 h5 55. g3 Ne6 56. Ra4 Nc5 57. Ra7+ Kf6 58. Ke2
g6 59. Ra5 Ncd7 60. Kd2 Nf8 61. Ra6+ Ne6 62. h4 Ng4 63. Bxe6 fxe6 64. Kd3 Ne5+
65. Ke4 Ng4 66. Kd4 Ne5 67. Ra8 Nd7 68. Rc8 Ke7 69. e4 Nf6 70. Rc7+ Kd6 71. Rg7
Ng4 1-0"""

val fromLichessBadPromotion = """
[Event "?"]
[Site "?"]
[Date "????.??.??"]
[Round "?"]
[White "?"]
[Black "?"]
[Result "*"]
[FEN "8/8/1KP5/3r4/8/8/8/k7 w - - 0 1"]
[SetUp "1"]

1. c7 Rd6+ 2. Kb5 Rd5+ 3. Kb4 Rd4+ 4. Kb3 Rd3+ 5. Kc2 Rd4 6. c8=R Ra4 7. Kb3 *
"""

val fromPositionEmptyFen = """
[Event "Casual game"]
[Date "2016.09.01"]
[White "lichess AI level 4"]
[Black "victor946c"]
[Result "0-1"]
[WhiteElo "?"]
[BlackElo "1500"]
[PlyCount "164"]
[Variant "From Position"]
[TimeControl "-"]
[ECO "?"]
[Opening "?"]
[Termination "Normal"]
[SetUp "1"]
[Annotator "lichess.org"]
1. d4 Nf6 2. c4 e6 3. Nc3 Bb4 4. Qc2 d5 { E34 Nimzo-Indian Defense: Classical Variation, Noa Variation } 5. Bg5 Bxc3+ 6. bxc3 dxc4 7. e3 b5 8. Nf3 O-O?! { (0.00 → 0.60) Inaccuracy. Best move was Bb7. } (8... Bb7 9. Qb2 Bxf3 10. gxf3 a6 11. a4 Nbd7 12. Bxf6 gxf6 13. f4 Rb8 14. axb5 axb5 15. Ra7) 9. a4 c6 10. axb5?! { (0.55 → 0.05) Inaccuracy. Best move was Be2. } (10. Be2 Bb7 11. Rb1 Nbd7 12. O-O Qc8 13. axb5 cxb5 14. Bxf6 Nxf6 15. Rxb5 Be4 16. Qb2 Bd3) 10... cxb5 11. Be2 h6? { (-0.29 → 1.62) Mistake. Best move was Bb7. } (11... Bb7 12. Nd2 h6 13. Bh4 Nbd7 14. O-O a5 15. h3 Qc8 16. Rfb1 Bc6 17. e4 a4 18. Bf3) 12. Bh4? { (1.62 → -0.26) Mistake. Best move was Bxf6. } (12. Bxf6 gxf6 13. Qe4 Qd5 14. Qg4+ Kh7 15. Ne5 Rg8 16. Qh4 fxe5 17. Bf3 Qd7 18. Bxa8 exd4) 12... a6 13. O-O Nbd7 14. Rfd1 Bb7 15. Ne5 g5 16. Bg3 Ne4 17. Bf3 f5 18. Ng6 Rf6 19. Ne5 Qe7 20. h3 Nxe5 21. dxe5 Rff8 22. Rd6 Nxg3 23. Bxb7 Qxb7 24. fxg3 Kf7 25. Qe2 Ke7 26. Qd1 Rfd8 27. g4 Rxd6 28. exd6+ Kf6 29. Kf2 Rd8 30. Qd2 Qd5 31. Ra2 Rxd6 32. Qd4+ Qxd4 33. cxd4 b4 34. Ra4 Rb6 35. Ke2? { (0.15 → -0.99) Mistake. Best move was h3. } (35. h3 Nxe5 36. Bxe5 a5 37. Qb2 Qe8 38. Rf1 Bc6 39. Ra2 Kh7 40. Rfa1 a4 41. Qb4 Rg8) 35... c3 36. Kd3 Ke7 37. Ra1 Kd6 38. Rb1?! { (-0.18 → -1.17) Inaccuracy. Best move was Bxe5. } (38. Bxe5 Rf7 39. Ra5 Rh7 40. Bxe4 Bxe4 41. Qe2 h5 42. f3 Bb7 43. e4 g4 44. Qe3 Rf8) 38... a5 39. gxf5? { (-1.16 → -2.48) Mistake. Best move was Bxe4. } (39. Bxe4 Bxe4 40. Qe2 a5 41. Rd6 a4 42. Qd1 Bd3 43. Qf3 Rfc8 44. Qh5 Kg7 45. h4 Qe8) 39... exf5? { (-2.48 → -0.25) Mistake. Best move was Nxd6. } (39... Nxd6 40. exd6 Qf7 41. Bxb7 Qxb7 42. Be5 a5 43. f4 Kh7 44. fxg5 hxg5 45. Rd1 Rad8 46. h4) 40. Ra1 Rb5 41. g3 Kd5?! { (-0.37 → 0.51) Inaccuracy. Best move was Rfe8. } (41... Rfe8 42. Qd1 Re7 43. g4 Qe4 44. Qd4 a5 45. gxf5 exf5 46. Rxh6 Qxe5 47. Rg6+ Kh7 48. Rxg5) 42. Rf1 Ke6?? { (0.23 → 5.13) Blunder. Best move was Qc8. } (42... Qc8 43. g4 Kg7 44. Rf1 Qe8 45. gxf5 exf5 46. Qf3 Qxe5 47. Qc6 Ra7 48. Rg6+ Kh8 49. Rxh6+) 43. Kc4?? { (5.13 → 0.10) Blunder. Best move was Qh5. } (43. Qh5 Qe4 44. Rad1 Qxe3+ 45. Kf1 Ra7 46. Re1 Qxe5 47. Rxe5 Kxd6 48. Re1 Rd7 49. Qg6 Re7) 43... Rd5 44. Ra1?! { (0.00 → -0.59) Inaccuracy. Best move was Qd4. } (44. Qd4 Rxd6 45. Qxd6+ Kf7 46. g4 Qd5 47. Qc7+ Kg8 48. gxf5 exf5 49. Qe7 a5 50. e6 Rf8) 44... c2?? { (-0.59 → 3.37) Blunder. Best move was Qa7. } (44... Qa7 45. Kh1 Qxe3 46. Raxa6 Qxe5 47. Rxe6+ Qxe6 48. Rxe6+ Kxe6 49. gxf5+ Kf6 50. Qh5 Kxf5 51. Kh2) 45. Rc1?? { (3.37 → 0.00) Blunder. Best move was Qxd6+. } (45. Qxd6+ Kf7 46. gxf5 Re8 47. fxe6+ Kg8 48. e7 Rxe7 49. Rxa6 Rg7 50. Qd8+ Kh7 51. Ra8 Qc7) 45... b3 46. Kxb3?! { (0.32 → -0.67) Inaccuracy. Best move was gxf5. } (46. gxf5 exf5) 46... Rb5+ 47. Kc3?! { (-0.90 → -1.85) Inaccuracy. Best move was gxf5. } (47. gxf5 exf5 48. Qh5 Kg7 49. Qd1 Qb6 50. d7 Qe6 51. Qd4+ Kf7 52. Rd1 a5 53. Qa7 Qc6) 47... a4?! { (-1.85 → -0.96) Inaccuracy. Best move was Qb6. } (47... Qb6 48. Rd1 f4 49. Ke2 fxe3 50. Qxe3 Rxd6 51. Rf1+ Ke7 52. Qf3 Rd3 53. Qf8+ Kd7 54. Rf7+) 48. Rxc2? { (-0.96 → -2.97) Mistake. Best move was Qxd5. } (48. Qxd5 exd5) 48... Kd5 49. g4? { (-2.97 → -4.00) Mistake. Best move was Qxd5. } (49. Qxd5 exd5 50. Ra1 fxg4 51. hxg4 Ke5 52. Ke1 Ke4 53. Ke2 Ke5 54. Kf3 Rc6 55. Ra2 Rf6+) 49... fxg4 50. hxg4?? { (-3.84 → -7.10) Blunder. Best move was exd4. } (50. exd4 fxg4) 50... Ke4 51. Ra2?? { (-6.69 → -10.84) Blunder. Best move was Ke2. } (51. Ke2 b3 52. Rb2 a5 53. Kd2 a4 54. Kc3 a3 55. Rb1 b2 56. g3 a2 57. Kxb2 axb1=Q+) 51... Rb3+ 52. Kc4 Rxe3 53. Rxa4 Rd3?? { (-10.53 → -4.87) Blunder. Best move was b3. } (53... b3 54. Ra1 c2 55. Rc1 b2 56. Kxc2 bxc1=R+ 57. Kxc1 Rb3 58. Kd2 Rb2+ 59. Ke1 Rxg2 60. Kf1) 54. Ra8 Rxd4+ 55. Kb3? { (-4.50 → -6.11) Mistake. Best move was Kc2. } (55. Kc2 Kd5) 55... Rd6 56. Rf8 Ke5 57. Rf2 Rd4 58. Rg2 Kf4 59. Ra2?! { (-4.64 → -5.48) Inaccuracy. Best move was Rh1. } (59. Rh1 Rb8 60. g4 f4 61. e4+ Kd6 62. h4 Rc8 63. hxg5 hxg5 64. Rh6+ Ke7 65. Rh1 a4) 59... Kxg4 60. Kc3? { (-4.96 → -7.24) Mistake. Best move was g4. } (60. g4 fxg4 61. hxg4 a4 62. Kc4 Rb7 63. Rh1 c2 64. Rxh6+ Kf7 65. Rh7+ Kg6 66. Rh1 Rc7+) 60... Rf4? { (-7.24 → -4.70) Mistake. Best move was Rb8. } (60... Rb8 61. Rc1 Rc8+ 62. Kb3 c2 63. Kb2 a4 64. Rxc2 Rxc2+ 65. Kxc2 a3 66. Kb3 Kd5 67. h4) 61. Ra6? { (-4.70 → -6.98) Mistake. Best move was g4. } (61. g4 fxg4 62. hxg4 Rd8 63. Kb3 Rc8 64. e4 c2 65. Rc1 Rc3+ 66. Kb2 a4 67. Rxc2 a3+) 61... h5?? { (-6.98 → -2.39) Blunder. Best move was Rd8. } (61... Rd8) 62. Ra2?? { (-2.39 → -6.50) Blunder. Best move was Kb3. } (62. Kb3 Rd8) 62... h4?? { (-6.50 → -0.84) Blunder. Best move was Rd8. } (62... Rd8 63. Kd3 b3 64. Kc3 a4 65. Kb2 Kd5 66. g4 f4 67. exf4 gxf4 68. h4 Kxd4 69. g5) 63. Rb2 Rf3+ 64. Kb4 Kg3 65. Ra2 g4 66. Kc4? { (-0.95 → -3.08) Mistake. Best move was Kd3. } (66. Kd3 a3 67. Kc3 Rb6 68. Ra2 Ra6 69. Kb4 Rb6+ 70. Kc3 Re6 71. Kd3 Ra6 72. g4 f4) 66... h3? { (-3.08 → -1.84) Mistake. Best move was f4. } (66... f4 67. exf4 Rb3+ 68. Kd2 gxf4 69. Rc5+ Kxd4 70. Rf5 Ke4 71. Ra5 Rd3+ 72. Ke1 a3 73. Ra6) 67. Ra1 Rf2? { (-1.71 → -0.47) Mistake. Best move was Rb3+. } (67... Rb3+ 68. Kd2 a3 69. Rc7 Ke4 70. Rc6 a2 71. Re6+ Kf3 72. Ra6 Rb2+ 73. Kc3 Re2 74. Kd3) 68. Kd4 Kg2 69. Ke4 g3 70. Ke5 h2 71. Ra5? { (-0.52 → -2.50) Mistake. Best move was Kc5. } (71. Kc5 Rd2 72. Ra6 Rc2+ 73. Kd6 Kxd4 74. Ke6 Ke4 75. Ra4+ Kf3 76. Kf5 Rc5+ 77. Kg6 Rc6+) 71... Kh3 72. Ra1?? { (-2.32 → -6.91) Blunder. Best move was Kc5. } (72. Kc5 Rd5+ 73. Kc4 Rd2 74. Ra6 Rh2 75. Rf6 Rh4 76. Ra6 Kf3 77. Kd5 Kxg4 78. Ke6 Rh3) 72... g2?? { (-6.91 → -2.79) Blunder. Best move was Kf3. } (72... Kf3) 73. Ra3+ Kg4? { (-3.21 → -1.69) Mistake. Best move was Ke3. } (73... Ke3 74. Kc2 Rd4 75. Rf6 Rxg4 76. Rxh6 Rh4 77. Rd6 g4 78. Rd3+ Ke4 79. Ra3 Rh3 80. Ra4+) 74. Re3? { (-1.69 → -2.82) Mistake. Best move was Rf1. } (74. Rf1 Rd4 75. Rg1 Re4 76. Kc3 Kf4 77. Rh1 Re6 78. Rg1 Re4 79. Rh1 Re6 80. Rg1 Kf3) 74... g1=Q 75. Re4+? { (-2.02 → -4.05) Mistake. Best move was Rh2. } (75. Rh2 Rxg4 76. Rxh6 Rh4 77. Rc6 g4 78. Kc2 Kf4 79. Kd3 Kf3 80. Rf6+ Kg2 81. Ke2 g3) 75... Kg5 76. Ke6?? { (-1.78 → -12.52) Blunder. Best move was Rh2. } (76. Rh2 Rd6 77. Rg2 Rd1 78. Rh2 Rd6 79. Rg2 Kf3 80. Rg1 Rd4 81. Rh1 Rxg4 82. Rh3+ Kg2) 76... Rf6+ 77. Ke7 Qc5+ 78. Kd8? { (-8.36 → -12.56) Mistake. Best move was Kd3. } (78. Kd3 h5 79. Rg2+ Kf5 80. Rh2 h4 81. Ke3 Kg4 82. Rg2+ Kh5 83. Rc2 Rf7 84. Ke4 h3) 78... Qd6+ 79. Kc8 Qc6+ 80. Kb8 Qxe4 81. Kc8 Qe7 82. Kb8 Rf8# { White is checkmated } 0-1
"""

val fromTcecWithEngineOutput = """
[Event "nTCEC - Stage 2 - Season 2"]
[Site "http://www.tcec-chess.net"]
[Date "2013.10.09"]
[Round "13.9"]
[White "Komodo 1092"]
[Black "Stockfish 160913"]
[Result "1-0"]
[WhiteElo "3113"]
[BlackElo "3110"]
[Time "05:39:46"]
[ECO "D10"]
[Opening "Slav: 3.Nc3 Nf6 4.e3"]
[TimeControl "7200+30"]
[PlyCount "165"]
[Number "117"]
[Termination "GUI adjudication"]
[WhiteType "program"]
[BlackType "program"]
[Variant "normal"]

{ 2 x Intel Xeon E5-2689 }
1.d4 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=d4, tb=0, R50=50, wv=0.00,  }
d5 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=d5, tb=0, R50=50, wv=0.00,  }
2.c4 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=c4, tb=0, R50=50, wv=0.00,  }
c6 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=c6, tb=0, R50=50, wv=0.00,  }
3.Nc3 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=Nc3, tb=0, R50=49, wv=0.00,  }
Nf6 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=Nf6, tb=0, R50=49, wv=0.00,  }
4.e3 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=e3, tb=0, R50=50, wv=0.00,  }
g6 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=g6, tb=0, R50=50, wv=0.00,  }
5.Nf3 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=Nf3, tb=0, R50=49, wv=0.00,  }
Bg7 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=Bg7, tb=0, R50=49, wv=0.00,  }
6.Be2 { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=Be2, tb=0, R50=48, wv=0.00,  }
O-O { ev=0.00, d=1, mt=00:00:00, tl=02:00:30, s=0 kN/s, n=0, pv=O-O, tb=0, R50=48, wv=0.00,  }
7.O-O { ev=0.26, d=22, pd=Be6, mt=00:01:39, tl=01:58:50, s=8705 kN/s, n=867251059, pv=O-O Be6 b3 a6 Bd2 Bf5 h3 Nbd7 Rc1 Re8 g4 Be6 Ng5 Nf8 f4 h6 Nxe6 Nxe6 cxd5 cxd5 h4 Qd6 g5 Ne4 Nxe4, tb=0, R50=47, wv=0.26,  }
Bg4 { ev=-0.44, d=32, pd=Qb3, mt=00:02:29, tl=01:58:00, s=17364 kN/s, n=2599449979, pv=Bg4 Qb3 Qb6 Bd2 e6 h3 Bxf3 Bxf3 Nbd7 Rfd1 Rfe8 Be2 dxc4 Bxc4 Qxb3 axb3 Red8 g4 Nb6 Kg2 Nfd5 Nxd5 Nxd5 Ba5 Nb6, tb=0, R50=47, wv=0.44,  }
8.Qb3 { ev=0.36, d=25, pd=Qb6, mt=00:02:59, tl=01:56:21, s=15490 kN/s, n=2790759992, pv=Qb3 Qb6 h3 Bxf3 Bxf3 dxc4 Qxc4 Nbd7 b4 e5 a4 a6 Rd1 Qc7 a5 Ne8 Qb3 Rd8 d5 e4 Be2 Ne5 Bb2 Nf6 Rac1, tb=0, R50=46, wv=0.36,  }
Qb6 { ev=-0.48, d=31, pd=h3, mt=00:01:26, tl=01:57:05, s=18266 kN/s, n=1576985656, pv=Qb6 h3 Bxf3 Bxf3 e6 Qa4 Qa6 Qxa6 Nxa6 Rb1 Nc7 b3 Rfe8 Bb2 Bf8 Rfc1 Kg7 g3 Bb4 Kg2 Rad8 a3 Be7 c5 Nd7, tb=0, R50=46, wv=0.48,  }
9.h3 { ev=0.34, d=25, pd=Bxf3, mt=00:02:01, tl=01:54:50, s=17533 kN/s, n=2135695329, pv=h3 Bxf3 Bxf3 e6 Bd2 Nbd7 Rfd1 Rfd8 Be2 Qc7 Rac1 Nb6 cxd5 exd5 Bd3 a5 a4 Nc8 f3 Qe7 Ne2 Nd6 Qc3 Bh6 Nf4, tb=0, R50=50, wv=0.34,  }
Bxf3 { ev=-0.38, d=34, pd=Bxf3, mt=00:01:20, tl=01:56:15, s=17740 kN/s, n=1422282657, pv=Bxf3 Bxf3 e6 Rd1 Nbd7 Bd2 Rfd8 Be2 dxc4 Bxc4 Qxb3 axb3 Nb6 Be2 Nbd5 g4 Bf8 h4 Be7 g5 Nxc3 bxc3 Ne4 Be1 Nd6, tb=0, R50=50, wv=0.38,  }
10.Bxf3 { ev=0.25, d=24, pd=e6, mt=00:00:57, tl=01:54:24, s=17221 kN/s, n=992438519, pv=Bxf3 e6 Na4 Qc7 cxd5 exd5 Nc5 b6 Na4 Re8 Nc3 Na6 Bd2 Qd6 Rfc1 Nc7 Rc2 Ne6 Rac1 Rad8 Be2 b5 Bd3 Ng5 a4, tb=0, R50=50, wv=0.25,  }
e6 { ev=-0.38, d=33, pd=Rd1, mt=00:02:52, tl=01:53:54, s=18817 kN/s, n=3240609540, pv=e6 Rd1 Nbd7 Bd2 Rfb8 Be2 dxc4 Bxc4 Qxb3 axb3 Rd8 g4 Nd5 Be2 Bf6 Kg2 Kg7 Ne4 Be7 g5 h6 h4 hxg5 hxg5 a6, tb=0, R50=50, wv=0.38,  }
11.Bd2 { ev=0.25, d=25, pd=Nbd7, mt=00:04:36, tl=01:50:18, s=14418 kN/s, n=3989679346, pv=Bd2 Nbd7 Rfd1 Rfd8 Rac1 Rac8 Be2 dxc4 Bxc4 Qxb3 axb3 Nb6 Be2 Nbd5 Ra1 a6 Bf3 Nd7 Ra5 Ra8 Ra4 N5b6 Raa1 f5 Ne2, tb=0, R50=49, wv=0.25,  }
Nbd7 { ev=-0.42, d=34, pd=Rfd1, mt=00:01:16, tl=01:53:08, s=17892 kN/s, n=1360474566, pv=Nbd7 Rfd1 Rfb8 Be2 dxc4 Bxc4 Qxb3 axb3 Rd8 g4 Nd5 Kg2 Bf6 Ne4 Be7 g5 Kg7 h4 a6 Rac1 Rac8 Be2 Rf8 Bf3 f5, tb=0, R50=49, wv=0.42,  }
12.Rfd1 { ev=0.26, d=25, pd=Rfd8, mt=00:02:26, tl=01:48:23, s=16901 kN/s, n=2486621695, pv=Rfd1 Rfd8 Rac1 Rac8 Be2 dxc4 Bxc4 Qxb3 axb3 Nb6 Be2 Bf8 Bf3 Nfd5 g3 Nb4 Be2 f5 Kg2 Ra8 Na4 N6d5 Nc5 Bxc5 dxc5, tb=0, R50=48, wv=0.26,  }
Rfb8 { ev=-0.40, d=33, pd=Rab1, mt=00:07:49, tl=01:45:49, s=19416 kN/s, n=9120778899, pv=Rfb8 Rab1 Qxb3 axb3 Nb6 Be2 Re8 g4 e5 dxe5 Rxe5 Kg2 Ne4 Nxe4 dxe4 Bb4 Ree8 Bd6 Be5 Bxe5 Rxe5 h4 Kg7 Ra1 a6, tb=0, R50=48, wv=0.40,  }
13.Be2 { ev=0.31, d=25, pd=Qc7, mt=00:04:42, tl=01:44:11, s=8437 kN/s, n=2390228202, pv=Be2 Qc7 Qc2 Rd8 Rac1 dxc4 Bxc4 Nb6 Bd3 Qd6 a3 e5 Ne2 exd4 Nxd4 Nfd5 Nf3 Rd7 h4 Re8 h5 Red8 Be2 Re8 hxg6, tb=0, R50=47, wv=0.31,  }
dxc4 { ev=-0.38, d=32, pd=Bxc4, mt=00:02:11, tl=01:44:08, s=20023 kN/s, n=2625161804, pv=dxc4 Bxc4 Qxb3 axb3 Rd8 Be2 Nd5 g4 Bf6 Ne4 Be7 g5 Kg7 h4 h5 Kg2 a6 Ba5 Rdc8 Bc3 Re8 Bd3 Rad8 Ba5 Rc8, tb=0, R50=50, wv=0.38,  }
14.Bxc4 { ev=0.39, d=23, pd=Qxb3, mt=00:01:41, tl=01:43:00, s=17663 kN/s, n=1803476132, pv=Bxc4 Qxb3 axb3 Rd8 g4 Nb6 Be2 Nbd5 Kg2 Ne8 Nxd5 exd5 Ba5 Rd7 h4 Nd6 Bb6 a6 h5 Bf6 Bd3 Re8 Rh1 Ne4 b4, tb=0, R50=50, wv=0.39,  }
Qxb3 { ev=-0.44, d=33, pd=axb3, mt=00:09:14, tl=01:35:24, s=20337 kN/s, n=11276634044, pv=Qxb3 axb3 Rd8 e4 b5 Bd3 Nc5 Bxb5 Rxd4 Be3 Rxd1 Rxd1 cxb5 Bxc5 Rc8 b4 a6 e5 Ne8 f4 f6 exf6 Bxf6 Kf2 Rd8, tb=0, R50=50, wv=0.44,  }
15.axb3 { ev=0.33, d=26, pd=Rd8, mt=00:03:31, tl=01:39:59, s=15744 kN/s, n=3341606297, pv=axb3 Rd8 g4 a6 g5 Nd5 Ne4 Bf8 Kg2 h5 h4 Be7 Be2 Kg7 Ba5 Rh8 Bc3 Rhf8 Rg1 Rac8 Bd3 Rb8 Ba5 Bb4 Rgd1, tb=0, R50=50, wv=0.33,  }
Ne8 { ev=-0.44, d=31, pd=g4, mt=00:01:46, tl=01:34:08, s=13322 kN/s, n=2151364497, pv=Ne8 g4 Nd6 Be2 Nb6 f3 Rc8 Kg2 a6 Ne4 Nxe4 fxe4 Bf6 Kg3 Bd8 Bc3 Bc7 Kg2 Bd8 Kf3 Nd7 Kg3 Bg5 d5 cxd5, tb=0, R50=49, wv=0.44,  }
16.Be2 { ev=0.47, d=25, pd=Nc7, mt=00:03:29, tl=01:37:01, s=18493 kN/s, n=3871138292, pv=Be2 Nc7 g3 Nd5 Kg2 Re8 e4 Nb4 Rac1 a5 Bf4 Nf6 Bg5 h6 Bd2 Red8 Bf4 Nd7 Bc7 Rdc8 Bd6 Bf8 Bf4 Kg7 e5, tb=0, R50=49, wv=0.47,  }
a6 { ev=-0.38, d=32, pd=g3, mt=00:04:06, tl=01:30:32, s=20400 kN/s, n=5029048486, pv=a6 g3 f5 Kg2 Nd6 Na4 Kf7 f3 Re8 Ba5 Nf6 Nb6 Nd5 Kf2 Bh6 Rd3 Nxb6 Bxb6 Nb5 Rdd1 Bf8 Ba5 Be7 h4 Bd6, tb=0, R50=50, wv=0.38,  }
17.Na4 { ev=0.47, d=24, pd=a5, mt=00:03:26, tl=01:34:05, s=18245 kN/s, n=3769943507, pv=Na4 a5 g3 Nc7 Kg2 Nd5 Nc3 Nb4 Ne4 Bf8 Bc3 Be7 Rdc1 Rd8 Nd2 Bd6 e4 Bc7 Ra3 Bb6 Nc4 Bc7 e5 h5 Nd2, tb=0, R50=49, wv=0.47,  }
f5 { ev=-0.30, d=32, pd=g3, mt=00:01:58, tl=01:29:04, s=19974 kN/s, n=2375071266, pv=f5 g3 Kf7 Be1 Nef6 b4 Nd5 Nc5 N7f6 Bd3 Ne8 Bd2 Nd6 f3 Re8 Kg2 Nc7 Rdc1 Rad8 Nxb7 Nxb7 Rxc6 Re7 Rac1 Rdd7, tb=0, R50=50, wv=0.30,  }
18.b4 { ev=0.41, d=25, pd=Kf7, mt=00:04:42, tl=01:29:54, s=13034 kN/s, n=3672809064, pv=b4 Kf7 Nc5 Nb6 g3 Nd6 f3 Re8 Bc1 Re7 e4 h6 Bf4 Rd8 b3 Nb5 Bxb5 axb5 Be5 Kg8 Kg2 fxe4 fxe4 Nc8 Nd3, tb=0, R50=50, wv=0.41,  }
Kf7 { ev=-0.30, d=34, pd=Bd3, mt=00:03:46, tl=01:25:48, s=20741 kN/s, n=4699142749, pv=Kf7 Bd3 Nd6 g3 Re8 Nc5 Nb6 Bc2 Rab8 Bb3 Ne4 Nxe4 fxe4 Kg2 Nd5 f3 Bh6 f4 Bf8 Kf2 Nxb4 Bxb4 Bxb4 Bc2 Kf6, tb=0, R50=49, wv=0.30,  }
19.Nc5 { ev=0.47, d=24, pd=Nb6, mt=00:04:59, tl=01:25:25, s=19256 kN/s, n=5784859753, pv=Nc5 Nb6, tb=0, R50=49, wv=0.47,  }
Nb6 { ev=-0.42, d=33, pd=g3, mt=00:01:19, tl=01:25:00, s=21132 kN/s, n=1680669218, pv=Nb6 g3 Nd6 Bd3 Nd5 f3 Re8 Kg2 Nb6 Rac1 Rab8 Re1 Bh6 Kf2 Bg7 Bc2 Bf6 Bc3 Nb5 Kg2 Bg5 h4 Bh6 Rcd1 Nd5, tb=0, R50=48, wv=0.42,  }
20.e4 { ev=0.54, d=26, pd=Bxd4, mt=00:04:22, tl=01:21:34, s=18725 kN/s, n=4921124085, pv=e4 Bxd4 exf5 gxf5 Bf4 e5 Bh5 Kg8 Rxd4 exd4 Bxb8 Rxb8 Rd1 Nd5 Rxd4 Nec7 Bf3 Kf7 g3 Kg7 Kg2 Kg6 g4 Nb5 Rd1, tb=0, R50=50, wv=0.54,  }
Bxd4 { ev=-0.34, d=32, pd=exf5, mt=00:01:18, tl=01:24:11, s=18657 kN/s, n=1469574656, pv=Bxd4 exf5 gxf5 Bh5 Kg8 Bf4 e5 Rxd4 exd4 Bxb8 Rxb8 Rd1 Nd5 Rxd4 Nec7 Rd1 Kg7 Bf3 Kh6 g3 Kg6 Kg2 Kg5 h4 Kh6, tb=0, R50=50, wv=0.34,  }
21.exf5 { ev=0.53, d=26, pd=gxf5, mt=00:02:35, tl=01:19:29, s=20845 kN/s, n=3247118248, pv=exf5 gxf5 Bh5 Kg8 Bf4 e5 Rxd4 exd4 Bxb8 Rxb8 Rd1 Nc7 Rxd4 Nb5 Rd1 Nd5 Bf3 Nbc7 g4 fxg4 hxg4 Nxb4 Rd7 Ncd5 Nxb7, tb=0, R50=50, wv=0.53,  }
gxf5 { ev=-0.42, d=34, pd=Bh5, mt=00:02:29, tl=01:22:13, s=20927 kN/s, n=3118475249, pv=gxf5 Bh5 Kg8 Bf4 e5 Rxd4 exd4 Bxb8 Rxb8 Rd1 Nd5 Rxd4 Nec7 Rd1 f4 Rd4 Nb5 Rd3 Nbc7 Rd1 h6 Rd3 Ne8 Rd4 Nef6, tb=0, R50=50, wv=0.42,  }
22.Bf4 { ev=0.49, d=28, pd=e5, mt=00:07:07, tl=01:12:52, s=22306 kN/s, n=9546958061, pv=Bf4 e5 Rxd4 exd4 Bxb8 Rxb8 Rd1 Nd5 Rxd4 Ke7 Bf3 Nef6 Rh4 Kf7 g3 Kg6 Rd4 h5 Kg2 Kg5 Bd1 Kg6 Bb3 Kg5 Bc2, tb=0, R50=49, wv=0.49,  }
e5 { ev=-0.54, d=36, pd=Bh5, mt=00:01:38, tl=01:21:05, s=19932 kN/s, n=1959582427, pv=e5 Bh5 Kg8 Rxd4 exd4 Bxb8 Rxb8 Rd1 Nd5 Rxd4 Nec7 Rd1 f4 Rd4 Nb5 Rd3 Nbc7 Rd1 h6 Rd4 Nb5 Re4 Nf6 Re5 Nc7, tb=0, R50=50, wv=0.54,  }
23.Bh5+ { ev=0.51, d=27, pd=Kg8, mt=00:02:30, tl=01:10:52, s=22206 kN/s, n=3362022616, pv=Bh5 Kg8 Rxd4 exd4 Bxb8 Rxb8 Rd1 Nd6 Rxd4 Nb5 Rd1 Nd5 Be2 Kg7 Bc4 Nbc7 g3 Kg6 Kg2 h5 Rd4 Nb5 Rh4 Nf6 Bb3, tb=0, R50=49, wv=0.51,  }
Kg8 { ev=-0.56, d=36, pd=Rxd4, mt=00:01:26, tl=01:20:10, s=21819 kN/s, n=1880010100, pv=Kg8 Rxd4 exd4 Bxb8 Rxb8 Rd1 Nd5 Rxd4 Nec7 Rd1 f4 Rd4 Nb5 Rd3 Nbc7 Rd1 Kg7 Re1 Kg8 Bf3 h6 Re4 Kg7 Kf1 Kg6, tb=0, R50=49, wv=0.56,  }
24.Rxd4 { ev=0.52, d=27, pd=exd4, mt=00:01:29, tl=01:09:53, s=22468 kN/s, n=2034374447, pv=Rxd4 exd4 Bxb8 Rxb8 Rd1 Kg7 Bf3 Nd5 Rxd4 Nef6 Kf1 Kg6 g3 h5 Be2 Ne8 Kg2 Nd6 Bf3 Nb5 Rh4 Nf6 Be2 Nc7 Rd4, tb=0, R50=50, wv=0.52,  }
exd4 { ev=-0.48, d=36, pd=Bxb8, mt=00:01:48, tl=01:18:52, s=22647 kN/s, n=2451254509, pv=exd4 Bxb8 Rxb8 Rd1 Nd5 Rxd4 Nec7 Rd1 b5 g3 Rd8 f4 Kf8 Kf2 Rd6 Bf3 Rh6 Nd7 Ke7 Ne5 Rd6 Nd3 Nb6 Re1 Kf6, tb=0, R50=50, wv=0.48,  }
25.Bxb8 { ev=0.53, d=26, pd=Rxb8, mt=00:02:38, tl=01:07:46, s=22512 kN/s, n=3582963397, pv=Bxb8 Rxb8 Rd1 Nd5 Rxd4 Nec7 Kh2 Kg7 Bf3 Kf7 g3 Kg7 Kg2 Kf7 Rc4 Kg6 g4 Nf6 Be2 fxg4 hxg4 h5 gxh5 Nxh5 Nd7, tb=0, R50=50, wv=0.53,  }
Rxb8 { ev=-0.52, d=37, pd=Rd1, mt=00:01:34, tl=01:17:49, s=21937 kN/s, n=2060527533, pv=Rxb8 Rd1 Nd5 Rxd4 Nec7 Rd1 b5 g3 Rd8 f4 Kf8 Kf2 Rd6 Bf3 Rh6 h4 Rd6 Nd3 Ke7 Ra1 Nb6 Re1 Kf6 Rc1 Nc4, tb=0, R50=50, wv=0.52,  }
26.Rd1 { ev=0.52, d=25, pd=Nd5, mt=00:00:45, tl=01:07:31, s=22461 kN/s, n=1030190815, pv=Rd1 Nd5 Rxd4 Kg7 Bf3 Nec7 Kf1 h6 Rc4 b5 Rd4 Re8 Rd1 Kg6 g3 Kf6 Rd4 Re7 Rh4 Kg7 Kg2 Re1 Nd3 Re7 Rh5, tb=0, R50=49, wv=0.52,  }
Nd5 { ev=-0.52, d=38, pd=Rxd4, mt=00:01:52, tl=01:16:26, s=21448 kN/s, n=2616184509, pv=Nd5 Rxd4 Nec7 Rd1 b5 g3 Rd8 f4 Kf8 Kf2 Rd6 Bf3 Rh6 h4 Rd6 Nd3 Ke7 Ra1 Nb6 Re1 Kf6 Rc1 Nc4 b3 Nd2, tb=0, R50=49, wv=0.52,  }
27.Rxd4 { ev=0.52, d=26, pd=Nec7, mt=00:00:52, tl=01:07:09, s=22317 kN/s, n=1181957570, pv=Rxd4 Nec7 Bf3 Kf7 Kh2 h6 g3 Nf6 Rh4 Kg6 Kg2 h5 Rd4 Ncd5 Rc4 Nc7 Rh4 Nb5 Kf1 Nc7 Rc4 Kg5 Rd4 Nfd5 Kg2, tb=0, R50=50, wv=0.52,  }
Nec7 { ev=-0.68, d=37, pd=Bf3, mt=00:11:51, tl=01:05:06, s=24031 kN/s, n=17087850847, pv=Nec7 Bf3 Kg7 Kh2 Kf7 g4 Ke7 gxf5 b6 Ne4 Rf8 Rc4 Rxf5 Bg4 Re5 Ng3 Ne6 Rxc6, tb=0, R50=49, wv=0.68,  }
28.Bf3 { ev=0.53, d=26, pd=h6, mt=00:01:14, tl=01:06:26, s=22252 kN/s, n=1668022467, pv=Bf3 h6 g3 Kg7 Kg2 Kf7 Rh4 Kg7 Rc4 Kg6 Bd1 Nb5 Bc2 h5 Rh4 Nbc7 Bd3 Ne8 Be2 Nef6 Rd4 Re8 Bd3 Re7 g4, tb=0, R50=49, wv=0.53,  }
Kf7 { ev=-0.80, d=34, pd=g3, mt=00:02:10, tl=01:03:27, s=22149 kN/s, n=2879316589, pv=Kf7 g3 Kg6 Kg2 Kf7 Kh2 Nf6 Bd1 Ke7 Bc2 Nb5 Rd1 b6 Re1 Kf7 Ne6 Nd5 Bxf5 h6 Nf4 Nd4 Bg6 Kg7 Re4 Nf3, tb=0, R50=48, wv=0.80,  }
29.Kh2 { ev=0.53, d=25, pd=h6, mt=00:02:06, tl=01:04:50, s=22272 kN/s, n=2822019119, pv=Kh2 h6 g3 Nf6 Be2 Nfd5 Rh4 Kg6 Kg2 Nb5 Bf3 Nf6 Rf4 Nc7 Rd4 Nfd5 g4 Nb5 Rd1 Nf6 Be2 h5 gxh5 Kh6 Bf3, tb=0, R50=48, wv=0.53,  }
Nf6 { ev=-0.62, d=35, pd=Rd6, mt=00:02:11, tl=01:01:46, s=21662 kN/s, n=2848166692, pv=Nf6 Rd6 Nce8 Rd3 Nc7 g4 fxg4 hxg4 Ne6 Kg3 Nxc5 bxc5 h6 Kh4 a5 Re3 Rd8 Rb3 Rd7 Bg2 Nd5 Be4 Kf6 f3 Nb4, tb=0, R50=47, wv=0.62,  }
30.Bd1 { ev=0.56, d=28, pd=Ncd5, mt=00:03:15, tl=01:02:05, s=18906 kN/s, n=3702145832, pv=Bd1 Ncd5 g4 fxg4 hxg4 b6 Nxa6 Rg8 b5 c5 Rc4 Rg5 b4 cxb4 Nxb4 Nxb4 Rxb4 h5 gxh5 Nxh5 Kh3 Re5 Bf3 Rf5 Bg4, tb=0, R50=47, wv=0.56,  }
Ncd5 { ev=-0.64, d=34, pd=g3, mt=00:01:29, tl=01:00:47, s=20807 kN/s, n=1860008814, pv=Ncd5 g3 Ke7 Bc2 Kd6 Rd1 Re8 Bxf5 Re2 Kg1 Kc7 Nd3 Ne7 Kf1 Rc2 Be6 Nfd5 Bg4 Kb6 b3 Ka7 Nc5 Rc3 Rd3 Rc1, tb=0, R50=46, wv=0.64,  }
31.g4 { ev=0.57, d=28, pd=fxg4, mt=00:04:13, tl=00:58:23, s=22544 kN/s, n=5715181655, pv=g4 fxg4 hxg4 b6 Nxa6 Rg8 b5 c5 Rc4 Rg5 b4 Nxb4 Nxb4 cxb4 Rxb4 h5 gxh5 Nxh5 Kh3 Rc5 Bf3 Rf5 Bg4 Rc5 Bd7, tb=0, R50=50, wv=0.57,  }
h6 { ev=-0.82, d=35, pd=gxf5, mt=00:01:27, tl=00:59:50, s=20539 kN/s, n=1802062055, pv=h6 gxf5 Ke7 Rh4 h5 Kg3 Rg8 Kf3 b5 Nxa6 Kd6 Nc5 Ke5 Ne6 Rg1 Ke2 Rh1 Bc2 Kd6 Ng5 Rc1 Bd3 Rg1 Ne6 Ra1, tb=0, R50=50, wv=0.82,  }
32.gxf5 { ev=0.60, d=25, pd=Ke7, mt=00:05:20, tl=00:53:33, s=3330 kN/s, n=1069159122, pv=gxf5 Ke7 Rh4 h5 Kg3 Rg8 Kf3 Rb8 Bb3 Kd6 Ne4 Ke5 Bxd5 cxd5 Nxf6 Kxf6 Rxh5 Rd8 b3 d4 Ke2 d3 Kd2 b5 Rh6, tb=0, R50=50, wv=0.60,  }
Ke7 { ev=-0.78, d=37, pd=Rh4, mt=00:01:09, tl=00:59:11, s=20737 kN/s, n=1442876766, pv=Ke7 Rh4 h5 Kg3 Rg8 Kf3 b5 Nxa6 Kd6 Nc5 Rg1 Ke2 Ke5 Ne6 Rh1 Bc2 Kd6 Nd8 Kc7 Nf7 Rc1 Bd3 Rg1 Ne5 Kd6, tb=0, R50=49, wv=0.78,  }
33.Kg3 { ev=0.67, d=29, pd=Kd6, mt=00:03:52, tl=00:50:12, s=22573 kN/s, n=5243033029, pv=Kg3 Kd6 Rh4 Rg8 Kf3 b6 Ne4 Ke5 Bc2 Kxf5 Nxf6 Kxf6 Rxh6 Ke5 Rh5 Kd6 Rh4 Rf8 Kg3 Nf6 Bd3 b5 Be4 Rf7 Bf3, tb=0, R50=49, wv=0.67,  }
Kd6 { ev=-1.03, d=35, pd=Rh4, mt=00:03:36, tl=00:56:06, s=22382 kN/s, n=4832102826, pv=Kd6 Rh4 Rg8 Kf3 b6 Nxa6 h5 Bb3 Rg5 Bc2 Rg8 b5 c5 b4 Rg1 bxc5 bxc5 Rc4 Nd7 Ke2 Ra1 Be4 Ra2 Kf1 Nf4, tb=0, R50=48, wv=1.03,  }
34.Rh4 { ev=0.69, d=27, pd=Rg8, mt=00:00:47, tl=00:49:54, s=22940 kN/s, n=1109422691, pv=Rh4 Rg8 Kf3 b6 Ne4 Ke5 Bc2 Kxf5 Nxf6 Kxf6 Rxh6 Ke5 Rh5 Kd6 Rh4 Rf8 Kg3 Nf6 f3 b5 Rh6 Kd5 f4 c5 bxc5, tb=0, R50=48, wv=0.69,  }
Rg8+ { ev=-0.96, d=36, pd=Kf3, mt=00:02:51, tl=00:53:45, s=23639 kN/s, n=4047301070, pv=Rg8 Kf3 b6 Nxa6 h5 Bb3 Rg5 Bc2 Rg8 b5 c5 b4 Rg1 bxc5 bxc5 Rc4 Nd7 Ke2 Ra1 Be4 Ra2 Rc2 Rxc2 Bxc2 c4, tb=0, R50=47, wv=0.96,  }
35.Kf3 { ev=0.74, d=27, pd=b6, mt=00:00:43, tl=00:49:42, s=23529 kN/s, n=1016046397, pv=Kf3 b6 Ne4 Ke5 Bc2 Kxf5 Nxf6 Kxf6 Rxh6 Ke5 Rh5 Kd6 Rh4 Rf8 Kg3 Nf6 f3 Rg8 Kf2 Rc8 Rd4 Ke5 Rd1 Nd5 Re1, tb=0, R50=47, wv=0.74,  }
b6 { ev=-1.03, d=37, pd=Ne4, mt=00:01:47, tl=00:52:29, s=21995 kN/s, n=2352191797, pv=b6 Ne4 Ke5 Rxh6 Nxe4 Re6 Kxf5 Rxe4 Rh8 h4 Nf6 Rf4 Ke6 Ba4 b5 Bb3 Ke5 Bc2 Ke6 Bg6 Rh6 Bf5 Ke5 Be4 Nd5, tb=0, R50=50, wv=1.03,  }
36.Ne4+ { ev=0.72, d=28, pd=Ke5, mt=00:02:02, tl=00:48:10, s=13563 kN/s, n=1672680987, pv=Ne4 Ke5 Bc2 Kxf5 Nxf6 Kxf6 Rxh6 Ke5 Rh5 Kd6 Rh4 Rf8 Kg3 Nf6 f3 Rg8 Kf2 Rc8 Rf4 Ke5 Rf5 Ke6 Rg5 Rh8 Kg3, tb=0, R50=49, wv=0.72,  }
Ke5 { ev=-0.98, d=39, pd=Rxh6, mt=00:01:13, tl=00:51:45, s=21817 kN/s, n=1610888716, pv=Ke5 Rxh6 Nxe4 Re6 Kxf5 Rxe4 Rh8 h4 Nf6 Rf4 Ke6 Ba4 b5 Bb3 Ke5 Bc2 Ke6 Bg6 Rh6 Bf5 Ke5 Be4 Nd5 Bxd5 cxd5, tb=0, R50=49, wv=0.98,  }
37.Bc2 { ev=0.73, d=28, pd=Kxf5, mt=00:02:24, tl=00:46:16, s=23160 kN/s, n=3337785673, pv=Bc2 Kxf5 Nxf6 Kxf6 Rxh6 Ke5 Rh5 Kd6 Rh4 Rf8 Kg3 Nf6 f3 Rg8 Kf2 Rc8 Rf4 Ke5 Rf5 Ke6 Rg5 Rh8 f4 Kd6 Kg3, tb=0, R50=48, wv=0.73,  }
Kxf5 { ev=-0.98, d=38, pd=Rxh6, mt=00:01:04, tl=00:51:11, s=20282 kN/s, n=1307439773, pv=Kxf5 Rxh6 Ke5 Nxf6 Nxf6 Rh4 Kd6 Rc4 Rb8 h4 Rh8 Bg6 b5 Rd4 Ke6 Rf4 Rh6 Bf5 Ke5 Be4 Nd5 Bxd5 cxd5 Ke3 Rh7, tb=0, R50=50, wv=0.98,  }
38.Nxf6+ { ev=0.73, d=28, pd=Kxf6, mt=00:01:31, tl=00:45:16, s=19957 kN/s, n=1818843406, pv=Nxf6 Kxf6 Rxh6 Ke5 Rh5 Kd6 Rh4 Rf8 Kg3 Nf6 Rd4 Ke5 Rc4 Kd6 Kg2 Rg8 Kf3 b5 Rf4 Ke5 h4 Rg1 Rf5 Ke6 Rg5, tb=0, R50=50, wv=0.73,  }
Kxf6 { ev=-0.98, d=33, pd=Rxh6, mt=00:00:20, tl=00:51:21, s=18200 kN/s, n=377122760, pv=Kxf6 Rxh6 Ke5 Rh5 Kd6 Rh4 Nf6 Rc4 Rb8 h4 Rh8 Bg6 b5 Rd4 Ke6 Rf4 Rh6 Bf5 Ke5 Be4 Nd5 Bxd5 cxd5 Ke3 Rh7, tb=0, R50=50, wv=0.98,  }
39.Rxh6+ { ev=0.71, d=26, pd=Ke5, mt=00:00:28, tl=00:45:18, s=23313 kN/s, n=667727965, pv=Rxh6 Ke5 Rh5 Kd6 Rh4 Rf8 Kg3 Nf6 f3 Ke5 Rc4 Rg8 Kf2 Rd8 f4 Kd5 Bd3 Rh8 Kg3 Rg8 Kh4 b5 Rc3 Kd6 Bf5, tb=0, R50=50, wv=0.71,  }
Ke5 { ev=-0.98, d=37, pd=Rh5, mt=00:01:06, tl=00:50:45, s=22357 kN/s, n=1480200115, pv=Ke5 Rh5 Kd6 Rh4 Nf6 Rc4 Rb8 h4 Rh8 Bg6 b5 Rd4 Ke6 Rf4 Rh6 Bf5 Ke5 Be4 Nd5 Bxd5 cxd5 Ke3 Rh7 Rg4 Rh8, tb=0, R50=49, wv=0.98,  }
40.Rh5+ { ev=0.72, d=27, pd=Kd6, mt=00:00:59, tl=00:44:49, s=23100 kN/s, n=1379492877, pv=Rh5 Kd6 Rh4 Rf8 Kg3 Nf6 f3 Rg8 Kf2 Rb8 Rd4 Ke5 Rc4 Kd6 Be4 c5 Bf5 Kd5 Bd3 cxb4 Rxb4 a5 Rc4 Ke5 f4, tb=0, R50=49, wv=0.72,  }
Kd6 { ev=-0.98, d=38, pd=Rh4, mt=00:02:17, tl=00:48:58, s=23289 kN/s, n=3202599280, pv=Kd6 Rh4 Nf6 Rc4 Rb8 h4 Rh8 Bg6 b5 Rd4 Ke6 Rf4 Rh6 Bf5 Ke5 Be4 Nd5 Bxd5 cxd5 Ke3 Rh7 Rg4 Rh8 Rd4 Rh5, tb=0, R50=48, wv=0.98,  }
41.Rh4 { ev=0.73, d=27, pd=Rf8, mt=00:02:33, tl=00:42:47, s=19449 kN/s, n=2985941436, pv=Rh4 Rf8 Kg3 Nf6 f3 Rg8 Kf2 Ke5 Rc4 Rd8 Kg3 Rg8 Kh2 Kd6 h4 Rh8 Kh3 b5 Rf4 Ke6 Bg6 Rh6 Be4 Rh8 Rf5, tb=0, R50=48, wv=0.73,  }
Nf6 { ev=-0.98, d=39, pd=Rc4, mt=00:02:39, tl=00:46:49, s=23268 kN/s, n=3712651549, pv=Nf6 Rc4 Rb8 h4 Rh8 Rf4 Ke6 Ba4 b5 Bb3 Ke5 Bc2 Rh6 Rf5 Kd4 Kf4 Nd5 Kg5 Rh8 Rf7 Nxb4 Bg6 Nd5 h5 a5, tb=0, R50=47, wv=0.98,  }
42.Rd4+ { ev=0.85, d=26, pd=Ke5, mt=00:01:31, tl=00:41:46, s=23087 kN/s, n=2110176502, pv=Rd4 Ke5 Rc4 Rf8 h4 Nd5 Kg3 Rg8 Kh2 Rf8 Re4 Kd6 Kg3 Nf6 Rd4 Ke5 Rc4 Kd6 Kg2 Nh5 f3 Nf6 Kh3 Kd5 Bb3, tb=0, R50=47, wv=0.85,  }
Ke5 { ev=-0.98, d=40, pd=Rc4, mt=00:01:18, tl=00:46:02, s=22082 kN/s, n=1721098426, pv=Ke5 Rc4 Kd6 h4 Rh8 Rf4 Ke6 Ba4 b5 Bb3 Ke5 Bc2 Ke6 Rd4 Ke5 Rf4, tb=0, R50=46, wv=0.98,  }
43.Rc4 { ev=0.86, d=26, pd=Nd5, mt=00:01:07, tl=00:41:10, s=24141 kN/s, n=1637132054, pv=Rc4 Nd5 Re4 Kd6 Rg4 Rf8 Kg3 Nf6 Rg6 Ke7 Rg7 Ke6 Rg5 Kd6 Bd3 Rh8 f4 Nh5 Kf3 Nf6 Bf5 Nd5 Rg6 Kc7 Rg7, tb=0, R50=46, wv=0.86,  }
Kd6 { ev=-1.15, d=40, pd=Rf4, mt=00:09:41, tl=00:36:52, s=24077 kN/s, n=14124694124, pv=Kd6 Rf4 Ke7 h4 Rg1, tb=0, R50=45, wv=1.15,  }
44.h4 { ev=0.85, d=28, pd=Rh8, mt=00:01:54, tl=00:39:46, s=18868 kN/s, n=2174131272, pv=h4 Rh8 Bg6 Ke7 Bf5 Kd6 Rf4 Ke5 Bg6 Ke6 Bd3 Rd8 Bc2 Ke5 Rc4 Rf8 Kg2 Kd6 Bd3 Nh5 Be2 b5 Rd4 Ke5 Rd2, tb=0, R50=50, wv=0.85,  }
Rh8 { ev=-0.98, d=41, pd=Rf4, mt=00:00:53, tl=00:36:29, s=21664 kN/s, n=1156190099, pv=Rh8 Rf4 Ke6 Ba4 b5 Bb3 Ke5 Bc2 Ke6 Bg6 Rh6 Bf5 Ke5 Bc2 Kd6 Bb1 Ke6 Ba2 Ke5 Ke3 Rh8 Rd4 Nd5 Bxd5 cxd5, tb=0, R50=49, wv=0.98,  }
45.Bf5 { ev=0.85, d=25, pd=Kd5, mt=00:01:47, tl=00:38:29, s=17023 kN/s, n=1838495310, pv=Bf5 Kd5 Bd3 Ke5 Rf4 Ke6 Bg6 Rh6 Bf5 Ke5 Bd3 Rh8 Bc2 Ke6 Bf5 Ke5 Bg6 Ke6 Rc4 Kd5 Bf7 Kd6 Rd4 Ke5 Rf4, tb=0, R50=49, wv=0.85,  }
Kd5 { ev=-0.98, d=40, pd=Bd3, mt=00:00:58, tl=00:36:01, s=21892 kN/s, n=1271869160, pv=Kd5 Bd3 b5 Rf4 Ke6 Bf5 Ke5 Bc2 Ke6 Bg6 Rh6 Bf5 Ke5 Bc2 Kd6 Bb1 Ke6 Ba2 Ke5 Ke3 Rh8 Rd4 Nd5 Bxd5 cxd5, tb=0, R50=48, wv=0.98,  }
46.Bd3 { ev=0.86, d=27, pd=Nd7, mt=00:00:48, tl=00:38:10, s=23871 kN/s, n=1172283690, pv=Bd3 Nd7 Ke3 Nf6 Rd4 Ke6 Bg6 Rh6 Bc2 Rh8 Kf3 Ke5 Rf4 Rd8 Bg6 Rd2 Kg3 Nd5 Rf5 Ke6 h5 Rxb2 h6 Nf6 Rf4, tb=0, R50=48, wv=0.86,  }
b5 { ev=-1.15, d=42, pd=Rf4, mt=00:08:01, tl=00:28:30, s=24743 kN/s, n=11914412982, pv=b5 Rf4 Ke6 Bf5 Kd6 Bb1 Ke7 Rd4 Ke6 Bc2 Ke5 Rf4 Nd5 Re4 Kf6 Kg2 Rh6 Rd4 Ke6 Kg3 Ke5 Rg4 Kf6 Bb1 Ke5, tb=0, R50=50, wv=1.15,  }
47.Rf4 { ev=0.76, d=26, pd=Ke6, mt=00:00:44, tl=00:37:56, s=25491 kN/s, n=1152450075, pv=Rf4 Ke6 Bg6 Ke7 Bf5 Kd6 Bb1 Ke6 Ba2 Ke5 Bb3 Rd8 Bc2 Ke6 Bg6 Rh8 Bb1 Rd8 Ke2 Rh8 Bg6 Nd5 Rd4 Ke5 Re4, tb=0, R50=49, wv=0.76,  }
Ke6 { ev=-0.98, d=41, pd=Bc2, mt=00:01:11, tl=00:27:49, s=21701 kN/s, n=1568310693, pv=Ke6 Bc2 Rh6 Bb1 Rh8 Ba2 Ke5 Bb1 Ke6, tb=0, R50=49, wv=0.98,  }
48.Bg6 { ev=0.77, d=29, pd=Ke7, mt=00:02:44, tl=00:35:42, s=15402 kN/s, n=2540066225, pv=Bg6 Ke7 Ke2 Rh6 Bf5 Kd6 Bc2 Ke5 Kf3 Rh8 Bb1 Ke6 Bf5 Ke5 Bh3 Rh6 Bc8 Kd6 Bf5 Ke5 Bb1 Ke6 Be4 Nd5 Bxd5, tb=0, R50=48, wv=0.77,  }
Rh6 { ev=-0.98, d=40, pd=Bf5, mt=00:02:37, tl=00:25:42, s=20735 kN/s, n=3269718263, pv=Rh6 Bf5 Ke5 Bb1 Ke6 Ba2 Ke5 Bf7 Kd6 Ba2 Ke5, tb=0, R50=48, wv=0.98,  }
49.Bf5+ { ev=0.93, d=26, pd=Kd5, mt=00:01:22, tl=00:34:50, s=24169 kN/s, n=2008523377, pv=Bf5 Kd5 Bb1 Ke6 Kg2 Rh5 Kg3 c5 bxc5 Rxc5 Rf3 Nh5 Kh3 a5 Ba2 Ke5 Bf7 Nf6 Bg6 Ke6 Re3 Kd6 Rd3 Ke7 Rg3, tb=0, R50=47, wv=0.93,  }
Kd6 { ev=-1.43, d=40, pd=Bb1, mt=00:03:05, tl=00:23:07, s=22303 kN/s, n=4130618123, pv=Kd6 Bb1 Ke7 Ba2 Rh8 Rd4 Rh6 Kg2 Rg6 Kf1 Rh6 f3 Nd5 Re4 Kf6 Kf2 Rh5 Kg3 Rh7 Bb1 Rh8 Bc2 Kf7 Rg4 Kf6, tb=0, R50=47, wv=1.43,  }
50.Be4 { ev=0.94, d=26, pd=Kd7, mt=00:02:06, tl=00:33:14, s=20919 kN/s, n=2657794772, pv=Be4 Kd7 Kg2 Nd5 Bf5 Ke7 Rg4 Nf6 Rd4 Rh8 Kg3 Rg8 Kh2 Rh8 Kg2 Rg8 Kf3 Rf8 Bh3 Rf7 Kg3 Rg7 Kf4 Nd5 Kf3, tb=0, R50=46, wv=0.94,  }
Kd7 { ev=-1.43, d=40, pd=Bb1, mt=00:01:13, tl=00:22:25, s=20710 kN/s, n=1521405869, pv=Kd7 Bb1 Ke7 Ba2 Rh8 Rd4 Rh6 Kg2 Rg6 Kf1 Rh6 f3 Nd5 Re4 Kf6 Kf2 Rh5 Kg3 Rh7 Bb1 Rh8 Bc2 Kf7 Rg4 Kf6, tb=0, R50=46, wv=1.43,  }
51.Kg2 { ev=0.97, d=28, pd=Nd5, mt=00:01:12, tl=00:32:32, s=22740 kN/s, n=1646409049, pv=Kg2 Nd5 Bf5 Kd6 Re4 Ne7 Bh3 Nd5 Kg3 Rg6 Rg4 Rh6 Bg2 Ke6 Bf3 Ke5 Bd1 Rd6 Bc2 Nf6 Rg5 Ke6 Bf5 Ke7 Rg7, tb=0, R50=45, wv=0.97,  }
Kd6 { ev=-1.43, d=35, pd=Bb1, mt=00:00:45, tl=00:22:10, s=20082 kN/s, n=908861798, pv=Kd6 Bb1 Ke7 Kh3 Nd5 Rd4 Kf6 Rg4 Kf7 Kg3 Kf6 f4 Nxb4 f5 Nd5 Rg6 Rxg6 fxg6 Ne7 h5 Kg5 g7 Kh6 Kg4 Kxg7, tb=0, R50=45, wv=1.43,  }
52.Bb1 { ev=1.07, d=26, pd=Ke7, mt=00:01:00, tl=00:32:02, s=23657 kN/s, n=1443022225, pv=Bb1 Ke7 Kh3 Rh8 Bg6 Rh6 Be4 Nd5 Rf3 Rh8 Bg6 Nxb4 h5 c5 Kg4 c4 Kg5 Nd5 h6 b4 Rf5 Nc7 Rf7 Kd6 Rf6, tb=0, R50=44, wv=1.07,  }
Ke7 { ev=-1.51, d=35, pd=Kh3, mt=00:00:47, tl=00:21:53, s=22193 kN/s, n=1045592842, pv=Ke7 Kh3 Nd5 Rg4 Kf6 Kg3 Ke7 Bf5 Kf6 Bc2 Rh8 Bd3 Ke7 Bb1 Nf6 Rg7 Ke6 Rg6 Ke7 f4 Re8 Rg5 Kd6 h5 Re1, tb=0, R50=44, wv=1.51,  }
53.Kh3 { ev=1.13, d=26, pd=Rh8, mt=00:02:12, tl=00:30:20, s=23961 kN/s, n=3182939945, pv=Kh3 Rh8 Bg6 Rh6 Be4 Nd5 Rf3 Rh8 Bg6 Nxb4 Kg4 c5 Kg5 Nd5 h5 c4 h6 b4 Rf7 Kd6 Ra7 Nc7 h7 c3 bxc3, tb=0, R50=43, wv=1.13,  }
Nd5 { ev=-1.73, d=35, pd=Rg4, mt=00:02:24, tl=00:19:59, s=23528 kN/s, n=3401044408, pv=Nd5 Rg4 Kf7 Kg3 Ke7 Bc2 Kf6 Bd3 Ke7 Bc2, tb=0, R50=43, wv=1.73,  }
54.Rf3 { ev=1.13, d=24, pd=Nxb4, mt=00:01:15, tl=00:29:36, s=24526 kN/s, n=1852644326, pv=Rf3 Nxb4 Kg4 Rf6 Bf5 Nd5 Kg5 Kf8 h5 Kg7 Be4 Rxf3 Bxf3 a5 h6 Kg8 Bg4 a4 Be6 Kh7 Bf5 Kh8 Bc2 Nb6 f4, tb=0, R50=42, wv=1.13,  }
Nxb4 { ev=-2.10, d=32, pd=Kg4, mt=00:00:41, tl=00:19:48, s=17527 kN/s, n=730893451, pv=Nxb4 Kg4 Rf6 Bf5 Nd5 Kg5 Kf8 Ra3 Kf7 h5 Ne7 Bg4 Rd6 Bf3 Rf6 Be4 Re6 f3 Re5 Kh4 Nf5 Kg4 Nh6 Kg3 Ke7, tb=0, R50=50, wv=2.10,  }
55.Kg4 { ev=1.44, d=24, pd=Rf6, mt=00:00:22, tl=00:29:44, s=25114 kN/s, n=572786733, pv=Kg4 Rf6 Bf5 a5 h5 Nd5 Kg5 a4 h6 Kf8 Rd3 Rd6 f4 Kg8 Rd4 Rd8 Be6 Kh7 f5 Re8 Bxd5 cxd5 Rxd5 Rg8 Kh5, tb=0, R50=49, wv=1.44,  }
Rf6 { ev=-2.34, d=35, pd=Bf5, mt=00:00:55, tl=00:19:23, s=19594 kN/s, n=1090977729, pv=Rf6 Bf5 Nd5 Kg5 Kf8 Ra3 Kg7 Rxa6 Ne7 Be4 Re6 f3 Re5 Kg4 Rc5 h5 Ng8 Ra7 Kf6 b4 Nh6 Kf4 Rxh5 Ra6 Nf7, tb=0, R50=49, wv=2.34,  }
56.Bf5 { ev=1.26, d=24, pd=Kf8, mt=00:00:28, tl=00:29:46, s=25965 kN/s, n=750983835, pv=Bf5 Kf8 Kg5 Nd5 h5 Kg7 Rd3 a5 f4 Rf8 Rd4 a4 Bg6 Kh8 h6 b4 f5 Ra8 f6 Nxf6 Kxf6 c5 Rd1 a3 bxa3, tb=0, R50=48, wv=1.26,  }
Nd5 { ev=-2.62, d=37, pd=Kg5, mt=00:01:00, tl=00:18:53, s=19435 kN/s, n=1167922719, pv=Nd5 Kg5 Kf8 Ra3 Kg7 Rxa6 Ne7 Be4 Re6 f3 Re5 Kg4 Rc5 h5 Ng8 h6 Nxh6 Kf4 b4 Bxc6 Ng8 Rb6 Ne7 Be4 Nd5, tb=0, R50=48, wv=2.62,  }
57.h5 { ev=1.14, d=25, pd=Kf8, mt=00:00:55, tl=00:29:22, s=25280 kN/s, n=1402815659, pv=h5 Kf8 Kg5 Kg7 Rd3 a5 Rd4 a4 f4 b4 Be4 Rd6 Bg6 Re6 h6 Kh8 Bf7 Re2 Bxd5 cxd5 Rxb4 Rg2 Kf6 Kh7 f5, tb=0, R50=50, wv=1.14,  }
Kf7 { ev=-2.12, d=36, pd=Kg5, mt=00:00:39, tl=00:18:44, s=16797 kN/s, n=667163528, pv=Kf7 Kg5 Kg7 Ra3 Ne7 Bg4 Ng8 f4 Nh6 Bc8 Nf7 Kg4 Rd6 Rxa6 Kf6 Ra7 Nh6 Kf3 Nf5 Rc7 Rd3 Ke2 Rd6 Bd7 b4, tb=0, R50=49, wv=2.12,  }
58.Kg5 { ev=1.21, d=26, pd=Kg7, mt=00:00:34, tl=00:29:18, s=26224 kN/s, n=918450737, pv=Kg5 Kg7 Rd3 a5 f4 a4 Rd4 Rf8 Bg6 Kh8 h6 b4 f5 Ra8 f6 Nxf6 Kxf6 a3 Rd1 a2 Ra1 b3 Be4 Re8 Bf5, tb=0, R50=49, wv=1.21,  }
Kg7 { ev=-2.36, d=39, pd=Rd3, mt=00:02:18, tl=00:16:56, s=20001 kN/s, n=2778051940, pv=Kg7 Rd3 a5 f4 Rf8 Rd4 a4 h6 Kh8 Be6 Kh7 Re4 Nf6 Re5 b4 f5 b3 Ra5 Ne4 Kf4 Nf6 Rxa4 Kxh6 Ra6 Rd8, tb=0, R50=48, wv=2.36,  }
59.Rd3 { ev=1.46, d=27, pd=a5, mt=00:01:44, tl=00:28:04, s=26189 kN/s, n=2747152311, pv=Rd3 a5, tb=0, R50=48, wv=1.46,  }
a5 { ev=-2.44, d=39, pd=f4, mt=00:00:46, tl=00:16:40, s=20111 kN/s, n=936862769, pv=a5 f4 Rf8 Rd4 a4 Bd7 Kf7 Bxc6 Rg8 Kh4 Ne3 Bd7 Rg1 Rb4 Rh1 Kg3 Rxh5 Bxb5 Kf6 Bxa4 Nf5 Kg2 Nd6 Bd1 Rc5, tb=0, R50=50, wv=2.44,  }
60.f4 { ev=1.50, d=25, pd=a4, mt=00:00:38, tl=00:27:56, s=25525 kN/s, n=1004264929, pv=f4 a4 Rd4 Rf8 Bg6 Kg8 Re4 b4 f5 Kh8 Rc4 Ra8 Rxc6 a3 bxa3 bxa3 Bf7 Ne7 Rh6 Kg7 Ba2 Nxf5 Rg6 Kh7 Kxf5, tb=0, R50=50, wv=1.50,  }
a4 { ev=-2.56, d=39, pd=Rd4, mt=00:01:02, tl=00:16:08, s=19746 kN/s, n=1236202016, pv=a4 Rd4 Rf8 Bd7 Kh8 Bxc6 Rg8 Kh4 Ne3 Kh3 Rb8 Re4 Nc4 Re2 Nd6 Kg4 Rg8 Kf3 Rc8 Bd7 Rc7 Be6 Kg7 Re5 b4, tb=0, R50=50, wv=2.56,  }
61.Rd4 { ev=1.60, d=26, pd=b4, mt=00:00:45, tl=00:27:40, s=25749 kN/s, n=1193840721, pv=Rd4 b4 Be4 Rxf4 h6 Kh8 Rxb4 Rf8 Rxa4 Rg8 Kf5 Re8 Ra3 Kh7 Ra6 Kh8 Ra7 Ne3 Kf4 Nd5 Kf3 Rf8 Kg3 Ne3 Bg6, tb=0, R50=49, wv=1.60,  }
b4 { ev=-3.05, d=39, pd=Be4, mt=00:03:55, tl=00:12:43, s=21422 kN/s, n=5038948625, pv=b4 Be4 Rxf4 h6 Kh8 Rxb4 Rf8 Rxa4 Rg8 Kh4 Ne3 Bb1 c5 Kh5 Nd5 Bd3 Rd8 Bg6 Rg8 Bb1 Nf6 Kh4 Rb8 Ra2 Kg8, tb=0, R50=50, wv=3.05,  }
62.Be4 { ev=1.60, d=28, pd=Rxf4, mt=00:00:53, tl=00:27:18, s=25164 kN/s, n=1345369401, pv=Be4 Rxf4 h6 Kh8 Rxb4 Rf8 Rxa4 Rg8 Kf5 Re8 Ra3 Kh7 Ra6 Kh8 Ra7 Ne3 Kf4 Nd5 Kf3 Rf8 Ke2 Nf6 Bxc6 Ng4 Ra8, tb=0, R50=49, wv=1.60,  }
Rxf4 { ev=-3.23, d=38, pd=h6, mt=00:00:40, tl=00:12:33, s=19599 kN/s, n=788859784, pv=Rxf4 h6 Kh8 Rxb4 Rf8 Rxa4 Rg8 Kf5 Re8 Ra7 Ne3 Kf4 Nd5 Kf3 Rd8 Bg6 c5 Rh7 Kg8 Rb7 Kh8 Ke4 c4 Ke5 Ne3, tb=0, R50=50, wv=3.23,  }
63.h6+ { ev=1.68, d=26, pd=Kh8, mt=00:00:24, tl=00:27:24, s=27153 kN/s, n=684938612, pv=h6 Kh8 Rxb4 Rf8 Rxa4 Rg8 Kf5 Re8 Ra7 Ne3 Kf4 Nd5 Kf3 Rd8 Bg6 c5 Rh7 Kg8 Rg7 Kh8 Ke4 c4 Rh7 Kg8 Rb7, tb=0, R50=50, wv=1.68,  }
Kh8 { ev=-3.39, d=39, pd=Rxb4, mt=00:00:52, tl=00:12:12, s=18897 kN/s, n=991435870, pv=Kh8 Rxb4 Rf8 Rxa4 Rg8 Kf5 Re8 Ra3 Kh7 Ra6 Kh8 Bb1 Ne7 Kg5 Rg8 Kh5 Re8 Ra7 Nd5 Bf5 Ne7 Bd3 Nd5 Kg5 Rf8, tb=0, R50=49, wv=3.39,  }
64.Rxb4 { ev=1.66, d=26, pd=Rf8, mt=00:00:27, tl=00:27:27, s=28204 kN/s, n=784938013, pv=Rxb4 Rf8 Rxa4 Rg8 Kf5 Re8 Ra7 Ne3 Kf4 Nd5 Kf3 Rf8 Kg3 Ne3 Bg6 Nc4 Rh7 Kg8 Rb7 c5 Kh4 Rd8 Kg5 Rd5 Kf4, tb=0, R50=50, wv=1.66,  }
Rf8 { ev=-3.39, d=41, pd=Rxa4, mt=00:00:34, tl=00:12:07, s=16725 kN/s, n=582664321, pv=Rf8 Rxa4 Rg8 Kf5 Re8 Ra3 Kh7 Ra6 Kh8 Bb1 Ne7 Kg5 Rg8 Kh5 Rb8 Bc2 Rb5 Kg4 Rb4 Kg3 Rb8 Ra7 Nd5 Kh4 Ne3, tb=0, R50=49, wv=3.39,  }
65.Rxa4 { ev=1.66, d=24, pd=Rg8, mt=00:00:19, tl=00:27:38, s=27502 kN/s, n=550464666, pv=Rxa4 Rg8 Kf5 Re8 Ra7 Ne3 Kf4 Nd5 Kf3 Rf8 Kg3 Ne3 Bg6 Nc4 Rh7 Kg8 Rb7 c5 Kh4 Ne5 Bh7 Kh8 Kh5 c4 Be4, tb=0, R50=50, wv=1.66,  }
Rg8+ { ev=-4.62, d=42, pd=Kf5, mt=00:04:28, tl=00:08:10, s=18405 kN/s, n=5006201437, pv=Rg8 Kf5 Nc7 Ke5 Rb8 Kd6 Ne8 Kxc6 Rd8 b4 Rd4 Bf5 Rd6 Kc5 Rf6 Be4 Nc7 b5 Ne6 Kd6 Rxh6 Ra6 Ng5 Ke5 Nf7, tb=0, R50=49, wv=4.62,  }
66.Kf5 { ev=1.66, d=24, pd=Re8, mt=00:00:18, tl=00:27:50, s=27831 kN/s, n=521453887, pv=Kf5 Re8 Ra7 Ne3 Kf4 Nd5 Kf3 Rd8 Rh7 Kg8 Rg7 Kh8 Rf7 Kg8 Bg6 c5 Ke4 c4 Rb7 Kh8 Ke5 Ne3 Rh7 Kg8 Rg7, tb=0, R50=49, wv=1.66,  }
Nb6 { ev=-4.24, d=39, pd=Ra7, mt=00:00:46, tl=00:07:54, s=13286 kN/s, n=620770403, pv=Nb6 Ra7 Rd8 Kg6 Nd5 Bf5 Nf4 Kf6 Nd5 Kg5 Rg8 Bg6 Rd8 Rh7 Kg8 Bb1 Ra8 Rg7 Kh8 Rd7 Rb8 Be4 Ra8 Rb7 Rg8, tb=0, R50=48, wv=4.24,  }
67.Ra7 { ev=1.82, d=25, pd=Rf8, mt=00:00:56, tl=00:27:25, s=27386 kN/s, n=1540626068, pv=Ra7 Rf8 Kg6 Nd5 Kg5 Rg8 Bg6 Rb8 Rh7 Kg8 Rg7 Kh8 Ra7 Ne3 Rh7 Kg8 Rg7 Kh8 Be4 Nd5 Rf7 Kg8 Bg6 Ne3 Rf6, tb=0, R50=48, wv=1.82,  }
Rd8 { ev=-5.49, d=39, pd=Kg6, mt=00:00:27, tl=00:07:57, s=14280 kN/s, n=386981247, pv=Rd8 Kg6 Nd5 Bf5 Nf4 Kg5 Ne2 b4 Nd4 Be4 Re8 Bb1 Rg8 Kf6 Rd8 Be4 Ne2 Rh7 Kg8 Rb7 Rf8 Kg5 Nd4 Rg7 Kh8, tb=0, R50=47, wv=5.49,  }
68.Kg6 { ev=2.37, d=26, pd=Nd5, mt=00:01:07, tl=00:26:48, s=25914 kN/s, n=1754924098, pv=Kg6 Nd5, tb=0, R50=47, wv=2.37,  }
Nd5 { ev=-6.26, d=41, pd=Bf5, mt=00:00:57, tl=00:07:30, s=13711 kN/s, n=784625644, pv=Nd5 Bf5 Nf4 Kg5 Nd5 Rh7 Kg8 Rg7 Kh8 Bd7 c5 Bf5 Ne3 Be6 Rd1 Re7 Rg1 Kf4 Nc2 Re8 Kh7 Bf5 Kxh6 Bxc2 Rf1, tb=0, R50=46, wv=6.26,  }
69.Bf5 { ev=2.38, d=27, pd=Rd6, mt=00:01:26, tl=00:25:51, s=21903 kN/s, n=1908353467, pv=Bf5 Rd6 Kg5 Rd8 Rh7 Kg8 Rd7 Rf8 b4 Kh8 b5 Nf6 Rd6 cxb5 Rxf6 Rxf6 Kxf6 b4 Kg5 b3 Kg6 Kg8 Kf6 b2 Bb1, tb=0, R50=46, wv=2.38,  }
Nf4+ { ev=-6.62, d=42, pd=Kg5, mt=00:00:35, tl=00:07:26, s=11504 kN/s, n=403827329, pv=Nf4 Kg5 Nd5 Rh7 Kg8 Rg7 Kh8 Bd7 c5 Bf5 Ne3 Be6 Rd1 Re7 Rg1 Kf4 Nc2 Re8 Kh7 Bf5 Kxh6 Bxc2 Rf1 Kg3 Kg7, tb=0, R50=45, wv=6.62,  }
70.Kg5 { ev=2.32, d=25, mt=00:00:31, tl=00:25:50, s=32345 kN/s, n=1027845318, pv=Kg5, tb=0, R50=45, wv=2.32,  }
Ne2 { ev=-6.82, d=44, pd=b4, mt=00:00:43, tl=00:07:13, s=12458 kN/s, n=537381739, pv=Ne2 b4 Nd4 Be4 Re8 Bg6 Rf8 Rh7 Kg8 Re7 Nf3 Kg4 Nd4 Bh7 Kh8 Be4 Kg8 Kg5 Rd8 Kf6 Rd6 Ke5 Rd8 Kf6, tb=0, R50=44, wv=6.82,  }
71.Bb1 { ev=2.80, d=25, pd=Nd4, mt=00:00:53, tl=00:25:27, s=30061 kN/s, n=1625883663, pv=Bb1 Nd4 Kg6 Rd6 Kf7 Rd8 Rc7 Rb8 b4 Rd8 Kf6 Kg8 Rg7 Kh8 Ke7 Rd5 Rf7 Re5 Kd8 Rd5 Rd7 Ne6 Ke7 Nf4 Rc7, tb=0, R50=44, wv=2.80,  }
Rg8+ { ev=-6.82, d=43, pd=Bg6, mt=00:00:59, tl=00:06:45, s=11718 kN/s, n=694850138, pv=Rg8 Bg6 Rd8 Rh7 Kg8 Re7 Nd4 b4 Kf8 Rf7 Kg8 Rg7 Kh8 Re7 Nf3 Kg4 Nd4 Be4 Rf8 Kg5 Rd8 Kg6 Kg8 Rg7 Kf8, tb=0, R50=43, wv=6.82,  }
72.Kf6 { ev=3.29, d=23, pd=Rf8, mt=00:00:20, tl=00:25:38, s=31085 kN/s, n=636227816, pv=Kf6 Rf8 Rf7 Rxf7 Kxf7 Nf4 Kf6 Nd5 Ke5 Nb6 Bd3 Na4 b4 Nc3 Kd6 Nd5 Kc5 Nc3 Bf5 Na2 Bg6 Nxb4 Kxb4 c5 Kc4, tb=0, R50=43, wv=3.29,  }
Rb8 { ev=-6.82, d=42, pd=Rh7, mt=00:00:28, tl=00:06:47, s=10625 kN/s, n=305491585, pv=Rb8 Rh7 Kg8 Rc7 Rf8 Kg5 Rd8 Rg7 Kh8 Rh7 Kg8 Re7 Nd4 b4 Nf3 Kg6 Rd6 Kh5 Rd8 Be4 Nd4 Kg5 Rf8 Kg6 Rd8, tb=0, R50=42, wv=6.82,  }
73.Rh7+ { ev=3.46, d=25, pd=Kg8, mt=00:00:42, tl=00:25:26, s=29635 kN/s, n=1263035475, pv=Rh7 Kg8, tb=0, R50=42, wv=3.46,  }
Kg8 { ev=-6.82, d=12, pd=Rc7, mt=00:00:00, tl=00:07:17, s=948 kN/s, n=948, pv=Kg8 Rc7 Rf8 Kg5 Rd8 Rg7 Kh8 Rh7 Kg8 Re7 Nd4 b4 Nf3 Kg6 Rd6 Kh5 Rd8 Be4 Nd4 Kg5 Rf8 Kg6 Rd8 Rg7 Kh8, tb=0, R50=41, wv=6.82,  }
74.Rc7 { ev=3.92, d=24, pd=Rf8, mt=00:00:20, tl=00:25:37, s=30953 kN/s, n=636424900, pv=Rc7 Rf8 Kg5 Rd8 Rxc6 Nd4 Rf6 Kh8 b4 Rg8 Kh5 Rd8 Bd3 Nb3 Kg6 Rg8 Kf7 Nc1 Rg6 Rd8 Bf5 Nd3 Ke7 Rd5 Bxd3, tb=0, R50=41, wv=3.92,  }
Rf8+ { ev=-8.54, d=42, pd=Kg5, mt=00:01:30, tl=00:06:17, s=14704 kN/s, n=1328868368, pv=Rf8 Kg5 Rd8 Rxc6 Nd4 Rg6 Kh8 Rb6 Kg8 Bg6 Ne2 Re6 Nd4 Re8 Rxe8 Bxe8 Ne6 Kf6 Nd4 Bg6 Nc6 Be4 Nb4 Ke6 Na6, tb=0, R50=40, wv=8.54,  }
75.Kg5 { ev=4.08, d=24, pd=Rd8, mt=00:00:15, tl=00:25:52, s=31568 kN/s, n=481137862, pv=Kg5 Rd8 Rxc6 Nd4 Rf6 Ne2 b4 Kh8 Bf5 Ng3 Bg6 Kg8 Re6 Rb8 Re5 Nf1 Re8 Rxe8 Bxe8 Ne3 b5 Nd5 Bd7 Nb6 Bc6, tb=0, R50=40, wv=4.08,  }
Rd8 { ev=-10.62, d=43, pd=Rxc6, mt=00:01:28, tl=00:05:19, s=14609 kN/s, n=1285922662, pv=Rd8 Rxc6 Nd4 Rg6 Kh8 Rb6 Rc8 Kf6 Kg8 Bg6 Rf8 Kg5 Rd8 b4 Kh8 Kf6 Ra8 Rd6 Nb5 Rd7 Rf8 Kg5 Na3 Be4 Nc4, tb=0, R50=39, wv=10.62,  }
76.Rxc6 { ev=4.08, d=29, pd=Nd4, mt=00:00:27, tl=00:25:55, s=30546 kN/s, n=864429078, pv=Rxc6 Nd4 Rf6 Rb8 Kg6 Kh8 b4 Rg8 Kh5 Rd8 Bd3 Nb3 Kg6 Rg8 Kf7 Nd2 b5 Rd8 Kg6 Rg8 Kh5 Rd8 Bf5 Nc4 b6, tb=0, R50=50, wv=4.08,  }
Nd4 { ev=-14.48, d=43, pd=Rg6, mt=00:02:16, tl=00:03:34, s=13591 kN/s, n=1847437481, pv=Nd4 Rg6 Kh8 Rb6 Ra8 Bg6 Rf8 b4 Nf3 Kh5 Rd8 Re6 Nd4 Re8 Rxe8 Bxe8 Ne6 Bg6 Nc7 Bf7 Nb5 Kg5 Nd6 Bg6 Nb5, tb=0, R50=49, wv=14.48,  }
77.Rg6+ { ev=4.84, d=32, pd=Kh8, mt=00:02:41, tl=00:23:44, s=31007 kN/s, n=5030257930, pv=Rg6 Kh8 Rb6 Rf8 b4 Nf3 Kg6 Nd2 Ba2 Nf3 b5 Ne5 Kg5 Nf3 Kh5 Rf5 Kg4 Rf8 Bb1 Nd2 Bf5 Nf1 Bg6 Ne3 Kg5, tb=0, R50=49, wv=4.84,  }
Kh8 { ev=-14.48, d=45, pd=Rb6, mt=00:00:10, tl=00:03:55, s=9592 kN/s, n=102359746, pv=Kh8 Rb6 Ra8 Bg6 Rf8 b4 Nf3 Kh5 Rd8 Re6 Nd4 Re8 Rxe8 Bxe8 Ne6 Bg6 Nc7 Bf7 Nb5 Kg5 Nd6 Bg6 Nb5 Kf5 Nd4, tb=0, R50=48, wv=14.48,  }
78.Rb6 { ev=5.10, d=29, pd=Nf3, mt=00:00:32, tl=00:23:42, s=31601 kN/s, n=1036245727, pv=Rb6 Nf3 Kf6 Nd2 Ke7 Ra8 Bd3 Rc8 b4 Nc4 Rb7 Ne3 b5 Nd5 Kf7 Rg8 Bg6 Ne3 Kf6 Rf8 Ke7 Rg8 Be4 Nc4 Kf6, tb=0, R50=48, wv=5.10,  }
Ra8 { ev=-18.42, d=46, pd=Rd6, mt=00:01:43, tl=00:02:42, s=12109 kN/s, n=1252881054, pv=Ra8 Rd6 Rg8 Bg6 Nb3 Rd7 Ra8 Bf5 Rg8 Kf6 Ra8 Rh7 Kg8 Rg7 Kh8 Rd7 Rb8 Ke7 Nc5 Rd8 Rxd8 Kxd8 Na4 b4 Nc3, tb=0, R50=47, wv=18.42,  }
79.b4 { ev=6.10, d=28, pd=Nf3, mt=00:00:42, tl=00:23:30, s=32164 kN/s, n=1385855432, pv=b4 Nf3 Kf6 Nd2 Bg6 Rg8 b5 Nc4 Rb7 Rd8 Ke7 Rg8 Bf5 Ne3 Be4 Rc8 Rd7 Nc4 Rd8 Rxd8 Kxd8 Na5 b6 Kg8 Ke7, tb=0, R50=50, wv=6.10,  }
Kg8 { ev=-18.52, d=33, pd=Rd6, mt=00:00:09, tl=00:03:03, s=6124 kN/s, n=56763876, pv=Kg8 Rd6 Nf3 Kh5 Re8 Rd7 Rb8 Bf5 Ne5 Rc7 Nf3 Be4 Nd4 Rg7 Kh8 Rh7 Kg8 Rd7 Ne2 Kg5 Nc3 Bh7 Kh8 Bf5 Nb5, tb=0, R50=49, wv=18.52,  }
80.Rb7 { ev=7.40, d=24, pd=Nf3, mt=00:00:24, tl=00:23:37, s=31904 kN/s, n=786378088, pv=Rb7 Nf3 Kh5 Nd4 b5 Ne6 Bf5 Nd4 Be4 Rd8 b6 Ne6 Re7 Nc5 Bf5 Kh8 Rh7 Kg8 Rg7 Kh8 b7 Nxb7 Rxb7 Rf8 Kg5, tb=0, R50=49, wv=7.40,  }
Ne6+ { ev=-76.96, d=35, pd=Kf6, mt=00:00:25, tl=00:03:08, s=7017 kN/s, n=181522471, pv=Ne6 Kf6 Nd4 b5 Rd8 Ba2 Kh8 Kg5 Nf3 Kh5 Re8 Bb1 Nd4 b6 Re5 Kg4 Re8 Bg6 Rf8 Rh7 Kg8 Rc7 Nb5 Rg7 Kh8, tb=0, R50=48, wv=76.96,  }
81.Kf6 { ev=9.23, d=24, pd=Nd8, mt=00:00:20, tl=00:23:47, s=34878 kN/s, n=729105782, pv=Kf6 Nd8 Rb6 Ra1 Bf5 Ra8 Ke7 Kh8 Bg6 Kg8 Be4 Rc8 Rd6 Nb7 Bxb7 Rc7 Rd7 Rxd7 Kxd7 Kh7 Be4 Kxh6 Ke7 Kg5 b5, tb=0, R50=48, wv=9.23,  }
Nf8 { ev=-133.68, d=38, pd=b5, mt=00:01:55, tl=00:01:42, s=10792 kN/s, n=3001098897, pv=Nf8 b5 Rd8 Ba2 Kh8 b6 Rd6 Ke7 Rxh6 Kxf8 Rc6 Rb8 Kh7 Ke7 Kg6 Be6 Kg5 b7 Rb6 Rg8 Kf4 b8=Q Rxb8 Rxb8 Ke3, tb=0, R50=47, wv=133.68,  }
82.b5 { ev=#21, d=24, pd=Rd8, mt=00:00:52, tl=00:23:25, s=30626 kN/s, n=1604808840, pv=b5 Rd8 Rg7 Kh8 b6 Nd7 Kg6 Ne5 Kh5 Nc6 Rc7 Na5 b7 Rd5 Kg6 Rd6 Kg5 Rd8 Bg6 Nxb7 Rxb7 Rd5 Bf5 Rd8 Rh7, tb=0, R50=50, wv=#21,  }
Rd8 { ev=-135.90, d=37, pd=Ba2, mt=00:01:13, tl=00:00:59, s=7185 kN/s, n=2277060548, pv=Rd8 Ba2 Kh8 b6 Rd2 Rb8 Rf2 Ke7 Re2 Kxf8 Rxa2 Rc8 Rb2 Ke7 Kh7 Rc6 Kg8 Kd6 Kf7 Kc7 Re2 h7 Re7 Kb8 Kg7, tb=0, R50=49, wv=135.90,  }
83.Rg7+ { ev=#20, d=22, pd=Kh8, mt=00:00:12, tl=00:23:43, s=22775 kN/s, n=287789565, pv=Rg7 Kh8 b6 Nd7 Kg6 Ne5 Kh5 Nc6 Rh7 Kg8 Rc7 Rd5 Kh4 Rd1 Bh7 Kf8 Be4 Ne7 h7 Kf7 h8=Q Ke6 Rxe7 Kxe7 b7, tb=0, R50=49, wv=#20,  }
1-0
; { Game Nr. 117 : Komodo 1092 wins against Stockfish 160913 by GUI adjudication
; GameDuration = 04:54:28, Finalposition 3r1nk1/6R1/5K1P/1P6/8/8/8/1B6 b - - 2 83 }
"""

val invalidVariant = """[Event "nTCEC - Stage 2 - Season 2"]
[Site "http://www.tcec-chess.net"]
[Date "2013.09.24"]
[Round "5.2"]
[White "Stockfish 160913"]
[Black "Spike 1.4"]
[Result "1-0"]
[Variant "starwars"]

1. d4 Nf6 2. c4 e6 3. Nc3 Bb4 4. Qc2 Nc6 5. e3 O-O 6. f4 Bxc3+ 7. bxc3 d6 8. Nf3
e5 9. fxe5 dxe5 10. Be2 Re8 11. O-O Qe7 12. Rb1 b6 13. Rb5 a6 14. Rb2 h6
15. Nh4 Qd6 16. Qd1 Rb8 17. Qd3 b5 18. cxb5 axb5 19. Nf5 Bxf5 20. Qxf5 b4
21. cxb4 Nxb4 22. a3 Nc6 23. dxe5 Nxe5 24. Rd1 Qc5 25. Qc2 Qa7 26. Rxb8 Qxb8
27. h3 c6 28. a4 Qa7 29. Rd4 Nd5 30. Bd2 Rd8 31. a5 c5 32. Re4 Nc6 33. a6 Qb6
34. Be1 Kf8 35. Qa2 Qc7 36. Qa4 Kg8 37. a7 Ra8 38. Bg3 Qc8 39. Bb8 Rxb8
40. axb8=R Qxb8 41. Qb5 Nf6 42. Ra4 Qxb5 43. Bxb5 Ne5 44. Ra8+ Kh7 45. Rc8 Kg6
46. Rxc5 Kf5 47. Ba4 Ke6 48. Bb3+ Kd6 49. Ra5 Nfd7 50. Rd5+ Ke6 51. Kf2 Kf6
52. Rd6+ Ke7 53. Rd4 Nc5 54. Bd5 h5 55. g3 Ne6 56. Ra4 Nc5 57. Ra7+ Kf6 58. Ke2
g6 59. Ra5 Ncd7 60. Kd2 Nf8 61. Ra6+ Ne6 62. h4 Ng4 63. Bxe6 fxe6 64. Kd3 Ne5+
65. Ke4 Ng4 66. Kd4 Ne5 67. Ra8 Nd7 68. Rc8 Ke7 69. e4 Nf6 70. Rc7+ Kd6 71. Rg7
Ng4 1-0"""

val chessbaseWeird = """[Event "Altibox Norway Chess 2017"]
[White "Aronian, Levon"]
[Black "Caruana, Fabiano"]
[Site "Stavanger"]
[Round "1"]
[Annotator "TA"]
[Result "1/2-1/2"]
[Date "2017.06.06"]
[WhiteElo "2793"]
[BlackElo "2805"]
[PlyCount "115"]

1. d4 {} d5 2. c4 dxc4 3. e3 Nf6 4. Bxc4 e6 5. Nf3 c5 6. 0-0 a6 7. b3 Nbd7 8. Bb2 b5 9. Be2 Bb7 10. a4 b4 11. Nbd2 Be7 12. Rc1 {D27: Queen's Gambit Accepted: Classical main line: 7 e4!?
and 7 a4} 0-0 13. dxc5 {LiveBook: 4 Games} Bxc5 14. Ne5 {White is slightly
better.} Rc8 15. Bf3 $146 ({Predecessor:} 15. Nxd7 Nxd7 16. Bf3 Bxf3 17. Qxf3 Be7 18. Rxc8 Qxc8 19. Rc1 Qd8 20. Ne4 f5 21. Qg3 e5 22. Nd2 {1/2-1/2 (22)
Akesson,R (2465)-Van Wely,L (2585) Antwerp 1995} )Nd5 16. Nxd7 Qxd7 17. Nc4 Qe7 18. Qe2 Ba7 19. g3 Bb8 20. Rfd1 Rfd8 21. Bg2 Bc7 22. Qg4 {
[#]} f6 23. h4 {} a5 24. Rd2 Ba6 25. Rdc2 Rb8 26. h5 Rd7 27. Qh4 Rf8 28. Bh3 Bb7 29. Bd4 (29. Qg4 $5 {} f5 30. Qe2 $11 )Qf7 30. Rd2 (30. h6 $5 {} )h6 31. Qg4 Re8 32. Qg6 Red8 33. Bg2 Ba6 34. Be4 {[#] aiming for Qh7+.} Qxg6 35. hxg6 Ne7 36. Bc5 f5 (36... Nd5 $1 $11 )37. Bxe7 $16 Rxe7 38. Bf3 ({Better is} 38. Rxd8+ $14 Bxd8 39. Bc6 )Rxd2 $11 39. Nxd2 Kf8 40. Rc5 Rd7 41. Nc4 Bxc4 $1 42. Rxc4 { Endgame KRB-KRB} Ke7 43. Rc6 Bd8 44. Be2 Rd6 45. Rxd6 (45. Rc8 {is interesting.} Kf6 46. Bc4 Bb6 47. Re8 Rc6 48. Kf1 )Kxd6 {KB-KB} 46. Bc4 e5 47. f4 Bb6 48. Kf2 exf4 49. gxf4 h5 50. Kf3 h4 51. Bf1 (51. Bd3 $5 {
} )Ke6 52. e4 fxe4+ 53. Kxe4 Kf6 54. f5 Bd8 55. Bh3 Bb6 56. Bf1 Bd8 57. Bh3 Bb6 58. Bf1 {Precision: White = 68%, Black = 58%.} 1/2-1/2"""

val chessbaseArrows = """[Event "?"]
[Site "?"]
[Date "2016.05.23"]
[Round "?"]
[White "Pgn pour thibault fleches"]
[Black "?"]
[Result "*"]
[ECO "A00"]
[PlyCount "0"]
[SourceDate "2016.05.23"]

{[%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]} 1. e4 {[%csl Gb4,Yd5,Rf6] blabla}
"""

val commentsAndVariations = """
[Event "ICC"]
[Site "Internet Chess Club"]
[Date "2013.09.29"]
[Round "?"]
[White "Pedro"]
[Black "burza"]
[Result "1-0"]
[ECO "B12"]
[WhiteElo "1536"]
[BlackElo "1467"]
[Annotator "Pedro"]
[PlyCount "103"]
[EventDate "2013.??.??"]
[SourceDate "2009.04.28"]
[TimeControl "1800"]

1. e4 d6 2. d4 c6 {I'm out of book right now.} 3. f4 {Black's queenside pawns
seemed a bit weird and not secure to long-castle; therefore I was expecting
short castle for black. Given the fact that black does not have serious piece
development at the moment I decided to go 3.f4 right away. Besides controling
more central squares, it also aims to support e5 advance which might kick out
a knight on natural square f6, making the king more vulnerable. Also it
provides Qe1-Qe3 maneuver which can be good for attack.} Qc7 4. Nf3 h6 $6 {
Losing development time with a profilactic move. The g5 square is not a
serious threat at the moment, specially at this early opening stage with only
one piece developed. Also, h6 might a target for attacking sacrifice.} 5. Bd3 (
{I've considered also} 5. Bc4 {but I was discouraged by} e6) 5... Bg4 6. O-O ; Openning route to ocupying b6 weak square by Na4-Nb6. This square seemed more important than f5 (Ne2-Ng3-Nf5) because its nearer the black's king.
Nd7 {Now I was getting worried the long-castle was comming anyway.
Nevertheless, I have better centre control and prospects of attacking kingside
anyway, specially if I manage to get a open f-file.} 7. Nc3 a6 $6 {I do not
understand why such caution on b5.} 8. Be3 e5 $2 {Breaking the centre too soon,
he must finish his development first, his king is still in the centre} 9. fxe5
dxe5 10. d5 $1 ({Perhaps black was expecting} 10. dxe5 $2 Nxe5 {freeing his
position}) 10... c5 (10... cxd5 $6 11. Nxd5 {occupying central weak square}) (
10... Ngf6 11. dxc6 {eternalizing central weak square or damaging the pawn
structure.}) 11. a4 {Preventing b5, which could cramp my queenside with
further b4 move.} Be7 {Now short-castle seems reasonable} 12. Qe1 g5 $2 {No
development, weakening f5.} 13. Qg3 f6 $4 {Too much worry overprotecting
what's already protected. It's impressive the pressure simple piece play makes,
making him blunder a piece.} 14. Qxg4 Qd6 15. Qh5+ {Stopping his castle.} Kd8
16. Qf7 {Threatning Qg8 taking the rook.} Bf8 17. a5 {Openning route to
ocupying b6 weak square by Na4-Nb6. This square seemed more important than f5
(Ne2-Ng3-Nf5) because its nearer the black's king.} h5 18. Na4 (18. Nxg5 $5 {
Should have been fun, but I didn't want to risk it because I didn't want to
get low on the clock. I think if it was a 45 45 game for example I would
calculate it further.} fxg5 19. Bxg5+ Ne7 (19... Kc8 $2 20. Qe8+ Kc7 21. Qxa8)
(19... Be7 20. Qg7) 20. Be2 $18) 18... Rb8 (18... Nh6 19. Qxh5) 19. Nb6 $1 {
removing the defender of Bf8} Nxb6 20. axb6 h4 {Black is just cramped, with no
moves.} (20... Qe7 {Impressive how white's queen paralize all black's kingside
pieces, an exchange of queens was called to diffuse that.} 21. Qxe7+ Bxe7) 21.
Nxg5 fxg5 22. Qxf8+ Kd7 {only move} (22... Qxf8 $4 23. Rxf8+ Kd7 24. Rxb8 $18)
23. Qxd6+ (23. Rf7+ {does not work due to} Ne7) 23... Kxd6 24. Rf7 Nh6 25. Rf6+
Ke7 26. Bxg5 Ng4 27. Rf4+ Kd6 28. Rxg4 Rhg8 29. Rf1 Rbf8 30. Rxf8 Rxf8 31. Bxh4
Rf7 32. Rg6+ Kd7 33. Bf2 Rxf2 34. Kxf2 a5 35. Bb5+ Ke7 36. Re6+ Kf7 37. Rxe5
Kf6 38. Rf5+ {Imprecise} ({better was} 38. Re6+ Kf7 39. Bc4 Kg7 40. d6) 38...
Ke7 39. h4 c4 40. Bxc4 Kd6 41. h5 Kc5 42. b3 a4 43. bxa4 Kxc4 44. h6 Kb4 45. h7
Kxa4 46. h8=Q Kb5 47. d6+ Kxb6 48. d7 Kc6 49. d8=Q b5 50. Qd5+ Kb6 51. Qxb5+
Kc7 52. Qhb8# 1-0
"""

val bySmartChess = """
[Event "?"]
[Site "Munich"]
[Date "1979.??.??"]
[Round "?"]
[White "Andersson, U"]
[Black "Robatsch"]
[Result "1-0"]
[Annotator  "Krush, I]

{
http://www.smartchess.com/SmartChessOnline/default.htm
English Double Fianchetto (A30)
When two grandmasters play out moves from a symmetrical opening and begin methodically exchanging pieces, it is almost as if you can hear the collective groan from the audience as they realize a quick draw may soon result.
However, openings such as the one we are about to examine are deceptive in their simplicity. In the hands of a strong technical player such as GM Ulf Andersson of Sweden, the Double Fianchetto Exchange Variation can be slow death for a player of the Black pieces who lets his guard slip for even a moment.
}
1.c4 c5 2.Nf3 Nf6 3.g3 b6 4.Bg2 Bb7 5.0-0 g6 6.b3 Bg7 7.Bb2 0-0 8.Nc3
{
If White strikes first with the advance of his d-pawn, then Black is able to equalize with 8.d4 cxd4 9.Qxd4 Nc6 10.Qd2 d5 11.cxd5 Qxd5, for example 12.Nd4 Ne4! 13.Nxc6 Qxc6 14.Bxg7 Kxg7 15.Qe3 Qc5 16.Qxc5 Nxc5 17.Bxb7 Nxb7 18.Nc3 Rac8 19.Rac1 Rfd8, is completely equal, Barcza-Steiner, Budapest 1948. If we compare this situation to the one that occurs in the line after 8.Nc3 d5, we note that here Black has completed his development and is able to challenge White on the c- and d-files with his rooks.
}
8...d5
{
Black breaks the symmetry of moves and provokes the following flurry of exchanges. In my opinion this move is a little dubious and Black can instead equalize in the variation with 8...Na6!
}
9.Nxd5
{
The only correct way to proceed. If 9.cxd5?! Nxd5 10.Na4 Bxb2 11.Nxb2 Nc6, Black has at least equalized as the White knight on b2 is misplaced.
}
9...Nxd5 10.Bxg7 Kxg7 11.cxd5 Qxd5
{
The exchanging sequence is forcing, as 11...Bxd5 12.d4, is good for White. Note that White wins immediately after 12...cxd4? 13.Qxd4+ Kg8 14.Rfd1, pinning and winning Black's bishop.
}
12.d4 cxd4
{
Black's last opportunity to avoid the endgame that occurs in the game is 12...Na6, when it is probably easier for Black to demonstrate equality.
}
13.Qxd4+
{
Less effective is 13.Nh4 Qd7 14.Bxb7 Qxb7 15.Qxd4+ Kg8 16.Rfd1 Nc6 17.Qd7 (17.Qe4 Rfd8 18.Nf3 0.5-0.5 Sanguinetti-Panno, Buenos Aires 1977) 17...Qxd7 18.Rxd7 Rfd8 0.5-0.5 Tal-Salov, Brussels 1988. In both cases Black equalized.
}
13...Qxd4 14.Nxd4 Bxg2 15.Kxg2
{
An important position in the Double Fianchetto Exchange Variation which in practice has been a deceptively tricky defensive chore for Black.
White has a small nagging initiative for the endgame in that:
a) his knight is ACTIVE and CENTRALIZED, while Black's knight is still at home (indeed, it is not clear where Black should develop his knight);
b) White's rooks are already CONNECTED;
c) White has the maneuver Nd4-b5 in reserve to create pressure against Black's queenside pawns and interfere with Black's development.
}
15...a6
{
Black prevents the possibility of Nc3-b5 and prepares to develop his rook with Ra8-a7 to facilitate doubling of the rooks on the c- or d-file (if allowed). Alternatives include:
15...Nd7, and now:
A) 16.Rfd1 Nf6 17.Nb5! Rfc8 18.Rac1 Rxc1 (if 18...a6? 19.Nc7 Rab8 20.Nxa6 Rxc1 21.Rxc1 Ra8 22.Nb4, and White has won a pawn) 19.Rxc1 a6 20.Nd4 Rd8 21.e3 Nd5 22.Rc6 Rd6 23.Rxd6! exd6 24.Kf1, and White enjoys an advantage in the knight endgame thanks to his better pawn structure, Andersson-Marovic, Banja Luka 1976.
B) 16.Rac1 Rfc8 17.Rfd1, and now:
B1) 17...Nc5 18.b4! Ne4 (18...Na4 19.Nb5! Rxc1 20.Rxc1 a5 21.a3! with a clear advantage for White Smyslov-Benko, Monte Carlo 1968, while worse is 18...Nd7? 19.Ne6+ fxe6 20.Rxc8 Rxc8 21.Rxd7, with a winning endgame for White) 19.Nb5! Rxc1 20.Rxc1 a5 21.Rc4, with an edge for White, Lisitsyn-Levenfish, USSR Ch. 1948.
B2) 17...Kf6 18.Nb5 Rxc1 19.Rxc1 Nc5 20.b4, with an edge for White, Ribli-Sapi, Budapest 1976. These examples show how White's Nd4-b5 idea can make life awkward for Black.
An interesting try is 15...Na6!? 16.Rfd1 Rfc8 17.Rac1, and now:
A) 17...Kf6 18.Nb5 Rxc1 (18...Nc5? - losing a pawn - 19.b4 Ne6 20.Rxc8 Rxc8 21.Nxa7, with a big advantage for White, Smyslov-Castro Rojas, Biel 1976) 19.Rxc1 Nc5 20.b4 Ne6 21.Rc6, reaches a position that occurred in the game Portisch-Pachman, Sarajevo 1963, when White stood better.
B) 17...Nb4 18.Rxc8 (Possible is 18.a3, when White may have a slight pull) 18...Rxc8 19.Rd2 a6 0.5-0.5 Benko-Weinstein, Lone Pine 1975.
Also possible is 15...Rc8, when 16.Rac1 Nd7 17.Rfd1 transposes to variations examined after 15...Nd7.
}
16.Rac1
{
16.Rfc1 Ra7 17.Rc2 (17.e3 Rd7 18.Rc2 e5?! 19.Nf3 f6 20.g4! with advantage to White, Szabo-Bisguier, Gothenburg 1955) 17...Rd8 18.e3 Kf8 (18...e5 19.Nf3 f6 20.Rac1, is good for White) 19.Rac1 Ke8 20.g4! (this idea is thematic - gaining space on the kingside while Black is busy preventing White from penetrating on the queenside) 20...h6 21.h4 Rad7 22.f4 a5 23.Kf3 Rd6 24.h5! with a clear plus for White, Andersson-Hort, Niksic 1978.
}
16...Ra7 17.Rc2 Rd8 18.e3
{
White anchors his centralized knight.
}
18...e5?!
{
A mistake, as White will be able to capitalize on the resultant weakening of the Black pawn structure.
Better is 18...Kf8 19.Rfc1, transposing to the Andersson-Hort game in the previous note, although as we saw, White also enjoys more pleasant prospects in that instance.
}
19.Nf3 f6
{
Passively defending the e-pawn with 19...Re7 can be met with 20.Rfc1 Kf8 21.Rc8, and White stands better.
}
20.g4!
{
White gains space on the kingside and has a perceptible advantage.
}
20...Rd6 21.Rfc1 Nd7 22.Rc6 Rxc6 23.Rxc6
{
An exchange of rooks has exposed Black's tender spots along his third rank (b6, d6).
}
23...Kf7 24.Nd2
{
Andersson redirects his knight to the more lucrative outpost on e4.
}
24...Ke7
{
Keeping the knight out of e4 with 24...f5? allows White to post the knight efficiently elsewhere with 25.gxf5 gxf5 26.Nc4, and Black is losing a pawn, as 26...b5 allows 27.Nd6+ etc.
}
25.Ne4 Rb7 26.b4!
{
Effectively binding Black's position.
}
26...Rb8 27.Nc3
{
Andersson now directs his knight to the more powerful post on d5.
}
27...f5
{
If 27...Rb7 28.Nd5+ Kf7 then 29.e4! leaves Black without any useful move, for example 29...a5 (29...Rb8 30.Rc7, and White wins a pawn after 30...Ke6 31.Ra7 a5 32.bxa5 bxa5 33.Ra6+ Kf7 34.Rxa5, etc., while 29...Kg7 is met by 30.Re6! g5 31.Re7+ Kg6 32.Ne3, and the Black king is practically in a mating net) 30.b5, maintains the bind - Black is running out of moves.
}
28.Nd5+ Kf7 29.Kg3 h5?
{
White's king penetrates easily after this move by Black. But even after 29...fxg4 30.Kxg4, there is a grim defensive task for Black.
}
30.gxf5 gxf5 31.Rd6 Rb7 32.Kh4
{
Andersson introduces his king as the last attacking unit in his endgame attack. Black's kingside pawns have become easy targets.
}
32...Kg7 33.Kxh5
{
The continuation might be 33...a5 34.b5 Kf7 (34...Kh7? loses to 35.Rxd7+ Rxd7 36.Nf6+) 35.Kg5, and White wins another pawn.
What we learn from a study of a game such as this is that no matter how quiet or simplified a position may appear on the surface, active and accurate defense is paramount as any mistake can lead to a technically lost endgame (with almost certain fatal consequences).
}
1-0
"""

val android = """[Event "AI Factory's Chess"]
[Site "Android Device"]
[Date "2014.04.23"]
[Round "1"]
[White "You"]
[Black "Cpu (3)"]
[PlyCount "69"]
[Result "1-0"]

1. e2e4 e7e5 2. Qd1f3 Ng8f6 3. Bf1c4 Nb8c6 4. Nb1c3 h7h6 5. Qf3d3 d7d6
6. Ng1f3 a7a6 7. Nf3xe5 Rh8g8 8. Ne5xf7 Bc8d7 9. Nf7xd8 Rg8h8 10. Nd8e6 Bd7xe6
11. Bc4xe6 Bf8e7 12. Nc3d5 Ke8d8 13. Nd5xe7 Kd8xe7 14. Qd3c4 b7b5 15. Qc4xc6 Ke7xe6
16. d2d3 Ra8c8 17. Bc1f4 Ke6e7 18. e4e5 Nf6e8 19. Qc6xa6 Rh8f8 20. Bf4g3 Rc8b8
21. Qa6a7 Rb8d8 22. Bg3h4+ g7g5 23. Bh4g3 Rf8f7 24. Qa7a6 b5b4 25. h2h4 b4b3
26. a2xb3 Rf7g7 27. Qa6b5 Rd8d7 28. Ra1a8 Rg7f7 29. Qb5b4 Rf7f8 30. Ke1d1 Rf8f5
31. Rh1e1 Rd7d8 32. e5xd6+ Ke7d7 33. Qb4a4+ Rf5b5 34. Qa4xb5+ c7c6 35. Qb5f5# 1-0"""

val chesskids = """[Site "ChessKid iPhone"]
[Date "04/29/2014 02:27PM"]
[White "NicePlatypus"]
[Black "Computer"]

1. e4 e5 2. Nc3 Nc6 3. Nf3 Bc5 4. Bb5 d6 5. Bxc6+ bxc6 6. d4 exd4 7. Nxd4 Ne7 8. Be3 Bb4 9. a3 Bxc3+ 10. bxc3 c5 11. Nf3 f5 12. exf5 Bxf5
13. 0-0 0-0 14. Bg5 c6 15. Rb1 Be4 16. Rb7 Re8 17. Re1 d5 18. Ne5 Qd6 19. f4 Qe6 20. Bxe7 Rxe7 21. Rxe7 Qxe7 22. Nxc6 Qe6 23. Ne5 Rf8 24. g3
Rb8 25. c4 Rf8 26. cxd5 Qxd5 27. Qxd5+ Bxd5 28. Nd3 c4 29. Nb4 Bf7 30. c3 Re8 31. Rxe8+ Bxe8 32. Kf2 Kf7 33. Ke3 Bb5 34. Ke4 Bd7 35. Ke5 a5
36. Nd5 Bc6 37. Nb6 Bb5 38. h4 Ba6 39. g4 h5 40. gxh5 a4 41. Kf5 Bb5 42. Kg5 Ke7 43. h6 gxh6+ 44. Kxh6 Kd6 45. f5 Kc5 46. f6 Be8 47. Nxa4+
Kd5 48. Nb6+ Ke6 49. Kg7 Bh5 50. a4 Be8 51. a5 Bh5 52. a6 Ke5 53. f7 Bxf7 54. Kxf7 Ke4 55. a7 Kd3 56. a8Q Kxc3 57. Qa5+ Kd3 58. Qd5+ Ke2 59.
h5 c3 60. Qc4+ Kd2 61. Na4 c2 62. Qc3+ Ke2 63. Qxc2+ Ke1 64. Kf6 Kf1 65. Kf5 Ke1 66. Kf4 Kf1 67. Kf3 Ke1 68. Qe2# 1-0"""

val variations = """
1. e4 (1. d4 d5) d6 (1... d5 2. h3) 2. d4 {choudidou d4} (2. d3 h6 3. h3) c6 {yep c6!} 3. f4  Qc7 4. Nf3 h6 $6  5. Bd3 (  5. Bc4  e6) 5... Bg4 6. O-O Nd7  7. Nc3 a6 $6  8. Be3 e5 $2  9. fxe5 dxe5 10. d5 $1 ( 10. dxe5 $2 Nxe5 ) 10... c5 (10... cxd5 $6 11. Nxd5 ) ( 10... Ngf6 11. dxc6 )
"""

val caissa = """
[Event "Anime Boston Cosplay Chess 2013 pt 3"]
[Site "?"]
[Date "2013.??.??"]
[Round "3"]
[White "Living"]
[Black "Spirit"]
[Result "0-1"]
[WhiteELO "?"]
[BlackELO "?"]
[SetUp "1"]
[FEN "b5nr/1p2kB1p/2p3p1/4p1q1/p1P5/N3RP2/P7/4K3 b KQkq - 0 22"]

%Created by Caissa's Web PGN Editor
22... Nf6 23. Rxe5+?? Kxf7?? 24. Ra5?? Qg1+? 25. Ke2 h5 26. Rxa8? h4?? 27.
Nc2?? h3?? 28. Rxa4 Rh4 29. f4? Rxf4 30. Kd3 b5 31. Nd4?? bxa4 32. c5 Qa1 33.
Nxc6 Nd7 34. Nd8+ Ke7 35. c6 Qxa2 36. cxd7 Kxd8 37. Ke3 Kxd7 38. Kxf4 h2 39.
Kg5 h1=Q 40. Kxg6 Qf2 41. Kg7 Qhg1+ 42. Kh7 Qfh2# 0-1
"""

val handwritten = """
[Event "XEQUE-MATEmatica 2014"]
[Site "Lisbon, Portugal POR"]
[Date "2014.05.19"]
[Round "2"]
[White "Oliveira, Paulo"]
[Black "Moreira, Simao"]
[Result "1-0"]
[Time "19:30:00"]
[Mode "OTB"]

1.  e4		e5
2.  Nf3		Nc6
3.  Bc4		Bc5
4.  c3		Nf6
5.  Ng5		O-O
6.  Qf3		d6
7.  h3		h6
8.  Bxf7+	Rxf7
9.  Nxf7	Kxf7
10. d3		Qh8
11. g4		Be6
12. Rg1		d5
13. Nd2		Be7
14. h4		Ke8
15. g5		hxg5
16. hxg5	Nh5
17. exd5	Bd7
18. dxc6	Bxc6
19. Ne4		Rd8
20. g6		Bxe4
21. Qxe4	Nf6
22. Rh1		Qf8
23. Qf5		Qg8
24. b3		Rd7
25. Bg5		Kd8
26. c4		Kc8
27. O-O-O	Qf8
28. Qxe5	Ng4
29. Bxe7	Rxe7
30. Qd4		Nxf2
31. Rdf1	Re2
32. Qxa7	Qf4+
33. Kb1		Qd2
34. Rh8+	Kd7
35. Qd4+	Ke7
36. Qd8+	Ke6
37. Re8+	Kf5
38. Rxe2	Qxe2
39. Qf8+	Kxg6
40. Rxf2	Qxd3+
41. Kb2		Qd4+
42. Ka3		Qa7+
43. Kb4		Qb6+
44. Kc3		Qe3+
45. Kc2		Qe4+
46. Kd2		Qd4+
47. Kc1		Qc3+
48. Rc2		Qe3+
49. Rd2		Qg1+
50. Kc2		Qg4
51. Qe8+	Kg5
52. Rd5+	Kh6
53. Qe3+	Kh7
54. Qd3+	Kh6
55. Rd4		Qg2+
56. Qd2+	Qg5
57. Qxg5+	Kxg5
58. Rd7		Kh6
59. Rxc7	g5
60. Rxb7	g4
61. Rd7		Kg5
62. Kd3		g3
63. Rg7+	Kh4
64. Ke2		Kh3
65. Kf3		Kh2
66. Rxg3	Kh1
67. Rg8		Kh2
68. Rg7		Kh1
69. Kf2		Kh2
70. Rh7++
1-0"""

val chessByPost = """
[Site "Chess By Post"]
[Date "2014.2.23"]
[White "paularsen"]
[Black "colonel68"]
[FinishedDate "2014.4.5"]
[Result "1/2-1/2"]

1. e2e4 c7c6 2. d2d4 d7d5 3. Nb1c3 d5xe4 4. Nc3xe4 Ng8f6 5. Ne4xf6+ g7xf6 6. c2c3 Bc8f5 7. Ng1f3 e7e6 8. g2g3 Bf8d6 9. Bf1g2 Nb8d7 10. O-O
Qd8c7 11. Rf1e1 O-O-O 12. b2b4 Bf5g6 13. a2a3 f6f5 14. c3c4 Qc7b8 15. Ra1b1 f5f4 16. Rb1b3 f4xg3 17. f2xg3 c6c5 18. b4xc5 Nd7xc5 19. Rb3c3
Nc5d3 20. Rc3xd3 Bg6xd3 21. Qd1xd3 Qb8c7 22. c4c5 Bd6e7 23. Nf3e5 Be7xc5 24. Bc1e3 Qc7xe5 25. d4xe5 Rd8xd3 26. Be3xc5 b7b6 27. Bg2f1 Rd3d5
28. Bf1a6+ Kc8c7 29. Bc5d6+ Rd5xd6 30. e5xd6+ Kc7xd6 31. g3g4 h7h5 32. g4g5 h5h4 33. Kg1g2 Rh8g8 34. g5g6 Rg8xg6+ 35. Kg2h3 f7f5 36. Kh3xh4
e6e5 37. Kh4h5 Rg6g7 38. Ba6c8 Rg7h7+ 39. Kh5g5 Rh7xh2 40. Bc8xf5 Rh2d2 41. Kg5f6 Rd2d5 42. Bf5e4 Rd5d4 43. Kf6f5 Rd4d2 44. Re1e3 Rd2d4 45.
Re3d3 Rd4xd3 46. Be4xd3 Kd6d5 47. Bd3b5 Kd5c5 48. a3a4 Kc5b4 49. Kf5xe5 a7a6 50. Bb5xa6 Kb4xa4  1/2-1/2"""

  val atomicRegression = """
[White "me"]
[Black "you"]
[Variant "Atomic"]
1. e4 d5 2. Nf3 dxe4 3. Bb5+ Qxd2#"""

val atomicPromotion = """
[Event "Rated game"]
[Site "https://lichess.org/MyjOSGx4"]
[Date "2015.02.25"]
[White "AngryBishop"]
[Black "Nyanta"]
[Result "1-0"]
[WhiteElo "1711"]
[BlackElo "1581"]
[PlyCount "37"]
[Variant "Atomic"]
[TimeControl "60+1"]
[ECO "?"]
[Opening "?"]
[Annotator "lichess.org"]

1. Nf3 f6 2. Nc3 c6 3. d4 d5 4. e3 e5 5. dxe5 d4 6. Ne4 d3 7. Nd6+ Bxd6 8. Nd4 d2+ 9. Ke2 dxc1=Q 10. g4 Nh6 11. Bg2 Nxg4 12. h3 Na6 13. Rad1 Nc5 14. Kf1 Ne4 15. f4 Nf2 16. Ne6 Qxd1 17. Nc7+ Ke7 18. Ne6 Kd6 19. Bxc6 1-0
"""

val weirdDashes = """
1. Nf3 d5 2. d4 Nf6 3. g3 g6 4. Bg2 c6 5. Bf4 Bg7 6. c3 b6 7. Nbd2 Nbd7 8. O‑O Ba6 9. Re1 O‑O 10. Qc2 Re8 11. b3 Nh5 12. Be5 f6 13. Bf4 Nxf4
14. gxf4 e5 15. fxe5 fxe5 16. dxe5 Nxe5 17. c4 Nxf3+ 18. Bxf3 Qg5+ 19. Kh1 Be5 20. Nf1 Bxa1 21. Rxa1 Rad8 22. Rd1 dxc4 23. Bxc6 Rxd1 24.
Qxd1 Rd8 25. Qc2 cxb3 26. Qxb3+ Kg7 27. Qc3+ Kh6 28. Qh3+ Qh5 29. Qe3+ Qg5 30. Qh3+ Kg7 31. Qc3+ Qf6 32. Qxf6+ Kxf6 33. Bf3 Rd7 34. Ne3 Rd2
35. Nd5+ Ke5 36. e3 Rxf2 37. Kg1 Rxf3 0-1
"""

val lichobile = """
[Event "Casual game"]
[Site "https://lichess.org"]
[Date "2015.12.13"]
[Result "0-1"]
[Variant "Standard"]

1. d4 d5 2. f4 Nc6 3. Nf3 f6
4. e3 Bg4 5. Be2 Bxf3
6. Bxf3 e5 7. Nc3 e4 8. Bh5+ g6
9. Be2 Qe7 10. Nxd5 Qd7
11. Nc3 f5 12. Nb5 Bb4+
13. c3 Be7 14. Rg1 a6
15. Na3 Nf6 16. h3 O-O-O
17. g4 Rhg8 18. Qb3 Na5
19. Qc2 h6 20. Rb1 c5
21. b4 cxb4 22. cxb4+ Nc6
23. Bc4 Rg7 24. b5 axb5
25. Nxb5 fxg4 26. Na7+ Kc7
27. d5 Nb4 28. Qc3 Kb8
29. Nc6+ bxc6 30. Ba3 c5
31. hxg4 Nxd5 32. Qxg7 Bh4+
33. Kf1 Nxe3+ 34. Ke2 Qd2# 0-1
"""

val overflow = """
[pgn]
[White "Him"]
[Black "Me"]

1. d4 d5 2. Nf3 Nc6 {variation on Chigorin's defense. White always plays Nf3 on move two, so I've been able to prepare against him. Getting him to play 2.c4 is like pulling teeth. It should be stated that I studied the theory to the Chigorin's and I believe without
 2.c4 and 3.Nc3 that black gets relatively easy equality, or practical equality.} 3. Bf4 Bg4 {He had been playing 3.c4 but he'd been losing all our games. I usually get the best of him maybe 3 out of 4 games, but lately he's been getting stronger and decided to try Bf4 in a London} 4. e3 Nf6 5. c4 e6 {positions with strategic ideas I'm familiar w
ith} 6. Nc3 Bb4 7. h3 Bh5 {Retreating the Bishop. I would've traded if it doubled his pawns. I tend to have an easier time playing closed games against double pawns.} 8. g4 Bg6 9. Bg5 h6 {Either he gives me the bishop pair or he gets a sub optimal bishop} 10. Bxf6 (10. Bxf6 Qxf6 {I have a hard time finding safe ideas for white}) gxf6 (10. Bh4 Qd7
 {If I was dead set on gxf6, perhaps just developing my queen to a square and preparing castling is better. The way I played it seems slow}){My idea. This is not the best move, infact, looking back on it, I think Qxf6 is much better. I had a hard time finding what to do with my queen, my King experienced safety issues, and I didn't even get to us
e the open g-file for pressure. Tactically and practically this is an alright move, but the position would've been much easier to play} 11. Qb3 dxc4 {I over looked this Queenmove. I've noticed I miss Qb3 in other Chigorin games like this, but it never proves to be a good move. I think I missed it because it was a bad move in other games. Here, i
t adds real pressure to d5. I take the c pawn because of my next idea.} 12. Bxc4 Be4 {I played this because I saw that the pin on the knight and control of e4 was strong. I felt the strength and saw the difficulty of getting out of the pin, and saw the tempos I would gain from this pin.} 13. Be2 (13. Be2 Qd5 14. Qxd5 Bxd5 15. O-O-O Bxc3 {with a p
ositions full of ideas.}) (13. Be2 a5 {This seems like an active way to continue the attack, trying for a4}) Bd5 {I think this is where I lose the thread of the game. After 13. Be2 I saw this position and I felt it was good. I have a nasty pin on the knight, I have the bishop pair and the game can open quic
kly, my king is safer and I have quick attacking ideas. However, in this 20|10 game, I had a hard time figuring out what to do next. I wanted to open the queenside and attackquickly while his king was still in the middle. I assumed it would be suicide for him to castle king side. I floundered in this great position just after 13 moves, and start
ed playing poorly with 13. ... Bd5} 14. Qa4 Qd7 {I missed this pin entirely, but I started having ideas. I feel like I missed a good opportunity, but I started looking at further ideas here. I have about 8 minutes to his 14 here.} 15. O-O-O Bxc3 16. bxc3 b5! {I love this move, technically I think I've blown my good position that I had just 3 move
s earlier, but I like the looks of this counter play I'm going to get against his king.} 17. Qxb5 Rb8 18. Qa4 O-O 19. Rh1g1 Rb6 {the position grows sharp and double edged. I ake up my mind to sac a piece.} 20. Nd2 Rf8b8 21. c4! (21. c4! Nb4! 22. Qxd7 Nxa2+ 23. Kc2 Rb2+ 24. Kd3 Nb4+ 25.Kc3 Na2+ {perpetual check and draw}) Rb4?! {I must admit that
 I wanted to sac a piece, but white forces the issue. Im getting low on time here. I'm at 3 minutes to his 8 and I had completely mis-evaluated c4, which seems obvious to me ow. I think I got tunnel vision with my double rooks. I found a really cool idea for sacing my queen with Nb4 instead of the howler Rb4. I saw that I got a lot of pressure a
nd I worked out a draw. However, this guy is usually a regular costumer for me and I got cocky and decided to go for the win. After Rb4, it dawned on me just how poor my position really was and I began to get very worried. I usually have a really good idea for evaluation of positions, but I began to see no real concrete threats and wondered if I
had mis-evaluated my attack.} 22. Qa3 Nxd4 23. exd4 Bc6 24. Nb3 Qd6 {I'm still in a worse position, but he hasn't defended accurately. Stockfish put the position around +1.6 in his favor, which isn't a good sign considering I'm a piece down. I try a cheapo with Qd6} 25. Qb2 a5 26. Qd2 a4 {all of a sudden I
feel like I've turned around my position. I calculated that Qh6 isn't going to be mating and my attack is coming MUCH sooner. I feel hopeful again. I have around 1 minute} 27. Qxh6?? axb3 {Joy!}28. Bd3!? {Pain. I have a minute on my clock and I missed his bishop idea. I knew that Qh7+ wasnt mate and stopped calculating his attack to conserve time
. What I missed was the Bishop check with Queen on h6 idea that I had seen a million times on tactics trainers. What I didn't know, was that I played the right move, and actually stockfish gives this position -3.5 in my favor. I found the correct idea, but I missed the follow up and ruined a great knockout. 30 seconds on my clock} Rxc4!? 29. Bxc4??
(29. Bxc4?? bxa2!! 30.Kc2 Be4+ 31. Bd3 Qxd4! 32. Qc1 Qc4+ 33.Kd2 Qxd3+ 34. Ke1 Rb1! 35. Rxd3 Rxc1+ 36. Kd2 Rxg1 {this is the best line stockfish can come up with for whiteafter Bxc4}) Qa3+?? {this super blunders the game back in his direction. After taking the rook the position is around -10.0 for black} 30. Kd2 Qb2+ 31. Ke3 Qc3+ 32. Bd3 e5?
33. Qh7+ Kf8 34. Qh8+ {resigns}
[/pgn]
"""

val overflow3 = """
[Event "8th Grand Slam Masters"]
[Site "Bilbao ESP"]
[Date "2015.10.28"]
[Round "3.1"]
[White "Ding Liren"]
[Black "Giri, A."]
[Result "1/2-1/2"]
[WhiteElo "2782"]
[BlackElo "2798"]
1. d4 Nf6 2. c4 g6 3. g3 c6 4. Bg2 d5 5. Nf3 Bg7 6. O-O O-O 7. Qb3 Qb6 8. Nc3 Rd8 9. Na4 Qxb3 10. axb3 Na6 11. Bf4 Ne8 12. Nc3 Nac7 13. Ra5 Be6 14. Ra4 a6 15. Rfa1 h6 16. h4 Nd6 17. e3 Rac8 18. Nd2 f6 19. Ra5 dxc4 20. Bxd6 exd6 21. bxc4 f5 22. R5a4 c5 23. Ne2 Rb8 24. b4 b5 25. cxb5 Nxb5 26. bxc5 dxc5 27. Rxa6 cxd4 28. Nxd4 Nxd4 29. exd4 Bxd4 30. Rf1 Bb6 31. Re1 Bxf2+ 32. Kxf2 Rxd2+ 33. Kg1 Bf7 34. Ra3 Rbb2 35. Bf3 g5 36. hxg5 hxg5 37. Rae3 g4 38. Be2 Bd5 39. Bf1 Rh2 40. R1e2 Rh1+ 41. Kf2 Rb1 42. Re1 Rb4 43. Re7 f4 44. Rc1 Rb2+ 45. Ke1 Rb8 46. gxf4 Bg2 47. Kf2 Bxf1 48. f5 Kf8 49. Ree1 Rb2+ 50. Kg3 Rh3+ 51. Kxg4 Rb4+ 52. Kg5 Rg3+ 53. Kf6 Bc4 54. Rcd1 Bd3 55. Rc1 Bc4 56. Rcd1 Rb8 57. Rd7 Bb3 58. Ree7 Rc3 59. Rd6 Kg8 60. Rg7+ Kh8 61. Rg5 Rh3 62. Ke5 Bg8 63. Kf6 Rh6+ 64. Rg6 Rh5 65. Ke5 Rb5+ 66. Kf4 Rhxf5+ 67. Kg4 Rf1 68. Rh6+ Kg7 69. Rdg6+ Kf7 70. Rb6 Re5 71. Rhc6 Re7 72. Rc3 Kg7 73. Rbc6 Be6+ 74. Kg3 Bd7 75. Rd6 Be8 76. Rf3 Rfe1 77. Rd2 Bc6 78. Rc3 Rg1+ 79. Kh2 Rh1+ 80. Kg3 Rh6 81. Rd4 Re2 82. Rg4+ Kf7 83. Rcc4 Rg2+ 84. Kf4 Rf6+ 85. Ke5 Re6+ 86. Kf5 Rge2 87. Rgf4 Rg6 88. Rcd4 Re7 89. Rh4 Bd7+ 90. Kf4 Rf6+ 91. Kg3 Re3+ 92. Kh2 Re2+ 93. Kg3 Re3+ 94. Kh2 Bf5 95. Kg2 Kg6 96. Kf2 Rc3 97. Rhf4 Rc2+ 98. Kg3 Re6 99. Rd8 Ree2 100. Rg8+ Kh7 101. Rd8 Be6 102. Rdd4 Kg6 103. Rfe4 Rg2+ 104. Kf4 Rgf2+ 105. Ke5 Bf5 106. Re3 Ra2 107. Rd6+ Kg5 108. Rg3+ Bg4 109. Rd4 Ra5+ 110. Kd6 Ra6+ 111. Kc5 Rf5+ 112. Kb4 Rb6+ 113. Kc4 Rc6+ 114. Kb4 Rf4 115. Rxf4 Kxf4 116. Rc3 Rg6 117. Rc4+ Ke5 118. Rc5+ Kd6 119. Rc4 Be6 120. Rd4+ Bd5 121. Kc3 Ke5 122. Rd3 Rc6+ 123. Kd2 Be4 124. Rc3 Rh6 125. Ke2 Rf6 126. Ke3 Rf8 127. Ke2 Kd4 128. Ra3 Bc2 129. Rh3 Rg8 130. Kf3 Be4+ 131. Kf4 Rf8+ 132. Kg5 Ke5 133. Rg3 Bf3 134. Kh4 Ke4 135. Rg7 Rf5 136. Kg3 Ke3 137. Rg8 Rh5 138. Rg7 Be4 139. Rg8 Rh1 140. Rg5 Rf1 141. Kg4 Bf3+ 142. Kf5 Kd4 143. Ke6 Rh1 144. Rg6 Be4 145. Rf6 Rh8 146. Kd6 Ra8 147. Ke7 Ra5 148. Rd6+ Bd5 149. Kf6 Ra7 150. Kf5 Re7 151. Rf6 Re1 152. Kg5 Be6 153. Kf4 Re3 154. Rf8 Bd5 155. Rf6 Re1 156. Kf5 Re5+ 157. Kf4 Be6 158. Kf3 Re3+ 159. Kf4 Re4+ 160. Kf3
Bg4+ 161. Kg3 Ke3 162. Rg6 Bf3 163. Rg5 Ra4 164. Re5+ Be4 165. Kg4 Ra1 166. Rg5 Rf1 167. Kh4 Bf5 168. Rg3+ Kf4 169. Rg4+ Ke5 170. Rg3 Kf6 171. Ra3 Rg1 172. Ra4 1/2-1/2
"""

val crazyhouse1 = """
[Event "internet, rated crazyhouse match"]
[Variant "Crazyhouse"]
[Site "ICS: freechess.org 5000"]
[Date "2001.01.02"]
[Time "07:19:??"]
[Round "-"]
[White "RabidWombat"]
[Black "JiggaB"]
[Result "1-0"]
[WhiteElo "2512"]
[BlackElo "2244"]
[TimeControl "60+0"]

1. e4 Nf6 2. Nc3 d5 3. exd5 Nxd5 4. Nxd5 Qxd5 5. N@e3 Qd8 6. Bb5+ P@c6 7. Bc4
e6 8. Nf3 N@b6 9. Be2 Bc5 10. d4 Bb4+ 11. Bd2 Be7 12. h4 N8d7 13. P@e5 c5 14.
dxc5 Nxc5 15. P@d4 Ne4 16. Bd3 Nxf2 17. Kxf2 P@f4 18. Nf1 P@e3+ 19. Bxe3 fxe3+
20. Nxe3 B@f4 21. Nc4 Nxc4 22. Bxc4 N@b6 23. Bb5+ Bd7 24. Bxd7+ Qxd7 25. Ke1
B@g3+ 26. P@f2 Bb4+ 27. P@c3 Bxf2+ 28. Kxf2 P@e3+ 29. Kg1 Be7 30. N@d3 Bh6 31.
N@c5 Bxc5 32. Nxc5 Qc6 33. B@e4 N@f2 34. Bxc6+ bxc6 35. Qe2 Nxh1 36. Kxh1 B@e7
37. Q@b7 Bxc5 38. Qxc6+ R@d7 39. Qeb5 N@f2+ 40. Kg1 O-O 41. dxc5 a6 42. Qf1
Rab8 43. cxb6 Rdd8 44. N@e7+ Kh8 45. bxc7 Rdc8 46. cxb8=Q Rxc6 47. Qxf8+ Q@g8
48. Qxg8#
{Black checkmated} 1-0
"""

val crazyhouse2 = """
[Event "internet, rated crazyhouse match"]
[Variant "Crazyhouse"]
[Site "ICS: freechess.org 5000"]
[Date "2001.01.02"]
[Time "20:02:??"]
[Round "-"]
[White "RabidWombat"]
[Black "marcusm"]
[Result "1-0"]
[WhiteElo "2540"]
[BlackElo "2235"]
[TimeControl "120+0"]

1. e4 Nf6 2. Nc3 Nc6 3. d4 e6 4. e5 Nd5 5. Nf3 Bb4 6. Qd3 d6 7. Bg5 Qd7 8. exd6
cxd6 9. Nd2 Bxc3 10. bxc3 f5 11. B@h5+ g6 12. P@c4 Nc7 13. Bf3 d5 14. cxd5 exd5
15. Qe3+ P@e4 16. P@d3 N@e6 17. dxe4 dxe4 18. Bxe4 fxe4 19. Nxe4 O-O 20. Nf6+
Rxf6 21. Bxf6 P@g7 22. Be5 Nxe5 23. dxe5 B@f4 24. P@f7+ Qxf7 25. Qe4 P@d2+ 26.
Ke2 B@d5 27. Qb4 N@c1+ 28. Kd1 Bd7 29. R@e7 Qxe7 30. P@f7+ Qxf7 31. N@e7+ Kh8
32. P@e3 Bxe5 33. f4 a5 34. Qa3 P@b4 35. cxb4 Bxa1 36. c4 P@b2 37. cxd5 Qxe7
38. dxe6 Nxe6 39. B@c2 R@e1+ 40. Kxd2 Rxe3 41. Qxe3 Qxb4+ 42. P@c3 Qe7 43. Bc4
N@d6 44. B2d3 Nxc4+ 45. Bxc4 b1=Q 46. Rxc1 R@b2+ 47. N@c2 Rxc2+ 48. Rxc2 N@f1+
49. Bxf1 Qxf1 50. R@f2 B@c1+ 51. Rxc1 Qxc1+ 52. Kxc1 P@b2+ 53. Rxb2 Bxb2+ 54.
Kxb2 B@a3+ 55. Kc2 R@b2+ 56. Kd1
{Black ran out of time} 1-0
"""

val crazyhouseNoVariantTag = """
[Event "internet, rated crazyhouse match"]
[Site "ICS: freechess.org 5000"]
[Date "2001.01.02"]
[Time "07:19:??"]
[Round "-"]
[White "RabidWombat"]
[Black "JiggaB"]
[Result "1-0"]
[WhiteElo "2512"]
[BlackElo "2244"]
[TimeControl "60+0"]

1. e4 Nf6 2. Nc3 d5 3. exd5 Nxd5 4. Nxd5 Qxd5 5. N@e3 Qd8 6. Bb5+ P@c6 7. Bc4
e6 8. Nf3 N@b6 9. Be2 Bc5 10. d4 Bb4+ 11. Bd2 Be7 12. h4 N8d7 13. P@e5 c5 14.
dxc5 Nxc5 15. P@d4 Ne4 16. Bd3 Nxf2 17. Kxf2 P@f4 18. Nf1 P@e3+ 19. Bxe3 fxe3+
20. Nxe3 B@f4 21. Nc4 Nxc4 22. Bxc4 N@b6 23. Bb5+ Bd7 24. Bxd7+ Qxd7 25. Ke1
B@g3+ 26. P@f2 Bb4+ 27. P@c3 Bxf2+ 28. Kxf2 P@e3+ 29. Kg1 Be7 30. N@d3 Bh6 31.
N@c5 Bxc5 32. Nxc5 Qc6 33. B@e4 N@f2 34. Bxc6+ bxc6 35. Qe2 Nxh1 36. Kxh1 B@e7
37. Q@b7 Bxc5 38. Qxc6+ R@d7 39. Qeb5 N@f2+ 40. Kg1 O-O 41. dxc5 a6 42. Qf1
Rab8 43. cxb6 Rdd8 44. N@e7+ Kh8 45. bxc7 Rdc8 46. cxb8=Q Rxc6 47. Qxf8+ Q@g8
48. Qxg8#
{Black checkmated} 1-0
"""

val stackOverflow = """
[Event "?"] [Site "Anytown"] [Date "2015.??.??"] [Round "?"] [White "Beginner"] [Black "Grandmaster"] [Result "*"] [ECO "C65"] [BlackElo "3000"] [Annotator "Pickard,S."] [PlyCount "8"] [EventDate "2011.04.09"] [SourceDate "2015.04.30"] 1. e4 {[%csl Ge4][%cal Re4d5,Yd1h5,Yf1a6] [A very good chess opening move, which conforms to all the chess strategy principles discussed. White occupies one key center square with a pawn, also attacking another central point. In addition, the move also liberates the White Queen and King's Bishop. As World Champion Bobby Fischer said of 1.e4, "Best by test." If unopposed White will likely play his d-pawn forward next move!]} ({The move} 1. d4 {[%csl Gd4][%cal Yd1d3,Yc1h6,Rd4e5] also meets our requirements, sending a pawn to the center and attacking another central square. The Queen defends the pawn, and she is free to move forward. In addition, the Queen's Bishop can now develop, and White "threatens" to play his e-pawn up two squares to dominate the center.}) ( {Even a move like} 1. Nf3 {[%csl Gd4,Ge5] is quite good, bringing the Knight toward the center, and attacking two center squares. The move also brings White closer to castling his King to safety - another goal of good chess strategy in the opening.}) 1... e5 {[%csl Ge5][%cal Re5d4,Yd8h4,Yf8a3] [Black answers by staking his own claim to the center squares, occupying one and attacking another. The move makes ready to deploy the Queen and King's Bishop to active central squares. Now White cannot hope for two pawns abreast in the center.]} ({Other moves are possible of course, but any good move here will be found to fight for the center and rapidly develop the pieces to squares of maximum efficiency. For example} 1... e6 {[%cal Re6d5,Yd8h4,Yf8a3] attacks an important central square and prepares to support the d-pawn's advance two squares into the center next move. Now after} 2. d4 {(White controls the center of the chess board, an ideal arrangement according to sound chess strategy in the opening)} d5 {Black quickly strikes back in the center, firmly establishing a pawn foothold on the d5 square, for if White captures Black retakes with his e-pawn. Notice that White's e-pawn is also threatened with capture. This position begins the French Defense, a well known chess opening.}) 2. Nf3 {[%csl Re5] [An ideal chess opening move. White develops a Knight to its best square (toward the center!) and attacks the enemy pawn. Black is limited in his reply.]} ({Again, White could make other good moves, like} 2. Nc3 {[%csl Gd5,Ge4] which also meets guidelines for proper chess opening strategy. A Knight is brought out toward the center, two center squares are influenced and the White e-pawn is solidly protected.}) 2... Nc6 {[%csl Yd4, Ye5] [Excellent - a Knight is developed actively, attacking two central squares and defending the Black e-pawn. The influence of White's Knight is thus counteracted.]} 3. Bb5 {[%csl Re5][%cal Ye1g1] [Rapid deployment and no wasted motion. This move adheres to the principles of chess strategy, by preparing to castle and by undermining Black's defense of this e-pawn. Without getting bogged down in chess tactics, observe that White is not yet threatening to win the Black e-pawn, even if he could move again.]} ({Instead} 3. Bc4 {[%cal Ye1g1,Ga2g8] illustrates good chess strategy as well, placing the King's Bishop on an active square where it commands two long diagonals, attacks the d5 central square and prepares to castle.}) 3... Nf6 {[%csl Re4] [Black counterattacks! He brings out the King's Knight and controls two center squares, besides placing the enemy e-pawn under attack.]} 4. O-O {[%csl Gg1] [All according to the best chess strategy. White's King is now safely tucked away in the corner, and his King's Rook is brought toward the center. Next he will plan the development of his Queenside pieces while hampering Black's attempt to smoothly develop.]} Bc5 {[%csl Rg1][%cal Ye8g8,Ga7g1] [The King's Bishop takes up its most active post, where it commands squares leading all the way to White's King. In addition, Black is now ready to castle. This position forms part of the Berlin Defense to the Ruy Lopez.]}*
"""

  val explorerPartialDate = """[Event "Linares"]
[Site "Linares"]
[Date "1978.??.??"]
[Round "?"]
[White "Palacios de la Prida, Ernesto"]
[Black "Debarnot, Roberto Luis"]
[Result "0-1"]
[WhiteElo "2335"]
[BlackElo "2425"]

1. d3 g6 2. f4 d5 3. Nf3 Bg7 4. g3 c6 5. Bg2 Qb6 6. c3 Nf6 7. Qb3 Nbd7 8. Qxb6 axb6 9. h3 Nc5 10. Nbd2 O-O 11. O-O b5 12. a3 Ra4 13. Kh2 Ne8 14. d4 Ne4 15. Nxe4 dxe4 16. Nd2 f5 17. e3 Be6 18. Re1 Ra7 19. Bf1 Nd6 20. b3 b6 21. Bb2 c5 22. Be2 Rc8 23. Rec1 h5 24. Bd1 Kf7 25. Kg2 Bf6 26. h4 Ke8 27. Kf2 Kd7 28. Rab1 Rac7 29. Ra1 Ra8 30. Rab1 Rca7 31. Rc2 Rxa3 32. Bxa3 Rxa3 33. dxc5 bxc5 34. Be2 Kc6 35. b4 c4 36. Rbc1 Nc8 37. Nb1 Ra1 38. Nd2 Ra7 39. Nb1 Nb6 40. Bd1 Nd5 41. Be2 Ra1 42. Rd1 Bf7 43. Bf1 e5 44. fxe5 Bxe5 45. Bg2 Bc7 46. Re2 Be6 47. Rb2 Nf6 48. Bh3 Nd7 49. Bf1 Ne5 50. Be2 Nd3+ 51. Bxd3 cxd3 52. Rf1 Be5 53. Kg2 Kd6 54. Rff2 Bf6 55. Rf1 Bg7 56. Kf2 Be5 57. Kg2 Bf6 58. Rff2 g5 59. Rf1 gxh4 60. gxh4 Bxh4 61. Rh1 Bg5 62. Kf2 f4 63. exf4 Bxf4 64. Re1 Ke5 0-1
"""
}
