package chess
package format.pgn

import scala._

class BinaryTest extends ChessTest {

  import BinaryTestData._
  import BinaryTestUtils._

  def compareStrAndBin(pgn: String) = {
    val bin = (Binary writeMoves pgn.split(' ').toList).get.toList
    ((Binary readMoves bin).get mkString " ") must_== pgn
    bin.size must be_<=(pgn.size)
  }

  "binary encoding" should {
    "util test" in {
      showByte(parseBinary("00000101")) must_== "00000101"
      showByte(parseBinary("10100000")) must_== "10100000"
    }
    "write single move" in {
      "simple pawn" in {
        writeMove("a1") must_== "00000000"
        writeMove("a2") must_== "00000001"
        writeMove("a3") must_== "00000010"
        writeMove("h4") must_== "00111011"
      }
      "simple piece" in {
        writeMove("Ka1") must_== "01000000,00100000"
        writeMove("Qa2") must_== "01000001,01000000"
        writeMove("Rh4") must_== "01111011,01100000"
      }
      "simple piece with capture" in {
        writeMove("Kxa1") must_== "01000000,00100100"
        writeMove("Qxa2") must_== "01000001,01000100"
        writeMove("Rxh4") must_== "01111011,01100100"
      }
      "simple piece with check" in {
        writeMove("Ka1+") must_== "01000000,00101000"
        writeMove("Qa2#") must_== "01000001,01010000"
        writeMove("Rxh4+") must_== "01111011,01101100"
      }
      "pawn capture" in {
        writeMove("bxa1") must_== "10000000,10000000"
        writeMove("gxh4") must_== "10111011,01000000"
      }
      "pawn capture with check" in {
        writeMove("bxa1+") must_== "10000000,10010000"
        writeMove("gxh4#") must_== "10111011,01100000"
      }
      "pawn promotion" in {
        writeMove("a1=Q") must_== "10000000,00000010"
        writeMove("h8=B") must_== "10111111,00001000"
        writeMove("h8=R") must_== "10111111,00000100"
      }
      "pawn promotion with check" in {
        writeMove("a1=Q+") must_== "10000000,00010010"
        writeMove("h8=B#") must_== "10111111,00101000"
      }
      "pawn promotion with capture" in {
        writeMove("bxa1=Q") must_== "10000000,10000010"
        writeMove("gxh8=B") must_== "10111111,01001000"
      }
      "pawn promotion with capture and check" in {
        writeMove("bxa1=Q+") must_== "10000000,10010010"
        writeMove("gxh8=B#") must_== "10111111,01101000"
      }
      "castling" in {
        writeMove("O-O") must_== "01000000,11000000"
        writeMove("O-O-O") must_== "01000000,11100000"
      }
      "castling with check" in {
        writeMove("O-O+") must_== "01000000,11001000"
        writeMove("O-O-O#") must_== "01000000,11110000"
      }
      "drop" in {
        writeMove("N@a1") must_== "01000000,10000010"
      }
      "drop with check" in {
        writeMove("N@a1+") must_== "01000000,10001010"
      }
      "drop with checkmate" in {
        writeMove("N@a1#") must_== "01000000,10010010"
      }
      "disambiguated by file" in {
        writeMove("Kfa1") must_== "11000000,00100000,00000101"
      }
      "disambiguated by file on h8" in {
        writeMove("Kfh8") must_== "11111111,00100000,00000101"
      }
      "disambiguated by rank" in {
        writeMove("K8a1") must_== "11000000,00100000,01000111"
      }
      "disambiguated fully" in {
        writeMove("Kf4a1") must_== "11000000,00100000,10101011"
      }
      "disambiguated fully with capture" in {
        writeMove("Kf4xa1") must_== "11000000,00100100,10101011"
      }
      "disambiguated fully with check" in {
        writeMove("Kf4a1+") must_== "11000000,00101000,10101011"
        writeMove("Kf4a1#") must_== "11000000,00110000,10101011"
      }
      "disambiguated fully with capture and check" in {
        writeMove("Kf4xa1+") must_== "11000000,00101100,10101011"
        writeMove("Kf4xa1#") must_== "11000000,00110100,10101011"
      }
      "disambiguated by rank with capture and check" in {
        writeMove("K8xa1+") must_== "11000000,00101100,01000111"
      }
    }
    "write many moves" in {
      "all games" in {
        forall(pgn200) { pgn =>
          val bin = (Binary writeMoves pgn.split(' ').toList).get
          bin.size must be_<=(pgn.size)
        }
      }
    }
    "read single move" in {
      "simple pawn" in {
        readMove("00000000") must_== "a1"
        readMove("00000001") must_== "a2"
        readMove("00000010") must_== "a3"
        readMove("00111011") must_== "h4"
      }
      "simple piece" in {
        readMove("01000000,00100000") must_== "Ka1"
        readMove("01000001,01000000") must_== "Qa2"
        readMove("01111011,01100000") must_== "Rh4"
      }
      "simple piece with capture" in {
        readMove("01000000,00100100") must_== "Kxa1"
        readMove("01000001,01000100") must_== "Qxa2"
        readMove("01111011,01100100") must_== "Rxh4"
      }
      "simple piece with check" in {
        readMove("01000000,00101000") must_== "Ka1+"
        readMove("01000001,01010000") must_== "Qa2#"
        readMove("01111011,01101100") must_== "Rxh4+"
      }
      "pawn capture" in {
        readMove("10000000,10000000") must_== "bxa1"
        readMove("10111011,01000000") must_== "gxh4"
      }
      "pawn capture with check" in {
        readMove("10000000,10010000") must_== "bxa1+"
        readMove("10111011,01100000") must_== "gxh4#"
      }
      "pawn promotion" in {
        readMove("10000000,00000010") must_== "a1=Q"
        readMove("10111111,00001000") must_== "h8=B"
        readMove("10111111,00000100") must_== "h8=R"
      }
      "pawn promotion with check" in {
        readMove("10000000,00010010") must_== "a1=Q+"
        readMove("10111111,00101000") must_== "h8=B#"
      }
      "pawn promotion with capture" in {
        readMove("10000000,10000010") must_== "bxa1=Q"
        readMove("10111111,01001000") must_== "gxh8=B"
      }
      "pawn promotion with capture and check" in {
        readMove("10000000,10010010") must_== "bxa1=Q+"
        readMove("10111111,01101000") must_== "gxh8=B#"
      }
      "castling" in {
        readMove("01000000,11000000") must_== "O-O"
        readMove("01000000,11100000") must_== "O-O-O"
      }
      "castling with check" in {
        readMove("01000000,11001000") must_== "O-O+"
        readMove("01000000,11110000") must_== "O-O-O#"
      }
      "drop" in {
        readMove("01000000,10000010") must_== "N@a1"
      }
      "drop with check" in {
        readMove("01000000,10001010") must_== "N@a1+"
      }
      "drop with checkmate" in {
        readMove("01000000,10010010") must_== "N@a1#"
      }
      "disambiguated by file" in {
        readMove("11000000,00100000,00000101") must_== "Kfa1"
      }
      "disambiguated by rank" in {
        readMove("11000000,00100000,01000111") must_== "K8a1"
      }
      "disambiguated fully" in {
        readMove("11000000,00100000,10101011") must_== "Kf4a1"
      }
      "disambiguated fully with capture" in {
        readMove("11000000,00100100,10101011") must_== "Kf4xa1"
      }
      "disambiguated fully with check" in {
        readMove("11000000,00101000,10101011") must_== "Kf4a1+"
        readMove("11000000,00110000,10101011") must_== "Kf4a1#"
      }
      "disambiguated fully with capture and check" in {
        readMove("11000000,00101100,10101011") must_== "Kf4xa1+"
        readMove("11000000,00110100,10101011") must_== "Kf4xa1#"
      }
      "disambiguated by rank with capture and check" in {
        readMove("11000000,00101100,01000111") must_== "K8xa1+"
      }
    }
    "be isomorphic" in {
      "for one" in {
        compareStrAndBin(pgn200(0))
      }
      "for all" in {
        forall(pgn200)(compareStrAndBin)
      }
    }
  }

}

object BinaryTestUtils {

  def showByte(b: Byte): String =
    "%08d" format {
      b & 0xff
    }.toBinaryString.toInt

  def writeMove(m: String): String =
    (Binary writeMove m).get map showByte mkString ","

  def readMove(m: String): String =
    readMoves(m).head

  def readMoves(m: String): List[String] =
    (Binary readMoves m.split(',').toList.map(parseBinary)).get

  def parseBinary(s: String): Byte = {
    var i    = s.length - 1
    var sum  = 0
    var mult = 1
    while (i >= 0) {
      s.charAt(i) match {
        case '1' => sum += mult
        case '0' =>
        case x   => sys error s"invalid binary literal: $x in $s"
      }
      mult *= 2
      i -= 1
    }
    sum.toByte
  }
}

object BinaryTestData {

