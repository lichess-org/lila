package lila.game

import org.lichess.compression.game.{ Encoder => GameEncoder, PerftTest, Board }

import org.specs2.mutable._

import chess.Pos

class HuffmanPgnTest extends Specification {

  def hexToBytes(str: String) =
    str.grouped(2).map(cc => Integer.parseInt(cc, 16).toByte).toArray

  "game compression" should {
    "compress and decompress" in {
      forall(fixtures) { pgn =>
        val pgnMoves = pgn.split(" ")
        val encoded = GameEncoder.encode(pgnMoves)
        val decoded = GameEncoder.decode(encoded, pgnMoves.size)
        pgnMoves must_== decoded.pgnMoves
      }
    }

    "unmoved rooks" in {
      import scala.collection.JavaConversions.asScalaSet
      val pgnMoves = "d4 h5 c4 Rh6 Nf3 Rh8".split(" ")
      val encoded = GameEncoder.encode(pgnMoves)
      val decoded = GameEncoder.decode(encoded, pgnMoves.size)
      asScalaSet(decoded.unmovedRooks) must_== Set(0, 7, 56)
    }

    "position hash 1. e4 d5 2. e5 f5 3. Ke2 Kf7" in {
      val pgnMoves = "e4 d5 e5 f5 Ke2 Kf7".split(" ")
      val encoded = GameEncoder.encode(pgnMoves)

      // initial position
      val d0 = GameEncoder.decode(encoded, 0)
      d0.positionHashes must_== hexToBytes("91fc9c")

      // 1. e4
      val d1 = GameEncoder.decode(encoded, 1)
      d1.positionHashes must_== hexToBytes("114196")

      // 1. e4 d5
      val d2 = GameEncoder.decode(encoded, 2)
      d2.positionHashes must_== hexToBytes("c50fb0")

      // 1. e4 d5 2. e5
      val d3 = GameEncoder.decode(encoded, 3)
      d3.positionHashes must_== hexToBytes("db29d4")

      // 1. e4 d5 2. e5 f5 (en passant matters)
      val d4 = GameEncoder.decode(encoded, 4)
      d4.positionHashes must_== hexToBytes("47ff78")

      // 1. e4 d5 2. e5 f5 3. Ke2
      val d5 = GameEncoder.decode(encoded, 5)
      d5.positionHashes must_== hexToBytes("47ff78" + "f242c1")

      // 1. e4 d5 2. e5 f5 3. Ke2 Kf7
      val d6 = GameEncoder.decode(encoded, 6)
      d6.positionHashes must_== hexToBytes("47ff78" + "f242c1" + "46bdd9")
    }

    "position hash 1. a4 b5 2. h4 b4 3. c4 bxc3 4. Ra3" in {
      val pgnMoves = "a4 b5 h4 b4 c4 bxc3 Ra3".split(" ")
      val encoded = GameEncoder.encode(pgnMoves)

      // 1. a4 b5 2. h4 b4 3. c4
      val d5 = GameEncoder.decode(encoded, 5)
      d5.positionHashes must_== hexToBytes("067637")

      // 1. a4 b5 2. h4 b4 3. c4 bxc3 4. Ra3
      val d7 = GameEncoder.decode(encoded, 7)
      d7.positionHashes must_== hexToBytes("2edfae" + "279560")
    }

    "pass perft test" in {
      // Running the entire suite can take minutes.

      /*
      PerftTest.batch0();
      PerftTest.batch1();
      PerftTest.batch2();
      PerftTest.batch3();
      PerftTest.batch4();

      PerftTest.batch5();
      PerftTest.batch6();
      PerftTest.batch7();
      PerftTest.batch8();
      PerftTest.batch9();

      PerftTest.batch10();
      PerftTest.batch11();
      PerftTest.batch12();
      PerftTest.batch13();
      PerftTest.batch14();

      PerftTest.batch15();
      PerftTest.batch16();
      */

      PerftTest.batch17();

      // These tests throw a RuntimeError if they fail.
      true must_== true
    }
  }

