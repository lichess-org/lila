package lila.game

import chess.*
import chess.format.pgn.PgnTree.*
import chess.format.pgn.{ Move, SanStr }

class PgnDumpTest extends munit.FunSuite:

  def assertPgnDump(pgn: String) =
    val sanStrs = pgn.split(' ').toList.map(SanStr(_))
    val output = PgnDump.makeTree(sanStrs, Vector.empty, Color.White).so(_.render(Ply.firstMove))
    val clean = output.split(' ').grouped(3).map(_.tail).flatten.mkString(" ") // remove ply number
    assertEquals(clean, pgn)

  def assertPgnDumpWithMoveNumbers(pgn: String) =
    val sanStrs = pgn.split(' ').toList.grouped(3).map(_.drop(1)).flatten.map(SanStr(_)).toList
    val output = PgnDump.makeTree(sanStrs, Vector.empty, Color.White).so(_.render(Ply.firstMove))
    assertEquals(output, pgn)

  test("roundtrip pgns"):
    raws.foreach(assertPgnDump(_))

  test("roundtrip pgns with move numbers"):
    pgns.foreach(assertPgnDumpWithMoveNumbers(_))

  test("pgn from few moves"):
    assertEquals(
      PgnDump
        .makeTree("e5 Ke2 Ke7".split(' ').toList.map(SanStr(_)), Vector.empty, Color.White)
        .get,
      Node(
        Move(SanStr("e5")),
        Some(
          Node(
            Move(SanStr("Ke2")),
            Some(
              Node(
                Move(SanStr("Ke7")),
                None
              )
            )
          )
        )
      )
    )

  val raws = List(
    "e3 Nc6 d4 Nf6 c3 e5 dxe5 Nxe5 Bb5 a6 Ba4 b5 Bb3 d5 e4 dxe4 f4 Qxd1+ Kxd1 Nd3 Be3 Ng4 Bd4 Ngf2+ Bxf2 Nxf2+ Ke1 Nxh1 Bd5 Ra7 Bc6+ Kd8 Bxe4 Bd6 g3 Re8 Nd2 f5 Ne2 fxe4 Kf1 e3 Kg2 exd2 Rxh1 Bb7+ Kf2 Bc5+ Kf1 d1=Q#",
    "c4 Nc6 e3 Nf6 h3 Ne4 d3 Nc5 a3 Ne5 d4 d6 dxe5 dxe5 b4 Qxd1+ Kxd1 Ne4 f3 Nf2+ Ke2 Nxh1 Nd2 Ng3+ Ke1 Bf5 Bd3 Bxd3 Rb1 Bxb1 Nxb1 Rd8 Bd2 e6 h4 Be7 Nh3 Bxh4 Nf2 Ke7 Bc3 f6 Nd2 h5 c5 g5 Nc4 Rhg8 Na5 Nh1 Ke2 Nxf2 Be1 Nd3 Nxb7 Bxe1 Nxd8 Rxd8 c6 a5 bxa5 Bxa5 a4 f5 Kd1 Nf4+ Kc2 Rd2+ Kc1 Nxg2 Kb1 Nxe3 Kc1 h4 Kb1 h3 Kc1 h2 Kb1 h1=Q#",
    "d4 Nf6 c4 Nc6 Nc3 e5 Nd5 Nxd5 cxd5 Nxd4 e3 Nf5 e4 Nd4 h4 Qf6 Bg5 Qb6 b3 h6 Bc4 hxg5 h5 Bc5 Ne2 Qa5+ Kf1 d6 Nxd4 Bxd4 Rc1 Qxa2 Rc2 Qa5 Qc1 g4 h6 g3 f3 gxh6 Rxh6 Rxh6 Qxh6 Bf2 Qh8+ Kd7 Qf8 Qe1#",
    "Nc3 c6 Nf3 Na6 b4 Nxb4 Rb1 c5 a3 Nxc2+ Qxc2 b6 Nb5 Ba6 Qa4 Bxb5 Rxb5 Nf6 Bb2 Nd5 Qg4 Nc7 Bxg7 Bxg7 Qxg7 Rf8 Rb3 Ne6 Qxh7 Qb8 Re3 f6 Qg6+ Rf7 g3 Nf8 Qg8 e5 d4 d6 dxc5 Qc7 cxd6 Qc1#",
    "d4 Nf6 Nf3 Nc6 Nbd2 e6 e4 d6 c4 Qe7 Bd3 e5 d5 Nd4 Nxd4 exd4 O-O Bg4 f3 Bd7 Nb3 Qe5 Be2 c5 dxc6 bxc6 Qxd4 Qxd4+ Nxd4 Rb8 b3 Be7 Be3 Bd8 Rfd1 Bb6 Kf2 Ke7 Rd2 Ba5 Rd3 Bb6 Rad1 Rhd8 g4 h6 Bf4 g5 Bg3 h5 h3 h4 Bh2 Rb7 e5 dxe5 Bxe5 Ne8 Kg2 Bc7 Bxc7 Rxc7 Nf5+ Kf8 f4 gxf4 Nxh4 Ng7 Bf3 Ne6 Nf5 Nc5 Rd4 Ne6 Rd6 c5 h4 Ng7 Nxg7 Kxg7 g5 a5 Kf2 Kf8 Bc6 Ke7 Ba4 Bxa4 Rxd8 Bc6 h5 Ke6 h6 Be4 Rh8 Re7 Re1 Kf5 h7 Kg6 Rc8 Kxh7 Rxc5 a4 b4 Kg6 b5 f6 gxf6 Kxf6 b6 a3 Rc7 Rxc7 bxc7 Bb7 Re8 Kf5 c5 Ba6 Ra8 Bb7 Rf8+ Ke5 c6 Ba6 Ra8 Kd6 Rxa6 Kxc7 Kf3 Kb8 Kxf4 Kc8 Ke5 Kc7 Ke6 Kd8 Kd6 Ke8 Ra7 Kf8 c7 Kf7 c8=Q+ Kg6 Qg4+ Kf6 Ra8 Kf7 Qf5+ Kg7 Ra7+ Kg8 Qc8#",
    "e3 Nc6 Nf3 Nf6 Nc3 e6 a3 Bd6 d4 Ng4 h3 Nf6 Bb5 a6 Bxc6 dxc6 e4 Nd7 O-O h5 h4 c5 Bg5 f6 Be3 cxd4 Bxd4 c5 Be3 Qe7 g3 Ne5 Nxe5 Bxe5 Qe2 Bxc3 bxc3 Kf7 Rad1 b5 c4 Rb8 Rd2 Bb7 Rfd1 Bc6 Kg2 Bxe4+ Kf1 Bc6 Bf4 e5 Be3 Qe6 Bxc5 Qh3+ Ke1 Qh1+ Qf1 Qe4+ Re2 Qxc4 Bd6 Rbd8 Bb4 Bf3 Rxd8 Rxd8 Re3 Rd1#",
    "e4 e5 Nf3 Nc6 Nc3 Bb4 Nd5 Nf6 Nxb4 Nxb4 c3 Nc6 Nxe5 Nxe5 d4 Ng6 Bg5 h6 Bxf6 Qxf6 e5 Qe6 Bd3 d6 Qe2 Nf4 Qe4 dxe5 Bb5+ c6 d5 Nxd5 Bc4 O-O O-O b5 Bb3 Bb7 Bc2 Nf4 Qh7#"
  )

  val pgns = List(
    "1. d4 e6 2. c4 d5 3. Nc3 Nf6 4. Nf3 c5 5. Bg5 Be7 6. e3 Nc6 7. cxd5 exd5 8. Bb5 O-O 9. O-O a6 10. Bd3 c4 11. Bc2 h6 12. Bh4 Nd7 13. Bxe7 Qxe7 14. Bb1 Nf6 15. Re1 b5 16. Qc2 b4 17. Ne2 b3 18. axb3 Nb4 19. Qc3 cxb3 20. Qxb3 Bg4 21. Ne5 Bxe2 22. Rxe2 Rfb8 23. Qa3 Qe6 24. f4 Ne4 25. Bxe4 dxe4 26. Rc1 f6 27. Nc4 Nd3 28. Rd1 Qxc4 29. h3 Rb3 30. Qe7 Rab8 31. d5 Rxb2 32. d6 Rxe2 33. d7 Rxg2+ 34. Kxg2 Qc2+ 35. Kg3 Qf2+ 36. Kg4 Qf3+ 37. Kh4 g5+ 38. fxg5 fxg5+",
    "1. d4 c5 2. d5 e6 3. c4 exd5 4. cxd5 Nf6 5. Nc3 d6 6. e4 g6 7. Bd3 Bg7 8. Nge2 O-O 9. O-O Re8 10. f4 a6 11. a4 Bd7 12. a5 b5 13. axb6 Qxb6 14. Kh1 c4 15. Bc2 a5 16. Qe1 Na6 17. Ng3 Nb4 18. Bb1 Nd3 19. Bxd3 cxd3 20. Be3 Qxb2 21. Rb1 Qa3 22. Qd2 Rab8 23. Qxd3 Rxb1 24. Rxb1 Rc8 25. Bd4 Nxd5 26. exd5 Bxd4 27. Qxd4 Qxc3 28. Qxc3 Rxc3 29. Rb8+ Rc8 30. Rxc8+ Bxc8 31. Ne2 Bb7 32. Nc3 a4 33. Nxa4 Bxd5 34. Nc3 Be6 35. Kg1 d5 36. Kf2 d4 37. Nd1 Bc4 38. Ke1 f5 39. Kd2 Kf7 40. Nf2 Ke6 41. Nd3 Kd5 42. Nc1 Ke4 43. Ne2 Bxe2 44. Kxe2 Kxf4 45. Kd3 Ke5 46. g3 Kd5 47. h3 f4 48. Kd2 fxg3 49. Ke2 d3+"
  )
