package shogi
package format.pgn

import scala._

class BinaryTest extends ShogiTest {

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
        writeMove("Pa1") must_== "00000000,00000000"
        writeMove("Pa2") must_== "00000001,00000000"
        writeMove("Pa9") must_== "00001000,00000000"
        writeMove("Pb1") must_== "00001001,00000000"
        writeMove("Ph1") must_== "00111111,00000000"
        writeMove("Ph4") must_== "01000010,00000000"
      }
      "simple piece" in {
        writeMove("Ka1") must_== "00000000,00010000"
        writeMove("Sa2") must_== "00000001,00110000"
        writeMove("Rh4") must_== "01000010,01110000"
      }
      "simple piece with capture" in {
        writeMove("Kxa1") must_== "00000000,00010010"
        writeMove("Sxa2") must_== "00000001,00110010"
        writeMove("Rxh4") must_== "01000010,01110010"
      }
      "piece with promotion" in {
        writeMove("Pa1+") must_== "00000000,00000100"
        writeMove("Ph4+") must_== "01000010,00000100"
        writeMove("Ph4=") must_== "01000010,00001000"
      }
      "piece promotion with capture" in {
        writeMove("Pxa1+") must_== "00000000,00000110"
        writeMove("Pxh4+") must_== "01000010,00000110"
        writeMove("Pxh4=") must_== "01000010,00001010"
      }
      "drop" in {
        writeMove("N*a1") must_== "00000000,10000001"
        writeMove("R*i9") must_== "01010000,11100001"
      }
      "disambiguated fully" in {
        writeMove("Kf4a1") must_== "10000000,00010001,00110000"
      }
      "disambiguated fully with capture" in {
        writeMove("Kf4xa1") must_== "10000000,00010011,00110000"
      }
      "disambiguated fully with promotion" in {
        writeMove("Gi9e5+") must_== "10101000,00100101,01010000"
        writeMove("Gi9e5=") must_== "10101000,00101001,01010000"
      }
      "disambiguated fully with capture and promotion" in {
        writeMove("Sc3xc4+") must_== "10010101,00110111,00010100"
        writeMove("Sc3xc4=") must_== "10010101,00111011,00010100"
        writeMove("Rc3xc4=") must_== "10010101,01111011,00010100"
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
        readMove("00000000,00000000") must_== "Pa1"
        readMove("00000001,00000000") must_== "Pa2"
        readMove("00000010,00000000") must_== "Pa3"
        readMove("01000010,00000000") must_== "Ph4"
      }
      "simple piece" in {
        readMove("00000000,00010000") must_== "Ka1"
        readMove("00000001,11010000") must_== "Da2"
        readMove("01000010,10110000") must_== "Ah4"
      }
      "simple piece with capture" in {
        readMove("00000000,00010010") must_== "Kxa1"
        readMove("00000001,10000010") must_== "Txa2"
        readMove("01000010,01110010") must_== "Rxh4"
      }
      "simple piece with promotion" in {
        readMove("00000000,00000100") must_== "Pa1+"
        readMove("01000010,01110100") must_== "Rh4+"
      }
      "simple piece with unpromotion" in {
        readMove("00000000,00001000") must_== "Pa1="
        readMove("01000010,01111000") must_== "Rh4="
      }
      "simple piece with capture and promotion" in {
        readMove("00000000,00000110") must_== "Pxa1+"
        readMove("01000010,01110110") must_== "Rxh4+"
      }
      "simple piece with capture and unpromotion" in {
        readMove("00000000,00001010") must_== "Pxa1="
        readMove("01000010,01111010") must_== "Rxh4="
      }
      "drop" in {
        readMove("00000000,10000001") must_== "N*a1"
        readMove("01010000,11100001") must_== "R*i9"
      }
      "disambiguated fully" in {
        readMove("10000000,00010001,00110000") must_== "Kf4a1"
      }
      "disambiguated fully with capture" in {
        readMove("10000000,00010011,00110000") must_== "Kf4xa1"
      }
      "disambiguated fully with promotion" in {
        readMove("10000000,00010101,00110000") must_== "Kf4a1+"
      }
      "disambiguated fully with unpromotion" in {
        readMove("10000000,00011001,00110000") must_== "Kf4a1="
      }
      "disambiguated fully with capture and promotion" in {
        readMove("10000000,00010111,00110000") must_== "Kf4xa1+"
      }
      "disambiguated fully with capture and unpromotion" in {
        readMove("10000000,00011011,00110000") must_== "Kf4xa1="
      }
    }
    "be isomorphic" in {
      "for one" in {
        compareStrAndBin(pgn200.head)
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

  val pgn200: List[String] = format.pgn.Fixtures.prod500standard.take(200)
}
