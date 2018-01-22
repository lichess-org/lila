package lila.game

import chess._
import chess.Pos._
import org.specs2.mutable._

import lila.db.ByteArray

class BinaryCLMTTest extends Specification {

  val _0_ = "00000000"
  def write(all: CastleLastMove): List[String] =
    (BinaryFormat.castleLastMove write all).showBytes.split(',').toList
  def read(bytes: List[String]): CastleLastMove =
    BinaryFormat.CastleLastMove read ByteArray.parseBytes(bytes)

  "binary CastleLastMove" should {
    "write" in {
      val clmt = CastleLastMove.init
      write(clmt) must_== {
        "11110000" :: _0_ :: Nil
      }
      write(clmt.copy(castles = clmt.castles without White)) must_== {
        "00110000" :: _0_ :: Nil
      }
      write(clmt.copy(castles = clmt.castles.without(Black, QueenSide))) must_== {
        "11100000" :: _0_ :: Nil
      }
      write(clmt.copy(lastMove = Some(A1 -> A2))) must_== {
        "11110000" :: "00000001" :: Nil
      }
      write(clmt.copy(lastMove = Some(B1 -> H8))) must_== {
        "11110010" :: "00111111" :: Nil
      }
      write(clmt.copy(check = Some(Pos.A1))) must_== {
        "11110000" :: _0_ :: List("00000000")
      }
      write(clmt.copy(check = Some(Pos.A3))) must_== {
        "11110000" :: _0_ :: List("00000010")
      }
      write(clmt.copy(check = Some(Pos.H8))) must_== {
        "11110000" :: _0_ :: List("00111111")
      }
    }
    "read" in {
      val clmt = CastleLastMove.init
      read("11110000" :: _0_ :: List.fill(3)(_0_)) must_== {
        clmt
      }
      read("00110000" :: _0_ :: List.fill(3)(_0_)) must_== {
        clmt.copy(castles = clmt.castles without White)
      }
      read("11100000" :: _0_ :: List.fill(3)(_0_)) must_== {
        clmt.copy(castles = clmt.castles.without(Black, QueenSide))
      }
      read("00000000" :: "00000001" :: List.fill(3)(_0_)) must_== {
        clmt.copy(castles = Castles.none, lastMove = Some(A1 -> A2))
      }
      read("11110010" :: "00111111" :: List.fill(3)(_0_)) must_== {
        clmt.copy(lastMove = Some(B1 -> H8))
      }
      read("11110000" :: _0_ :: _0_ :: _0_ :: "00000001" :: Nil) must_== clmt

      read("11110000" :: _0_ :: "00000001" :: "10000110" :: "10011111" :: Nil) must_== clmt

      read("11110000" :: _0_ :: Nil) must_== clmt

      read("11110000" :: _0_ :: List("00000010")) must_== {
        clmt.copy(check = A3.some)
      }

      read("11110000" :: _0_ :: "00000001" :: "10000110" :: "10011111" :: "00111111" :: Nil) must_== {
        clmt.copy(check = Some(H8))
      }
    }
  }
}
