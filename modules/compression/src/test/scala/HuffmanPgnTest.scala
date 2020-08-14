package org.lichess.compression.game

import org.specs2.mutable._

class HuffmanPgnTest extends Specification {

  def hexToBytes(str: String) =
    str.grouped(2).map(cc => Integer.parseInt(cc, 16).toByte).toArray

  def base64ToBytes(str: String): Array[Byte] =
    java.util.Base64.getDecoder.decode(str)

  "game compression" should {
    "compress and decompress" in {
      forall(fixtures) { pgn =>
        val pgnMoves = pgn.split(" ")
        val encoded = Encoder.encode(pgnMoves)
        val decoded = Encoder.decode(encoded, pgnMoves.size)
        pgnMoves must_== decoded.pgnMoves
      }
    }

    "stable format" in {
      forall(v1 zip fixtures) { case (encoded, pgn) =>
        val pgnMoves = pgn.split(" ")
        val decoded = Encoder.decode(base64ToBytes(encoded), pgnMoves.size)
        pgnMoves must_== decoded.pgnMoves
      }
    }

    "least surprise" in {
      val n = 22
      val decoded = Encoder.decode(Array.fill(n)(0.toByte), n)
      decoded.pgnMoves.mkString(" ") must_== "e4 e5 Nf3 Nf6 Nxe5 Nxe4 Nxf7 Kxf7 d4 Nxf2 Kxf2 d5 Nc3 Nc6 Nxd5 Qxd5 Kg1 Nxd4 Qxd4 Qxd4+ Be3 Qxe3#"
    }

    "unmoved rooks" in {
      import scala.jdk.CollectionConverters._
      val pgnMoves = "d4 h5 c4 Rh6 Nf3 Rh8".split(" ")
      val encoded = Encoder.encode(pgnMoves)

      val d1 = Encoder.decode(encoded, 0)
      d1.unmovedRooks.asScala must_== Set(0, 7, 56, 63)

      val d2 = Encoder.decode(encoded, pgnMoves.size)
      d2.unmovedRooks.asScala must_== Set(0, 7, 56)
    }

    "half-move clock" in {
      val pgnMoves = "e4 e5 Nf3 Nc6 Nc3 Nf6 Bb5 d6 O-O Be7 d4 exd4 Nxd4 Bd7 Bg5 O-O Nxc6 bxc6 Bd3 h6 Bh4 Ne8 Bxe7 Qxe7 Qf3 Nf6 Rfe1 Rfe8".split(" ")
      val encoded = Encoder.encode(pgnMoves)
      val halfMoveClocks = List(0, 0, 0, 1, 2, 3, 4, 5, 0, 1, 2, 0, 0, 0, 1, 2, 3, 0, 0, 1, 0, 1, 2, 0, 0, 1, 2, 3, 4)
      (0 to pgnMoves.size).map(Encoder.decode(encoded, _).halfMoveClock) must_== halfMoveClocks
    }

    "last uci" in {
      val pgnMoves = "e4 e5 Nf3 Nc6 Bc4 Nf6 d4 exd4 O-O Bc5 e5 d5 exf6 dxc4 Re1+ Be6 Ng5 Qxf6 Nxe6 Qxe6".split(" ")
      val encoded = Encoder.encode(pgnMoves)

      val empty = Encoder.decode(encoded, 0)
      Option(empty.lastUci) must_== None

      val decoded = Encoder.decode(encoded, pgnMoves.size)
      Option(decoded.lastUci) must_== Some("f6e6")
    }

    "position hash 1. e4 d5 2. e5 f5 3. Ke2 Kf7" in {
      val pgnMoves = "e4 d5 e5 f5 Ke2 Kf7".split(" ")
      val encoded = Encoder.encode(pgnMoves)

      // initial position
      val d0 = Encoder.decode(encoded, 0)
      d0.positionHashes must_== hexToBytes("463b96")

      // 1. e4
      val d1 = Encoder.decode(encoded, 1)
      d1.positionHashes must_== hexToBytes("823c9b")

      // 1. e4 d5
      val d2 = Encoder.decode(encoded, 2)
      d2.positionHashes must_== hexToBytes("0756b9")

      // 1. e4 d5 2. e5
      val d3 = Encoder.decode(encoded, 3)
      d3.positionHashes must_== hexToBytes("662faf")

      // 1. e4 d5 2. e5 f5 (en passant matters)
      val d4 = Encoder.decode(encoded, 4)
      d4.positionHashes must_== hexToBytes("22a48b")

      // 1. e4 d5 2. e5 f5 3. Ke2
      val d5 = Encoder.decode(encoded, 5)
      d5.positionHashes must_== hexToBytes("652a60" + "22a48b")

      // 1. e4 d5 2. e5 f5 3. Ke2 Kf7
      val d6 = Encoder.decode(encoded, 6)
      d6.positionHashes must_== hexToBytes("00fdd3" + "652a60" + "22a48b")
    }

    "position hash 1. a4 b5 2. h4 b4 3. c4 bxc3 4. Ra3" in {
      val pgnMoves = "a4 b5 h4 b4 c4 bxc3 Ra3".split(" ")
      val encoded = Encoder.encode(pgnMoves)

      // 1. a4 b5 2. h4 b4 3. c4
      val d5 = Encoder.decode(encoded, 5)
      d5.positionHashes must_== hexToBytes("3c8123")

      // 1. a4 b5 2. h4 b4 3. c4 bxc3 4. Ra3
      val d7 = Encoder.decode(encoded, 7)
      d7.positionHashes must_== hexToBytes("5c3f9b" + "93d326")
    }

    "position hash threefold" in {
      // https://lichess.org/V0m3eSGN
      val pgnMoves = "Nf3 d5 d4 c5 dxc5 e6 c4 Bxc5 Nc3 Nf6 e3 O-O cxd5 Nxd5 Nxd5 Qxd5 Qxd5 exd5 Be2 Nc6 a3 Bf5 b4 Bb6 Bb2 Rfd8 Rd1 Rac8 O-O Ne7 Nd4 Bg6 Rc1 Rxc1 Rxc1 Nf5 Bf3 Kf8 Nb3 Nxe3 Bd4 Nc2 Bxb6 axb6 Bd1 Re8 Bxc2 Bxc2 Nd4 Bd3 f3 Bc4 Kf2 Re5 g4 g6 Rc3 Ke7 Re3 Kf6 h4 Rxe3 Kxe3 Ke5 f4+ Kd6 g5 Ke7 Nf3 Ke6 Nd4+ Ke7 Nf3 Ke6 Nd4+ Ke7".split(" ")
      val encoded = Encoder.encode(pgnMoves)
      val decoded = Encoder.decode(encoded, pgnMoves.size)

      val threefold = "966379"
      val ncheck = "65afff"
      val ke6 = "1bc865"
      val nf3 = "e804e3"
      val g5 = "ef8a0b"
      decoded.positionHashes must_== hexToBytes(threefold + ncheck + ke6 + nf3 + threefold + ncheck + ke6 + nf3 + threefold + g5)
    }

    "position hash compat" in {
      // https://lichess.org/DoqH1EQP
      val pgnMoves = "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nc6 Nc3 g6 Be3 Bg7 Bc4 Nf6 f3 O-O Qd2 Nd7 O-O-O a5 g4 Nce5 Be2 a4 a3 Nb6 h4 Nbc4 Bxc4 Nxc4 Qf2 Qb6 b3 Nxe3 Qxe3 e5 Nf5 Qxe3+ Nxe3 axb3 cxb3 Rxa3 Kb2 Ra6 h5 h6 hxg6 fxg6 Ned5 Rxf3 Ne7+ Kf7 Nxc8 Ke6 Nxd6 Rf2+ Kb1 Rxd6 Nd5 Rc6 Rc1 Rxc1+ Rxc1 Re2 Rc7 Rxe4 Nb6 Bf8 Rxb7 Rb4 Rb8 Rxb3+ Kc2 Rb5 Rxf8 Rxb6 Rg8 Kf6 Rf8+ Kg5 Rh8 Rd6 Re8 Kxg4 Rxe5 g5 Re3 Kf5".split(" ")
      val encoded = Encoder.encode(pgnMoves)
      val decoded = Encoder.decode(encoded, pgnMoves.size)
      decoded.positionHashes must_== base64ToBytes("oB9I1h1e6YDy")
    }

    "work with all black legal moves in YycayYfM" in {
      // Exclude compression as cause of issues with https://lichess.org/YycayYfM
      val prefix = "e4 c6 Nf3 d5 exd5 cxd5 d4 Nc6 c3 Nf6 Bf4 Bg4 Be2 e6 Nbd2 Bd6 Bxd6 Qxd6 O-O O-O Re1 a6 Ne5 Bxe2 Qxe2 Nd7 Nxd7 Qxd7 a4 Rab8 Nf3 b5 axb5 axb5 Ne5 Nxe5 Qxe5 b4 c4 dxc4 Rac1 Rbc8 Qa5 Qb7 Re2 c3 bxc3 bxc3 Rec2 Qe4 Qe5 Qxe5 dxe5 Rc5 f4 Rfc8 Kf2 f6 exf6 gxf6 Ke3"
      val legals = "Kh8 Kf8 Kg7 Kf7 Rf8 Re8 Rd8 Rb8 Ra8 R8c7 R8c6 R5c7 R5c6 Rh5 Rg5 Rf5 Re5+ Rd5 Rb5 Ra5 Rc4 h6 f5 e5 h5".split(" ")
      forall(legals) { legal =>
        val pgnMoves = (prefix + " " + legal).split(" ")
        val encoded = Encoder.encode(pgnMoves)
        val decoded = Encoder.decode(encoded, pgnMoves.size)
        pgnMoves must_== decoded.pgnMoves
      }
    }

    "work with CwdQG2Es" in {
      // Exclude compression as cause of https://github.com/ornicar/lila/issues/5594
      val prefix = "c4 e5 g3 h5 Nc3 h4 Bg2 Nf6 d3 Bb4 Bd2 d6 Nf3 h3 Bf1 Nc6 e3 Bg4 Be2 d5 Nxd5 Nxd5 cxd5 Qxd5 Bxb4 Nxb4 Qa4+ c6 Qxb4 Bxf3 Bxf3 Qxf3 Rg1 O-O-O Qe4 Qf6 O-O-O Rd5 f4 Rhd8 Rgf1 Qe6 Kb1 f5 Qc4 e4 d4 Kb8 Rc1 Qe7 Rg1 Qd7 Qc2 Re8 Qe2 Ra5 g4 g6 gxf5 gxf5 Qh5 Rd8 Qh6 c5 Rg7 Qa4 a3 Qb3 Qf6 Rc8 Qd6+ Ka8"
      val pgnMoves = s"$prefix Rxc5 Raxc5".split(" ")
      val encoded = Encoder.encode(pgnMoves)
      val decoded = Encoder.decode(encoded, pgnMoves.size)
      pgnMoves must_== decoded.pgnMoves
    }

    "pass perft test" in {
      // Running the entire suite can take minutes.

      PerftTest.trickyBatch();

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
    "d4 b6 e4 Bb7 Ba6 g6 Bxb7 Na6 Bxa8 Qxa8 Nc3 c6 Qe2 b5 Nf3 Qb7 O-O b4 Nd1 c5 dxc5 Nxc5 c3 Qxe4 Qxe4 Nxe4 Ne3 bxc3 bxc3 Nxc3 Bb2 Ne2+ Kh1 f5 Rfe1 Nf4 g3 Nh5 Ng5 Bh6 f4 Bxg5 fxg5 h6 Nd5 hxg5 Bxh8 Kf7 Bb2 e6 Nc3 Ngf6 Rad1 Ne4 Nxe4 fxe4 Rxd7+ Ke8 Rxa7 Nxg3+ hxg3 g4 Rc1 g5 Rc8#",
    "g3 c5 Bg2 d6 Nf3 e6 O-O Nf6 d3 Be7 Nbd2 h6 b3 a6 Bb2 Nbd7 Ne4 Nxe4 dxe4 Rb8 e5 d5 Nd2 b6 c4 dxc4 Nxc4 b5 Nd6+ Kf8 Bc6 Bb7 Bxb7 Qc7 Bg2 Bg5 Ne4 Be7 f4 Nb6 Rc1 Rd8 Qc2 Nd5 Nxc5 Ne3 Nxe6+ fxe6 Qxc7 Nxf1 Rxf1",
    "e4 b6 d3 Bb7 Nc3 d6 Be3 e5 Nf3 f6 Be2 g5 O-O h5 Qd2 g4 Ne1 Qd7 f3 Bh6 fxg4 Bxe3+ Qxe3 hxg4 Qg3 Qh7 Bxg4 Nh6 h3 Nxg4 Qxg4 Qg8",
    "e4 e6 Nf3 c6 Nc3 d5 exd5 cxd5 d4 Nf6 Bd3 Be7 Ne5 O-O O-O Nbd7 f4 a6 Kh1 b5 a3 Bb7 Qf3 h6 Qg3 Nh7 Ng4 f5 Nxh6+ Kh8 Qg6 gxh6 Qxe6 Rf6 Qe2 Rg6 Bxf5 Rg7 Qe6 Ndf6 Re1 Bd6 Qe3",
    "c4 c6 Nc3 e6 e4 Nf6 Nf3 Be7 d4 d5 cxd5 exd5 e5 Nfd7 Bd3 Na6 a3 Nc7 O-O Nb6 h3 Be6 Re1 Nd7 Qc2 c5",
    "e4 c6 d3 d5 Nc3 dxe4 dxe4 Nd7 Nf3 Ngf6 Bd3 e6 O-O Bd6 Re1 Qc7 Bg5 Ng4 g3 h6 Bd2 Nde5 Nxe5 Nxe5 Bf4 Nxd3 Bxd6 Qxd6 Qxd3 Qxd3 cxd3 O-O Kf1",
    "e4 e5 Qf3 Nf6 a4 Nc6 a5 Nxa5 Rxa5 c6 Rxe5+ Be7 Qg3 d6 Rxe7+ Qxe7 Qxg7 Rg8 Qh6 Qxe4+ Kd1 Bg4+ f3",
    "e4 e6 Nf3 d5 e5 c5 d3 Nc6 Nbd2 Nge7 b3 Ng6 Bb2 Qc7 Qe2 Be7 O-O-O O-O Ne4 dxe4 Qxe4 b5 d4 c4 bxc4 bxc4 Bxc4 Na5 Bd3 Bb7 Nh4 Bxe4 Bxe4 Rac8",
    "e4 e5 Nf3 Bc5 c3 f6 d4 Be7 Be3 Nc6 Nbd2 Nxd4 cxd4 exd4 Nxd4 Bb4 Be2 Qe7 O-O c5 Nf5 Qe5 Nc4 Qxe4 Nxg7+ Kd8 Qd6 Ne7 Ne6+ Ke8 Nc7+ Kf8 Bh6+ Kf7 Bh5+ Ng6 Bxg6+ Kxg6 Bf4 Qxc4 Nxa8 Qf7 Qd3+ f5 Qd6+ Qf6",
    "d4 d5 Bg5 Nd7 g3 Ngf6 Bh3 e6 Nc3 Be7 Bxf6 Nxf6 e3 a6 Nge2 b5 O-O O-O Qb1 c5 dxc5 Bxc5 b4 Bb6 a4 Bb7 a5 Bc7 Nd1 e5 c3 e4 f4 Bc8 Bxc8 Rxc8 Nd4 Bb8 Nf2 Qd7 Ra3 Ng4 Nxg4 Qxg4 Rf2 h5 Kg2 h4 Raa2 h3+ Kh1 Ba7 Rac2 Bxd4 cxd4 Rxc2 Qxc2 Rc8 Qb2",
    "d4 d5 f3 e6 Nh3 f5 g3 Nf6 e3 Be7 f4 c5 Qd2 cxd4 Qxd4 Nc6 Qd2 Bd7 Nc3 O-O Qe2 d4 Bd2 dxc3 Bxc3 b6 O-O-O Bc5 Kb1 b5 Bd4 Bxd4 exd4 Be8 c4 Bf7 d5 Ne7 d6 Nc6 cxb5 Nb8 d7 a6 Rc1 Nfxd7 Qd2 Nf6 Qxd8 Rxd8 bxa6 Nbd7 Bb5 Rxa6 Bxa6 Nc5 Bc4 Rc8 b4 Nce4 Bb3 Rd8 Rhd1 h6 Rxd8+ Kh7 Rdd1 Kg6 Rg1 Nd7 Rgf1 Nb6 Nf2 Nd6 Nd3 Nb5 Nc5 Nd5 Bxd5 exd5 Nd7 d4 Ne5+ Kf6 Nxf7 Kxf7 Rfd1 Nc3+ Rxc3 dxc3 Rc1 c2+ Rxc2 Kg6 Rc5 Kf6 h3 g6 Rc6+ Kf7 g4 fxg4 hxg4 g5 fxg5",
    "e4 e5 Nf3 Nc6 Bb5 d6 Bxc6+ bxc6 d4 Nf6 dxe5 dxe5 Qxd8+ Kxd8 Nxe5 Bd6 Nxf7+ Ke8 Nxh8 Bb7 f4 Ke7 e5 Bb4+ Nc3 Bxc3+ bxc3 Ne4 O-O Nxc3 Bd2 Ne2+ Kh1 Rxh8 f5 Rf8 Rae1 Nd4 Bg5+ Ke8 f6 g6 e6 Nf5 e7 Rf7 g4 Nxe7 Rxe7+ Kf8 Rxf7+ Kxf7 Re1 c5+ Kg1 Bd5",
    "e4 e6 c4 d5 exd5 exd5 cxd5 Nf6 b3 Nxd5 Bb2 Nf6 Nf3 Be7 Be2 O-O O-O h6 Re1 Re8 d4 Nbd7 Nc3 Nb6 Qc2 c6 Rad1 Nbd5 Ne5 Be6 Bd3",
    "e4 e5 Nc3 d6 Bc4 f5 exf5 Bxf5 d4 e4 d5 Nf6 Nge2 Nbd7 Ng3 Bg6 h4 Ne5 Be2 Bf7 h5 g6 h6 Nfg4 Be3 Nxe3 fxe3 Qf6 Qd2 Qe7 O-O-O O-O-O Rdf1 a6 Rf2 Be8 Rhf1 Bd7 Ngxe4 Ng4 Bxg4 Bxg4 Nf6 Bxh6 Nxg4 Bg5 Qe2 Rhe8",
    "d4 d5 Nf3 Nf6 e3 Bg4 h3 Bh5 g4 Bg6 Ne5 Nbd7 Nxg6 hxg6 Bg2 e6 a3 Nb6 g5 Nh5 e4 c6 exd5 Nxd5 Bxd5 cxd5 Nd2 Qxg5 Nf3 Qd8 Bg5 Be7 Bxe7 Qxe7 Qd3 O-O O-O-O Rac8 Ne5 Qc7 Rhg1 Nf4 Qd2 Nxh3 Rg3 Nxf2 Qxf2 Qb6 c3 Qb3 Kb1",
    "e4 c6 Nc3 d6 d4 g6 f4 Bg7 Nf3 Nf6 Bd3 O-O O-O Bg4 Qe1 Nbd7 Be3 c5 Rd1 cxd4 Bxd4 Qa5 h3 Bxf3 Rxf3 e5 Be3 exf4 Bxf4 Ne5 Bxe5 dxe5 Nd5 Qxe1+ Rxe1 Nxd5 exd5 f5 Bc4 e4 Rb3 b6 d6+ Kh8 d7 Rad8 Bb5 a6 Bc6 b5 Rd1 Rf6 Kf2 Rxc6 a4 Rc7 axb5 Rdxd7 Rxd7 Rxd7 bxa6 Bd4+ Ke2 Ra7 Rb8+ Kg7 Rb7+ Rxb7 axb7 Ba7 c4 Kf7 b4 Ke7 c5 Kd7 b5 Kc7 c6 Bb8 Ke3 Kb6 Kd4 Kxb5 Kd5 Kb6 Ke6 Kxc6 Kf7 f4 Kg7 e3 Kxh7 e2 Kxg6 e1=Q Kg5 Qg3+ Kf5 Qxg2 h4 f3",
    "e3 e6 b3 d5 Bb2 c5 h3 Nf6 g4 Bd6 h4 d4 h5 dxe3 dxe3 Nc6 h6 g6 g5 Rg8 Bxf6 Qb6 Nc3 e5 Nd5 Qa5+ Qd2 Qa3 Nf3 Nb4 Rd1 Be6 Bb5+ Nc6 Bxc6+ bxc6 Nc3 Be7 Ne4 Bxf6 Nxf6+ Ke7 Nxg8+ Rxg8 Qd7+ Kf8 Qd8#",
    "Nc3 Nf6 e4 d5 e5 d4 exf6 dxc3 fxg7 cxd2+ Qxd2 Qxd2+ Bxd2 Bxg7 O-O-O O-O Ne2 Nc6 Bc3 Bg4 f3 Bf5 Bxg7 Kxg7 Ng3 Bg6 Bd3 Rad8 Be4 Bxe4 Nxe4 b6 h4 e6 c3 Ne5 Rh3 Rxd1+ Kxd1 Rd8+ Kc2 f6 h5 Kf7 Rg3 Nc4 f4 Nd6 Nxd6+ cxd6 Kd3 f5 Rg5 Rg8 Kd4 e5+ Kd5",
    "e4 d5 exd5 Qxd5 Nc3 Qd7 Bc4 e6 Nf3 Bd6 O-O h6 d4 c6 Ne5 Bxe5 dxe5 Qxd1 Rxd1 Ne7 b4 O-O Rd3 b5 Rg3 Nf5 Rg4 bxc4 Ne4 Nd7 Rb1 Ba6 a4",
    "d4 d5 Nf3 Nd7 c3 e6 Bf4 Ngf6 e3 c5 Bd3 c4 Bc2 b5 Nbd2 b4 Qe2 a5 e4 a4 O-O dxe4 Nxe4 Ba6 Nd6+ Bxd6 Bxd6 b3 Bb1 Nd5 Re1 Ne7",
    "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Bc4 Bg7 f3 O-O Be3 Nc6 Qd2 Bd7 O-O-O Rc8 Bb3 a6 g4 Ne5 Rdf1 Nc4 Bxc4 Rxc4 Bh6 b5 Bxg7 Kxg7 g5 Nh5 b3 Rc5 f4 Qc7 Nd5 Rxd5 exd5 Rc8 f5 Qc3 Qxc3 Rxc3",
    "e3 Nf6 d4 g6 c4 Bg7 Nc3 d6 Nf3 O-O Be2 Bg4 h3 Bxf3 Bxf3 c6 O-O d5 Bd2 dxc4 Be2 b5 b3 cxb3 axb3 Nd5 Nxd5 Qxd5 Qc2 c5 dxc5 Bxa1 Bxb5 Bg7 Bc4 Qc6 Rd1 Qxc5 Bxf7+ Rxf7 Qxc5 Nd7 Qc7 Nf6 Rc1 Raf8 f3 Nd5 Qc5 Rf5 Qd4 Bxd4 exd4 Nf4 Bxf4 Rxf4 Rc4 R4f7 Ra4 Rf6 Rxa7 Rb6 Ra3 Rd8 Ra4 Rxb3 Kf2 Rd3 d5 R3xd5 Re4 e5 Kg3 Kf7 f4 exf4+ Rxf4+ Ke6 Re4+ Re5 Rf4 Rf5 Rg4 Kf6 Kh2 Rd3 Rd4 Rf2 Rd6+ Rxd6 Kg3 Rdd2 Kh4 Rxg2",
    "e4 e6 d4 d5 exd5 exd5 Nf3 Nf6 Be2 h6 Nc3 c6 Bd2 Bf5 a3 Bd6 Nh4 Bh7 Be3 Ne4 Nxe4 dxe4 g3 O-O Ng2 f5 Bc4+ Kh8 Nf4 Qb6 Bb3 a5 O-O Nd7 Ne6 Rf6 d5 c5 c4",
    "c4 e5 e4 Nc6 d3 a6 Nf3 d6 a3 Bg4 Be2 Qf6 O-O h5 Nc3 Be6 Nd5 Bxd5 cxd5 Nd4 Nxd4 exd4 Bxh5 g6 Bf3 O-O-O Bg4+ Kb8 b4 Bh6 b5 a5 Bd2 b6 Bxh6 Rxh6 a4 Ne7 Qc2 Qe5 h3 f5 Bf3 f4 Bg4",
    "e4 c6 Nf3 d5 exd5 cxd5 d4 Nc6 c4 dxc4 Bxc4 e6 O-O Nf6 Re1 Bb4 Nc3 O-O Bd2 a6 a3 Ba5 b4 Bb6 Be3",
    "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 e5 Nxc6 bxc6 Nc3 Bc5 Be3 Bxe3 fxe3 Qh4+ g3 Qd8 Qd2 Nf6 O-O-O O-O h3 a5 g4 h6 Qg2 Qb6 g5 Qxe3+ Kb1 hxg5 Be2 Rb8 h4 Qxc3 b3 a4 Bc4 a3 Kc1 Qa1+ Kd2 Qxa2 hxg5 Nh7 Qh3 Re8 Qxh7+ Kf8 Qh8+ Ke7 Qxg7 Rf8 Qf6+ Ke8 g6 fxg6 Rh8 Rxh8",
    "e4 e5 Nf3 Nf6 Bc4 Bc5 O-O O-O Nxe5 Nxe4 Re1 Qe8 d4 Bb4 Rxe4 d6 Bxf7+ Rxf7 Nxf7 Qxf7 Qe2 Qg6 Re8+ Kf7 Qe7#",
    "e4 g6 d4 d6 Nf3 Bg7 Be3 b6 c4 Ba6 b3 e6 Nc3 Ne7 Bd3 c5 d5 Bxc3+ Nd2 Bxa1 Qxa1 exd5 Qxh8+ Kd7 Qf6 dxc4 bxc4 Ng8 Qxf7+ Kc6 Qd5+ Kd7 e5 Nc6 Qxd6+ Kc8 Qxc6+ Qc7 Qe6+ Kb8 Be4 Bb7 Qxg8+ Qc8 Qxc8+ Bxc8 Bxa8 Kxa8 O-O Be6 f4 Kb7 g4",
    "e4 e6 d4 d5 e5 Nc6 c3 g6 Nf3 Bh6 Bxh6 Nxh6 Bd3 O-O Qd2 Nf5 h4 Qe7 h5 g5 Bxf5 exf5 Nxg5 f6 Nf3 Qg7 g3 f4 Qxf4 fxe5 Qg5 Qxg5 Nxg5 exd4 cxd4 Nxd4 Na3 Nf3+ Kf1 Nxg5 Kg2 Bh3+ Rxh3 Nxh3 Kxh3 Rxf2 b3 c5 Nb5 c4 bxc4 dxc4 Rc1 Rxa2 Rxc4 Ra5 Nd6 Rxh5+ Kg4",
    "e4 e6 d4 d6 Nc3 Nd7 Nf3 Ne7 Bd3 Ng6 Be3 Be7 Qd2 c6 O-O-O O-O h4 b5 h5 Nh8 Kb1 a5 e5 d5 Bh6 Re8 Rh3 f5 exf6 Nxf6 Rg3 Bf8 Ne5 Nf7 Nxf7 Kxf7 Bg5 h6 Bg6+ Kg8 Bxe8 Qxe8 Bxf6 Qxh5 Qf4 Qf5 Qxh6 Kf7",
    "d4 d6 e4 e5 dxe5 dxe5 Qxd8+ Kxd8 Nf3 Nc6 Bb5 Bd7 Nc3 f6 O-O g5 h3 h5 a3 a6 Bxc6 Bxc6 Rd1+ Ke8 Nd5 Bd6 c4 Ne7 Nxf6+ Kf7 Nd5 g4 Ng5+ Ke8 h4 Ng6 Nf6+ Ke7 Nd5+ Bxd5 cxd5 Nf4 Bxf4 exf4 Ne6 c6 Rac1 f3 g3 Rh6 Ng5 cxd5 exd5 Bc7 Re1+ Kd6 Rcd1 Bd8 Re6+ Rxe6 Nxe6",
    "e4 d5 exd5 Qxd5 Nc3 Qe5+ Qe2 Qxe2+ Bxe2 Bf5 Nd5 Kd8 c3 c6 Ne3 e6 Nxf5 exf5 Nf3 g6 Bc4 Ke7 O-O Nf6 Re1+ Kd7 Ne5+ Kc7 Nxf7 Rg8 Ng5 Rh8 Ne6+ Kc8 d3 Nbd7 Bg5 Be7",
    "e4 e5 Nf3 d6 d4 exd4 Qxd4 Nf6 Nc3 Nc6 Bb5 Bd7 Bxc6 Bxc6 O-O Be7 Re1 O-O Bf4 Nd7 Qd2 Bf6 Nd4 Ne5 Bxe5 Bxe5 Nxc6 bxc6 Rab1 g6 Ne2 Qh4 f4 Bg7 g3 Qf6 c3 a6 Nd4 c5 Nf3 Rab8 Re2 a5 Rc1 a4 a3 Rb3 Rc2 Rfb8 Qd5 Qe6 Qd3",
    "e4 e5 Nc3 Qe7 Bc4 c6 d3 h6 Nge2 a5 a3 g5 Ng3 Bg7 Nf5 Qf6 O-O d6 Bd2 Bxf5 exf5 Qxf5 Ne4 d5 Nd6+ Kd7 Nxf5 Nf6 Nxg7 dxc4 dxc4 Kc8 Nf5 Nbd7 Bc3 Rd8 Nd6+ Kb8 Nxf7 Rf8 Nxe5",
    "e4 e6 d4 c5 d5 exd5 Qxd5 d6 Bc4 Be6 Qd3 d5 exd5 Bxd5 Bxd5 Nf6 Bxb7 Qxd3 cxd3 Bd6 Bxa8 O-O Bf3 Nbd7 Nh3 Ne5 Be2 Rb8 Nc3 a6 O-O Nc6 b3 Nb4 Bb2 Nc2 Rac1 Nd4 Ne4 Nxe2+ Kh1 Nxc1 Rxc1 Nxe4 dxe4 Re8 f3 f6 Nf2 Rc8",
    "e4 c5 b3 Nc6 Bb2 d6 Bb5 Nf6 Qe2 e5 f4 a6 Bxc6+ bxc6 fxe5 dxe5 Nf3 Bd6 O-O O-O d3 Re8 Nbd2",
    "g3 d5 Bg2 e6 e4 Nf6 exd5 exd5 d4 Nc6 c3 Be6 h3 Be7 Bf3 Qd7 g4 O-O-O Ne2 a6 Nf4 Bd6 Be3 Rde8 Nd2 g5 Ng2 h6 h4 gxh4 Nxh4",
    "b4 e6 Bb2 d5 a3 Nf6 e3 Be7 Bxf6 O-O Bxe7 Qxe7 d4 b6 Nd2 Bb7 c4 Nd7 cxd5 exd5 Ngf3 c5 bxc5 bxc5 Nb3 c4 Nbd2 Bc6 Nb1 Rab8 Nc3 Rb3 Qc2 Rfb8 Rb1 Qxa3 Rxb3 Rxb3 Be2 Rxc3 Qb1 Rc1+ Qxc1 Qxc1+ Bd1 Qc3+ Nd2 a5 O-O a4 Nb1 Qb2 Bxa4 Bxa4 g3 c3 Kg2 c2 Na3 Qxa3",
    "e4 d6 Nf3 Bd7 e5 Qc8 Ng5 Nc6 Nxf7 Kxf7 exd6 cxd6 Bc4+ Ke8 Bxg8 Rxg8 O-O Nd8 Qh5+ g6 Qxh7 Rg7 Qh4 Be6 d3 Bf7 Nc3 g5 Qa4+ Nc6 d4 Be6 d5 Bd7 dxc6 Bxc6 Qd4 g4 Bf4 Rf7 Bg3 Bg7 Qd3 Bd7 Qh7",
    "c4 e6 Nf3 Nf6 Nc3 d5 d4 Be7 Bg5 Nbd7 e3 c6 c5 h6 Bh4 a5 a3 O-O Bd3 b6 b4 axb4 axb4 Rxa1 Qxa1 bxc5 bxc5 Nh7 Bxe7 Qxe7 Qb1 Nhf6 O-O e5 dxe5 Nxe5 Nxe5 Qxe5 Ne2",
    "d4 d5 c4 Nc6 Nc3 e5 e3 exd4 exd4 dxc4 Bxc4 Nf6 Nge2 Bb4 O-O Bg4 f3 Bh5 a3 Bd6 b4 O-O Qb3 Bg6 Bb2 Nxd4 Nxd4 Bxh2+ Kxh2 Qxd4 Ne4 Qd7 Rad1 Qf5 Ng3 Qf4 Rd4 Qh6+ Kg1 Rfe8 Rg4 Nxg4 fxg4 Qe3+ Kh2 Qxb3 Bxb3 Rad8 Nh5 Rd2 Bc1 Rd3 Bc4 Rc3 Bb5 c6 Ba4 b5 Bb2 Rce3 Bd1 a6 Nxg7 Bd3 Rf3",
    "e4 e5 f4 d6 fxe5 dxe5 Nf3 Nc6 Bc4 Be7 O-O Nf6 Ng5 O-O Qf3 Nd4 Qh3 h6 Rxf6 Bxh3 Nxf7 Ne2+ Kh1 Rxf7 Bxf7+ Kh8 gxh3 Bxf6 d3",
    "c4 e5 Nc3 Nf6 g3 Nc6 d3 d5 cxd5 Nxd5 Bg2 Be6 h4 Bb4 Bd2 Nxc3 Bxc6+ bxc6 bxc3 Ba3 h5 Qd5 Nf3 e4 dxe4 Qxe4 Rh4 Qf5",
    "e4 e5 Nf3 Nc6 Bc4 Bc5 d3 Nf6 Nc3 d6 Bg5 Bg4 Nd5 Nd4 h3 Bxf3 gxf3 c6 Nxf6+ gxf6 Be3 Ne6 Qd2 Qb6 O-O-O Rg8 Rhg1 O-O-O Bxe6+ fxe6 Bxc5 Qxc5 Qe3 Qxe3+ fxe3 Kd7 f4 Ke7 f5 d5 Rxg8 Rxg8 fxe6 Kxe6 exd5+ cxd5 Rd2 Rg3 Rh2 Rxe3 Kd2 Rg3 b3 d4 c3 Kd5 c4+ Ke6 a3 f5 b4 e4 dxe4 fxe4 h4 Rxa3 Rg2 Ra2+",
    "e4 e5 Nf3 Nc6 Bb5 Nf6 d3 a6 Ba4 b5 Bb3 Na5 c3 c6 Bc2 Bc5 b4 Bxf2+ Kxf2 Ng4+ Ke2 O-O bxa5",
    "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be3 e5 Nb3 Be6 f3 Be7 Qd2 Nbd7 g4 Qc7 O-O-O Rc8 g5 Nh5 Kb1 b5 Nd5 Bxd5 exd5 O-O Na5 Nb8 Bd3 f5 gxf6 Bxf6 Rhg1 Kh8 Qg2 Qxa5 Qg4 Nf4 Bxf4 exf4 Qh5 h6 Qg6 Kg8 Rde1 Be5 Qh7+ Kf7 Qf5+ Ke7 Rxg7+ Kd8 Rxe5 dxe5 Qxe5 Re8 Qd6+ Nd7 Qxd7#",
    "e4 e5 Nf3 Nc6 Bb5 Nf6 O-O Be7 d4 exd4 Nxd4 Nxd4 Qxd4 O-O Nc3 d6 h3 h6 Bf4 b6 e5 c5 Qc4 Be6 Qa4 Nd5 Nxd5 Bxd5 Rad1 Bh4 Rxd5 Qe7 Rxd6 Qe6 Bxh6 Rfd8 Rxd8+ Rxd8 Qxh4 gxh6 Qxd8+ Kg7",
    "e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 dxc6 Nxe5 Bd6 Nf3 Nf6 Nc3 Bc5 h3 O-O O-O b5 d4 Bb4 Bg5 Be7 Qd2",
    "Nf3 c5 g3 Nc6 Bg2 e5 Nc3 e4 Nh4 d5 b3 Be7 Bb2 Bxh4 gxh4 Qxh4 Nxd5 Qd8 Bxg7 Qxd5 Bxh8 f6 e3 Ne5 Qh5+ Kf8 d4 Bg4 Qh4 Nf3+ Bxf3 Bxf3 Bxf6 Bxh1 O-O-O Bf3 Be5",
    "e4 e5 Bc4 c6 Nf3 d5 exd5 cxd5 Bb3 e4 d3 exf3 Qxf3 Be6 Nc3 Nf6 Bg5 Bg4 Bxf6 Bxf3 Bxd8 Kxd8 gxf3 Bc5 Bxd5 Nc6 Bxf7 Rf8 Bb3 Rxf3 O-O-O Rxf2 Ne4 Be3+ Kb1 Nd4 Nxf2 Bxf2 Rhf1 Be3 Rfe1 Bg5 Rf1 Kc7 Rde1 a6 Rf7+ Kb6 c3",
    "f4 e6 b3 c6 Bb2 d5 Nf3 Nf6 e3 Nbd7 c4 b6 d3 Bb7 Be2 c5 O-O Qc7 Nbd2 Be7 Qc1 O-O Ne5 Nxe5 fxe5 Nd7 d4 Bg5 Rf3 Rad8 Rg3 Bh6 Nf3 dxc4 Bxc4 cxd4 Bxd4 Nc5 Ng5 Qe7 h4 Kh8 Qc2 g6",
    "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 Bc4 Bg7 Be3 e6 Nc3 Nge7 O-O O-O f4 a6 a4 Nxd4 Bxd4 Nc6 Bxg7 Kxg7 f5 exf5 exf5 Qe7 f6+ Qxf6 Rxf6 Kxf6 Nd5+ Kg7 Qf3 f5 Qf4 b5 axb5 axb5 Rxa8 Bb7 Rxf8 Kxf8 Bxb5 Kg7 Bxc6 Bxc6 Qe5+ Kf7 Qe7+ Kg8 Nf6+ Kh8 Qxh7#",
    "e4 e5 Nf3 Nc6 Bb5 d6 Nc3 f5 d4 fxe4 Nxe4 Bg4 d5 a6 Bxc6+ bxc6 dxc6 Nf6 Nxf6+ Qxf6 Bg5 Qg6 Qd2 Qe4+ Qe3 Bxf3 gxf3 Qxc6",
    "e4 c5 d3 d6 f4 Nf6 Nf3 g6 Nc3 Bg7 Bd2 O-O Be2 Nc6 O-O a6 h3 b5 g4 Qb6 Qc1 b4 Nd1 a5 Kh2 a4 f5 Bb7 Nf2 a3 b3 e6 Rb1 exf5 gxf5 Ne5 Nxe5 dxe5 Be3 Qc7 Ng4 Nxg4+ Bxg4 Rfd8 Bh6 f6 Bxg7 Qxg7 Rg1 g5 h4 h6 hxg5 hxg5 Kg3 Kf7 Bh5+ Ke7 Bg6 Rh8 Qe3 Rh6 Qxc5+ Kd7 Rh1 Rah8 Qb5+ Kc7 Qc4+ Kb8 Kg2 Rxh1 Rxh1 Rxh1 Kxh1 Qh6+ Kg2",
    "e4 d5 exd5 Qxd5 Nc3 Qa5 d4 c6 Nf3 Bf5 Bd3 e6 Bxf5 Qxf5 O-O Qa5 Bd2 Nd7 Qe2 Ngf6 a3 Qc7 Rfe1 Bd6 h3 O-O Ne4 Nxe4 Qxe4 Nf6 Qh4 Rfe8 c4 Be7 Bf4 Qd8 Be5 Nd7 Qe4 Nxe5 dxe5 Qb6 b4 c5 b5 Red8 a4 a5 Red1 Qc7 Qe2 b6 Qc2 h6 Rxd8+ Rxd8 Rd1 Rxd1+ Qxd1 Qd8 Qxd8+ Bxd8 Kf1 f6 Ke2 Bc7 exf6 gxf6 g4 Kf7 Ke3 Kg6 Ke4 Kf7 Nh4 Bd6 f4 Be7 f5",
    "d4 Nf6 c4 e6 Nf3 Bb4+ Bd2 Qe7 g3 Bxd2+ Qxd2 Nc6 Bg2 d5 O-O O-O b3 Ne4 Qc2 f5 Nbd2 Bd7 Nxe4 fxe4 Ne5 Nxd4 Qc3 Nxe2+",
    "e4 c5 f4 Nc6 Nf3 d6 d3 Bd7 Be3 Qc7 Nbd2 O-O-O a3 e6 g3 Be7 Bg2 f6 O-O h5 b4 h4 bxc5 hxg3 hxg3 dxc5 Rb1 g5 fxg5 f5 Bf4 e5 Be3 f4 Bf2 fxg3 Bxg3 Bxg5 Nc4 Nf6 Nxg5 Rdg8 Qd2 Ng4",
    "e4 e6 Nf3 g6 d4 Bh6 Bd3 d6 O-O Bd7 c4 Qe7 Nc3 Nc6 d5 exd5 exd5 Ne5 Re1 f6 Bxh6 Nxh6 Qd2 O-O-O Nd4 f5 Qxh6 Ng4 Rxe7 Rhe8 Qxh7 Rxe7 Qxe7 Re8 Qg5 f4 Qxf4 Nxh2 Kxh2 Rh8+ Kg1 Rh5 Bxg6 Re5 Qf8+",
    "d4 d5 Nf3 Nf6 e3 Nc6 Be2 Bf5 a3 e6 O-O Bd6 Ne1 e5 c3 e4 f4 O-O c4 Qe7 Nc3 Be6 cxd5 Bxd5 Bg4 Bc4 Rf2 Kh8",
    "e3 e5 d4 Nc6 d5 Nb4 e4 a6 a3 Nxd5 exd5 c6 c4 cxd5 cxd5 d6 Nc3 b6 Nge2 Bb7 g3 Nf6 Bg2 Qc8 Bg5 Be7 Rc1 h6 Bxf6 Bxf6 Ne4 Qd8 Nxf6+ Qxf6 O-O O-O f4 exf4 Nxf4 Qe7 Re1 Qc7 Rxc7 Rae8 Rxe8 Rxe8 Rxb7 Re5 Bh3 Kh7 Qc2+ g6 Rxf7+ Kg8 Qxg6+ Kh8 Qg7#",
    "e4 c6 Nf3 Qc7 d4 Nf6 Bd3 d6 O-O Bg4 c4 Nbd7 Nbd2 e5 d5 Be7 Qc2 h5 h3 Bxf3 Nxf3 O-O b3 cxd5 exd5 Nc5 Bf5 e4 Ne1 a5 Be3 b6 Bxc5 Qxc5 Bxe4 Qd4 Bd3 Qxa1 Nf3",
    "e4 d5 exd5 Qxd5 Nc3 Qd8 Nf3 Nf6 d4 e6 Bd3 Bb4 O-O O-O Be3 Nd5 a3 Bxc3 bxc3 Nxc3 Qd2 Nd5 Ne5 Nxe3 Qxe3 Nd7 Qh3 g6 Ng4 h5 Ne5 Nxe5 dxe5 Qg5 f4 Qh6 Qg3 Bd7 f5 exf5 Bxf5 Bxf5 Rxf5 Kh7 Rff1 Rae8 Rab1 b6 Rbc1 c5 Rcd1 Qg7 Rd7 Rxe5 h3 Rf5 Rxf5 gxf5 Qf3 Qd4+ Rxd4 cxd4 Qxf5+",
    "e4 d5 exd5 Qxd5 Nc3 Qa5 d4 e5 Nf3 Bg4 Be2 Bb4 Bd2 Nc6 Nxe5 Bxe2 Qxe2 Nxe5 dxe5 O-O-O a3 Ne7 O-O Bxc3 Bxc3 Qb6 Rad1 Nd5 Bd4 Nf4 Qg4+ Qe6 Qxf4 Rd5 Bc3 Rhd8 Rxd5 Rxd5 h3 h6 Qg4 Qxg4 hxg4 Kd7 Re1 c5 f4 g6 Kf2 b5 Re2",
    "e4 c5 f4 g6 Nf3 Bg7 a3 f6 c3 Qc7 e5 fxe5 fxe5 Bxe5 Nxe5 Qxe5+ Be2 d5 O-O Nf6 d4 Qd6 Bf4 Qb6 Be5 O-O dxc5 Qxc5+ Bd4 Qd6 Nd2 Nc6 Bf2 Bf5 Nf3 Ng4 Nd4 Qxh2#",
    "e4 d5 exd5 Qxd5 Nc3 Qd8 Nf3 Nf6 Be2 e6 O-O Be7 d4 O-O Bg5 b6 h3 Ba6 Bxa6 Nxa6 a3 c5 Qd3 cxd4 Qxd4 Qxd4 Nxd4 Rac8 Rad1 h6 Bh4 Nc5 Rfe1 g5 Bg3 Nh5 Bh2 Bd8 Ndb5 a6",
    "e4 c5 d3 Nc6 f4 e5 c3 d6 Nf3 Bg4 Be2 Bxf3 Bxf3 Nf6 O-O Be7 a4 O-O Na3 a6 Be3 Rc8 Nc4 b5 axb5 axb5 Nd2 Qd7 Qe2 h6 Ra6 b4 Rfa1 Rc7 Nc4 Rb7 fxe5 dxe5 Rxc6 Qxc6 Nxe5 Qe6 Nc4 Rbb8 e5",
    "e4 e5 Nf3 d6 Bc4 Bg4 O-O Qf6 h3 Bxf3 Qxf3 Qxf3 gxf3 Nc6 Nc3 Nd4 Bd3 c6 Ne2 Nxf3+ Kg2 Nh4+ Kg3 Ng6 Bc4 Nh6 d4 Be7 dxe5 Bh4+ Kh2 dxe5 Be3 b5 Bb3 O-O Bxh6 gxh6 Ng3",
    "g3 c6 Bg2 d5 e3 e6 Ne2 Nf6 d4 Be7 O-O O-O b3 h6 c4 b6 cxd5 exd5 Nbc3 Bb4 Bb2 Bxc3 Bxc3 Bf5 Nf4 Ne4 Bb4 Re8 f3 Nd6 Bh3 Bxh3 Nxh3 a5 Bxd6 Qxd6 Re1 c5 dxc5 bxc5 Nf4 d4 exd4 Rxe1+ Qxe1",
    "d4 d5 c4 c6 Nf3 Bg4 cxd5 cxd5 Qb3 Qd7 Ne5 Qc8 Nxg4 Qxg4 Qxb7 Qd7 Qxa8 Qc7 Nc3 e6 Bd2 Bb4 Rc1 Nf6 a3 Ba5 b4 Bb6 Nxd5 Qxc1+ Bxc1 O-O Nxf6+ gxf6 Bh6 Re8 e3 e5 Bb5 Rc8 O-O exd4 exd4 Bxd4 Qf3 f5 Qxf5 Rd8 Bd3",
    "c4 e5 g3 Nf6 Bg2 Nc6 e3 d6 Nc3 Be6 b3 Rb8 Nge2 a6 d4 exd4 exd4 Bf5 O-O Be7 Bg5 O-O Bxf6 Bxf6 d5 Ne5 Nd4 Bg6 f4 Nd7 Nde2 h6 Rc1 Bf5 Nd4 Bxd4+ Qxd4 Qf6 Qd2 Qd4+ Qxd4",
    "Nf3 d5 g3 Nf6 Bg2 c5 O-O Nc6 d4 cxd4 Nxd4 e5 Nxc6 bxc6 c4 Bb7 cxd5 cxd5 Nc3 Bc5 Bg5 Bd4 Bxf6 gxf6 Qb3 Bxc3 Qxb7 Bd4 Bxd5 O-O Qxa8 Qxa8 Bxa8 Rxa8 Rab1 Rb8 b4 Kf8 e3 Bc3 b5 Ke7 Rfc1 Ba5 a4 Rd8 Rd1 Rc8 Rbc1 Rb8 Rc6 Bb6 Kg2 Rg8 e4 h5 h4 Rg4 f3 Rg8",
    "e4 e6 f4 Ne7 Nf3 d5 exd5 exd5 Be2 Nf5 O-O Be7 d4 O-O Nc3 Nh4 Nxh4 Bxh4 Be3 Bf6 Qd3 c6 Bd2 Nd7 Rab1 Qc7 Nd1 c5 dxc5 Qxc5+ Be3 d4 Bf2 Qc7 Bxd4 Bxd4+ Qxd4 Nf6 Bd3 Bd7 Qf2 Bc6 Nc3 Qb6 Qxb6 axb6 a3 Nd5 Nxd5 Bxd5 c4 Bc6 f5 f6 Rbe1 Rfd8 Be4 Rd2 Bxc6 bxc6 Rf2 Rd3 Rf3 Rd2 Rf2 Rd4 Rf3 Kf7 Rb3 b5 cxb5 cxb5 Rxb5 Ra7 Ra1 Rd2 a4 Rad7 a5 Re7",
    "d4 Nf6 c4 g6 Nc3 Bg7 e4 O-O e5 Ne8 f4 d6 Nf3 dxe5 fxe5 f6 Be2 fxe5 dxe5 Qxd1+ Bxd1 Nc6 Bf4 e6 Bg3 Bd7 Bc2 Rxf3 gxf3 Nd4 O-O-O Nxf3 Rhf1 Nxe5 Bxe5 Bxe5 Nb5 Bxb5 cxb5 Nf6 Kb1 Nd5 Bb3 Ne3",
    "d4 b6 Nf3 Bb7 c4 Nf6 Nc3 g6 g3 Bg7 Bg2 O-O Nh4 d5 cxd5 Nxd5 Nxd5 Bxd5 e4 Bb7 Be3 e6 O-O Nd7 Rc1 Rc8 Qa4 a5 Qb5 Nf6 f3 Ra8 a4 Ba6 Qc6 Bxf1 Bxf1 Rc8 Ba6 Ra8 Bb7 Ra7 e5 Qxd4 Bxd4 Ne8 Qd7 Rxb7 Bc3 f6 Qe7 c5 Qxe6+ Kh8 exf6 Nxf6",
    "d4 f5 Nf3 Nf6 h4 e6 Ng5 d5 e3 c6 c3 Be7 Nd2 b5 Ndf3 a5 Ne5 Ne4 Ngf7 Qc7 Nxh8 Bf6 Qh5+ Kf8 Qxh7 Bxe5 Ng6+ Kf7 Nxe5+ Kf8 Qh8+ Ke7 Qxg7+ Kd8 Qf8#",
    "Nc3 Nf6 e4 d6 f4 g6 Nf3 Bg7 Bc4 c5 O-O O-O d3 Nc6 Kh1 a6 a3 b5 Ba2 Bb7 Nd5 Qc7 c4 Rad8 Rb1 b4 Nxc7",
    "d4 d5 c4 e6 a3 Nf6 Nc3 Be7 Bf4 Nh5 Be5 f6 Bg3 Nxg3 fxg3 dxc4 e4 c5 d5 O-O Bxc4 a6 Nf3 b5 Be2 c4 O-O Qb6+ Kh1 e5 Nh4 Rd8 Bg4 Bb7 Nf5 Bf8 Ne3 Nd7 Be6+ Kh8 Nf5 Nc5 Qg4 Nxe6 dxe6 Qxe6 Rad1 g6 Ne3 Qxg4 Nxg4 Rxd1 Rxd1 f5 exf5 gxf5 Nxe5 Bg7 Nf7+ Kg8 Rd7 Bc6 Rc7 Be8 Nd6 Rd8 Nxf5 Rd1+ Nxd1",
    "e4 e5 Nc3 Bb4 a3 Ba5 b4 Bb6 Na4 Nc6 Nxb6 axb6 Nf3 d6 Bc4 Bg4 h3 Bxf3 Qxf3 Qf6 Qb3 Nd4 Qa2 Qg6 O-O b5 Bd5 c6 Bb3 Nf6 d3 O-O Be3 Nf3+ Kh1 Nh4 Rg1 Qh5 c4 Ng6 Bd1 Qh4",
    "d4 g6 e3 Nf6 Bd3 d6 h3 Bg7 f4 b6 c4 Bb7 Nf3 O-O O-O c5 d5 Qc8 Nh4 e6 e4 h6 Nc3 a6 f5 gxf5 exf5 exf5 Nxf5 Qe8",
    "e4 e5 Nf3 d6 Bc4 Bg4 Nc3 Bxf3 Qxf3 Nf6 Nd5 Nbd7 Nxf6+ Nxf6 d3 Qd7 Bg5 Be7 Bxf6 O-O-O Bxe7 Qxe7 Bxf7 Rhf8 Qf5+ Qd7 Qxh7 Qxf7 Qh3+ Kb8 O-O-O Qf4+ Kb1",
    "e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 bxc6 Nxe5 Qe7 d4 d6 Nf3 Qxe4+ Be3 Nf6 Nc3 Qg6 O-O Bf5 Nh4 Qh5 Qxh5 Nxh5 Nxf5 g6 Ng3 Nxg3 fxg3 Bg7 Rae1 O-O",
    "e4 a6 Nf3 b5 Be2 Bb7 e5 Nc6 O-O e6 d4 Bb4 c3 Ba5 Bg5 Nge7 a4 h6 Bxe7 Qxe7 axb5 axb5 Bxb5 O-O Nbd2 Bb6 Qc2 Ra5 Bd3 Rfa8 Rxa5 Rxa5 Nc4 Ra2 Nxb6 cxb6 Qb3 Ra8 Qxb6 d5 Qb3 g5 h3 Na5 Qc2 Ba6 Bxa6 Rxa6 Qa4 Qb7",
    "e4 d5 exd5 Qxd5 Qf3 Qe6+ Qe3 Nc6 Qxe6 Bxe6 c3 Bf5 d4 O-O-O Be3 e5 dxe5 Nxe5 f3 b6 Nd2 Nd3+ Bxd3 Bxd3 Kf2 Nf6 Ne2 Bd6 Nb3 Rhe8 Rad1 Bc4 Nbc1 Nd5 Bd4 c5 Bxg7 Bxe2 Nxe2 Kc7 Rxd5 Bg3+ hxg3 Rxd5 Bh6 a5 Bf4+ Kc6 Rh6+ Kb7 Rxh7 Re7 g4 Red7 g3 Rd1 Be3 Ra1 a3 Ra2 Bc1 Ra1 g5 Kc6 Rh6+ Kb5 b3 Rb1 c4+ Ka6",
    "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g6 h3 Bg7 Nf3 O-O Bd3 a6 a4 Re8 O-O Nbd7 Bf4 Qc7 Rc1 Nh5 Bh2 Ne5 Nxe5 Bxe5 Bxe5 Rxe5 f4 Re8 e5 dxe5 f5 c4 Ne4 Bxf5 Rxf5 gxf5 Qxh5 fxe4 Qg5+ Kh8 Qf6+ Kg8 Bxe4 Qe7 Qf5 f6 Rc3 Kh8 d6 Qf7 Rxc4",
    "e4 e5 Bc4 Nf6 Nc3 Bb4 Nd5 Nxd5 Bxd5 O-O a3 Ba5 b4 Bb6 Qe2 c6 Bb3 d5 exd5 cxd5 Nf3 Bg4 h3 Bh5 g4 Bg6 d3 Nc6 Be3",
    "d4 d5 c4 Nf6 Nc3 Bf5 Nf3 Nc6 e3 e6 Be2 Bb4 O-O O-O cxd5 Nxd5 Bd2 a5 a3 Nxc3 bxc3 Bd6 c4 Qf6 c5 Be7 Rc1 Qg6 Kh1 Rfe8 Qb3 Bf6 Qxb7 Be4 Qxc7 e5 Qd7 exd4 exd4 Nxd4 Rfe1",
    "c4 e5 Nc3 Nf6 d3 Ng4 e4 f5 exf5 Bb4 Qxg4 h5 Qe2 d5 Qxe5+ Qe7 Qxe7+ Kxe7 Bd2 Bxf5 Nxd5+ Kd6 Nxb4 b5 g3 bxc4 Bg2 cxd3 Bxa8 Re8+ Kd1 h4 gxh4 c5 Nxd3 c4 Nf4 Ke5 Nge2 c3 bxc3 Nd7 Bc6 Kd6 Bxd7 Kxd7 Rb1 a5 Rb5 a4 Rxf5 a3 Ra5 Rb8 Rg1 Rb1+ Kc2 Rb6 Rxg7+ Kd6 Rg6+ Kc7 Rxb6 Kxb6 Rxa3 Kb5 c4+ Kxc4 Rd3 Kc5 Ne6+ Kc4 Rd8 Kb5 Kc3 Ka4 Kc4 Ka3 Rb8 Kxa2 Kc3 Ka1 Kc2",
    "c4 Nf6 Nc3 c5 e3 d6 d4 cxd4 exd4 a6 Nf3 Bg4 Be2 Bxf3 Bxf3 Nc6 O-O e6 Bg5 Be7 a3 O-O b4 e5 b5 axb5 cxb5 Nxd4 Bxb7 Rb8 Ba6 Qa5 Bd2 Nb3 Ra2 Nxd2 Qxd2 Qb6 a4 Nh5 a5 Qc5 b6 Nf4 Rb1 Qc6 f3 d5 Bb5 Qc5+ Kf1 d4 Ne4",
    "d3 Nf6 g3 g6 Bg2 Bg7 Nf3 O-O e3 d6 h4 h5 Nh2 c5 Nc3 a6 f3 Nc6 g4 hxg4 fxg4 Qd7 Bd2 Nxg4 Nxg4 Qxg4 Qxg4 Bxg4 Bh3 Bh5 Ne2 e6 Nf4 Bf3 Rg1 e5 Ne2 Nb4 Bxb4 cxb4 Ng3 f5 Rf1 Bg4 Bxg4 fxg4 O-O-O Rxf1 Rxf1 Rf8 Rxf8+ Bxf8 Ne4 Be7 Ng5 Bxg5 hxg5 Kf7 Kd2 Ke6 Ke2 Kf5 Kf2 Kxg5 Kg3 e4 dxe4 Kf6 Kxg4 b5 Kf4 a5 e5+ dxe5+ Ke4 a4",
    "e4 e5 Nf3 Nc6 Bc4 d6 Nc3 Nf6 d4 exd4 Nxd4 Bd7 Bf4 Be7 O-O O-O Nf3 Nh5 Be3 Bg4 h3 Bxf3 Qxf3 Ne5 Qxh5 Nxc4 Bc1 Bf6 Re1 Ne5 Bf4 Ng6 Re3 Bd4 Rg3 Nxf4 Qg4 Ng6 Rd1 Bxc3 bxc3 Qe7 Rd5 Qe6 Rg5 Qxg4 R3xg4 f6 Rh5 Ne5 Rf4 g6 Rhh4 g5",
    "d4 d5 c4 dxc4 Nc3 Nf6 Nf3 e6 a3 Nc6 g3 Be7 Bg2 O-O O-O Bd6 e4 e5 dxe5 Nxe5 Nxe5 Bxe5 Qxd8 Rxd8 Bg5 Rd6 Nd5 Nxd5 exd5 b5 Be7 Rd7 Bb4 Rxd5 Bxd5 Ba6 Bxa8",
    "e4 e5 Nf3 Nc6 c3 Nf6 d3 d6 Bg5 Bg4 h3 Bxf3 Qxf3 Be7 Bxf6 Bxf6 Nd2 Qe7 a3 O-O-O b4 Bg5 Nb3 b6 a4 h5 b5 Na5 Nxa5 bxa5 Be2 Qe6 O-O",
    "d4 e6 c4 d5 Nf3 c6 Nc3 Bd6 e3 f5 Bd3 Nf6 Qc2 O-O O-O Bd7 c5 Bc7 b4 Be8 Ng5 Ne4 Nxe6 Qh4 Nxf8 Qxh2#",
    "e4 b6 d4 Bb7 Bd3 e6 c4 Bb4+ Nc3 Bxc3+ bxc3 h6 Nf3 Nf6 Qe2 O-O O-O d6 h3 Nbd7 a4 e5 Re1 a5 Nh2 Nh7 d5 Nc5 Bc2 Bc8 f4 Qh4 Rf1 Nf6 fxe5 dxe5 Nf3 Qg3 Kh1",
    "e4 d5 exd5 Qxd5 Nc3 Qd8 Bc4 Nf6 d3 Bg4 f3 Bf5 Be3 e6 Nge2 c6 Ng3 Bg6 Qd2 Bd6 Nce4 Nxe4 Nxe4 Bc7 Bb3 Ba5 c3 Bxe4 fxe4 O-O O-O-O b5 h4 Bb6 d4 Nd7 g4 c5 g5 cxd4 Bxd4 Bxd4 Qxd4 Nb8 Qe3 Qc7"
  )

  val v1 = List(
    "7qasJezzPJK15lj9CbbYheEA63S9DE37qYM/HcONsibhbJM/2xJqSwr/nVAX79Rn3x/vsAA=",
    "KjTb/Zzt6FTIF/lVyHjtbeOzYeV9uhNzDfuV/699pPx/1XWiwVs31MA=",
    "Mp0orWLvti0lxmh6kBmGf5IqTYEAdXvgx/3Jnivwhju9A6ImWcvOcc9n1FmEwA==",
    "MhU6x0SImzC1OgAhmyHHSZLcNtUGucvp9TLlpoA=",
    "PDdknk9du7oA11Y1tRdCpolRK+yysDyJ9z1Q",
    "s3sbOnTq9vX15Npv7x4fJ97xFroPTbOLG+n9Q3639s5WH/7BQA==",
    "Hw15mn6XrBtZGTjK0A==",
    "Ugpnwa0n6QjIy8kUOHvF4vWAfNWGLlSpu4AGDXj+CUAfbz9YqN7Jq1sqLU2rqQmcfA==",
    "19Sn95fufWzCaFjfjA==",
    "Hw3FxvG9IvO817llUSMRAfV5xvfz/7Ez4kfrQWMgfg1NZz1n1D9gW0AqLQ==",
    "LM7AHuV0bWvXpOGmfCsWvct0pUMzO1AV0XTqwBX68pFIq0XP0rvp2g==",
    "zkmxSlt9/tqJ03f9B/Lbv8WnSSTNFLf11Wuz8dxe1//QCb9wv4i1/3JwIZWzXWaTwgMK9vGUr/ZVqt91WbzrlUA=",
    "CGtPQ+eYf80+X/0C63rNmndIuJCBKzcx5b4u4nm3ha8Ul3updJ5mjhqLG4eUexdgdHNLHYWqpOg=",
    "k8XmH/XqWTUtzb0FfAEODSU9gT2plavcE2l1IS+2kEcMmOdTr4P22vtsvWFnPMz9Zf3Ly1mBM0VfUV4TK/ZCZzu3AA==",
    "k+fO9m7ugAzYRzplfkRc++fc6BmXoIFCkQ9zO7c6",
    "BUL1i68rpzjsWPUJ2Pdk92pZJ6nCGB72yZwpuLCz0C7bn9wXPruaw0gbdav0rZ1ffFWg6A==",
    "LOthhPsUt3Z477iYLiHYfBdAEvKxfMyDjTPrr0K7AhTjndfDLSzmoMa8tqkb30g=",
    "AnSG1kzU5Yq/3W1r8pc++W3e8G/rdkvOFn93x/v+rkwd0PU11u9ULwgHmeCdv3BHXteI712hWYNcoA==",
    "PDfxJxP78de/6HhPgcrkATDkByNP67npuEF2FeGmmgiA",
    "mArWBeET583tvuNqHEQkAbLGO4J/OeGnlzebr+lIgaBv7cA=",
    "ty73VQUu24W1KDl7tqeOC+TKoueDkJ+WLfYnAkA=",
    "MF7OrJjpv1PueNH5uAZ4958JYA==",
    "KnCPQhSuzsnzxJbZrPH/mK3pkFuPLbPWc98A",
    "ysrQlz6bg1S5bgjzeeXJQA==",
    "LFb0KkXcNZ3f2PBtQhABf4A=",
    "Bg1PYNwxwIj/tXo=",
    "KmPn/CTPMXaQz7Uae8eQ4uevEMA=",
    "A618/WLNgHe98/vso2VXG9HicZOPF3sqzyMg",
    "kvv8yK3ZWdJ+Iw/Bbb+r1P9r7H0w3LUN/gza+Y7c9wRAHZA=",
    "k/0760KrEmRDhIzLFWHxrVuiH9Kr3+50x038r7eBXSSkP99u68l7WrvrGTlFQf6QKoMIXXUdWNE7AA==",
    "Andh9gQNgU0xiZBfIqcgy86PbN9q/+SAuig=",
    "K+8IExxnDUMbfTwtmne8O0su",
    "CVSUMTl6Sc8/JxJ+pfX1Xa7lah9meFtCcE9FwUA=",
    "kDe8/E+orRd88dwxtGhpTW9ARh5rEb/mYc4gmoA=",
    "LNob+KTfOVRolcRnunH82UEhJgrmfdm1znTvc+Bj8NIM1PQC1GHviouxfWOuL2gdrjoXyqS4",
    "VeEdjtGlsxcoa8Utvm92IsOEa866aNNLKkLxsA==",
    "qCefpoSA3lN73H4DrIi8nznXtZhWpe47p0iSzXItfUA=",
    "Hw3Gq73PyY3AB+unnhf0ucss",
    "kN+zTf+9SWT92zEowhgma9xPUYw=",
    "PBf5IfZfEMt13fvnNv5H38D16TuLwbXtaevbTQ==",
    "WGHCpp5fvgWYfrjTWMjJL8VKerZcArLrTuM12HsGwZ756ZZKwtz6OLf+Qdct0L7EveqUsA==",
    "KPwAiyY70yvnGX5k+YV/2S7n/VpPi9aA",
    "yCVgblV+5/46gWBj3m0/sxpu/kw2mIS118A=",
    "LI2BEkK/7hW4MfwSJF6A",
    "PDfxISLpH9w5P6PcHee9xw1ecXry8azxeIh/+MhwB+5iKMg=",
    "AKXNwe8rYUQnjecT",
    "Mijq//lj3oo9eBUDTsjcpBiZAnhnUgB7tu4A",
    "KPzZxNGQT1m2uxXaT8vP57BM2Av+FPkDJoxyeA/00A==",
    "KKlErsNbydzbX/cHneNuv2SepoijLP4Gl79+gA==",
    "ijQARzN4ef+W+bKHatW6bfjv17zlvQoB7dL/+vg0jvcLgA==",
    "Hw3/RL0zFev0EarLk7tLPO80aL0=",
    "Ar8BUYsLNYtZ8NrZA8/24fP35eGc1r1t+1nluSNy5A==",
    "Cc1+r2sEPuqnP64hvbaJANL98J6A",
    "KMeJCkLZxEJA0vfbz905zbTT635tQhDf3+rg",
    "PP2up0ynHt0htYcq6g==",
    "t3q/4EMWf6sv5lPOqqKb4I+GgA==",
    "wW+5xenkBqqSUBxw51HLWE/rnlVRT4+Bu+oW3I33qN1Q",
    "FjvmdkNJVB5+ZBO/Nf5b4Uxzexnq5edsx8Q=",
    "yrTf297tPr69zzvTWISECwPNwfgD4A==",
    "k+bvxZIbt0+pHi14k996oAV+85eGH/GdzCbIcfcaV/D8k/fEctOPbA==",
    "DGoG0zKWZ3Ri5UseardCAA==",
    "yNGyvxLz6rDSYlDJxgWfgA==",
    "AlLU3aquoXcZKTWrbo8hJWj7XP/hAA6+w958dn9g7uphyLQ=",
    "AnSvMbff/5P/TQW75A==",
    "PBf5IW1ElYqorh374xIv7ux2P9VkmXDA7Va/h1bMsZ3SKFeGgA==",
    "AnTzvDYHVmTFtxHydu/Lk5/R09nkJDQ=",
    "AncICtSWO5R+idYO",
    "3LKlNwT2x3SA/CYnfbjy7NIBG+uA",
    "BfkxwR9+wWh31do2pC7h8q0UHVH7PMny+KA=",
    "zqzheaUYv1au7CqrC6wsSW2s5sduyr8H+CVj0A==",
    "PDfxH171aqzx7y0S4PCb6pBTbe/ANIaTdQNE",
    "And38kC6LaCaV9dzuwA=",
    "PKX6DR2s9l8qbzZz61Zn797XGvrxshsFz90X9xBb37yb9ep6zlfxLfcaoNAnkA==",
    "Hw3FMUvXQy/dXu2s53zuhEtcTXf/L/Sbj4VvmbL2fO3CQcHee6F+b+dpVve4",
    "g1JW3bIVjsu9rz7XYKZw",
    "PGobF3d9dE6jaGJP+yCEu4/aR8akFFy9/A==",
    "Kn0wuuDGvJvHmuyxQQ/1x+HAO8UkcZjngA==",
    "kDS9c7zX14f2YOa7LSctZcc=",
    "Ux1gmtqcnDGFbj2m5uEdY0B7B+72GGdzdIhJn6oBaA==",
    "LIpO1gr+zHwWm1oZ9k3NHFbX7YYLoA==",
    "Hw15mlUuGWs52FMgbi2Yvmg5Ozu8QAm27/++feV2Nt6X9AA=",
    "Hw3FHmesQ7pDTLDtujGKhNkGkXmsdhDMyRnh5cA=",
    "PGsBef76/gAFdX35X7zII3VJOs4uYA==",
    "Hw15muXlxnv/mwEfKIGTLPmq1XxK/8zA",
    "PK38njqKyEf191+aZvDR72/lnF/8dzoBkn6w",
    "Aqu5uYBJFv8cH6q7TUjQb/8RscA=",
    "tbvq0pKw8Tzx/STLSbF/O4rP0loMSBOOaA==",
    "k+fZcHe+tUTJaqve4V5x/9ap3TPtZ8+tgdUuwA==",
    "yLDopokZvj0PPspQvBnxtK6xgB+8AA==",
    "3cIv1EaJIPehs793eyvZ5VQ8Zk3LI/z/bQtjLOzg7X38sag=",
    "K/7LAczy54Y4TVa+ROYn6Te8FIUzvFVZQ80G84nF3TV/0rGqxuSgf2qwdHA=",
    "g15tDk7MXCB+RJJpSKXTU5mQT0rd7YA=",
    "mE7/kser33E8CE1uut9fzQ/YuIeYrV5rMEMzQFv4bztZNgA=",
    "ngW6+L9e7v9CaENOjMwrsImE/A==",
    "qC/W53U/M5PmXjfyr08sfeYA",
    "k+K/u9Xh5jwkDGkzLdbe+5+1SbPF3/4vZld0Ijj8zZE4F/53rml7GgA=",
    "CXyx/+8NAXes0vnfG37yxKzsZOOcGP8F4Pg=",
    "mSl6sd++fLz19Kjl78v5vnFugwA=",
    "Aqu6BVSbVSeQl77Iw/9A",
    "AncJDmtxgp+9rG1IYJki3A==",
    "Gb5fqN92K1wXqTIAHat075BvSXYarXeHUwYg",
    "HwRZVB+8EdSBwxO/gzJtnefuJvzikOAkk+JY6wGuu/Zu/od+w8/219zQAA==",
    "g03g3QQ/JqZVa0zBYvmN6pCPo4piXKbAZFJ5z+5+qiA=",
    "BTers9z+p1zbk+G99SNLW0A=",
    "k+Te7N29ZINd882eyt4Z2flvRzHg3AbA",
    "yNKyHpcbseyQ9JfUnydJ2nmfh1iScNlQLWudN++/rRBXFoCqZnNZmIlSFheA",
    "ylZ7XEFp1gDUqZO4fsQ0DPFR8xWqmf/i6mveCeIu9/bz0A==",
    "7P9qO6ilfsxp1Y9nwnl4ANJG6VfJ8tD1/nKGAKG64EL7t9EIIXXqlKA=",
    "Albpi7O7qd0nDJ2QVpxky7SfzOwR+vIuDGbH2F3+qA==",
    "k+GyV5v1e84dMcCE22afckIDmA==",
    "AjRJW+QgSMR9fZwwbXHpXXA=",
    "lJ/o9i7j3b+eVTb/jdx/YA==",
    "MBXVjuQzk9ZbSKw/IzSfmL9/8WX+4bn+",
    "Hw15XVdxZr3eLz6xem8XLy8NLfv1/u0n/U0ifPQ="
  )
}
