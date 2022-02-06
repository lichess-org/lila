package shogi
package format.usi

import scala._

import variant._

class BinaryTest extends ShogiTest {

  import BinaryTestUtils._

  def compareStrAndBin(usisStr: String) = {
    val bin = Binary.encodeMoves(Usi.readList(usisStr).get, Standard).toVector
    Binary.decodeMoves(bin, Standard).map(_.usi).mkString(" ") must_== usisStr
  }

  "binary encoding" should {
    "util test" in {
      showByte(parseBinary("00000101")) must_== "00000101"
      showByte(parseBinary("10100000")) must_== "10100000"
    }
    "write single move" in {
      "simple move" in {
        encodeMove("1a1b") must_== "00000000,00001001"
        encodeMove("1b1a") must_== "00001001,00000000"
        encodeMove("1a2a") must_== "00000000,00000001"
        encodeMove("7g7f") must_== "00111100,00110011"
        encodeMove("8h2b") must_== "01000110,00001010"
        encodeMove("1i1h") must_== "01001000,00111111"
        encodeMove("1i2i") must_== "01001000,01001001"
        encodeMove("9a8a") must_== "00001000,00000111"
        encodeMove("9a9b") must_== "00001000,00010001"
        encodeMove("9i8i") must_== "01010000,01001111"
        encodeMove("9h9i") must_== "01000111,01010000"
        encodeMove("1a9i") must_== "00000000,01010000"
      }
      "move with promotion symbols" in {
        encodeMove("8h2b+") must_== "01000110,10001010"
        encodeMove("8h2b=") must_== "01000110,00001010"
      }
      "drop" in {
        encodeMove("P*5e") must_== "10000001,00101000"
        encodeMove("L*6c") must_== "10000010,00010111"
        encodeMove("N*3d") must_== "10000011,00011101"
        encodeMove("S*2b") must_== "10000100,00001010"
        encodeMove("G*9a") must_== "10000101,00001000"
        encodeMove("B*9i") must_== "10000110,01010000"
        encodeMove("R*1i") must_== "10000111,01001000"
      }
      "simple move minishogi" in {
        encodeMove("1a1b", Minishogi) must_== "00000000,00000101"
        encodeMove("1a2a", Minishogi) must_== "00000000,00000001"
        encodeMove("4e5e", Minishogi) must_== "00010111,00011000"
        encodeMove("5a1e", Minishogi) must_== "00000100,00010100"
        encodeMove("B*3d", Minishogi) must_== "10000110,00010001"
      }
    }
    "write many moves" in {
      "all games" in {
        forall(format.usi.Fixtures.prod500standard) { usisStr =>
          val bin = Binary.encodeMoves(Usi.readList(usisStr).get, Standard).toList
          bin.size must be_<=(usisStr.size)
        }
      }
    }
    "read single move" in {
      "simple move" in {
        decodeMove("00000000,00001001") must_== "1a1b"
        decodeMove("00001001,00000000") must_== "1b1a"
        decodeMove("00000000,00000001") must_== "1a2a"
        decodeMove("01000111,01010000") must_== "9h9i"
        decodeMove("00111100,00110011") must_== "7g7f"
      }
      "simple move with promotion" in {
        decodeMove("01000110,10001010") must_== "8h2b+"
        decodeMove("00000000,11010000") must_== "1a9i+"
      }
      "drop" in {
        decodeMove("10000011,00011101") must_== "N*3d"
        decodeMove("10000111,01010000") must_== "R*9i"
      }
    }
    "be isomorphic" in {
      "for one" in {
        compareStrAndBin(format.usi.Fixtures.prod500standard.head)
      }
      "for all" in {
        forall(format.usi.Fixtures.prod500standard)(compareStrAndBin)
      }
    }
    "for all move combinations" in {
      val allMoves = for {
        orig <- Pos.all
        dest <- Pos.all
      } yield Usi.Move(orig, dest, true)
      forall(allMoves.map(_.usiKeys))(compareStrAndBin)
      forall(allMoves.map(_.usi))(compareStrAndBin)
    }
    "for all drop combinations" in {
      val allDrops = for {
        role <- Role.all
        pos  <- Pos.all
      } yield Usi.Drop(role, pos)
      forall(allDrops.map(_.usi))(compareStrAndBin)
    }
  }

}

object BinaryTestUtils {

  def showByte(b: Byte): String =
    "%08d" format {
      b & 0xff
    }.toBinaryString.toInt

  def encodeMove(m: String, variant: Variant = Standard): String =
    Binary.encodeMoves(List(Usi(m).get), variant) map showByte mkString ","

  def decodeMove(m: String, variant: Variant = Standard): String =
    decodeMoves(m, variant).head

  def decodeMoves(m: String, variant: Variant = Standard): Seq[String] =
    Binary.decodeMoves(m.split(',').toList.map(parseBinary), variant).map(_.usi)

  def parseBinary(s: String): Byte = {
    var i    = s.size - 1
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