  val fixtures = List(
    "d3 d5 g3 e6 Bg2 Nf6 Nf3 Be7 O-O O-O Re1 a6 e4 c5 e5 Nfd7 d4 Nb6 dxc5 Bxc5 Nc3 N8d7 a4 Be7 a5 Nc4 b3 Ncxe5 Nxe5 Nxe5 Rxe5 Bd6 Re1 Bd7 Bf4 Bc6 Bxd6 Qxd6 Na4 Rad8 Nb6 Rfe8 Ra4 Bxa4 bxa4 Qc5 Qa1 Qxa5 Qd4 Rd6 Nc4 Qb4 Nxd6 Qxd4 Nxe8 Qd2 Rb1 Qxc2 Rxb7 Qxa4 Rb8 Kf8 Nd6+ Ke7 Nf5+ Kf6 Nh4 Qd1+ Bf1 Qd4 Kg2 a5 Rb7 a4 Rxf7+ Kxf7 Nf3 Ke7 Ne5 Kd6 Nf3 Qc4 Nd4 Qc3 Nf5+ Ke5 Ne3 Kf6 Nxd5+ exd5",
    "e4 e6 Nf3 c5 g3 a6 Bg2 Nc6 O-O d6 h3 Be7 Nc3 Qc7 d4 cxd4 Nxd4 Nxd4 Qxd4 Bf6 Qd1 e5 Nd5 Qc6 Nxf6+ Nxf6 Re1 O-O Bg5 Nd7 f4 exf4 Bxf4 Ne5 Bxe5 dxe5 a3 Be6 b4 Rad8 Qe2 Rd4 Rad1 Rfd8 Rxd4 Rxd4 c3 Rc4 Qc2 f6 Rd1 Qc7 a4 Rxb4 Rc1 Rc4 Bf1 Rc5 c4 Qb6 Qd2 Rxc4+ Kh1 Rxc1 Qxc1 Qc6 Qd1 Qxe4+ Bg2 Qd4 Qc1",
    "e4 g6 Nf3 Bg7 d4 e6 Nc3 Ne7 Be3 O-O Be2 d6 O-O b6 Qd2 Bb7 Bh6 c5 Bxg7 Kxg7 dxc5 bxc5 Rad1 d5 exd5 exd5 Rfe1 d4 Nb5 a6 Na3 Kg8 c3 Nbc6 cxd4 cxd4 Nxd4 Nxd4 Qxd4 Qxd4 Rxd4 Nc6 Rd7 Rab8 Red1 Ne5 Re7 Nc6 Rc7 Nb4 Rdd7 Be4 Bc4 Bd5 Bxd5 Nxd5 Rxd5 Rxb2 Rc2 Rfb8 f3 Rxc2 Nxc2 Rb2 Rd8+ Kg7 Rc8 Rxa2 Nb4 Ra4 Nd5 Ra1+ Kf2 a5 Kg3 a4 Ra8 a3 Nc3 Rc1 Nb5 Rb1 Nxa3 Ra1",
    "e4 g6 d4 Bg7 e5 e6 f4 Ne7 Nf3 d5 Nc3 O-O Be3 Nd7 Bd3 b6 Nb5 c5 Nd6 cxd4 Nxd4 Nxe5 fxe5 Bxe5 Nxc8 Rxc8 O-O Nc6 Nxc6 Rxc6 c3 Bc7 Bd4 Qd6 Qg4 Qxh2+ Kf2 e5 Rh1 Qf4+ Qxf4 exf4 Rh6 Re6 Rah1 f5 Rxh7 Bd8 Rh8+ Kf7 R1h7+ Ke8 Bb5+ Rc6 Bxc6#",
    "e4 c5 Nf3 Nc6 Bc4 Nf6 Nc3 e5 O-O d6 a3 Nd4 Nxd4 exd4 Nd5 Be6 Nxf6+ Qxf6 Bxe6 Qxe6 c3 d3 Qa4+ Ke7 e5 d5 Re1 c4 Qb4+ Kd7 Qxb7+ Ke8 Qxa8+ Kd7 Qxa7+ Kc8 b3 Qf5 bxc4 dxc4 Qa8+ Kc7 Rb1 Bc5 Rb7+ Kc6 Qa6+ Kd5 Rf1 Re8 Rc7 Rxe5 Qc6#",
    "b3 d5 Bb2 Nf6 g3 e5 Bg2 Nc6 d3 Be6 e3 Qd6 Nd2 O-O-O Ne2 g6 c4 dxc4 bxc4 Bg7 O-O Nd7 d4 exd4 exd4 Nb4 Ne4 Qe7 d5 Bg4 Bxg7 Rhg8 Bc3 Bxe2 Qxe2 f5 Bxb4 Qxb4 Rab1 Qa3 Nc3 Rde8 Qb2 Qa6 Qb5 Qa3 Qb4 Qa6 a4 Nb6 Qb5 Qxb5 axb5 Nxc4 d6 Nd2 Bxb7+ Kxb7 Rbe1",
    "e4 d5 exd5 Qxd5 Nc3 Qd8 Nf3 Nf6 d4 g6 Bc4 Bg7 Bg5 O-O O-O Be6 Bxe6 fxe6 Re1 Nc6 d5 e5 dxc6",
    "e3 e5 Nc3 Nf6 Bc4 d5 Bb3 c5 Ba4+ Bd7 Bxd7+ Qxd7 d4 exd4 exd4 cxd4 Qxd4 Qe6+ Be3 Ne4 Nf3 Nxc3 Qxc3 Nc6 Nd4 Nxd4 Qxd4 a5 a3 b5 O-O Be7 Rad1 O-O Qxd5 Qxd5 Rxd5 b4 Rd7 Rfe8 axb4 Bxb4 c3 Be7 Rfd1 a4 Ra1 h6 Rd4 Bf6 Rdxa4 Rxa4 Rxa4 g5 g3 h5 Kg2 Kg7 h4 gxh4 gxh4 Bxh4 Rxh4 Kg6 b4 Re5 Bd4 Rg5+ Kf3 f5 c4 Rg4 Rxg4+ fxg4+ Kg3 Kg5 b5 h4+ Kg2 h3+ Kh2 Kh4 Be5 Kg5 b6 Kf5 Bg3 Ke4 b7 Kf3 b8=Q Ke2 Qd8 Kf1 Qd1#",
    "Nf3 f6 d3 e5 Nbd2 g5 e4 g4 Ng1 Bb4 Qxg4 d5 Qh5+ Kf8 a3 dxe4 axb4 exd3 Bxd3 e4 Bc4 f5 Qf7#",
    "e4 d5 exd5 Qxd5 Nc3 Qa5 b3 c6 Bb2 Nf6 Be2 Bf5 Nf3 e6 O-O Nbd7 a3 Qc7 a4 Bd6 d3 Ng4 h3 Ngf6 Ne4 Nxe4 dxe4 Bg6 Bd3 f6 Nh4 Bf7 g3 Qb6 Kg2 g5 Nf3 h5 Nh2 Qc7 Qf3 Ne5 Qxf6 Nxd3 Qxh8+ Kd7 Qg7 Nxb2 Qxf7+ Kc8 Qe8+ Qd8 Qxe6+ Kc7 Qf7+ Be7 Qxh5 Qd2 Qf7 Qd6 Rfb1 Rf8 Qg7 Qf6 Qxf6 Bxf6 Ra2 c5 Raxb2 Bxb2 Rxb2 Rd8 c4 Rd3",
    "e4 c6 e5 d6 exd6 exd6 d4 g6 Bd2 Bg7 Bc3 Ne7 Bc4 Be6 b3 b5 Bxe6 fxe6 d5 Nxd5 Bxg7 Rg8 Bh6 Qf6 c3 Nd7 Qf3 O-O-O Ne2 Qxf3 gxf3 Ne5 Nd4 Rde8 f4 Nd3+ Kf1 Kb7 Nd2 N5xf4 Bxf4 Nxf4 Ne4 Rgf8 Nxd6+ Kb6 Nxe8 Rxe8 Re1 c5 Rxe6+ Nxe6 Nxe6 Rxe6 Rg1 a5 Rg4 Kc6 Rg5 Kd6 Kg2 Re5 Rg4 h5 Rxg6+ Kd5 Ra6 c4 Rxa5 Kc6 b4 Rg5+ Kf1 Rg8 Ra6+",
    "f4 d5 Nf3 Nc6 e3 e6 Bb5 Nf6 d3 Bd7 O-O a6 Ba4 b5 Bb3 Bd6 c3 O-O Bc2 h6 e4 dxe4 dxe4 Be7 e5 Nd5 Qd3 g6 f5 exf5 Bxh6 Re8 Qxd5 Be6 Qxd8 Raxd8 Nbd2 Rd5 Rae1 Bc5+ Kh1 Red8 Nb3 Bb6 Bg5 R8d7 Bf6 a5 Rd1 a4 Nc1 Be3 Nd3 Na5 Nb4 Rxd1 Rxd1 Rxd1+ Bxd1 Nc4 Nd3 Na5 Bd8 Bb6 a3 Bc4 Nb4 Kf8 Nd4 Ke8 Bf6 Bxd4 cxd4 Kd7 d5 Nb7 Bf3 Nc5 Nc6 Nd3 Na5 Nxb2 Nxc4 bxc4 e6+ fxe6 Bxb2 exd5 Bxd5 Kd6 Bxc4 g5 Bb5 c6 Bxa4 Kd5 Bc2 f4 Bc1 Ke5 Kg1 c5 Kf2 g4 g3 f3 Be3 Kd5 Bf4 c4 a4 Kc5 Be5 Kb4 a5",
    "e4 e5 Nc3 Nf6 d3 c6 f4 exf4 Bxf4 d5 e5 d4 Nce2 Nd5 Bd2 c5 c4 Ne3 Bxe3 dxe3 Nf3 Be7 Nc3 Bg4 Be2 O-O O-O Nc6 Nd5 Nd4 Nxd4 cxd4 Bxg4 Bc5 e6 fxe6 Bxe6+ Kh8 Rxf8+ Qxf8 Qf3 Qd6 Bf5 Rf8 Qe4 g6 Bg4 a5 Bf3 Ba7 a3 Bb8 g3 Ba7 b4 h5 c5 Qd7 Qxg6 Qh7 Qxh7+ Kxh7 Bxh5 Bb8 Rf1 Rd8 Nf6+ Kh6 g4 Kg5 Kg2 axb4 axb4 Rc8 Ne4+ Kh6 Rf6+ Kg7 g5 Be5 Rf7+ Kg8 Rxb7 Rf8 Bf3 Ra8 Ng3 Ra2+ Ne2 Bf4 h4 Kf8 b5 Rd2 c6 Rxd3 b6",
    "d4 d5 h3 e6 a3 Nf6 Nf3 b6 e3 c5 c4 cxd4 Qxd4 Nc6 Qd1 Be7 cxd5 Nxd5 Bb5 Bb7 Bxc6+ Bxc6 e4 Nf6 Qxd8+ Rxd8 Nc3 O-O e5 Bxf3 gxf3 Nd5 Nxd5 Rxd5 f4 f6 Be3 fxe5 fxe5 Rxe5 O-O-O Bf6 Kb1 Rb5 Rd2 Rd8 Rhd1 Rf8 Ka2 a5 Rd6 e5 b4 axb4 axb4 Ra8+ Kb3 Be7 Rxb6 Rxb6 Bxb6 Rb8 Ba5 Rb5 Ka4 Rb7 Re1 Bd6 Rd1 Rd7 Rc1 Kf7 Rg1 g6 h4 Ke6 Rg5 Rf7 Bb6 Rf4 h5 Kf6 Rg2 gxh5 Ba5 Rg4 Rh2 h4 f3 Rf4 Rh3 Kf5 Kb5 e4 fxe4+ Kxe4 Kc6 Be5 b5 Rf6+ Kd7 Rd6+ Ke7 Rd5 Rxh4+ Kf3 Rb4 Bd6+ Ke6 Bxb4 Kxd5 Bxa5 Kc6 h5 Kb7 h4 Ka6 Bd8 b6 Bxb6 Kxb6 h3",
    "d4 d5 c4 c5 e3 e6 cxd5 cxd4 dxe6 dxe3 Qxd8+ Kxd8 exf7 exf2+ Kxf2 Nf6 Bg5 Be6 Nf3 Bxf7 Nc3 Be7 Be2 Nc6 Rhd1+ Kc7 Bf4+ Kc8 Bb5 Bc5+ Kf1 Nb4 a3 Nc2 Rac1 Ne3+ Bxe3 Bxe3 Rc2 a6 Re2 axb5 Rxe3 Bc4+ Kg1 Re8 Rxe8+ Nxe8 Ne5 h6 Nxc4 bxc4 Rd4 b5 Nxb5 Kb7 Nd6+ Kb6 Nxc4+ Kc5",
    "e4 e5 Bc4 Qf6 d3 Bc5 Qf3 d6 Nc3 c6 Bg5 Qxf3 Nxf3 h6 Bh4 g5 Bg3 Nf6 h4 g4 Nd2 h5 f3 Nbd7 fxg4 Nxg4 Rf1 Ndf6 Nf3 Be6 Ng5 Bxc4 dxc4 O-O-O Bf2 Nxf2 Rxf2 Bxf2+ Kxf2 Ng4+ Kg1 Rdf8 Rf1 f6 Ne6 Kd7 Nxf8+ Rxf8 Rf5 Ke6 Rxh5 Ne3 Rh7 Nxc4 b3 Ne3 Rxb7 Nxc2 Rxa7 Rh8 g3 Rg8 Kh2 Ne3 Ra6 c5 Nb5 Rd8 h5 Ng4+ Kh3 f5 exf5+ Kxf5 Rxd6 Rxd6 Nxd6+ Kg5 a4 Nf6 a5 Nd7 Ne4+ Kxh5 a6 Kg6 a7 Nb6 Nxc5 Kf5 Nd7 e4 Nxb6 e3 a8=Q e2 Qf3+",
    "e4 c6 Bc4 d5 exd5 cxd5 Bb3 Nf6 c3 Nc6 d3 e5 h3 Bd6 Ne2 Be6 O-O O-O f4 e4 dxe4 Nxe4 Nd4 Kh8 f5 Nxd4 cxd4 Bd7 Bxd5 Bc6 Bxc6 bxc6 Nc3 Nxc3 bxc3 c5 d5 c4 Be3 Re8 Bd4 Be5 Bxe5 Rxe5 Qd4 Qb6 Qxb6 axb6 d6 Rd5 Rf4 b5 Rd4 Rxd4 cxd4 Rd8 Rb1 g6 Rxb5 Rxd6 fxg6 fxg6 Rc5 Rxd4 a4 c3 a5 Ra4 Rxc3 Rxa5 g4 Ra2 Rc7 h5 Rc6 Kg7 gxh5 gxh5 Rc4 Kg6 h4 Kf5 Rc5+ Kg4 Rc4+ Kg3 Re4 Ra1+ Re1 Rxe1#",
    "e4 e5 Nf3 Nc6 Bb5 Nf6 Bxc6 dxc6 d3 Bd6 O-O O-O Nbd2 b5 Qe2 a5 h3 Bd7 a4 b4 b3 c5 Nc4 Be7 Ncxe5 Be6 Bb2 Qc8 Rad1 h6 Kh2 Re8 Ng1 Nh7 f4 f6 Nef3 Bf7 f5 Bd6+ Kh1 c6 Bc1 Bc7 Qf2 Bh5 g4 Bf7 Nh4 Ng5 Bf4 Bxf4 Qxf4 Re7 Ng2 Qc7 Qxc7 Rxc7 h4 Nh7 Kh2 Re8 Kg3 Rd7 Nf4 Nf8 Nf3 Red8 g5 hxg5 hxg5 fxg5 Nxg5 Be8 Rh1 g6 Rh6 Kg7 Rdh1 Kf6 Nf3 Rg7 Kg4 Ke7 e5 Kd7 f6 Rf7 e6+ Kc7 exf7 Bxf7 Rh7 Rd7 Rxf7 Rxf7 Kg5 Kd6 Ne6",
    "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 e5 Nb3 a6 Nc3 b5 Nd5 Bb4+ c3 Bf8 Be3 Nf6 h3 Nxe4 Bd3 f5 Bxe4 fxe4 O-O Be7 Bc5 Bxc5 Nxc5 d6 Nxe4 Be6 Ne3 O-O Qxd6 Qxd6 Nxd6 e4 a3 Ne5 Nxe4 Nd3 Rad1 Nxb2 Rd2 Nc4 Nxc4 Bxc4 Nd6 Bxf1 Kxf1 Rad8 Ne4 Rxd2 Nxd2 Rc8 c4 bxc4 Ne4 c3 Nd6 c2 Ne4 c1=Q+ Ke2 Rc2+ Kf3",
    "d4 b6 e4 Bb7 Ba6 g6 Bxb7 Na6 Bxa8 Qxa8 Nc3 c6 Qe2 b5 Nf3 Qb7 O-O b4 Nd1 c5 dxc5 Nxc5 c3 Qxe4 Qxe4 Nxe4 Ne3 bxc3 bxc3 Nxc3 Bb2 Ne2+ Kh1 f5 Rfe1 Nf4 g3 Nh5 Ng5 Bh6 f4 Bxg5 fxg5 h6 Nd5 hxg5 Bxh8 Kf7 Bb2 e6 Nc3 Ngf6 Rad1 Ne4 Nxe4 fxe4 Rxd7+ Ke8 Rxa7 Nxg3+ hxg3 g4 Rc1 g5 Rc8#"
  )
}