  val pgn200: List[String] = augmentString(
    """
c7 Rd6+ Kb5 Rd5+ Kb4 Rd4+ Kb3 Rd3+ Kc2 Rd4 c8=R Ra4 Kb3
d4 Nc6 d5 Ne5 f4 Nc4 b3 Nb6 c4 Nh6 Nc3 e6 dxe6 dxe6 Be3 Nf5 Bxb6 Qxd1+ Kxd1 cxb6 Kd2 Bc5 g3 Bd7 Nf3 Ne3 Ne4 Nxf1+ Rhxf1 Bb4+ Kc2 O-O-O Rfd1 Kb8 Nc3 h5 Rd4 f6 Rad1 Kc7 Ne4 f5 Nd6 Bc6 Nf7 Rxd4 Rxd4 Rf8 Nf7g5 Rf6 h4 Be8 e3 Bc6 Ne5 Be7 Kc3 a6 b4 Bd6 a3 Be7 c5 b5 Nxc6 Kxc6 e4 b6 e5 Rg6 cxb6 Bxg5 Rd6+ Kb7 hxg5 Kb8 Rd7 a5 bxa5 b4+ Kxb4 Ka8 b7+ Kb8 a6 Ka7 Kb5 h4 Rd3 h3 Rd2 h2 Rxh2 Rxg5 fxg5 g6 Rh6 f4 gxf4 Kb8 Rxg6 Ka7 Rg8
e4 e5 f4 d5 exd5 exf4 Nf3 Nc6 dxc6 bxc6 Bc4 Nf6 d4 Bd6 O-O Qe7 Re1 Rb8 Rxe7+ Kxe7 Qe2+ Ne4 Qxe4+ Be6 Bxe6 fxe6 Ng5 Rhf8 Qxe6+ Kd8 Nf7+ Rxf7 Qxf7 Be7 Bxf4 Rxb2 Nc3 Rxc2 Re1 Rxa2 Qxe7+ Kc8 Qxc7#
e4 e5 Nf3 f6 Nc3 Bc5 Bc4 Ne7 d3 c6 Be3 Bb6 d4 d5 exd5 cxd5 Bb3 e4 Nd2 Be6 Ncxe4 Nbc6 Nc5 Bf7 c3 Nf5 Qf3 Nce7 Nxb7 Qc7 Nc5 Nh4 Qh3 g5 O-O-O Bh5 f3 Nef5 Bf2 Bg6 g3 Nxf3 Nxf3 h5 g4 Nd6 gxh5 Rxh5 Rde1+ Kf8 Ne6+
e4 e5 Nf3 Nc6 Nc3 Nf6 Be2 Bc5 Bc4 a6 O-O b5 Bd5 Bb7 a3 O-O d3 Ng4 Bg5 Bxf2+ Rxf2 Nxf2 Kxf2 Qe8 Kg1 b4 Ne2 bxa3 bxa3 Ne7 Bxe7 Qxe7 Bxb7 Rab8 Qb1 Qc5+ d4 exd4 Nexd4 d5 exd5 f5 Kh1 Rf6 a4 f4 Nc6 Re8 Qb3 Re3 Qb2 Rg6 Ng5 Rxg5 Nd4 f3 g3 f2 Rf1 Qa3 Qxa3 Rxa3 Ne6 Rf5 g4 Rff3 Kg2 g6 Bxa6 h5 Nxc7 hxg4 Bb5 g5 Rxf2 Rxf2+ Kxf2 g3+ hxg3 g4 d6 Rf3+ Kg2 Kf8 Bd3 Kf7 Bc4+ Kf6 Nb5 Ke5 Bd3 Kd5 a5 Rf7 c3 Kc6 Bg6 Rg7 Be8+ Kb7 d7 Kb8 d8=Q+ Kb7 Qd5+ Ka6 Qa8+ Ra7 Qxa7#
e4 e5 Nf3 Nf6 Nxe5 d6 Nf3 Nxe4 Qe2 Qe7 d3 Nf6 Bg5 Nbd7 Nc3 Qxe2+ Bxe2 Be7 O-O O-O h3 Ne5 Nxe5 dxe5 Rfe1 Re8 Bxf6 Bxf6 Nd5 Bd8 Bf1 c6 Nc3 Bc7 f4 Bd7 fxe5 Bxe5 Ne4 Bxb2 Rab1 Bd4+ Kh1 b5 c3 Bb6 d4 Be6 a3 Bd5 Bd3 Ba5 Re2 f5 Nd6 Rxe2 Bxe2 Bxc3 Nxf5 Re8 Bf3 Bxf3 gxf3 Rf8 Ne7+ Kh8 Nxc6 a6 Rd1 Rxf3 Kg2 Rf6 Ne5 Kg8 Ng4 Rf4 d5 Rd4 Rxd4 Bxd4 d6 Kf7 Kf3 Ke6 Ke4 Bb2
e4 e5 Nc3 Nc6 Nf3 Nh6 d4 exd4 Nxd4 Bc5 Nxc6 dxc6 Qxd8+ Kxd8 Bxh6 gxh6 O-O-O+ Bd7 Bc4 Bxf2 Bxf7 Ke7 Bh5 Be6 Rhf1 Rhf8 Kb1 Bc4 Rh1 a6 e5 Be6 Ne4 a5 Bf3 Ba7 b3 Bb6 Ng3 Rad8 Rxd8 Rxd8 Nh5 Rd2 Ng7 Bd7 Re1 Bd4 e6 Bf2 Re4 Be8 Nf5+ Kf6 Nxh6 Bc5 Ng4+ Kg6 e7 Kg7 Ne5 b5 Rg4+ Kf6 Nxc6 Bxe7 Rf4+ Kg5 Rg4+ Kf6 Nxa5 Kf5 Re4 Kf6 Rh4 Bg6 Rf4+ Bf5 Bg4 Rd5 Bxf5 Rxf5 Rxf5+ Kxf5 Nc6 Bc5 a4 bxa4 bxa4 Ke4 a5 Bd6 a6 Bc5 Nd8 Kd5 Nf7 Be3 Nd8 Ba7 Nf7 Be3 g4 Ba7 h4 c6 g5 Bf2 h5 Bc5 g6 hxg6 hxg6 Kd4 g7 Ba7 g8=Q Kc4 Nd6+ Kb4 Qg4+ Ka3 Qc4 c5 Qb3#
d4 Nf6 Nc3 d5 Bg5 Nbd7 e3 e6 Bd3 h6 Bf4 g5 Bg3 Bb4 Ne2 Nh5 O-O Nxg3 Nxg3 Nf6 h3 h5
d4 d5 c4 Nf6 cxd5 Nxd5 e4 Nb6 Nc3 e6 Nf3 Bb4 a3 Be7 Bb5+ Bd7 Bxd7+ Nb8xd7 O-O O-O h3 c5 Nb5 cxd4 Nbxd4 Bf6 Be3 Rc8 Rc1 Qe7 Nb5 a6 Nc3 Rfd8 Qb3 Bxc3 Rxc3 Rxc3 bxc3 Nc8 Bg5 f6 Bf4 Kh8 Qxb7 e5 Bc1 Nc5 Qxe7 Nxe7 Be3 Nxe4 Rb1 Nxc3 Rb6 a5 Ra6 a4 Bc5 Ned5 Nd2 h6 Nc4 Rc8 Ra5 Ne2+ Kf1 Nd4 Bxd4 exd4 Rxd5 Rxc4 Ke2 Kh7 Kd3 Rc3+ Kxd4 Rxa3 Ra5 Ra2 Kc4 Rxf2 Rxa4 Rxg2 Ra7 Rg3 Kd5 Rxh3 Ke6 Rg3 Kf5 h5 Kf4 h4 Re7 Kh6 Re3 g5+ Ke4 Rxe3+ Kxe3 Kh5 Kf3 g4+ Kf4 h3 Kg3 f5 Kh2 Kh4 Kg1 f4 Kf2 g3+ Kf3 g2 Kf2 f3 Kg1 f2+ Kxf2 Kg4 Kg1 Kf4 Kf2 Ke4 Kg3 Ke3 Kxh3 g1=Q Kh4 Kf4 Kh5 Qg5#
Nf3 d5 c4 dxc4 e4 Bg4 Bxc4 Bxf3 Qxf3 e6 Nc3 a6 b3 Nf6 Bb2 Bb4 O-O Nc6 Rad1 Qe7 a3 Bxc3 Bxc3 O-O-O d4 h5 d5 Ne5
f4 d6 Nf3 Nh6 Nc3 e6 e3 g6 Bb5+ c6 Bd3 Bg7 O-O O-O Nd4 Nd7 Ne4 e5 fxe5 Nxe5 Qe2 Bg4 Qf2 f5 Nc3 d5 Ne6 Qd6 Nxf8 Qxf8 Be2 Qb4 Qf4 Qxf4 Rxf4 Re8 Kf2 Bxe2 Kxe2 Nhg4 h3 Nf6 d3 a6 Bd2 Ned7 g4 Bh6 Rb4 b6 gxf5 gxf5 Rf1 a5 Rb3 b5 Rxf5 Kg7 Ra3 Kg6 Rf1 a4 h4 Bf8 h5+ Nxh5 Rg1+ Kf7 b4 Bxb4 Nxb5 cxb5 Bxb4 Nb6 Kf3 Nf6 Rc3 Re6 Rc5 Nfd7 Rxb5 Rf6+ Ke2 h5 Bc3 Re6 Rg7+ Ke8 Kf3 Nf6 Bd4 Nbd7 Rb7 h4 Ra7 Nb6 Bxb6 Rxb6 Ra8+ Rb8 Rxb8#
h4 h5 a4 a5 d4 d5 f3 f6 b3 e5 c3 c5 f4 e4 Ba3 Na6 Bxc5 Nxc5 dxc5 Bxc5 e3 Bxe3 Ne2 Bg4 c4 d4 Qc2 d3 Qc3 dxe2 Bxe2 Bd4 Qc2 Bxa1 Qxe4+ Qe7 Qg6+ Kd8 O-O Qxe2 Qxg7 Bd4+ Kh2 Qxf1 Qf8+ Kc7 Qxa8 Qg1+ Kg3 Bf2#
d4 d5 Nc3 Nf6 Bg5 c6 Nf3 e6 e4 dxe4 Nxe4 Be7 Nxf6+ Bxf6 Bxf6 Qxf6 Bd3 Nd7 O-O O-O Re1 Rd8 c3 Nf8 Bc2 Ng6 Rc1 a5 g3 b5 Qd3 Rd5 Ne5 Bb7 b3 Rad8 c4 bxc4 bxc4 Rd5d6 Nxg6 hxg6 Qb3 Ba6 c5 Rxd4 Qb6 Rd2 Qxa6 Qxf2+ Kh1 Qxh2#
a4 e5 b3 Nf6 c3 Bc5 b4 Bd6 b5 c6 bxc6 dxc6 a5 b6 axb6 Qxb6 e4 Nxe4 d3 Nf6 d4 exd4 cxd4 Bb4+ Bd2 Bxd2+ Nxd2 Qxd4 Ra4 Qe5+ Ne2 O-O f4 Qd6 Rd4 Qc5 h4 Bg4 Rc4 Qb5 Nd4
e4 e5 f3 Nc6 c3 a6 b4 Nf6 g4 h6 h4 d6 b5 axb5 Bxb5 Bd7 d4 exd4 cxd4 Nb4 Bxd7+ Qxd7 a3 Nc6 d5 Ne5 Bb2 Qb5 Bxe5 dxe5 Ne2 Bb4+ Nec3 Bxc3+ Nxc3 Qc5 Qb3 b6 Qb5+ Qxb5 Nxb5 Kd7 Rc1 Rhc8 O-O Ra5 Rb1 Ne8 Rb3 Rca8 Rc1 Rc8 Kf2 Nd6 Nxd6 cxd6 Rxc8 Kxc8 Rxb6 Kc7 Rb3 Rc5 Ke3 f6 Kd3 Rc1 Rc3+ Rxc3+ Kxc3 Kb6 Kb4 Ka6 a4 Kb6 a5+ Ka6 h5
d3 d5 e4 Nf6 e5 e6 exf6 c5 fxg7 Bxg7 Qg4 Bf6 Qf4 Nc6 c3 Be5 Qh6 Qf6 Qxf6 Bxf6 Bf4 Ne5 Bxe5 Bxe5 g3 Bg7 Ne2 Bd7 Bg2 b5 c4 bxc4 dxc4 Bxb2 cxd5 O-O-O dxe6 Bxe6 Nd2 Bxa1 O-O Bg7 Ne4 Kd7 Ng5 Ke7 Nxe6 fxe6 Nf4 Rc8 Bf3 e5 Nd3 c4 Nb4 c3 a3 c2 Rc1 e4 Nxc2 exf3 h3 Kf7 Kh2 Rhd8 g4 Be5+ Kg1 Rxc2 Rxc2 Rd1#
e4 Nf6 Nc3 e5 d4 d6 dxe5 Ng4 f3 Nc6 fxg4 Qh4+ g3 Qxg4 Be2 Qe6 exd6 Bxd6 Nf3 Bd7 b3 Ne5 Nxe5 Qxe5 Bc4 Qxc3+ Bd2 Qd4 Qf3 Qxa1+ Ke2 Qb2 Qxf7+ Kd8 Rc1 Bh3 Bg5+ Kc8 Be6+ Bxe6 Qxe6+ Kb8 a4 Rhf8 Be7 Qxc1 Bxf8 Qxc2+ Kf3 Qc3+ Kg2 Qe5 Qg8 Qb2+ Kh3 a5 Bxg7+ Ka7 Qxa8+ Kxa8 Bxb2 Ka7 Kh4 Kb8 Kh5 Kc8 Kh6 Kd7 Kxh7 c6 Kg8 Bc5 h4 Bd6 g4 Ke6 h5 Be7 h6 b5 h7 bxa4 h8=Q a3 Ba1 Bd8 Qh6+ Ke7 Qg5+ Kd7 Qf5+ Kd6 Qf4+ Ke6 Qd2 Be7 Qxa5 Kd7 Qa7+ Ke6 Qb6 Kd7 Qd4+ Ke6 Qc4+ Kd6 e5+ Kd7 e6+ Kd6 Qb4+ Kxe6 Qc4+ Kd6 Qa4 Ke6 Bc3 Kd7 Bb4 Bxb4 Qxb4 a2 Qa4 Kd6 Qxa2 Kc5 Qg2 Kb5 Qg3 Kb6 g5 Kb7 g6 Kc8 g7 Kd7 Kf8 c5 g8=Q Kc6 Qg8g6+ Kb5 Qc2 Kc6 Qgg6+ Kb7 Qd6 Ka7 Qdxc5+ Ka6 Qb4 Ka7 Qa2#
e4 c6 d4 d5 e5 Bf5 Nc3 e6 a3 c5 Bb5+ Nc6 Nf3 a6 Bxc6+ bxc6 O-O cxd4 Nxd4 Ne7 Bg5 Qc7 Re1 c5 Nxf5 Nxf5 Qd2 Be7 Bxe7 Qxe7 Na4 O-O Rab1 Nd4 c3 Nb3 Qc2 c4 Re3 Qd7 Nb6 Qb7 Nxa8 Rxa8 Rh3 h6 f4 Nc5 Rg3 Nd3 h3 Rb8 Rxd3 cxd3 Qxd3 a5 b4 axb4 axb4 Qa7+ Kh2 Qa2 f5 Qf2 fxe6 fxe6 Rf1 Qb6 Qg6 Rb7 Qe8+ Kh7 Rf3 Ra7 Rg3 Qc7 Qg6+ Kh8 Qxe6 Qb7 Qg6 Ra6 e6 Qe7 Qf5 Qxe6 Qf8+ Qg8 Qxg8+ Kxg8 Rd3 Rd6 c4 d4 c5 Rd8 c6 Kf7 c7 Rc8 Rxd4 Rxc7 b5 Ke6 b6 Rb7 Rb4 Kd5 Kg3 Kc5 Rb1 Rxb6 Rc1+ Kd6 Rd1+ Ke6 Kg4 g6 Rf1 Rb5 h4 Rf5 Re1+ Kf6 g3 h5+ Kh3 g5 Re4 g4+ Kg2 Re5 Rf4+ Rf5 Ra4 Ke5 Ra5+ Ke4 Ra4+ Ke3 Ra3+ Ke2 Ra2+ Kd3 Ra3+ Kc4 Ra8 Rb5 Rf8 Rb2+ Kg1 Kd3 Rd8+ Ke3 Kf1 Kf3 Rd3+ Ke4 Rc3 Rh2 Kg1 Ra2 Rc4+ Kf3 Rf4+ Ke2 Rf5 Ra3 Kg2 Ke3 Rxh5 Ra2+ Kg1 Kf3 Rg5 Kxg3 h5 Ra1#
e4 e5 Bc4 Nc6 Qf3 Nh6 d3 Nd4 Qd1 f6 c3 Nc6 Bxh6 gxh6 Qh5+ Ke7 Qf7+ Kd6 Qd5+ Ke7 Nf3 d6 Qf7#
e4 e6 d4 d5 Nc3 Nf6 Bg5 Be7 Bxf6 Bxf6 e5 Be7 Bb5+ c6 Ba4 a5 a3 O-O Nf3 b5 Bb3 a4 Ba2 b4 Nb1 Qb6 Nbd2 Ba6 axb4 Qxb4 Bb1 c5 dxc5 Bxc5 Qc1 Nc6 c3 Qg4 Rg1 Nxe5 Nxe5 Qe2#
e4 c5 e5 e6 Nc3 Nc6 Qe2 Nd4 Qd1 d6 exd6 Bxd6 Nf3 Nf6 Nxd4 cxd4 Nb5 e5 Nxd6+ Qxd6 d3 O-O Bd2 b5 a3 a5 Qe2 Bg4 f3 Be6 O-O-O b4 axb4 axb4 Kb1 Qa6 c3 Qa1+ Kc2 b3#
Nc3 b6 e3 Bb7 Nf3 Nc6 d4 h6 e4 g5 h4 g4 d5 Nb8 Nd4 h5 Bf4 Bg7 Bb5 Nf6 Nf5 Rg8 Qd2 e6 Nh6 Bxh6 Bxh6 Qe7 O-O-O c6 d6 Qd8 Bc4 b5 Be2 a5 Kb1 b4 Na4 Na6 Qd4 c5 Qd3 Bxe4 Qe3 Bf5 Bb5 Ne4 g3 Nb8 Qf4 Nc6 Bxc6 dxc6 d7+ Ke7 Qe5 f6 Qf4 Ra7 Rde1 Qxd7 Rd1 Qc7 Qe3 c4 Nc5 Nxc5 Qxc5+ Kf7 Qxc4 Rd8 Bf4 Qe7 Rde1 Rd5 Ka1 Qd7 Rhg1 Kg6 Rh1 Ra8 Rhf1 Re8 Rh1 Rd8 c3 Rd1+ Bc1 Rxe1 Rxe1 bxc3 bxc3 Kg7 Be3 Rb8 Bh6+ Kxh6 Qf4+ Kg7 Qxb8 Qd2 Rh1 Qxc3+ Qb2 Qxb2+ Kxb2 c5 Rg1 c4 Rc1 Bd3 Kc3
d4 d5 Nf3 e6 c4 Bb4+ Bd2 Qd6 Nc3 Bxc3 Bxc3 dxc4 e4 f6 Bxc4 c6 O-O a5 Re1 Ne7 e5 fxe5 Nxe5 O-O Qh5 b5 Bd3 Nf5 g4 g6 Nxg6 Re8
d4 d5 Bf4 Nf6 Bg5 Bg4 Bxf6 exf6 Qd2 Na6 e3 Bd6 Be2 Bb4 c3 Bxe2 Qxe2 Be7 Nf3 c6 Nbd2 Bd6 O-O h5 g3 Qd7 Qd3 O-O-O Nh4 Qg4 Qf5+ Kb8 Qxg4 hxg4 a4 g5 Ng2 Rh3 b4 Nc7 a5 Ne6 f3 gxf3 Rxf3 Be7 g4 Rhh8 Raf1 Rde8 Rf5 Reg8 Nf3 Rc8 Nd2 Bd6 Nf3 Rh3 Kf2 Rf8 Rh1 Bg3+ Kf1 Bc7 Rxf6 Ka8 Rf5 Bd6 Nxg5 Nxg5 Rxg5 Rfh8 Rh5 Kb8 Rxh8+ Rxh8 Ke2 Kc7 Kd3 Bxh2 Nf4 Kd7 Nh5 Bc7 Rf1 Ke7 e4 b6 exd5 Rg8 Rg1 Rg6 Rg2 cxd5 c4 dxc4+ Kxc4 Kd7 Kb5 Bd6 Ka6 Bc7 Kxa7 Rc6 axb6 Rxb6 Rf2 Rxb4 Rxf7+ Kd8 Rf8+ Ke7 Rg8 Bd6 Rg7+ Ke8 g5 Ra4+ Kb6 Rxd4 Kc6 Kf8 Rg6 Ke7 Ng7 Bf4 Nf5+ Kf8 Nxd4 Be3 Nf5 Bc1 Rg7 Bf4 g6 Be5 Rh7 Bb2 Rb7 Be5 Kd7 Kg8 Ke6 Bf4 Rd7 Bg5 Ra7 Bd8 Ra8 Kh8 Rxd8#
e4 e6 d4 d5 e5 a6 Nf3 c5 c3 Nc6 a3 Qb6 b4 cxd4 cxd4 Nge7 Bd3 Ng6 O-O Be7 Nc3 O-O Be3 Bd7 Na4 Qc7 Rc1 b5 Nc5 Bxc5 Rxc5 Qb6 Nd2 Nce7 g4 Rac8 f4 Rxc5 dxc5 Qc7 Nb3 Nc6 Nd4 f6 exf6 Rxf6 Nxc6 Bxc6 Bxg6 Rxg6 Bd4 Qf7 Kh1 Rh6 Qe2 Qg6 f5 exf5 Rxf5 Rh4 Qf3 h6 h3 Kh7 Kh2
e4 g6 Bc4 Bg7 a3 e6 d3 Ne7 h3 O-O Nf3 d5 exd5 exd5 Ba2 c5 Nc3 Nbc6 g4 Be6 Bf4 a6 Qd2 b5 h4 b4 axb4 Nxb4 h5 Nxa2 Rxa2 Nc6 hxg6 fxg6 Ra1 Bxg4 Nh2 Bf5 Bh6 Qe7+ Ne2 Nd4 Bxg7 Kxg7 Ng4 Nf3+
d4 Nf6 c4 Nc6 Nc3 h6 Nf3 d5 e3 e6 Be2 Be7 Bd2 Bd7 a3 a6 O-O O-O b4 Qe8 c5 Ne4 Be1 f5 b5 axb5 Nxb5 Rc8 a4 Qg6 a5 f4 a6 fxe3 fxe3 Rb8 Nxc7 bxa6 Bxa6 Rb2 Be2 Bd8 Nb5 e5 Rb1 Rxb1 Qxb1 exd4 exd4 Be7 Bd3 Qg4 Nc3 Ng5 Bg3 Rxf3 gxf3 Qxd4+ Kh1 Bxc5 Ne2 Qa4 Bg6 Qc4 Nf4 Be3 Ng2 Bc5 Rc1 Qd4 Qc2 Bb4 Qa4 Nxf3 Be1 Qg1#
Nc3 d6 e3 c5 Nh3 g6 Nf4 e5 Nd3 f5 Be2 e4 Nf4 Be7 Nfd5 g5 Nb5 Bd7 Nbc7+
Nf3 d6 g3 e5 d3 f6 Bg2 Nh6
e4 Nf6 Nc3 d5 e5 Bg4 f3 Bf5 exf6 exf6 d4 Bb4 a3 Be7 Be3 Nc6 Nh3 O-O Nf4 Re8 Nfxd5 a6 Bd3 Bxd3 cxd3 f5 O-O Bd6 Qb3 Qc8 Rae1 Qb8 Bg5 Rxe1 Rxe1 Nxd4 Qd1 f6 Be3 c5 b4 cxb4 axb4 Qa7 Bxd4 Qxd4+ Kh1 Rc8 Ne7+ Bxe7 Rxe7 Rxc3 Qe1 Qxb4 Re8+ Kf7 Qe6+ Kg6 Qe1 Qb2 Qg3+ Kh6 Re1 g5 h4 f4 hxg5+ Kg7 gxf6+ Kh6 Qxf4+ Kh5 Qe5+ Kg6 f7 Kxf7 Qe7+ Kg8 Qe8+ Kg7 Re7+ Kh6 Re6+ Kg7 Qe7+ Kg8 Qe8+ Kg7 Re7+ Kf6 Qf7+ Kg5 f4+ Kg4 Qe6+ Kg3 Qe3+ Kg4 Qf3+ Kf5 g4+ Kf6 Re1 Kg7 Qg3 Qa3 f5 Qc5 g5 Qd5+ Kh2 Rc2+ Kh3 Qxf5+ Kh4 b5 Re5 Qf7 d4 b4 Qg4 Rh2+ Kg3 Qf2#
e4 e5 Qh5 Nf6 Qxe5+ Qe7 d4 Nc6 Qf4 d6 Qe3 Qxe4 Qxe4+ Nxe4 f3 Nxd4 Na3 d5 fxe4 Bxa3 bxa3 Nxc2+ Kd2 Nxa1 Bb2 dxe4 Bxa1 Be6 Bxg7 Rg8 Bh6 Bd5 Bb5+ c6 Be2 Rxg2 Nh3 O-O-O Ke1 Bxa2 Rf1 Rxh2 Bg4+ Be6 Bxe6+ fxe6 Ng5 Rxh6 Nf7 Rh3 Nxd8 Kxd8 a4 Ra3 Rf8+ Ke7 Rf4 Rxa4 Ke2 Kd6 Rf6 Ra3 Rf7 b5 Rxh7 b4 Rh1 Kd5 Rb1 Kc4 Kd2 b3 Kc1 Ra2 Kd1 Kc3 Rc1+ Rc2 Rb1 b2 Rxb2 Rxb2 Kc1 e3 Kd1 Rc2 Ke1 e2 Kf2 Kd2 Kf3 e1=Q
g4
e4 e5 Nf3 Nc6 Bc4 Bc5 c3 d6 O-O h6 d4 Bb6 dxe5 Nxe5 Nxe5 dxe5 Bxf7+ Ke7 Qh5 Qd6 Rd1 Qf6 Bb3 Bxf2+ Kh1 Be6 Rf1 Rf8 Be3 g6 Qe2 Bxb3 Rxf2 Qc6 axb3 Qxe4 Bc5+
b3 e6 e3 Qf6 Nf3 Qxa1 c3 Qxb1 d3 Qxa2 Nd4 Be7 c4 Bh4 g3 e5 Nf5 d5 Nxh4 Nc6 cxd5 Nb4 Qh5 Nc2+ Kd1 Qxb3 Qxe5+ Ne7 Ng2 Qxd3+ Bxd3
e4 e5 Nf3 Nf6 Bc4 Nc6 O-O Nxe4 Re1 Nc5 d4 d5 Bb5 Nd7 dxe5 Bb4 c3 Ba5 e6 Nc5 exf7+ Kxf7 Ng5+ Kg8 Bxc6 bxc6 Qe2 Bf5 b4 Rb8 Be3 d4 Bxd4 Na4 bxa5 h6 Ne6 Qd5 Qe5 Qxe5 Bxe5 Bxe6 Bxc7 Kf7 Re3 Rb7 Bd6 Rb2 Na3 Rd8 Bb4 Rbd2 Rae1 Re8 Nc4 Bxc4 Rxe8 c5 Ba3 Kg6 h3 Bf7 Re8e3 Bc4 Bc1 Rd6 Re4 Bb5 c4 Bd7 Bf4 Rd4 Rxd4 cxd4 Rd1 Be6 Rxd4 Nc5 Rd6 Kf5 Be3 Na4 c5 Bxa2 c6 a6 c7 Nb6 Rxb6 Ke5 c8=Q g6 Qc5+ Ke4 Qd4+ Kf5 Rf6#
e4 c5
e4 c5 Nf3 d6 Bb5+ Nc6 d4 cxd4 Nxd4 Bd7 O-O Nf6 Nc3 g6 Bg5 Rc8 Re1 h6 Bh4 g5 Bg3 e6 Nf3 g4 Nd4 Be7 Nxc6 Bxc6 Bxc6+ Rxc6 e5 dxe5 Bxe5 Qxd1 Raxd1 O-O Re2 Rd8
e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O b5 Bb3 Bc5 Nc3 Na5 Nxe5
d4 d5 c4 e6 Nc3 f5 e3 dxc4 Bxc4 e5 Bb3 exd4 exd4 f4 Nf3 c5 Bxf4 cxd4 Nxd4 Bc5 Be3 Bf5 O-O Bg6 Re1 h5 Bg5+ Kd7 Bxd8 Kxd8 Nf3+ Kc7 Ne5
e4 d5 exd5 Nf6 Bb5+ Bd7 Bc4 c6 dxc6 Nxc6 Nf3 h6 O-O e6 d4 Bd6 Be2 Qc7 c4 O-O Nc3 Ng4 h3 Bh2+ Kh1
e4 Nf6 f3 d5 d3 d4 f4 c5 f5 Nc6 g4 e5 Bd2 a5 Na3 b6 Nb5 Ba6 c4 g6 g5 Nd7 f6 h6 Qf3 h5 h4 Nb4 O-O-O Nxa2+ Kb1 Nb4 b3 Bxb5 cxb5 a4 bxa4 Rxa4 Ne2 Ra3 Nc1 Qa8 Bh3 Ra1+ Kb2 Qa3#
e4 e6 d4 d5 e5 c5 c3 Nc6 Nf3 Qb6 Be3 Qxb2 Nbd2 Qxc3 Rc1 Qa5 dxc5 Nge7 Qc2 Nf5 Qb3 Be7
f3 Nc6 c4 Nf6 d3 d6 e4 Nd4 f4 Bg4 Qd2 e5 Qe3 Nc2+ Kd2 Nxe3
d4 d5 c4 Nf6 f4 Be6 cxd5 Bxd5 e3 Ne4 Nc3 e6 Nxd5 exd5 Qa4+ c6 Qb3 Qa5+ Ke2 Qa6+ Kf3 Qxf1+
e4 e5 Nf3 d6 Bc4 Be6 Bxe6 fxe6 Nc3 Qd7 d3 Nc6 Bg5 Be7 Qd2 h6 Bh4 g5 Bg3 Nd4 Nxd4 exd4 Ne2 e5 O-O-O O-O-O h4 g4 f3 h5 fxg4 hxg4 h5 Rf8 h6 Rf6 h7 Nh6 Rxh6 Rxh6 Qxh6 Qd8 Kb1 Bg5 Qg7 Qf6 Qxf6 Bxf6 Rh1 Kd7 a3 Ke7 Rh6 Kf7 Nc1 Kg7 Rh1 Rxh7 Rxh7+ Kxh7 Nb3 Kg7 Na5 b6 Nc6 a5 b4 axb4 Nxb4 Kf7 Nd5 Bd8 Kb2 Ke6 Kb3 Kd7 Kb4 c6 Nc3 dxc3 Kxc3 c5 Kc4 Kc6 a4 Bc7 Bh4 Bb8 Be7 Kd7 Bf6 Bc7 c3 Ke6 Bg7 Kf7 Bh8 Kg6 d4 Kh5 g3 Bd8 dxe5 dxe5 Bxe5 Kg6 Kb5 Kf7 Kc6 Ke6 Bc7 Bf6 Bxb6 Bxc3 a5 Bxa5 Bxa5 Ke5 Kxc5 Kxe4 Bc7 Kf3 Kd4 Kg2 Ke4 Kh3 Kf4 Kg2 Kxg4
d4 Nf6 Bg5 Ne4 Bc1 f5 f3 e5 fxe4 Qh4+ g3 Qxe4 Nf3 Bb4+ c3 Bd6 Bg2 Qg4 O-O e4 Ne5 Bxe5 dxe5 Nc6 Bf4 h5 h3 Qg6 e3 h4 Qe1 d6 exd6 cxd6 Nd2 Be6 Nb3 Bc4 Nd4 Ne5 Qf2 Bxf1 Rxf1 hxg3 Bxg3 Nf3+ Bxf3 Rxh3 Nxf5 Qxf5 Bg2 Qxf2+ Bxf2
e4 e5 d4 Bd6 Bc4 Nf6 Qf3 Nc6 dxe5 Nxe5 Qb3 Nxc4 Qxc4 O-O h4 Be5 Nc3 d6 Bg5 Be6 Qe2 Re8 O-O-O Qc8 Nd5 Nxd5 exd5 Bg4 f3 Bxb2+ Kxb2 Rxe2 fxg4 Rxg2 h5 Qxg4 Be7 Qb4+ Kc1 Qc5 Rd2 Rxg1+ Rxg1 Qxg1+ Rd1 Qg4 h6 f6 hxg7 Kf7 Bxf6 Kxf6 g8=Q Rxg8 Rf1+ Ke5 Re1+ Kxd5 Rd1+ Kc6 Rd3 Qf4+ Kb2 Qb4+ Rb3 Qxb3+ axb3 h5
f4 Nf6 d4 Nc6 Nf3 d5 Ne5 Bf5 Be3 Ne4 Nc3 Nxe5 dxe5 Nxc3 bxc3 Qd7 Rb1 c6 Bd4 b6 Bxb6 axb6 Rxb6 c5 Rb2 h5 h4 Rh6 e3 Qc6 Bd3 Bc8 Bb5 Qxb5 Rxb5 Rxa2 Rxc5 Be6 e4 dxe4 Rb5 Bc8 Rd5 f5 Rd8+ Kf7 Rxc8 Ra5 Qd5+ Rxd5 Kf2 Rd2+ Kg3 Ra6 Rc1 e6 Rc7+ Kg8 Rb1 Ra4 Rb8 Ra2 Rcc8 g6 Rxf8+ Kg7 Rg8+ Kh7 Rh8+ Kg7 Rhg8+ Kh7 Rh8+ Kg7 Rhg8+ Kf7 Rgf8+ Ke7 Rfe8+ Kf7 Rf8+ Ke7 Rfe8+ Kf7 Rf8+ Ke7 Rfe8+ Kf7 Rf8+ Ke7 Rfe8+ Kf7 Rf8+ Ke7 Rbd8 Rxd8 Rxd8 Kxd8 c4 Ke7 c5 Rb2 c3 Rb3 Kh3 Kd7 g3 Ra3 c4 Kc6
e4 c5
c4 c6 g3 d5 cxd5 cxd5 Bg2 Nf6 Nc3 d4 Ne4 Nbd7 Nxf6+ Nxf6 Nf3 d3 O-O e6 Ne5 dxe2 Qxe2 a6 d4 Be7 Be3 O-O Rac1 Nd5 a3 Bf6 f4 b6 Rc2 Bb7 Rfc1 Qd6 Bxd5 Qxd5 Rc7 Qh1+ Kf2 Qxh2+ Ke1 Qxg3+ Bf2 Qxf4 Be3 Qh4+ Bf2 Qh1+ Kd2 Bg5+ Be3 Bxe3+ Kxe3 Qe4+ Kf2 Qxd4+ Kf1 Qf4+ Ke1 Rad8 Rc1c4 Qg3+ Kf1 Bg2+ Qxg2 Qxe5 Qe2 Qxe2+ Kxe2 Rb8 Rd7 a5 Rcc7 b5 Kd3 b4 axb4 Rxb4 Kc3 h5 Rd1 h4 Rh1 g5 Rc5 f5 Rxa5 Rfb8 Ra7 Rxb2 Re1 Rb2b3+ Kc2 Rb2+ Kc3 Rb2b3+ Kc4 Rb8b4+ Kc5 Rb5+ Kc6 Rb6+ Kc7 h3 Rh1 g4 Ra8+ Kf7 Kd7 Rb7+ Kc6 Rb3b6+ Kc5 Rb1 Kc4 Rxh1 Ra6 Rc1+ Kd3 h2 Ke2 h1=Q
e4 Nc6 Qf3 Ne5 Qf4 f6 d4 Nf7 Bc4 Ngh6 e5 g5 Qf3 b6 exf6 Bb7 d5 Nd6 fxe7 Qxe7+ Qe2 Qxe2+ Kxe2 Nxc4 b3 Na3 Nxa3 Bxd5 Bxg5 Nf5 f3 Bc5 Rad1 Ne3 Rxd5 Nxd5 Nb5
e4 d6 d4 e6 c4 b6 Nc3 Bb7 Nf3 Be7 Be3 Nf6 Bd3 Nbd7 O-O Rc8 d5 c6 dxe6 fxe6 Nh4 c5 Bg5 Nxe4 Qh5+ g6 Nxg6 Bxg5 Nxh8+ Ke7 Qf7#
e4 e5 Bc4 Bc5 d3 d6 h3 Qf6 Qf3 h6 c3 c6 Be3 Qxf3 Nxf3 Nf6 Bxc5 dxc5 Nxe5 O-O Nd2 Nbd7 Nxd7 Bxd7 O-O Be6 Rfe1 b5 Bb3 c4 dxc4 bxc4 Bxc4 Bxc4 Nxc4 Rfd8 Na5 c5 e5 Nd5 c4 Nf4 Nc6 Rd2 b3 Nd3 Ref1 Nb4 Nxb4 cxb4 Rfd1 Rad8 Rxd2 Rxd2 f4 Kf8 a3 a5 axb4 axb4 Ra4 Ke7 Rxb4 Ke6 c5 Kd5 Rc4 Rb2 Rc3 Kc6 f5 Re2 e6 fxe6 fxe6 Rxe6 Kf2 Re5 b4 Re4 Rb3 g5 Kf3 Rc4 g4 Rf4+ Kg3 Rc4 h4 Rf4 h5 Rc4 Kf3 Rd4 Rb1 Rc4 Re1 Kd5 c6 Kxc6 Re6+ Kb5 Rxh6 Kxb4 Rg6 Rc5 h6 Kc4 Rg7 Kd4 h7 Rc3+ Ke2 Rh3 Rd7+ Ke4 Re7+ Kf4 Rf7+ Kxg4 Rc7 Rh2+ Ke3 Rh3+ Ke4 Rh5 Ke5 Kh4 Kf6 g4 Rg7 g3 Kf7 Kh3 Kg8 g2 h8=Q Rxh8+
d4 e6 e3 d6 a3 Nf6 Nf3 Be7 Nc3 O-O h3 Nfd7 g3 d5 Bg2 c5 O-O cxd4 Qxd4 b6 Qd1 Ba6 Re1 Nc6 Bf1 f6 Bxa6 Nde5 Nxe5 Nxe5 b3 Rc8 Bb2 Rc6 Bb5 Rc5 b4 Rc7 f4 Nc4 Bxc4 Rxc4 h4 a6 b5 axb5 Nxb5 Rc5 a4 e5 Ba3 Rc4 Bxe7 Qxe7 fxe5 fxe5 Rf1 d4 Rxf8+ Qxf8 exd4 Qf3 Qf1 Qe3+ Qf2 Qxf2+ Kxf2 exd4 Rc1 d3 cxd3 Rxc1 Ke3 Ra1 Nc3 Ra3 Kd4 g6 Kc4 Kf7 d4 Ke6 d5+ Kd6 Kb5 Rb3+ Ka6 Rxc3 Kxb6 Kxd5 a5 Ra3 a6 Kd6 a7 Kd7 Kb7 Rb3+ Ka6 Ra3+ Kb7 Rb3+ Ka8 Kc7 g4 Rb4 h5 gxh5 gxh5 Ra4 h6 Rxa7+ Kxa7 Kd6 Kb7 Ke6 Kc6 Kf6 Kd5 Kg6 Ke5 Kxh6 Kf4 Kg6 Kg4 h6 Kh4 h5 Kg3 Kg5 Kh3 h4 Kg2 Kg4 Kh2 h3 Kg1 Kg3 Kh1 Kh4 Kh2 Kg4 Kh1 Kg3 Kg1 Kf3 Kh1 Kg4 Kh2 Kh4 Kg1 Kg3 Kh1 h2
d4 d5 f4 Bf5 c3 Nf6 e3 e6 Bd3 Be4 Nf3 Nbd7 O-O c5 Ne5 Nxe5 fxe5 Nd7 Bxe4 dxe4 Qg4 f5 exf6 Nxf6 Qxe6+ Qe7 Qf5 Nd7 Na3 g6 Qd5 Nb6 Qb3 O-O-O Nc4 Nd5 Bd2 Bg7 Qa4 Kb8 Qb5 a6 Qa5 Rhf8 Nb6 Nxb6 Qxb6 cxd4 cxd4 Rd6 Rxf8+ Qxf8 Qc5 Rc6 Qxf8+ Bxf8 Bc3 Rf6 Rf1 Rf5 Rf4 Bd6 Rxe4 Rh5 h3 Rf5 Re8+ Kc7 e4 Rf7 e5 Be7 d5 Kd7 e6+ Kxe8 exf7+ Kxf7 Kf2 Bd6 Ke3 h5 Ke4
d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 Nbd7 Nge2 c5 d5 Re8 Qd2 a6 Ng3 b5 cxb5 axb5 Bxb5 Ba6 O-O Qb6 a4 Reb8 b4 Bxb5 axb5 Rxa1 Rxa1 Ne8 Nge2 Nc7 bxc5 Nxc5 Nd4 Bxd4 Bxd4
c4 e5 Nc3 Nf6 e4 Bb4 d3 Bxc3+ bxc3 O-O Nf3 Nc6 Qe2 b5 cxb5 Na5 Nxe5 Nb3 axb3
Nd3 b6 Ne3 c5 f3 f6 O-O-O Bd5 Nxd5 Qxd5 e4 Qg5 Ne1 Ne6 Bf2 Nd6 Be3 Qg6 g3 O-O-O Nd3 f5 e5 Nf7 f4 d6 b4 c4 Nb2 c3 Na4 cxd2+ Rxd2 dxe5 Re1 exf4 gxf4 Rxd2 Bxd2 Rd8 Qb2 Qg2 Qc3+ Kb7 Rxe6 Bxf4 Bxf4 Qg1+ Re1 Qxh2 Qc7+
Nc3 Nc6 d4 a6 Nh3 b5 e3 b4 Na4 d5 Nf4 Bd7 Nc5 Qb8 Nxd5 e6 Bc4 exd5 Be2 Bd6 Nxa6 Qb6 Nc5 Nf6 Nd3 Rb8 O-O h6 Qe1 g5 f3 h5 f4 g4 Bd2 Ne4 Qd1 Rd8 Rb1 Ke7 f5 Rdg8 g3 Rg5 f6+ Kd8 Rc1 h4 Nf4 Qa5 Be1 hxg3 hxg3 Nxf6 a3 Ne4 Ra1 Rg7 Bxb4 Qb6 Nxd5 Qb8 Bxd6 cxd6 Qe1 Qxb2 c4 Rgh7 Rb1 Rh1+ Kg2 Rh8h2#
e4 e5 d3 Bc5 Be3 Na6 d4 exd4 Bxd4 Nf6 Bxf6 Qxf6 c3 Qxf2#
exd4 d5 Qe2+
Qf3 Nc6 Bc4 e6 Nh3 Nf6 Nf4 Be7 Nc3 Ne5 Qe2 g5 Nh3 Nxc4 Qxc4 d5 Qb5+ c6 Qa4 b5 Qd4 b4 Na4 Qc7 b3 e5 Qd3 g4 Ng5 h6 Nxf7 Kxf7 exd5 cxd5
e4 Nc6
e4 e6 d4 d5 e5 c5 c3 cxd4 cxd4 Nc6 Nf3 Qb6 a3 a5 Be2 Nge7 O-O Nf5 Be3 Qxb2 Nbd2 Qc3
e4 Nc6 Nf3 f6 d3 Ne5 Nc3 Nxf3+ Qxf3 d6 d4 h5 Bf4 Bg4 Qg3 Qd7 Bc4 h4 Qe3 a6 f3 Be6 Bd5 Bxd5 Nxd5 c6 Nb6 Qe6 Nxa8 h3 Nc7+
Nh3 Nc6 Nc3 Nf6 Na4 Nd4 Rb1 Ne4 Nf4 d5 Nxd5 Qxd5
e4 e5 Nf3 Nc6 d4 exd4 Nxd4 Bc5 Nxc6 bxc6 Nc3 Nf6 e5 Qe7 Be2 Qxe5 Na4 Bd6 g3 Qa5+ Nc3 O-O Bf4 Ba6 O-O Bxf4 gxf4 Bb7 Re1 Qf5 Bd3 Qxf4 Ne4 c5 Nxf6+ Qxf6 Re4 Bxe4 Bxe4 Rae8 Bd3 Re5 f4 Qxf4 Qf1 Rg5+ Kh1 Qd4 c3 Qd5+ Be4 Qxe4+ Qg2 Qxg2#
d4 Nf6 Bg5 d6 Bxf6 exf6 c4 g6 e3 Bg7 Nc3 O-O Bd3 Re8 Nf3 Bg4 Qc2 Nd7 O-O-O a6 h3 Bxf3 gxf3 c6 f4 b5 h4 bxc4 Bxc4 d5 Bd3 Nb6 h5 Qd6 hxg6 fxg6 Bxg6 hxg6 Qxg6 Qe6 Rdg1 Ra7 Rh7 Ree7 Rgh1
e4 a6 d4 b5 f4 Bb7 Bd3 e6 Nf3 c5 c3 Nf6 Qe2 Qc7 O-O d5 e5 Ne4 Nbd2 b4 Nxe4 dxe4 Bxe4 bxc3 Bxb7 cxb2 Bxb2 Qxb7 dxc5 Bxc5+ Kh1 O-O Nh4 Nd7
e4 e5 Nf3 Nf6 Nxe5
e4 e5 Ne2 Nf6 d4 exd4 Qxd4 Nc6 Qc4 d6 Nd4 Bd7 Nxc6 Bxc6 Nd2 d5 exd5 Bxd5 Qb5+ Qd7 Qxd7+ Nxd7 c4 Bc6 Bd3 Bb4 f3 O-O O-O Ne5 a3 Bxd2 Bxh7+ Kxh7 Bxd2 Nxc4 Bb4 Rfe8 Rfe1 Nxb2 Rxe8 Rxe8 Rc1 Nd3 Rd1 Nf4 Kf2 f6 g3 Nd5 Bd2 a6 f4 Ba4 Rc1 c6 Rc4 Bb5 Rd4 a5 Bxa5 Ra8 Bb4 Nxb4 axb4 Ra4 g4 c5 Rd5 Ra2+ Kg3 Bc6 Rh5+ Kg6 f5+ Kf7 bxc5 Ra5 g5 fxg5 Rxg5 Rxc5 Kf4 Rc4+ Ke5 Rh4 Rg6 Rxh2 Rd6 Re2+ Kf4 Re7 Rd8 g6 fxg6+ Kxg6 Rb8 Kf7 Rh8 Re4+ Kf5 Ke7 Rh6 Re1 Rg6 Be4+ Kg5 Bxg6 Kxg6
b3 e5 Bb2 Nc6 e4 Nf6 f4 Nxe4 fxe5 Qh4+ g3 Nxg3 Nf3 Qe4+ Be2 Nxe2 Qxe2 Qxc2 Bc3 b6 O-O Bc5+ Kh1 Bb7 Qd1 Qxd1 Rxd1 Nd4 Bxd4 Bxf3+ Kg1 Bxd4+
Nc3 d6 Nf3 d5 d4 c6 e3 b6 Be2 a6 O-O e6 Qd3 f6 a4 g6 e4 h6 Nh4 dxe4 Qxe4 b5 Qxg6+ Kd7 axb5 cxb5 Bf3
d4 d5 c4 dxc4 e3 g6 Bxc4 Bg7 Nf3 h6 O-O Nf6 Nc3 a6 Qb3 e6 e4 b5 Bd3 O-O Bf4 Nbd7 Rad1 Bb7 Rfe1 Re8 Rc1 e5 dxe5 Nxe5 Nxe5 Ng4
e4 Nf6 e5 Nd5 c4 Nb6 d4 d6 f4 g5 exd6 gxf4 dxc7 Qxc7 Nc3 e6 a3 Bd7 b3 Na6 Nf3 O-O-O Ne5 Be8 Bxf4 Bd6 Qe2 f6 Nd3 Bxf4 Qxe6+ Bd7 Qxf6 Rhf8
Nc3 Nf6 e4 Nc6 Bc4 e6 d3 d5 exd5 Nb4 dxe6 Nbd5 exf7+ Kxf7 Nxd5 Be6 Qf3 Bxd5 Bxd5+ Qxd5 Qxd5+ Nxd5 Nf3 Bc5 O-O a6 Be3 Bxe3 fxe3 Nxe3 Rfe1 Nxc2 Re2 Nxa1 Re5 Rhd8 d4 h6 d5 g6 Re6 Rd6 Rxd6 cxd6
e4 d6 d4 c6 Nc3 Qc7 Nf3 Bg4 Be2 Bxf3 Bxf3 e5 Be3 Be7 O-O a5 Ne2 a4 c3 Nd7 Rc1 Bf6 Bg4 Ne7 Re1 g6 f4 Bg7 f5 Nf6 fxg6 Nxg4 gxf7+ Kf8 Bg5 h5 Ng3 Bf6 Bxf6 Nxf6 Qf3 Kxf7 Rf1 Ng8 Nxh5 Rxh5 Qxh5+ Kg7 Qg5+ Kf7 Rc2 Rf8 Rcf2 Qe7 Qh5+ Kg7 Qg5+ Kf7 h4 Ke8 h5 Rf7 g4 Rg7 Qh4 Rxg4+ Qxg4 Nxg4 Rf8+ Qxf8 Rxf8+ Kxf8 Kg2 Ng4f6 h6 Nxh6 dxe5 dxe5 Kf3 Ke7 b3 axb3 axb3 b6 Ke3 Ke6 Kd3
e4 e6 d4 d6 c4 c6 Nc3 Nd7 d5 Nc5 Be3 b6 Be2 Be7 Nf3 h5 Bxc5 dxc5 e5 g5 d6 Bf8 Ne4 Bh6 h3 Bb7 g4 f6 Nxf6+ Nxf6 exf6 Qxf6 Bd3 O-O-O Be4 e5 Bf5+ Kb8 d7 e4 Nd2 Qxb2 Nxe4 Qb4+ Qd2 Bg7 O-O Qxd2 Nxd2 Bxa1 Rxa1 hxg4 hxg4 Rh4 f3 Rhh8 Ne4 Rhf8 Nd6 Rxf5 Nxf5 Rxd7 Re1 a5 Re6 Ka7 Rg6 Rd1+ Kg2 Rd2+ Kg3 Rxa2 Rxg5 a4 Rh5 a3 Rh1 Rc2 g5 a2 g6 Rb2
d4 d5 Nc3
e4 e5 Bc4 Nf6 d3 Nc6 Bg5 Na5 Nc3 Bc5 Nf3 Nxc4 dxc4 O-O Qd2 d6 O-O-O Be6 b3 Bb4 Qd3 Bxc3 Qxc3 Nxe4 Qe3 Nxg5 Nxg5 Qf6 Ne4 Qg6 Ng3 f5 f4 e4 Rdg1 a5 a4 Qf6 Kb1 Qh4 Ne2 Qg4 h3 Qg6 g4 fxg4 hxg4 Bxg4 Rg3 Qf5 Rhg1 h5 Nd4 Qxf4 Qxf4 Rxf4 Ne2 Bxe2 Rxg7+ Kh8 R7g5 Rf7 Rg6 Bf3 Rh6+ Rh7
e4 e5 f4 Bd6 Nf3
e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 c3 b5 Bb3 O-O Re1 d6 d4 Bg4 Qd3 Na5 Nbd2 Nxb3 axb3 Qd7 dxe5 dxe5 Nxe5 Qxd3 Nxd3 Rad8 Nb4 Ra8 Nd5 Nxd5 exd5 Bd6 c4 Rfe8 Nf3 Bxf3 Rxe8+ Rxe8 gxf3 bxc4 bxc4 h6 Be3 Ra8 c5 Be5 Ra2 Kf8 f4 Bf6 Kg2 Ke8 Kf3 Kd7 b4 Bc3 Ra3 Bxb4 Rc3 Bxc3
e4 g6 d4 Bg7 Nf3 d6 Nc3 Nf6 Bd3 O-O O-O Nc6 e5 dxe5 dxe5 Ng4 Nh4 Ngxe5 Ne4 Nxd3 cxd3 Nd4 Be3 e5 Nf3 Bg4 Bxd4 exd4 h3 Bxf3 Qxf3 f5 Nd2 c6 Rae1 Qf6 Nc4 Rfe8 Qf4 Rad8 Rxe8+ Rxe8 Nd6 Re7 Nc4 b5 Nd2 Qe5 Qf3 Qd6 Qd1 Qe6 Nf3 h6 Re1 Qf7 a3 Rxe1+ Qxe1 Kf8 Nd2 Qe7 Qc1 Qd6 b4 Bf6 Nb3 Qd5 Na5 Bg5 Qa1 c5 bxc5 Qxc5 Nb3 Qc3 Qb1 Be7 Nc1 Bxa3 Ne2 Qb2 Qxb2 Bxb2
d4 Nf6 c4 Nc6 Nf3 d6 Nc3 Bf5 Bg5 h6 Bxf6 exf6 e3 Bd7 Bd3 Nb4 Be2 Be7 Qb3 c5 a3 Nc6 O-O cxd4 Nxd4 Nxd4 exd4 Qb6 Qxb6 axb6 Bf3 Rc8 b3 Bc6 Nd5 b5 cxb5 Bxd5 Bxd5 b6 Rfc1 Rxc1+ Rxc1 Kd7 Bc6+ Kd8 Rc4 Rf8 Ra4 g6 Ra8+ Kc7 Ra7+ Kc8 Rxe7 f5 Bb7+ Kd8 Re1 Re8 Rxe8+ Kxe8 b4 Kd7 Bc6+ Ke6 a4 f4 a5 d5 a6 f3 gxf3 Kf5 Bxd5 Kf6 a7 Kg5 a8=Q Kh4 Qb8 f5 Qg3+ Kh5 h4 g5 Bf7#
Nc3 e6 e3 Qf6 d4 d5 Nge2 Nc6 Bd2 Nb4 Nb5 Nc6 Nxc7+ Ke7 Nc3 Rab8 Nc3xd5+ Kd6 Nxf6
d4 e6 Nf3 Nf6 g3 d5 Bg2 Nc6 O-O Bb4 c3 Bf8 Bg5 Be7 Nbd2 h6 Bxf6 gxf6 e4 Rb8 exd5 exd5 Re1 Rg8 Qc2 Rh8 Nh4 Kf8 Nf5 Ra8 Re2 Be6 Rae1 Qd7 Nxe7 Nxe7 Nf3 Re8 Nh4 b6 Qd3 h5 Qf3 Rh6 Qf4 Kg7 f3 c6 Rxe6 fxe6 g4 hxg4 Qxg4+ Kf7 Bh3 Nf5 Nxf5 exf5 Qxf5 Rxe1+ Kf2 Qe7
e3 e5 d4 e4 f3 d5 Nc3 Bb4 Bd2 Bxc3 Bxc3 Nf6 fxe4 Nxe4 Bb4 Nc6 Ba3 Qh4+ g3 Nxg3 hxg3 Qxh1 Ne2 Qf3 Nc3 Qxg3+ Ke2 Bg4+ Kd3 Bxd1 Rxd1 O-O-O Be7 Nxe7
d4 Nc6 c3 d6 e3 Bf5 Bb5 Qb8 Bxc6+ bxc6 b3 Qb5 a4 Qd3 Ne2 Qxd1+ Kxd1 g6 Na3 h5 g3 h4 gxh4 Rb8 b4 Nf6 Rg1 Bg7 Ng3 Bd7 e4 Rxh4 e5 Nd5 Kd2 Kf8 exd6 exd6 Rh1 a5 Kd3 Nxc3 Bg5 Rg4 Bd2 axb4 Bxc3 bxa3 Rxa3 Bc8 Re1 Ba6+ Kd2 Rh4 d5 Bh6+ Kc2 Rxh2 Bd2 Bxd2 Kxd2 cxd5 Rae3 Rxf2+ Kd1 Rb1#
e4 e5 Nf3 Nc6 Bb5 Nf6 d3 Bc5 c3 d6 h3 O-O O-O a6 Ba4 b5 Bb3 Na5 Bc2 b4 d4 exd4 cxd4 Bb6 Rfe1 Bb7 Qd3 c5 Bg5 c4 Qd2 Nxe4 Bxe4 Qc7 Bxb7 Nxb7 Re7 Qc6 d5 Qc5 Be3 c3 bxc3 bxc3 Nxc3 Qa5 Rxb7 Bxe3 fxe3 Rac8 Rc1 Rc5 Ng5 h6 Nge4 Rc4 Qf2 Rxe4 Nxe4 Qxd5 Qf4 Qxb7 Qg4 f5 Nxd6 Qb6 Nxf5 Qxe3+ Nxe3
d4 Nf6 Nc3 Nc6 Nf3 d5 Bg5 h6 Bxf6 exf6 e3 Qd6 Nb5 Qd8 c3 Be7 Bd3 O-O O-O Bd7 Na3 Bxa3 bxa3 Na5 Rb1 c6 Re1 b6 e4 Qc7 exd5 Rad8 dxc6 Nxc6 Bb5 Be6 Qc2 Ne7 Ba4 a6 Bb3 Nd5 Bxd5 Rxd5 Rb2 Rc8 Rc1 b5 Qd2 Rdd8 Ne1 Bd5 Nd3 Qd6 Nc5 Bxg2 Kxg2 Rxc5 dxc5 Qc6+ Kg1 Rxd2 Rxd2 Kh7 Rd3 h5 c4 Qxc5 Rd5 Qxa3 Rdd1 bxc4 Rxc4 g5 Rc7 Qf3 Rxf7+ Kg6 Rd6 Qg4+ Kh1 Kxf7
d4 f6 e3 e6 Nc3 a6 Bd3 f5 Nh3 Be7 d5 c6 dxe6 dxe6 O-O Nf6 f3 Nbd7 e4 Ne5 exf5 exf5 Nf4 Qc7 Qe2 Qd6 Bd2 Qb4 b3 Bd6 Ne4 Nxe4 Bxb4 Bxb4 fxe4 fxe4 Qxe4 Bd6 Rae1 O-O Qc4+ Kh8 Rxe5 Bxe5 Nh5 Bd7 Rxf8+ Rxf8 h3 g6 Ng3 b5 Qc5 Bxg3 Qxf8#
e4 d5 exd5
d4 d6 e3 e6 Nh3 h6 Nc3 b6 Be2 f5 Bd2 Qh4 Nb5 Nc6 Nxc7+ Kd7 Nxa8 d5 Nf4 Nf6 O-O Ng4 Bxg4 fxg4 e4 dxe4 Re1 Nxd4 Rxe4 Nf3+ gxf3 gxf3 Bb4+ Ke8 Nc7+ Kf7 Qd2 Qg4+ Kf1 Bxb4 Qxb4 Ba6+ Nxa6 Rd8 Rxe6 Rd1+ Rxd1
h3 d5 g4 Nd7 d3 e5 b3 Bc5 Bb2 Qf6 f3 Qh4+ Kd2 Ngf6 Qe1 Bb4+ Bc3 Qg5+ Kd1 Be7 Bd2 Qg6 Na3 Bxa3 e4 d4 c3 O-O cxd4 exd4 e5 Bb2 Rb1 Ba3 exf6 Qxf6 b4 Qc6 b5 Qc5 Ne2 Re8 f4 Nb6 f5 f6 g5 Bxf5 gxf6 Bxd3
c4 Nf6 a3 d5 cxd5 Qxd5 e3 Bf5 Be2 Nc6 g3 Qxh1 Nc3 Qxg1+ Bf1 O-O-O Na4 Ne4 b3 Qxf2#
d4 Nf6 c4 e6 Nf3 b6 g3 Ba6 Qc2 c5 dxc5 Bxc5 Bg2 O-O O-O Nc6 Nbd2 Ng4 e3 d5 cxd5 Qxd5 Rd1 Rad8 b3 Bd3 Qc3 Rd7 Bb2 e5 Ne1 Bb4 Bxd5 Bxc3 Bxc6 Rd6 Bxc3 Rxc6 Nxd3 Rxc3 Ne4 Rc2 h3 f5 Nd6 e4 Nb4 Rxf2 hxg4 fxg4 Rf1 Rxf1+ Rxf1 Rxf1+ Kxf1 a5 Nc6 h5 Nxe4 Kf7 Nd2 Ke6 Nc4 Kd5 Nd4 Ke4 Kf2 h4 gxh4 a4 bxa4 g5 hxg5 Kd5 Nxb6+ Ke5 Kg3 Kd6 Kxg4 Ke5 g6 Kf6 Kh5 Ke7 Kh6 Ke8 g7 Kf7 Kh7 Kf6 g8=Q Ke7 Qe6+ Kd8 Qd7#
d4 e6 c4 d5 e3 Nf6 Nc3 Bb4 Qb3 Nc6 a3 Bxc3+ Qxc3 Ne4 Qc2 O-O Bd3 Ng5 Ne2 e5 dxe5 Nxe5 cxd5 Qxd5 Nf4 Nxd3+ Qxd3 Qd8 O-O Re8 b4 Ne4 Bb2 Bf5 Qb5 Qc8 Rac1 Nd2 Rfd1 Nb3 Rc3 Rd8 Re1 Nd2 Ba1 c6 Qe2 a5 bxa5 Rxa5 Bb2 b6 Bc1 Nb1 Rb3 Qa6 Qxa6 Rxa6 g4 Be4 Ne2 Nd2 Rb4 Nf3+ Kf1 Nxh2+ Kg1 Nf3+ Kf1 f5 Bb2 c5 Rb3 Nd2+ Kg1 Nxb3
Nc3 d5 Nf3 c5 e3 Nc6 Bb5 e6 d4 a6 Ba4 b5 Bb3 c4 O-O cxb3 axb3 Bb4 Qd3 Bd6 Nxb5 Nb4 Nxd6+ Qxd6 Qe2 Bd7 Ne5 Bb5 c4 dxc4 Qh5 Nh6 Nxc4 Bxc4 bxc4 O-O f4 Nf5 c5 Qd5 Qf3 Qxf3 gxf3 Nd3 Bd2 Nxb2 Kg2 Nc4 e4 Nxd2 Rf2 Nb3 Ra3 Nfxd4 Rc2 Rfc8 Rc3 Rxc5 Rcxb3 Nxb3 Rxb3 a5 h3 a4 Rb7 a3 Kf2 a2 Ke2 a1=Q Kf2 Qd4+ Kg3 Qg1+ Kh4 g6 f5 exf5 f4 fxe4 f5 gxf5 Re7 Qf2+ Kh5 f4+ Re5 Rxe5+ Kg4 h5#
e4 Nf6 Nc3 e6 f4 Bb4 Nf3 Bxc3 bxc3 Nxe4 d3 Nxc3 Bb2 Nxd1 Bxg7 Rhg8 Raxd1 Rxg7 Be2 Rxg2 Rhg1 Rxg1+ Nxg1 b6 Kf2
e4 Nf6
e4 Nc6 d4 d5 e5 f6 exf6 Nxf6 c3 Bg4 Be2 Qd7 h3 Bxe2 Nxe2 O-O-O O-O e5 dxe5 Nxe5 Nd4 c5 Nf3 Nxf3+ Qxf3 Re8 Bg5 Re6 Nd2 Be7 Bxf6 Rf8 Qd3 Rexf6 Qxh7 g5 Rae1 Re6 Rxe6 Qxe6 Nf3 Qe4 Qxe4 dxe4 Nd2 g4 hxg4 e3 fxe3 Rh8 Rf3 Kd7 Ne4 c4 g5 b6 g6 a6 g7 Rd8 Rf7 Ke6 Rf4 Rg8 Rg4 Kf7 Nd2 Bc5 Kf2 a5 Nxc4 Kf6 a3 a4 Nd2 Rxg7 Rxg7 Kxg7 Ne4 Kf7 Nxc5 bxc5 Kf3 Ke6 Ke4 Kd7 Kd5 Ke7 Kxc5 Ke6 Kb4 Ke5 Kxa4 Kd6 Kb4 Ke5 Kc4 Kd6 a4 Kc6 b4 Kb6 a5+ Kb7 Kd5 Kb8 e4 Kc8 g4 Kd7 e5 Kd8 Ke6 Kc7 g5 Kc6 g6 Kb5 g7 Kc4 g8=Q Kb5 Kd5 Ka4 Qe8+ Kb3 a6 Kc2 c4 Kb3 b5 Kc2 a7 Kc3 a8=Q Kd2 e6 Ke2 Qf7 Kd1 e7 Kc2 e8=Q Kb2 Qf5 Kc3 Qee5+ Kd2 Qa2+ Kc1 Qfc2#
e4 e5 Bc4 Nf6 d3 c6 f4 exf4 Bxf4 d6 Nf3 Bg4 h3 Bxf3 Qxf3 Nbd7 g4 Nb6 Nd2 Qe7 a3 O-O-O O-O-O h6 Rde1 g5 Bg3 Nfd7 Bxf7 Bg7 Rhf1 Rhf8 Rf2 Rxf7 Qxf7 Qxf7 Rxf7 Be5 Bxe5 Nxe5 Rf6 Ned7 Rxh6 Nc5 Rg6 d5 Rxg5 Ne6 Re5 Rd6 exd5 Nxd5 Rxe6 Rxe6 Rxe6 Nf4 Re3
e4 e6 d4 d5 Nc3 Nf6 Bg5 Bb4 e5 h6 exf6 hxg5 fxg7 Rg8 Qf3 Nc6 Bb5 Bd7 a3 Bxc3+ Qxc3 Qf6 f3 Nxd4 Bxd7+ Kxd7 O-O-O e5 g3 Rxg7 f4 gxf4 gxf4 Qxf4+ Kb1 c6 Nh3 Qf5 Qb4 Qxc2+ Ka2 b6 Rxd4 exd4 Qxd4 Rag8 Nf4 Rg4 Qf6 Qc4+
e4 e6 Nf3 c5 Nc3 Nc6 d4 cxd4 Nxd4 d6 Bb5 Bd7 O-O a6 Bxc6 bxc6 Be3 Ne7 f4 c5 Nf3 Bc6 a4 Qd7 b3 g6 e5 d5 Bxc5 Nf5 Bxf8 Kxf8 Qd2 Kg7 Nd4 Nxd4 Qxd4 h5 h4 Qe7 g3 Rhc8 Na2 Rc7 Rf3 Bb7 Rc3 Rxc3 Nxc3 Rc8 Qd3 Qc5+
f3 b6 e4 a5 d4 a4 a3 Ba6 Ne3 g6 Qf2 Bxd4 c3 Bd3 Nac2 Bxe3 Qxe3 Bxc2+ Kxc2 Ne6 b4 axb3+ Kxb3 Nc5+ Kc2 f6 h4 Na4 g4 Qa2+ Kd3 Qxb1+ Kd4 c5+ Kc4 b5+ Kd5 Na8b6#
e3 e5 Qh5 d6 Bc4 g6 Qf3 Nf6 d4 Nc6 dxe5 Nxe5 Qf4 Nh5 Qe4 Bf5 Qxb7 Nxc4 b3 Bg7 c3 Bxb1 Rxb1 Bxc3+ Ke2 Nb6 Qc6+ Qd7 Qxc3 O-O a4 Nd5 Qd4 Qc6 Bb2 Rfe8 Qh8#
e3 a5 Bd3 d6 Nf3 Nh6 Nc3 Nc6 a3 Be6 O-O b6 b3 g6 e4 Bg7 h3 f5 Bb5 Qd7 Bxc6 Qxc6 exf5 gxf5 Qe1 f4 Qxe6 Be5 Nxe5 Rf8 Nxc6 Rf6 Qxe7#
e4 e5
e4 e6 Nc3 Nf6 e5 Nd5 Nb5 d6 Qe2 dxe5 Qxe5 Nc6 Qe4 a6 Nd4 Bc5 Nxc6 bxc6 Qe5 O-O d4 Bb4+ c3 Nxc3 bxc3 Bxc3+ Bd2 Bxa1 Bg5 Bxd4 Qg3 f6 Bh6 f5 Nf3 f4 Qg4 Bc3+ Ke2 a5 g3 Ba6#
e4 e6 d4 d5 e5 a6 Qd3 Bb4+ c3 Ba5 Qg3 g6 Nf3 Nc6 Bg5 f6 exf6 Nxf6 Ne5 Nxe5 dxe5 Rf8 Bxf6 Qd7 Bd3 d4 O-O dxc3 Nxc3 Qc6 Rad1 Bd7 Be4 Qb6 Rxd7 Kxd7 Na4 Qb5 Qd3+ Kc8 Qxb5 axb5 Nc5 Ra7 Nxe6 Re8 Rd1 c6 Nc5 Bb6 Nxb7 Kxb7 Rd7+ Bc7 Rxh7 Rxa2 g3 Ra1+ Kg2 Raa8 Bxg6 Rg8 Bf7 Rg4 e6 Bd6 Bh5+
e4 e5 Nf3 Nc6 c3 d5 exd5 Qxd5 d3 Bg4 Be2 e4 dxe4 Qxd1+ Bxd1 O-O-O O-O Nf6 Nbd2 Bxf3 Bxf3 Bc5 Nc4 b5 Nd2 a6 b4 Ba7 Nb3 Rhe8 Re1 g6 a4 bxa4 Rxa4 Kb7 e5 Nh5 Bxc6+ Kxc6 Rxa6+ Kb7 Ra2 Rxe5 Kf1 Rxe1+ Kxe1 Re8+ Kf1 Rd8
e4 e5 f4 exf4 Nf3 d6 Bc4 h6 d4 Be7 Bxf4 a6 O-O b5 Bb3 c6 e5 d5 a4 Be6 axb5 cxb5 Nc3 Nc6 Qe2 Qd7 Nxb5 Qb7 Nd6+ Bxd6 exd6 Nf6 Ba4 O-O c3 Ne4 Ne5 Nxe5 Bxe5 Qe7 dxe7
d4 d5 Nc3 Nc6 Nf3 e6 e3 Bb4 Bd2 Nf6 Bb5 Ng4 Ne5 Qf6 Nxg4 Qf5 Be2 e5 dxe5 Nxe5 Nxd5 h5 Nxe5 Qxe5 Bxb4 Qg5 Nxc7#
b4 e5 Bb2 Bxb4 Bxe5 Nf6 Bb2 Nc6 Nc3 O-O a3 Ba5 e3 d6 Nf3 Re8 Be2 a6 O-O Bb6 Bd3 Bg4 Be2 Ne5 Nxe5 Bxe2 Qxe2 Rxe5 f4 Rh5 Ne4 Rh6 Nxf6+ gxf6 Rf3 Rg6 Qf2 Qf8 Rg3 Qh6 Rxg6+ Qxg6 Qf3 Kg7 Qxb7 Rg8 Rc1 a5 Qf3 h6 Kf2 Qf5 d3 h5 Qe4 Qg4 Qf3 Qxf3+ Kxf3 Kg6 Rg1 f5 d4 c5 c3 cxd4 cxd4 d5 Rc1 Rb8 Rc6+ f6 Bc3 a4 Bb4 Kf7 Rd6 Bd8 Rxd5 Ra8 Rxf5 h4 Rh5 Ke6 Rxh4 Be7 f5+ Kd7 Rh7 Re8 Bxe7 Rxe7 Rxe7+ Kxe7 Kf4 Kd6 e4 Kc6 e5 fxe5+ dxe5 Kc5 f6 Kc4 f7 Kb3 f8=Q
e3 e5 Nf3 d6 Na3 Nc6 Bc4 Nh6 d4 Nf5 O-O a6 c3 b5 Bb3 d5 dxe5 Be7 Bc2 Bh4 Nxh4 Nxh4 g3 Nf5 f4 O-O e4 Na5 exf5 c6 b4 Nb7 Be3 g6 g4 h5 Qd2 hxg4 fxg6 fxg6 Rae1 Qh4 Bxg6 g3 f5 gxh2+ Qxh2 Qxh2+ Kxh2 Nd8 Bh6 Rf7 Kg3 Rh7 Bxh7+ Kxh7 Bd2 Nf7 e6 Ne5 Rh1+ Kg7 Rxe5 Kf6 Rhe1 Bb7 Kf4 Rh8 Kf3 Re8 Bf4 d4 Nb1 dxc3 Nxc3 a5 Ne4+ Kg7 f6+ Kg6 Rg1+ Kh7 Rh5#
g3 b6 Nab3 g6 d3 d6 Bc3 c5 Bxh8 Bb5 Bc3 Nc7 Bd2 f6 c4 Bc6 Bxc6 Ne6 Bg2
Nf3 Nf6 Nc3 d5 d4 e6 Bg5 Bb4 Qd2 Nc6 a3 Bxc3 Qxc3 h6 Bxf6 gxf6 e3 Qd6 Be2 Bd7 O-O O-O-O Nd2 Kb8 b4 Rdg8 g3 e5 dxe5 Qxe5 Qxe5 fxe5 Rfb1 Rd8 b5 Ne7 a4 Bh3 Nf3 Bg4 a5 f6 b6 cxb6 axb6 axb6 Rxb6 Nc6 Rab1 Kc7 Rxb7+ Kd6 Rb7b2 h5 c3 Rb8 Kg2 Rxb2 Rxb2 e4 Nd4 Kc7 Nxc6 Kxc6 Bxg4 hxg4 Ra2 Kd7 Ra7+ Ke6 Rc7 Kd6 Rf7 Rh6 Rg7 Rh3 Rxg4 Rh8 Rg6 Ke6 h4 Kf7 Rg4 f5 Rg5 Kf6 f4 Rc8 g4 fxg4 Rxg4 Rxc3 Rg3 d4 exd4 Rc4 h5 Rc2+ Kh3 Kf5 h6 Rc7 Rg7 Rc3+ Kg2 Rc2+ Kh3 Rc3+ Kh4 Rc8 Kg3 Rc3+ Kh4 Rc8 Rf7+ Ke6 Rg7 Kf5 h7 e3 Rg8 e2 h8=Q e1=Q+ Kh3 Qf1+ Kg3 Rc3+ Kh2 Qxf4+ Kh1 Qf1+ Rg1 Qf3+ Kh2 Rc2+ Rg2 Qf4+ Kh3 Rc3+ Rg3 Qf1+ Kh4 Qh1+ Rh3 Rxh3#
e4 Nf6 d3 d6 g3 Nbd7 Be2 g6 Nh3 Bg7 O-O Nb6 c4 Bxh3 Kh1 Bxf1 Bxf1 O-O h4 Qc8 Kh2 Ng4+ Kg2 Ne5 f4 Ned7 Qf3 Nc5 b4 Bxa1 Nd2 Ne6 Nb3 Nd4 Nxd4 Bxd4 Be3 Bxe3 Qxe3 Qg4 Be2 Qd7 Qf3 c5 a3 Qa4 Bd1 Qxa3 Qe3 Qa2+ Qe2 Qb1 Bc2 Qxb4 g4 Rfc8 Bd1 d5 exd5 Nxc4 dxc4 Rd8 Qd3 Qe1 Bf3 e5 Qe2 Qxh4 Qxe5 Re8 Qc7 Qe7 d6 Qe1 Qxc5 Re2+ Bxe2 Qxe2+ Kh3 Qf3+ Kh2 Qxf4+ Kh3 g5 Qg1 Qf3+ Qg3 Qf1+ Qg2 Qxc4 d7 Qd3+ Qg3 Qxd7 Qe3 Re8 Qxg5+ Kf8 Qh6+ Kg8 Qg5+ Kh8 Qf6+ Kg8 Qg5+ Kf8 Qh6+ Ke7 Qxh7 Qd3+ Kh4 Qxh7+ Kg5 Kd6 Kf6 Re6+ Kg5 Qh6+ Kf5 f6 g5 Qxg5#
e4 g6 d4 Bg7 Nf3 b6 Be3 Bb7 Bc4 Bxe4 Ng5 Bb7 Nxf7 d5 Nxd8 Kxd8 Bb5 c6 Bd3 Nf6 Nc3 Nh5 g4 Nf6 g5 Ng4 Qxg4 Bc8 Qf3 h5 gxh6 Rhf8 Qe2 Bh8 Bxg6 Nd7 h7 Nf6 Bh6 Bg4 Qe5 Nd7 Qg5 Rf4 Qxf4 Bh3 Bf5 Bxf5 Qxf5 Bxd4 Ne2 Bxb2 Rab1 Bf6 Rhg1 Kc7 Bg7 Bxg7 Rxg7 Nf6 Nf4 Rg8 hxg8=Q Nxg8 Rxg8 Kb7 Qc8#
e4 e5 Bc4 Bc5 Ne2 Ne7 Nbc3 Nbc6 a3 a6 b4 b5 bxc5 bxc4 O-O O-O Bb2 Bb7 d4 d5 Nxd5 Nxd4 Nxe7+ Kh8 Nf5 Nxe2+ Qxe2 g6 Bxe5+ Kg8 Nh6#
e4 c5 f3 e6 d4 Qa5+ c3 Bd6 dxc5 Bf8 c6 Nxc6 Bd2 Qh5 Be2 Nf6 f4 Qh4+ g3 Qh6 Bf1 Nxe4 f5 Nxd2 Qxd2 Qf6 Na3 Bxa3 bxa3 Nd8 Rc1 Qxf5 Qd1 Rb8 Rc2 Ke7 Rd2 Qg5 Nf3 Qc5 a4 Rg8 Nh4 f6 Nf3 Qxc3 Nd4 h6 Qc2 Qa1+ Qd1 Qc3 Qc2 Qa1+ Rd1 Qxd1+ Qxd1 Nc6 Nb5 d5 Nxa7 Nxa7 Qd2 Ra8 Qb4+ Ke8 a5 Nc6 Qb5 d4 a6 e5 axb7 Bxb7 Qxb7 Ra3 Qxc6+ Kf8 Qc5+ Ke8 Qxa3 Rh8 Qa7 g6 Qg7 f5 Qxh8+ Kd7 Qxh6 Kc8 Qxg6 Kd8 Qxf5 e4 Qxe4 Kc8 Qxd4 Kc7 h4 Kc8 Ba6+ Kc7 Qc4+ Kd7 Qe4 Kd6 Rh2 Kd7 Rd2+ Kc7 Qc4+ Kb6 Rb2+ Ka7 Rb7+ Ka8 Qc8#
e4 g6 d4 d6 Nf3 Nh6 Bc4 f6 O-O Bg7 Bd2 Nf7 Na3 O-O Bd5 c6 Bxf7+ Rxf7 Nc4 b5 Ne3 c5 Nd5 e6 Nc3 b4 Nb1 cxd4 Nxd4 e5 Nb5 a6 Bxb4 axb5 Qxd6 Qxd6 Bxd6 Ba6 Nc3 b4 Bxb4 Bxf1 Kxf1 Nc6 Bc5 Nd4 Rd1 Nxc2 h4 Nd4 Kg1 Ne6 Bb4 Nf8 Bd6 Rd8 f3 Rfd7 Nb5 Ne6 a4 Bf8 Kf2 Bxd6 Kf1 Bf8 Re1 Rb7 g3 Nd4 Rd1 Nxb5 Rxd8 Nd4 h5 gxh5 Rc8 Nxf3 Kg2 Nd4 Kf2 Rxb2+ Kf1 Rb3 Kg1 Rxg3+ Kh1 Re3 Kh2 Rxe4 a5 Nb5 Kg3 Ra4 Ra8 e4 Re8 f5 Re5 Nd6 Kf4 Bh6+ Kg3 Ra3+ Kh4 e3 Kxh5 Kg7 Rd5 e2 Rd4 e1=Q Rg4+ Kh8 Kxh6 Qh1+ Kg5 Qc1+ Rf4 Nf7+ Kf6 Qxf4 Kxf7 Qc7+ Kf8 Qg7+ Ke8 Re3+ Kd8 Rd3+ Kc8 Qd7+ Kb8 Rb3+ Ka8 Qa7+ Kxa7 f4 Ka8 f3 a6 f2 a7 f1=Q
e4 d5 Bb5+ c6 Bd3 dxe4 Bxe4 f5 Bf3 e5 Be2 Nf6 Nf3 e4 Ne5 Qd4 Nc4 Bc5 O-O f4 d3 f3 Be3 fxe2 Qxe2 Qd5 Bxc5 Qxc5 dxe4 Be6 Nca3 O-O Nc3 Ng4 h3 Nf6 Rfe1 Na6 e5 Nd5 Nxd5 Qxd5 Rad1 Qc5 Nb1 Qb6 b3 Qc5 c4 Rae8 Nc3 Rd8 Kh1 Nc7 f4 Qa3 Ne4 Rxd1 Rxd1 Rxf4 Nd6 b6 Qd2 Rf8 Ne4 h6 Qd6 Qxa2 Qxc7 Bf5 Rd7 Qb1+ Kh2 Bxd7 Qxd7 Qxe4
e4 e5 Nf3 Nf6 Nc3 d6 d4 exd4 Nxd4 Nc6 Nxc6 bxc6 Bc4 Be7 Bf4 O-O e5 Ng4 exd6 cxd6 O-O Ne5 Bb3 Ng6 Bg3 d5 Na4 Bf5 Rc1 Bg5 Rb1 Bf4 Bxf4 Nxf4 Nc5 Qg5 g3 Rfe8 Re1 Nh3+ Kg2 a5 Rxe8+ Rxe8 Qf3 Re2 Qc3 Rxf2+
e4 c5 g3 Nc6 Bg2 d5 exd5 Nd4 c3 Nf5 c4 e5 b3 Nd4 Nf3 Nxf3+ Bxf3 e4 Bxe4 Nf6 Bg2 g5 O-O Rg8 Bb2 Ng4 Re1+ Ne5 d4 Qe7 Rxe5
e4 e5 Nf3 d5 exd5 e4 Qe2 Nf6 d3 Qxd5 Nc3 Qa5 Bd2 Qb6 O-O-O Bc5 dxe4 O-O Bg5 Nbd7 e5 Re8 Bxf6 Nxf6 Qc4 Bxf2 exf6 Be3+ Kb1 Qxf6 Nd5 Qe6
e4 e5 Bb5 Nf6 d3 Nc6 Bxc6 dxc6 f4 Bd6 f5 h6 Nf3 Kd7 O-O Bc5+ Kh1 Qe7 a3 a5 c3 b5 b4 Ba7 c4 Ng4 c5 Ba6 Bb2 Rhd8 h3 f6 hxg4 axb4 axb4 Bxc5 bxc5 Qxc5 d4 Qc4 dxe5+ Kc8 Qe1 b4 Nbd2 Qc2 Qc1 Rxd2 Nxd2 b3 Qxc2 bxc2 Rfc1 Bb7 Rxc2 Rxa1+ Kh2 Ra2 e6 c5 Nc4 Bc6 Rd2 Bb5 Na3 Bc6 e7 Be8 Rd8+ Kb7 Rxe8 Rxb2 Rf8 Rb3 e8=Q Rxa3 Qb5+ Ka7 Qb8+ Ka6 Qa8+ Kb5 Qxa3 c4 Re8 Kc6 Re6+ Kb5 e5 c5 Qa6+ Kb4 Rd6 Kc3 Qa3+ Kc2 Qxc5 Kc3 Rc6 Kb2 Qxc4 Ka3 Ra6+ Kb2 Rb6+ Ka1 Qa4#
e4 e6 d4 Nc6 Nf3 Be7 Nc3 Nh6 h3 d5 exd5 exd5 a3 Be6 Be3 O-O Bxh6 gxh6 Qd2 Bg5 Qe2 Qf6 Qd1 a6 Bd3 Rad8 O-O Rde8 Re1 Qf4 Ne5 Bh4 Rf1 Nxe5 dxe5 d4 Ne4 Bf5 Qf3 Qxf3 gxf3 b5 Nc5 Bxd3 cxd3 Rxe5 Nd7 Re6 Nxf8 Kxf8 Rac1 Re7 Kh1 f6 Rg1 f5 Rgf1 Bg5 Rce1 Bd2 Rd1 Ba5 f4 Re2 Rb1 Ke7 f3 c5 Rbc1 Bb6 Rce1 Re3 Rxe3+ dxe3 Re1 c4 dxc4 bxc4 Re2 Ke6 Kg1 h5 Kf1 Bc5 Ke1 Bd4 Kd1 h4 Kc2 Kd5 b3 h6 bxc4+ Kxc4 Kd1 Kd3
e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Rfe1 b5 Bb3 O-O c3 d5 exd5 Nxd5 Nxe5 Nxe5 Rxe5 c6 d4 Bd6 Ree1 Qh4 g3 Qh3 Nd2 Bg4 f3 Bxg3 Qe2 Bd7 hxg3 Qxg3+ Kh1 Rae8 Ne4 Qh4+ Kg2 f5 Reh1 Qd8 Bg5 Qxg5+ Nxg5 Rxe2+ Kf1 Rxb2 Nxh7 Re8 Rae1 Rxe1+ Kxe1 Rb1+ Kf2 Rxh1 Ng5 a5 c4 Nf6 cxb5+ Nd5 bxc6 Bxc6 Ne6 a4 Bc4 Rc1 Bd3 g6 Nd8 Nb4
f4 e6 Nf3 d5 e3 c5 Nc3 c4 d3 cxd3 Qxd3 Ne7 Qb5+ Nec6 Bd2 Be7 O-O-O O-O Re1 a6 Qa4 Qa5 Bd3 Qxa4 Nxa4 b5 Nb6 Bb7 Nxa8 Bxa8 e4 Nb4 Bxb4 Bxb4 Nd2 dxe4 Bxe4 Bxe4 Rxe4 a5 a3 Bxd2+ Kxd2 Na6 Rd4 g6 Re1 Nc5 Ke3 Na4 b3 Nb6 Kf3 Nd5 c4 bxc4 bxc4 Nf6 Re5 Ra8 c5 Ne8 Rd7 Kg7 Re1 Nf6 Rb7 Nd5 Rxe6
e4 Nf6 Nc3 Nc6 a3 d5 e5 Nxe5 Qe2 Nc6 Nf3 Bg4 h3 Bd7 Ne5 Nxe5 Qxe5 e6 Bb5 Bc5 d4 Bd6 Qg5 g6 Bxd7+ Nxd7 Qe3 O-O O-O Qf6 Nb5 Rfc8 Qh6 a6 Bg5 Bf4 Bxf4 axb5 c3 e5 dxe5 Qf5 Rae1 Re8 e6 Nc5 exf7+ Qxf7 Rxe8+ Qxe8 Bxc7 Qe2 Qg5 Qd3 f3 Re8 Bb6 Ne6 Qg3 Rc8 Qd6 Ng7 Bd4 Kh8 Qe6 Qxd4+ cxd4 Nxe6 Re1 Nf4 Re5 Rc1+ Kh2 Kg8 Re7 h6 Rxb7 b4 Rxb4 Kf8 a4 Rc2 a5 Rxg2+ Kh1 Kf7 a6 Rc2 a7 Rc1+ Kh2 Rc2+ Kg3 Ne2+ Kg4 Rc8 Rb8 h5+ Kg5 Rc6 a8=Q Rd6 Qa7+ Ke6 Re8#
e4 e5 d3 d6 Nf3 h6 Nc3 Nc6 a3 a6 Be2 Nf6 Nd5 Be7 O-O O-O c3 Nh7 d4 exd4 Nxd4 Nxd4 Qxd4 c6 Nxe7+ Qxe7 Bd3 c5 Qd5 Nf6 Qb3 b5 c4 Rb8 cxb5 Be6 Qc2 axb5 b4 Rfc8 bxc5 Rxc5 Qe2 Bc4 Be3 Bxd3 Qxd3 Re5 f3 d5 Bd4 dxe4 fxe4 Rd8 Rad1 Nxe4 Bxe5 Rxd3 Rxd3 Qxe5 Rfd1 Qc5+ Rd4 Nc3 Rd3 Ne2+ Kh1 Nxd4
d4 d5 c4 c6 Nc3 Bf5 e4 dxe4 d5 cxd5 cxd5 Nf6 f3 exf3 Nxf3 Nxd5 Nxd5 e6 Ne3 Bg6 Qa4+ Nc6 Nd4 Rc8 Bb5 Qb6 Nxc6 bxc6 Be2 Bb4+ Kf2 O-O Re1 Bxe1+ Kxe1 c5 Nc4 Qb4+ Qxb4 cxb4 Bd2 Be4 g3 Bd5 Nd6 Rc6 Bxb4 Rd8 Nb7 Rb8 Na5 Rxb4 Nxc6 Bxc6 Rc1 Rb6 Bf3 Be8 Rc8 Kf8 Rc7 Rb3 axb3 g6 Rxa7 Kg7 b4 Bb5 Be2 Bc6 b5 Bd5 b6 e5 b7 Bxb7 Rxb7 e4 b4 Kf6 Rb5 Ke6 Ra5 f5 Bc4+ Kd7 b5 Kc7 Bg8 h6 Bh7 Kb6 Ra1 Kxb5 Bxg6 e3 Bxf5 Kc4 Ke2 Kd5 Rd1+ Ke5 Bh3 Ke4 Rd3 h5 Rxe3+ Kd5 g4 hxg4 Bxg4 Kc6 h4 Kd5 h5 Kd4 h6 Kd5 h7 Kc4 h8=Q
e4 c6 d4 d5 f3 e6 Bd3 Nf6 Be3 Na6 Ne2 Bd6 f4 Nxe4 g3 O-O Nbc3 Qa5 Bd2 Nxd2 Qxd2 Nb4 a3 Bd7 O-O Nxd3 b4 Nxb4 axb4 Qxb4 Nxd5 Qxd2
f4 Nf6 g3 e6 Bg2 Be7 Kf1 d5 Ke1 c5 e3 Nc6 Nf3 d4 b3 d3 Na3 dxc2 Qxc2 a6 Nc4 b5 Na5 Nxa5 Qxc5 Bxc5 Ng5 Bb7 Bxb7 Nxb7 Bb2 Bb4 Nxf7 Qxd2+ Kf1 Qxb2 Nd8 Qxa1+ Kf2 Qxh1 g4 Kxd8
e4 d5 exd5 Qxd5 d4 Qe4+ Qe2 Qxe2+ Bxe2 Nc6 Nf3 Bg4 h3 Bxf3 Bxf3 Nxd4 O-O Nxc2 Bxb7 Rb8 Bc6+ Kd8 Nc3 Nxa1 Bf4 e6 Rxa1 Bd6 Rd1 Ne7 Bxd6 Nxc6 Bg3+ Ke7 Rd2 Rhd8 Re2 Rb6 Kh2 Nd4 Re3 Nc2 Rf3 Rxb2 Bxc7
e4 e5 Nf3 Bc5 d4 exd4 Nxd4 Bf8 Bc4 c6 Qf3 d5 exd5 cxd5 Bxd5 g6 Qxf7#
d4 c5 c4 cxd4 c5 Nc6 e4 e6 Nf3 Bxc5 Be3 dxe3 fxe3 Bxe3 e5 Qc7 Bb5 Nxe5 Nc3 Nxf3+ gxf3 a6 Rf1 axb5 Nxb5 Qa5+ Ke2 Qxb5+ Kxe3 d5 Qd3 Qxd3+ Kxd3 Nf6 Rfe1 Bd7 Kc3 O-O Rad1 Rfc8+ Kd2 Rxa2 Ke2 Rxb2+ Kf1 Rcc2 Rd2 Rxd2
e4 d5 exd5 Qxd5 Nc3 Qe6+ Be2 Qf5 Nf3 e5 O-O e4 Nd4 Qe5 Ncb5 c6 f4 Qe7 Nc3 e3 dxe3 c5 Nd5 Qd6 Nb5 Qc6 Nbc7+ Qxc7 Nxc7+ Ke7 Nxa8
e4 g6 d4 b6 Be3 Bb7 Bd3 Bg7 Ne2 h5 c3 a5 O-O h4 b3 a4 b4 a3 Nd2 h3 g3 d5 e5 f6 f4 fxe5 fxe5 Nd7 Bb5 Qc8 e6 c6 exd7+ Qxd7 Bd3 c5 Qe1 cxd4 cxd4 Kd8 Qf2 Kc7 Bf4+ e5 dxe5 Kc8 Nd4 Qc7 Rac1 Qxc1 Rxc1+ Kb8 Bb5 Ka7 e6 Bxd4 Qxd4 Rh5 Be3 Kb8 Qxb6 d4 Qd6+ Ka7 Bxd4+ Rc5
e4 e5 Qh5 Nc6 Bc4 Qf6 Nf3 g6 Qg5 Bc5 Qxf6 Nxf6 d3 O-O O-O d6 Nc3 Nd4 Nxd4 Bxd4 Nb5 Bb6 Bg5 Nd7 Bh6 Re8 Rad1 a6 Na3 Bd4 c3 Bc5 b4 Bb6 d4 exd4 cxd4 Nf6 f4 Rxe4 Nc2 Bf5 Bd5 Re2 Bxb7 Rb8 Rf2 Rxc2 Rxc2 Bxc2 Rd2 Rxb7 Rxc2 Bxd4+ Kf1 Ng4 Re2 Nxh6 g3 Rxb4 a3 Ra4 Re4 c5 h3 Rxa3 g4 Rxh3 g5 Ng4 f5 Rf3+ Kg2 Rf2+ Kg3 gxf5 Re8+ Kg7 Kh4 Rh2+ Kg3 Be5+ Kf3 Kg6 Rg8+ Bg7 Kf4 Rf2+ Kg3 c4 Rc8 c3 Rc6 c2 Rxd6+ Bf6 gxf6 c1=Q Rd3 Qg1+ Kh4 Rh2+ Rh3 Qf2#
d4 d5 e3 Nf6 f3 Nc6 g4 e6 h4 e5 c3 Be7 h5 Be6 a3 a6 b4 Qd6 Kf2 exd4 cxd4 O-O Bb2 Rfe8 Bg2 Rac8 Nh3 Ra8 Qd3 Kh8 Nc3 h6 Nf4 Kg8 Bf1 Nd7 Be2 Bf6 Bd1 Nb6 Bc2 Kf8 Bc1 Bd7 Bb3 a5 b5 a4 Bxd5 Nxd5 Nfxd5 Na5 e4 Kg8 Nxa4 Bd8 Nc5 c6 bxc6 Nxc6 Nxd7 Qxd7 a4 Qd6 Bd2 Bf6 Bc3 Kh8 Kg2 Bg5 Rhe1 Kg8 Nb6 Rad8 Nc4 Qf6 e5 Qe6 Re2 Bh4 a5 Ra8 Rea2 Qd7 a6 bxa6 Rxa6 Rad8 Nd6 Rf8 Ba5 Ra8 Rxa8 Rxa8 Bc3 Rb8 Rb1 Rxb1 Qxb1 Qe6 Qb7 Qd5 Qc8+ Bd8 Qb7 Be7 Qc7 Bxd6 Qxd6 Qc4 Bb2 Nb4 Qd8+ Kh7 Qd6 Qe2+ Kg3 Qxb2
e4 e6 e5 d5 d4 c5 c3 Nc6 Nf3 Qc7 a3 Nge7 h3 Ng6 Bd3 Be7 Bxg6 hxg6 O-O Bd7 Bg5 Bxg5 Nxg5 Rh5 Qd2 Qd8 Nf3 Qe7 Qe2 O-O-O Nbd2 b6 Nb3 c4 Nbd2 Rdh8 Qd1 g5 Nh2 f5 b4 g4 b5 Nb8 Qa4 Be8 Rfe1 gxh3 Ndf3 hxg2 Kxg2 g5 Rh1 g4 Nd2 Qg7 Ndf1 f4 Qd1 g3 fxg3 fxg3 Nxg3 Rh3 Qf3 Rg8 Kxh3 Qh7+ Kg2 Bh5 Qf6 Qe4+ Kh3 Rxg3+ Kxg3 Qe3+ Nf3 Qxc3 Qxe6+ Nd7 Qg8+ Kc7 Raf1 Qd3 Rxh5 Qxf1 Qxd5 Qd3 Qd6+ Kc8 Qc6+ Kd8 Rh8+ Ke7 Qc8 Qg6+ Kf4 Qf7+ Ke4 Nf6+ exf6+ Kxf6 Rh6+ Kg7 Qh8#
e4 e5 Nf3 d6 Bc4 Be7 c3 Nf6 Qb3 O-O d3 Nc6 O-O Na5 Qa4 Nxc4 Qxc4 Bg4 Be3 Bxf3 gxf3 Nh5 d4 Bg5 Qe2 Bf4 dxe5 Qh4 Bxf4 Nxf4 Qd2 Qg5+
e4 c5 Nc3 d6 d3 Nf6 g3 g6 Bg2 Bg7 Be3 O-O Qd2 Ng4 Bf4 e5 Bg5 Qc7 h3 h6 hxg4 hxg5 Qxg5 Qb6 b3 Qb4 Ne2 Be6 O-O-O Nc6 Nd5 Bxd5 exd5 Nd4 Nxd4 cxd4 f4 Rac8 f5 Qc5 Rd2 f6 Qxg6 Qa3+ Kd1 Qxa2 Qh7+ Kf7 Qg6+ Kg8 Rh7 Rf7 Ke2 Rxc2 Rxc2 Qxc2+ Kf3 Qxd3+ Kf2 Qe3+ Kf1 Qd3+ Kg1 Qe3+ Kh2
d3 Nc6 Nc3 Nf6 Nf3 d5 Bg5 h6 Bh4 g5 Bg3 e6 e4 Bd6 e5 d4 exd6 dxc3 dxc7 Qd5 Qb1 g4 Ng1 h5 a3 h4 Bf4 e5 Bg5 Ke7 Bxf6+ Kxf6 b4 e4 Qd1 Kg5 Qc1+ Kh5 Ne2 f5 Nf4+
e4 g6 Nf3 Bg7 d4 b6 Nc3 Bb7 Bc4 e6 O-O a5 a3 Ba6 Bxa6 Nxa6 Qd3 c5 Bg5 Ne7 Rad1 O-O Qe3 Qe8 Nb5 f6 Bh6 g5 Bxg7 Kxg7 Nd6 Qh5 dxc5 Nxc5 b4 axb4 axb4 Na4 h3 e5 Nf5+ Nxf5 exf5 Qf7 Qe4 Rad8 h4 h6 hxg5 hxg5 Rfe1 Nc3 Nxg5 Nxe4 Nxf7 Rxf7 Rxe4 d5 Rg4+
e4 e6 d4 d5 exd5 exd5 Nf3 Bd6 Bd3 Ne7 a3 O-O O-O Bg4 Re1 Nd7 Nc3 c6 Bxh7+ Kh8 Bd3 f6 h3 Bf5 Rxe7 Bxd3 Re6 Bf5 Rxd6 Qc7 Bf4
d4 d6 e4 Bd7 h3 c5 Nf3 h6 Bc4 a6 Be3 e6 Qe2 b5 Bd3 c4 d5 e5 O-O Nf6 c3 cxd3 Qxd3 Be7 b4 O-O Nbd2 Nh5 g4 Nf4 Bxf4 exf4 Nh2 g5 f3 Bf6 Rfe1 Be8 Rac1 Nd7 Qb1 Qb6+ Kh1 Qf2 Ndf1 Ne5 Rc2 Qb6 Rd1 Rc8 Qb3 Nc4 Rd3 Be5 a4 bxa4 Qxa4 Bxa4
Nc3 e5 Nf3 Bd6 d4 exd4 Bg5 f6 Qxd4 Nc6 Qe4+ Nge7 Bh4 f5 Qe3 f4 Qe4 Nb4 Nd5 Nxc2+ Kd1 Nxa1 Nxc7+ Qxc7 Bxe7 Bxe7 e3 Rf8 exf4 Rxf4 Qxh7 Qd6+ Bd3 Rb4 Re1 Rxb2 Qg8#
d4 d5 f4 Bf5 Nf3 Nf6 e3 e6 Bd3 Bg4 Nbd2 Bb4 c3 Bd6 O-O O-O Qc2 Nc6 Ne5 Nxe5 fxe5 Bxe5 dxe5 Nh5 Bxh7+ Kh8 Nf3 Qg5 Nxg5 Rg8 Nxf7#
e4 c6 Nf3 d5 exd5 cxd5 d4 Bg4 h3 Bxf3 Qxf3 e6 Bb5+ Nc6 a3 a6 Bxc6+ bxc6 O-O Nf6 Nc3 Be7 Bg5 O-O Bxf6 Bxf6 Qe3 Qb6 Rfd1 Qxb2 Na4 Qxc2 Nc5 Rfd8 Nb7 Rdb8 Nc5 Qg6 Nd7 Rc8 Nxf6+ Qxf6 Rab1 Rab8 a4 a5 Qd3 g6 Qa6 Ra8 Qb6 e5 dxe5 Qxe5 Re1 Qc7 Qc5 Re8 Rec1 Rac8 Rb6 Qe7 Rxc6 Rxc6 Qxc6 d4 Qd5 Rd8 Qxa5 d3 Rd1 Qe2 Rd2 Qe1+ Kh2 Re8 Qc3 Re2 Rxd3 Qxc3 Rxc3 Rxf2 Rc8+ Kg7 a5 Rf6 Ra8 g5 a6 Kg6 a7 Kg7 Rb8
e4 c5 f4 d5 exd5 Nf6 c4 e6 dxe6 Bxe6 d3 Nc6 Nf3 Be7 Be2 O-O O-O Bf5 Nh4 Qd7 Nxf5 Qxf5 g4 Nxg4 Bxg4 Qg6 f5 Qf6 Nc3 Qd4+ Kh1 a6 Rf4 Qd8 Be3 Re8 Rf3 Ne5 Nd5 Bd6 f6 Nxg4 Bg5 Nxh2 Rh3 h6 Bxh6 g6 Bg7
d4 d5 e3 e6 f4 Bd6 Nf3 e5 fxe5 Bb4+ c3 Ba5 Bd3 f6 exf6 gxf6 O-O Nh6 Nh4 O-O Qh5 f5 Qxh6
g3 e5 Bg2 d5 c3 Nf6 Qa4+ Bd7 Qb3 Bc6 d3 Bd6 Bg5 O-O Bxf6 Qxf6 e4 d4 c4 b6 Nf3 h6 O-O Na6 Qd1 Nc5 a3 Ba4 Qd2 Nb3 Qe2 Nxa1 b4 Bd7 Qb2 Bg4 Nfd2 Be2 Re1 Bxd3 Qxa1 Bxb1 Qxb1 c5 b5 Qe6 Qd3 f5 Bh3 g6 exf5 gxf5 Qf3 Qf6 Qh5 Qg5 Qxg5+ hxg5 Nf3 g4 Bxg4 fxg4 Ng5 Rf6 Ne4 Re6 Rf1 Rf8 Kg2 Rg6 Re1 Bb8 h4 Rf3 a4 Ra3 h5 Rh6 Rh1 Rxa4 Rh4 Kf7 Nd2 Ra2 Ne4 Rc2 Rxg4 Rxh5 Ng5+ Kf6 Ne4+ Ke6 Rg6+ Kf5 Rg8 Kxe4 Rxb8 Rf5 Rb7 Rfxf2+ Kh3 Rxc4 Rxa7 Rb4 Kg4 Rxb5 Kg5 Rb3 g4 Rg2 Rg7 Rbg3 Kh5 Rxg4 Rxg4+ Rxg4 Kxg4 b5 Kg3 b4 Kf2 b3 Ke2 c4 Kd1 c3 Kc1 Kd3 Kb1 Kc4 Kc1 d3 Kb1 e4 Kc1 e3 Kb1 e2 Kc1 e1=Q#
e4 c5 Nf3 d6 c4 Nc6 Nc3 Nf6 d4 cxd4 Nxd4 e6 Be2 Be7 O-O O-O h3 Bd7 Be3 Rc8 b3 Nxd4 Bxd4 Bc6 Bf3 a6 Qe2 Nd7 Rad1 Qc7 Qe3 Nc5 Ne2 e5 Bb2 f5 exf5 Rxf5 Bg4 Rcf8 Bxf5 Rxf5 Ng3 Rg5 Bc1 Rg6 Kh1 h5 Rg1 Qc8 Kh2 h4 Ne4 Nxe4 Bb2 Bg5 Qd3 Bf4+ g3 hxg3+ fxg3 Rxg3 Rxg3 Bxg3+ Kg1 Qxh3 Rd2 Nxd2 Qxd2 Qh1#
e4 e5 Nf3 d6 Bc4 Nf6 Ng5 d5 exd5 Nxd5 Qf3 c6 Qxf7#
d4 e6 e4 Nc6 Be3 Qe7 Nd2 f5 e5 Nh6 c3 g5 Ndf3 g4 Bxh6 Bxh6 Nd2 d6 exd6 Qxd6 Ne2 b6 g3 e5 dxe5 Qd5 Rg1 Bd7 Bg2 Qc5 Nb3 Qxe5 Bxc6 Bxc6 Qd4 Qg7 Qxg7 Bxg7 O-O-O Bh6+ Kb1 O-O Nf4 Be4+ Ka1 Bxf4 gxf4 Rf7 Nd4 Rd8 Nb5 Re8 Nxa7 c6 Nxc6 Bxc6 Rd6 Rc8 Rgd1 Re7 a3 Kf7 Rh6 Kf8 Rf6+ Rf7 Rxf7+ Kxf7 Rd6 h5 Rh6 Kg7 Rxh5 Rd8 Rxf5 Rd1+ Ka2 Bd5+ b3 Rd2+ Kb1 Be4+ Kc1 Rc2+ Kd1 Bxf5 c4 Rxf2 Ke1 Rb2 b4 Rxh2 c5 b5 c6 g3 Kf1 Rc2 Kg1 Be4 Kf1 Kf6 c7 g2+ Kg1 Rc1+ Kh2 Rh1+ Kg3 g1=Q#
e4 e5 Nf3 Nc6 g3 Nh6 Bg2 Bc5 O-O d5 Nc3 d4 Nd5 Be6 c3 dxc3 dxc3 Ng4 Be3 Bxe3 Nxe3 Nxe3 fxe3 Bg4 Qa4 O-O Rad1 Qe7 h3 Rfd8 hxg4 Qc5 Rxd8+ Rxd8 Kf2 Rd3 Re1 Ne7 Qe8#
d4 e6
Nf3 Nf6 Nc3 d5 e3 e6 Be2 Nc6 a3 Ne4 O-O Be7 Nxe4 dxe4 Ne1 e5 c3 Be6 f3 exf3 Bxf3 Na5 Qa4+ c6 Be2 Nb3 Rb1 Nxc1 Rxc1 Qxd2 Ba6 Qxc1 Bxb7 Qxe3+ Kh1 O-O Bxa8 Qe2 Rg1 Rxa8 Qxc6 Rc8 Qb7 Bc5 h3 Bxg1 Kxg1 Qxe1+ Kh2 Qf2 b4 Rxc3 Qb5 Rxh3#
e4 e6 d4 d5 e5 c5 c3 Nc6 Nf3 cxd4 Nxd4 Nge7 Bb5 Bd7 Bxc6 Nxc6 Na3 Nxe5 Bf4 Qf6 Nab5 Bxb5 Nxb5 Rc8 g3 Bc5 Qa4 O-O O-O-O Bxf2 Nd6 Rb8 Rhf1 Ng4 h3 Ne3 Bxe3 Bxe3+ Kb1 Qg6+ Ka1 Qxg3 Nb5 Qxh3 Rh1 Qg3 Qc2 f5 Rde1 f4
e4 d5 e5 Be6 Qf3 Qd7 c4 Bg4 Qb3 Be6 Qxb7 Qc6 Qb4 dxc4 d3 Qd5 Be3 a5 Qc3 g6 Na3 cxd3 Bxd3 Qxg2 Qxc7 Qxh1 Bb5+ Bd7 Bxd7+ Nxd7 Qb7 Qxb7
e4 e5 Nf3 Nc6 d4 exd4 Nxd4 Nxd4 Qxd4 d6 Nc3 Nf6 Be3 c5 Qd1 Qe7 Qf3 Bg4 Qf4 g6 Nd5 Nxd5 exd5 Bf5 Bd3 Bxd3 cxd3 Bg7 Rb1 O-O O-O Rae8 d4 b6 dxc5 bxc5 Rfe1 a6 Kf1 Be5 Qh6 Bg7 Qf4 Qc7 b3 Qa5 a4 Re5 Bd2 Qb6 Bc3 Rf5 Qd2 c4 bxc4 Bxc3 Rxb6 Bxd2 Re2 Bc3 Rxd6 a5 c5 Bb4 g4 Rg5 Ra6 Rxd5 c6 Rd6 Rc2 Rc8 Rb6 Kg7 h3 Rd1+ Kg2 Rd6 Kg3 Kf6 f4 Ke7 f5 Kd8 fxg6 fxg6 h4 Kc7 Rb7+ Kd8 c7+ Kd7 h5 gxh5 gxh5 Rd3+ Kg4 Rc3 Rd2+ Kc6 Rb8 Kxc7 Rb5 Rc4+ Kf3 Bxd2 Ke2
d4 f5 c4 Nf6 e3 e6 Bd3 Be7 Nf3 O-O Nc3 d5 cxd5 exd5 Qc2 Ne4 Nxd5 Qxd5 Bc4 Nf6 Bxd5+ Nxd5
e4 e6 d4 d5 e5 a6 f4 c5 c3 Nc6 Nf3 b5 Bd3 c4 Bc2 Nh6 h3 Bd7 g4 g6 O-O Ng8 Ng5 Be7 Qf3 h6 b3 Bxg5 fxg5 Qe7 a4 O-O-O Qxf7 hxg5 Qxe7 Ngxe7 Bxg5 Rxh3 axb5 Nb8 Bxe7 Rdh8 Nd2 Bxb5 Rf8+ Rxf8 Bxf8 Rxc3 Bxg6 Rg3+ Kh2 Rxg4 Bf7 Rxd4 Bxe6+ Kd8 Nf3 Rf4 Bxd5 Rxf8 bxc4 Rxf3 Bxf3 Bxc4 Rd1+ Ke7 Bd5 Bb5 Rc1 Nd7 Rc7 Kd8 Rb7 Nxe5 Kg3 Bc6 Bxc6 Nxc6 Rb6 Kc7 Rxa6 Kb7 Ra1 Kb6 Kf4 Kb5 Ke4 Nb4 Rb1 Kc4 Rh1 Nc6 Rc1+ Kb5 Kd5 Nb4+ Kd6 Ka4 Rh1 Kb5 Rh5+ Kc4 Rc5+ Kd4 Rb5 Nd3 Ra5 Ke4 Ke6 Kd4 Kf6 Nc5 Kg6 Kc4 Kf5 Nb3 Re5 Nd4+ Ke4 Nc6 Rd5 Nb4 Rd4+ Kc5 Rd8 Kc4 Rc8+ Kb5 Kd4 Na6 Kd5 Kb6 Rc1 Nc7+ Kd6 Nb5+ Kd7 Ka5 Kc6 Na3 Kc5 Ka4 Rc3 Nb1 Rh3 Na3 Kd5 Kb4 Kc6 Nc4 Rh4 Kc3 Kd5 Nd2 Rd4 Nb3 Rh4 Kd3 Rh3+ Kc2 Kc4 Nd2+ Kd4 Kd1 Kd3 Ke1 Ke3 Nf1+ Kd3 Kf2 Ke4 Ng3+ Kf4 Ne2+ Ke4 Ng3+ Kd4 Ne2+ Kc4 Ng3 Rh2+ Kg1
e4 e5 Nf3 d5 exd5 Qxd5 Nc3 Qc5 Qe2 Bg4 Qxe5+ Qxe5+ Nxe5 f6 Nxg4 Nd7 Bc4 Ne7 d3 c6 Bf4 b5 Bb3 Nc5 O-O Rd8 Rae1 a5 a3 a4 Ba2 b4 axb4 Ne6 Rxe6
Nc3 d5 Nf3 d4 Na4 Bg4 e4 dxe3 fxe3 Bxf3 gxf3 Nc6 Qe2 a6 Qf2 Qd7 Nc3 O-O-O d4 e6 f4 Bb4 Bg2 Bxc3+ bxc3 Nge7 Bf3 f5 Bd2 h6 Rg1 g5 fxg5 hxg5 Rxg5 Rhg8 Rxg8 Rxg8 c4 Kb8 Rb1 Nc8 Qf1 Ka7 c5
e4 d6 d4 Nf6 Nc3 e6 Bd3 h6 Be3 Be7 Nf3 e5 dxe5 dxe5 O-O Bd6 Nd5 Be6 c4 Bxd5 cxd5 Nbd7 Qb3 Nb6 a4 a5 Bxb6 cxb6 Bb5+ Nd7 Rac1 Rc8 Bxd7+ Qxd7 Rfe1 O-O Qxb6 Rxc1 Rxc1 Rc8 Rxc8+ Qxc8 h4 Bb4 Kh2 Qc1 g3 Qxb2 d6 Qe2 Nxe5 Qxe4 d7 Be7 d8=Q+ Bxd8 Qxd8+ Kh7 Qxa5 Qf5 f4 Qe6 Qb5 f6 Qd3+ g6 Qxg6+
e4 e5 Nf3 Nc6 Bc4 f6 d4 Bd6 dxe5 Nxe5 Nxe5 Bxe5 Bxg8 Rxg8 f4 Bd6 Qh5+ Kf8 Qxh7 Qe7 Nc3 b6 Bd2 Bb7 O-O-O Bxe4 Nxe4 Kf7 Rhe1 Qf8 Ng5+ fxg5 Qf5#
d4 c5 dxc5 e5 e4 Bxc5 Be3 Bxe3 fxe3 Qb6 Qe2 Qxb2
f4 d5 Nf3 Bf5 e3 e6 Bb5+ c6 Be2 Na6 O-O Nc5 b3 Be7 Bb2 Bf6 Bxf6 Qxf6 Nc3 Ne7 d4 Ne4 Nxe4 Bxe4 Ne5 O-O Bd3 Bxd3 Qxd3 Qf5 c4 f6 Qxf5 Nxf5 Nd7 Rfd8 Nc5 b6 Nxe6 Re8 Nc7 Nxe3 Nxa8 Nxf1 Rxf1 Rxa8 cxd5 cxd5 Rc1 Re8 Kf2 Re4 g3 Rxd4 Rc8+ Kf7 Rc7+ Kg6 Rxa7 Rd2+ Kg1 d4 Ra6 d3 Rxb6 Rxa2 Rd6 d2 b4 Rb2
d4 d5 e3 e6 c4 Nf6 Nc3 dxc4 Bxc4 c6 e4 b5 Bb3 a5 a3 a4 Bc2 Be7 Bg5 O-O Nf3 h6 Bh4 c5 e5 Nd5 Bxe7 Qxe7 dxc5 Qxc5 Qd3 Nxc3 Qh7#
e4 d5 exd5 c5 Bb5+ Bd7 Bd3 e6 Nc3 f5 Rb1 Nf6 dxe6 Bxe6 Kf1 Bxa2 Qe1+ Be6 b3 a5 Qxe6+ Be7 Bxf5 Ra6 Qe2 g6 Qb5+ Nbd7 Qxb7 gxf5 Qxa6 Qc7 Nb5 Qc8 Qxc8+ Kf7 Qxh8 Ke6 Nf3 Nb6 Bb2 Nbd5 Rd1 f4 Nh4 Nd7 Qg8#
h3 d5 d4 e6 Bf4 Nf6 a3 Nc6 Nc3 Ne4 Nb5 Bd6 Bxd6 Nxd6 Nxd6+ Qxd6 e3 Bd7 Nf3 O-O-O Ng5 Qe7 h4 h6 Nf3 f6 c4 dxc4 Bxc4 e5 d5 Nb8 e4 g5 hxg5 fxg5 Nd2 Rdf8 Qe2 a6 O-O-O Qc5 Nf3 Bb5 b3 Qxa3+ Kd2 Qb2+ Ke1 Qc3+ Kf1 Bxc4 Nxe5 Bxe2+ Kxe2 Qxe5
b3 c5 Na3 Nc6 Nc4 d5 Nb2 Nf6 c4 e6 cxd5 exd5 Nd3 Bd6 Bb2 c4 bxc4 dxc4 Nc1 O-O Bc3 Ne4 Bxg7 Kxg7 Qc2 Bb4 Nf3 Nd4 Qxe4 Nxf3+ gxf3 Qxd2#
e4 e5 Bc4 Nf6 Nf3 Nxe4 Nxe5 d5 Qf3 Qe7 Bxd5 Ng5 Qe3 c6 Bb3 Nd7 d4 f6 Nxd7 Qxe3+ Bxe3 Bxd7 Nc3 Nf7 Bxf7+ Kxf7 Ne4 Bb4+ c3 Be7 O-O-O Rhe8 f3 Bf5 Ng3 Bg6 Bd2 Bd6 Rhe1 Bxg3 hxg3 a5 g4 a4 f4 f5 gxf5 Bxf5 Re5 Bg6 Rde1 Rxe5 fxe5 a3 b3 b5 Rf1+ Ke6 Bg5 h6 Bf6 gxf6 Rxf6+ Ke7 Rxg6 Rf8 Rxh6 Rf1+ Kd2 Rf2+ Ke3 Rxa2 Rxc6 Rb2 Ra6 Rxb3 g4 Rxc3+ Ke4 b4 g5 Rg3 Kd5 Rxg5 Kc4 Rg4 Kxb4 Rxd4+ Kxa3 Re4 Ra5 Ke6 Kb3 Re3+ Kc4 Re4+ Kd3 Rh4 Ra6+ Ke7 Ra5 Rh3+ Ke4 Rh4+ Kf5 Rh5+ Kg4 Rh1 Kf5 Rf1+ Ke4 Re1+ Kf5 Rf1+ Ke4 Re1+ Kd5 Rd1+ Ke4 Re1+ Kf4 Rf1+ Kg5 Rg1+ Kf5 Rf1+
e4 e6 Nc3 Ne7 d4 Nbc6 f3 d5 e5 Nf5 f4 Ncxd4 Nb5 c5 Nxd4 Nxd4 c3 Nc6 Bb5 Qa5 Bxc6+ bxc6 Nf3 h5 g3 Ba6 b4 Qd8 a3 Bc4 Ng5 Rb8 Be3 h4 gxh4 Be7 Bxc5 Bxc5 bxc5 Rxh4 Qd2 Kf8 O-O-O Qa5 Nf3 Qxa3+ Kc2 Qa2+ Kc1 Qb1#
e4 Nf6 e5 Ne4 f3 Nc5 d4 Nca6 Bxa6 Nxa6 a3 e6 c4 d5 c5 Be7 b4 Bh4+ g3 Be7 b5 Bxc5 bxa6 Bb6 axb7 Bxb7 Be3 O-O Ne2 Ba6 Nec3 Rb8 a4 Ba5 Bd2 c6 Na3 Bd3 Na2 Re8 Rc1 Qg5 f4 Qd8 Rxc6 Be4 Rf1 Qd7 Rd6 Qc7 Nb5 Bxd2+ Qxd2 Qc8 Kf2 a6 Na7 Qc4 Rc1 Qb3 Rc3 Qb1 Rc1 Qb7 Nc6 Ra8 Na5 Qa7 Nb4 Rab8 Rxa6 Qe7 Nbc6 Qd7 Ra7 Rb2 Qxb2 Qxa7 Nxa7 h6 Rc8 Rxc8 Nxc8 h5 Qb8 Kh7 Nd6 f5 Nxe4 fxe4 Qd6 Kh8 Qxe6 g5 f5 Kg7 Qg6+ Kf8 Qxg5 Kf7 Qxh5+ Ke7 Qh6 Kd8 Qd6+ Ke8 Qxd5 e3+ Kxe3 Kf8 Nc4 Ke7 a5 Ke8 a6 Ke7 a7 Kf8 a8=Q+ Kg7 Qa1 Kh7 Qah1 Kg7 h4 Kf8 h5 Kg7 Qdg2 Kg8 g4 Kf7 g5 Ke7 f6+ Kf8 g6 Ke8 h6 Kd7 g7 Kd8 g8=Q+ Kc7 Qg8g3 Kd7 h7 Kd8 h8=Q+ Kc7 Qh8h2 Kd7 Qh2g1 Ke8 Qg3h2 Kd7 Qg2f1 Kd8 f7 Kd7 f8=Q Kc7 Qf8f2 Kb8 e6+ Ka7 Ke4 Ka6 e7 Kb5 Ne3+ Kc6 e8=Q+ Kb7 Qeh5 Ka8 Qh5f3 Ka7 d5 Kb7 d6 Kb6 d7 Kc6 Qhh3 Kb6 d8=Q+ Kc5 Qc2+ Kb4 Qfb1+ Ka3 Qa8#
e4 Nf6 e5 Nd5 d4 d6 f4 c5 c3 Nc6 exd6 Qxd6 dxc5 Qe6+ Qe2 Qxe2+ Bxe2 Bf5 Nf3 e5 fxe5 Be7 b4 Bxb1 Rxb1 Nxc3 Ra1 Nxb4 O-O Bxc5+ Kh1 Nc2 Bd3 Nxa1 Bb2 Rd8 Bxc3 Rxd3 Bxa1 Rd7
e4 c5 Bc4 e6 Nc3 Qg5 Qf3 Nf6 Nh3 Qe5 O-O Bd6 g3 O-O Nb5 Qxe4 Qxe4 Nxe4 d3 a6 dxe4 axb5 Bxb5 Nc6 Bxc6 dxc6 Nf4 Rd8 Be3 Ra4 f3 e5 Nd3 c4 Nc5 Ra5 b4 cxb3 Nxb3 Ra4 Bb6 Re8 Rfd1 Ba3 Rd3 Bb2 Rad1 Rxa2 Rd8 Rf8 Rxf8+ Kxf8 Rd8+ Ke7 Rxc8 Bc3 Rc7+ Kf6 Rxb7 Rxc2 Bd8+ Kg6 Ba5 Bxa5 Nxa5 Rc1+ Kg2 c5 Rc7 Rc2+ Kh3 f6 Nb7 c4 Na5 c3 Nc4 Ra2 Ne3 Rf2 Rxc3 Kf7 Nf5 g5 g4 Re2 Kg3 Re1 Rc7+ Kg8 Rg7+ Kh8 Rf7 Kg8 Rxf6 Rd1 Re6 Kf7 Rxe5 Rg1+ Kf2 Rh1 Kg2 Ra1 Nd6+ Kg6 Rf5 Rb1 Nf7 h6 Ne5+ Kh7 Nd3 Rd1 Nf2 Ra1 e5 Rb1 e6 Re1 Ne4 Re2+ Kg3 h5 Nxg5+ Kh6 Ne4 Kg6 h3 hxg4 hxg4 Kg7 e7 Rxe4 fxe4 Kg6 e8=Q+ Kh6 Qe6+ Kg7 Rg5+ Kh7 Qg6+ Kh8 Qg7#
e3 d5 Qf3 Nf6 Nc3 e5 d4 Nc6 dxe5 Nxe5 Qf4 Qd6 e4 dxe4 Nxe4 Nxe4 Qxe4 f5 Qe3 Be7 Nf3 Nxf3+ gxf3 Be6 b3 O-O-O c4 Qd1#
b3 Nc6 e4 d5 Nc3 Nf6 exd5 Nxd5 Bc4 e6 Qe2 Nf4 Qf3 Nb4
g3 c6 Qg2 d5 c3 Bc7 h4 Nb6 h5 e5 g4 Qe7 g5 Bd7 d3 O-O g6 fxg6 hxg6 h6 Bxh6 gxh6 Rxh6 Qg7 Rh7 Qf6 d4 e4 f3 Ng7 fxe4 dxe4 Bxe4 Nc4 Nf3 Ne3 Qg5 Nxd1 Qxf6 Rxf6 Ng5 Bf5 Bxf5 Rxf5 Nf7 Rxf7 gxf7+ Kxh7 Nc2 Nxb2 Ne3 Rf8
e4 e5 Nf3 d6 Bc4 Qe7 a4 Be6 Qe2 Bxc4 Qxc4 Nc6 d3 g6 Nc3 Bh6 O-O Bxc1 Raxc1 f5 Qb5 Rb8 exf5 a6 Qd5 gxf5 Rfe1 Nf6 Qa2 Kd7 a5 Qg7 g3 h5 Nh4 Qg5 Qf7+ Ne7 f4 Qg4 Ng6 Rhf8 Qxe7+ Kc6 fxe5 Rfe8 Qxf6 h4 Nxh4 Qd4+ Kg2 Rf8 Qe6 Rbe8 Qc4+ Qxc4 dxc4 dxe5 b4 f4 Ng6 f3+ Kf2 Rf6 Nxe5+ Kd6 Rcd1+ Ke6 Ng4+ Kf5 Nxf6 Kxf6
e4 d5 exd5 Qxd5 Nc3 Qa5 a3 Nc6 b4 Nxb4 axb4 Qxa1 Bb5+ c6 Bc4 b5 Bd3 Nf6 Ne4 Nd5 c4 Nxb4 cxb5 Nxd3+ Kf1 Qxc1
Nh3 Nf6 Ng5 e6 b3 Bb4 Nc3 Nc6 Nb5 a6 c3 axb5 cxb4 Nd5 Bb2 Qxg5 Bxg7 Qxg7 g3 Nd4 Rc1 Rxa2 f3 Qe5 f4 Qe4 e3 Nxe3 Qe2 Nxe2 Bxe2 Qxh1+
e4 e5 Nf3 d6 d4 exd4 Nxd4 g6 Bf4 c5 Bb5+ Bd7 Qg4 Qa5+ b4 Qxb4+ c3 Qxb5 Nxb5 Bxg4 Nc7+ Kd7 Nxa8 Bg7 Nd2 Nc6 Rb1 Na5 Rb5 Bxc3 O-O Ne7 f3 Be6 Nc4 Bxc4 Rxb7+ Nxb7
e4 c6 Nc3 Nf6 Nge2 c5 Nf4 Nc6 h4 e5 Nh3 Ng4 Qxg4 h5 Qg5 f6 Qg6+ Ke7 Bc4 Rh6 Qf7+ Kd6 Nb5#
e4 e5 f3 Na6 Na3 Nf6 Ne2 Nb4 c3 Nd3#
e4 e5 Nf3 Nf6 Nxe5 Nxe4 Qf3 Ng5 Qd5 Qe7 d4 d6 Bxg5 Qxg5 Qxf7+ Kd8 Nd2 dxe5 O-O-O exd4 Kb1 Qf5 Bc4 Qxf7 Bxf7 Nc6 Nf3 Bg4 Rhe1 Bxf3 gxf3 Bd6 Bd5 Bxh2 Bxc6 bxc6 Rxd4+ Kc8 Re7 Bg1 Rdd7 Bxf2 Rxc7+ Kd8 Rxg7 Bd4 Rcd7+ Ke8 Rxd4 c5 Rdd7 h5 Rxa7 Rxa7 Rxa7 h4 Ra8+
Rb1 Qxa2 Ra1 Qc2 Rfc1 Qb3 Rcb1 Rb2 Rxb2 Qxb2 Rxa7 Rd8 Qa3 Qb1+ Kh2 Qd1 Ra8 Rxa8 Qxa8+ Kh7 Qe4+ g6 Qe3 Nc5 Qxc5 Qxd3 Nxe5 Bxe5 Qxe5 Qxc4 g4 Qc2 Kg3 Qd2 Be4 Qc3+ Qxc3 Bd5 Bxd5
e4 e5 Nf3 Nc6 Bc4 Bc5 Nc3 Nf6 d3 d6 Bg5 h6 Bh4 g5 Bg3 Bg4 Qd2 Bxf3 gxf3 Nd4 O-O-O Nxf3 Qe3 Bxe3+ fxe3 Nh4 d4 Nxe4 dxe5 Nxg3 Ne4 Nxe4 h3 d5 Bxd5 c6 Bxe4 Qb6 e6 Qxe3+ Kb1 Qxe4 Rhe1 Qg2 exf7+ Kxf7 Ref1+ Kg6 Rfg1 Qxh3 Rd6+ Kh5 Rdd1 Rhf8 Rgh1 Qf3 a3 Rae8 b3 Re2 Kb2 Qf2 Rdc1 Rd8 Rhf1 Qd4+ Kb1 Qc3 Rfd1 Rxd1
e4 e6 d4 d5 e5 c5 c3 Nc6 Nf3 Bd7 Be2 Nge7 O-O Nf5 Bd3 Nh4 Nbd2 Ng6 g3 Be7 Nb3 c4 Bxg6 fxg6 Nc5 Bxc5 dxc5 O-O b4 Qc7 Re1 h6 h4 a5 Bf4 axb4 cxb4 Nxb4 Qd2 Nd3 Bxh6 Rxf3 Qg5 Nxe1 Qxg6
d4 e6 Qd3 h5 Bf4 Nf6 Qe3 b5 Bg5 Bb4+ Nc3 Bc5 Qf3 d5 Nxb5 Qd7 e3 Qxb5 dxc5 Qxc5 Bxf6 gxf6 Qxf6 Rh7 Qd4 Qxc2 Rd1 Qc6 Nf3 Ba6 Ng5 Rh6 e4 dxe4 Qxe4 Qd7 Qf4 e5 Qxe5+ Kd8 Rxd7+ Kxd7 Bxa6 Nxa6 Ke2 f6 Qf5+ Kc6 Rc1+ Kb7 Qb5+ Kc8 Rd1 Rh7 Nxh7 Rb8 Qxa6+ Rb7 Qxf6 Rxb2+ Rd2 Rb6 Rd8+ Kb7 Qf3+ Ka6 Nf6 c6 Qd3+ Rb5 Nd7 Ka5 Qa3#
d4 d5 c4 dxc4 b3 c3 Bb2 cxb2 Qc2 bxa1=Q Qc6+ Bd7 Kd1 Qxd4+ Kc2 Qxf2 Qa6 bxa6 b4 Qh4 Nh3 Qxb4 a3 Ba4+ Kc1 Qc3+ Nxc3 Bc6 Nd5 Qxd5 e4 Qxe4 Bd3 Qxd3 Rd1 Qe2 Re1 Qxe1+ Kc2 Qh1 Ng1 Qxg1 Kd2 Qxg2+ Ke3 Qxh2 a4 a5 Kd3 Na6 Kc4 Bxa4 Kd3 Rd8+ Kc4 Qc2#
d4 d5 Nf3 c6 Bf4 Nf6 Nbd2 Qb6 c3 Qxb2 e3 Bf5 h3 Bc2 Rb1 Bxb1 Qxb1 Qb6 Qxb6 axb6 Bd3 Rxa2 O-O Na6 Bb1 Rb2 g4 h5 Bd3 hxg4 hxg4 Nxg4 Bf5 Nf6 Rb1 Ra2 Rxb6 e6 Bd3 Rh5 Rxb7 Nd7 Ra7 Be7 Bxa6 Ra1+ Kg2 f5 Ra8+ Kf7 Nb3 Ra2 Nc5 Nxc5 dxc5 Bxc5 Ne5+ Kf6 Nd7+ Ke7 Nxc5 g5 Ra7+ Kf6 Nd7+ Ke7 Ne5+ Kd6 Nf7+ Kc5 Nxg5 Rh4 Nxe6+ Kb6 Bb8 Rg4+ Kf3 Ra3 Rf7 Rxa6 Rxf5 Rg8 Bc7+ Kb7 Rf7 Raa8 Bd6+ Kb6 Rf8 Raxf8+ Bxf8 Rxf8+ Nxf8 Kb5 Ne6 c5 Nd4+ Kc4 Ne6 Kb5 e4 d4 Nxd4+ cxd4 cxd4 Kb4 d5 Kc5 Kf4 Kd6 f3 Kc5 Ke5 Kc4 d6 Kc3 d7 Kc2 d8=Q Kc1 f4 Kc2 f5
f4 Nf6 Nf3 h6 d3 d5 e3 a6 Nc3 c6 Nd4 e6 Be2 Bb4 Bd2 c5 Nf3 O-O O-O Nc6 a3 Bxc3 bxc3 Qe7 Rb1 b5 d4 c4 Ne5 Nxe5 fxe5 Ne4 Bf3 Bb7 Bxe4 dxe4 Be1 g5 Bg3 Kh7 Rf6 Rg8 Qh5 Raf8 Qxh6#
e4 e5 Nf3 Nf6 Nc3 Nc6 Bb5 Bd6 d3 a6 Ba4 h6 h3 Qe7 Nd5 Nxd5 exd5 Nd4 Nxd4 exd4+ Kf1 O-O Qh5 Rfe8 g3 g6 Qg4 Qe5 Kg2 g5 Bd2 f5 Qf3 Qe2 Qxf5 Qxd2 Qg6+ Kf8 Qxh6+ Ke7 Rae1+ Kd8 Qf6+ Be7 Rxe7 Rxe7 Qf8+ Ree8 Qf6+ Re7 Qh8+ Ree8 Qf6+ Re7 Re1 Qxe1
e4 d5 Qh5 dxe4 Bc4 e3 Qxf7+ Kd7 Qe6+ Ke8 Qf7+ Kd7 Be6+ Kc6 Bd5+ Kd7 Qf5+ Ke8 Qf7+ Kd7 dxe3 Nf6 Nf3 Nxd5 Qxd5+ Ke8 Ne5 Qxd5 f4
e3 e5 Qf3 Nf6 Qe2 Nc6 d3 d5 f4 Bd6 fxe5 Nxe5 Nf3 O-O Nxe5 Bxe5 Nc3 Bg4 Qd2 c5 h3 Bh5 g4 Bg3+ Ke2 Bg6 Rg1 Bh4 Bg2 Re8 b3 Qd6 Bb2 Rad8 Kd1 Qh2
Re1 Bxe1 Kxe1 Qg3+ Kd1 Rxe3 Kc1 Re1+ Nd1 d4 a3 Nd5 Kb1 Nc3+ Bxc3 dxc3 Qc1 Qxg2 Ka2 Rxd3 Rb1 Rdxd1 Qxd1 Rxd1 a4 Qxc2+ Ka3 Rxb1
"""
  ).linesIterator.filter(_.nonEmpty).toList
}
