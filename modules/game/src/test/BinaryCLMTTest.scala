package lila.game

import scala.concurrent.duration._

import chess._
import chess.Pos._
import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryCLMTTest extends Specification {

  val _0_ = "00000000"
  def write(all: CastleLastMoveTime): List[String] =
    (BinaryFormat.castleLastMoveTime write all).showBytes.split(',').toList
  def read(bytes: List[String]): CastleLastMoveTime =
    BinaryFormat.castleLastMoveTime read ByteArray.parseBytes(bytes)

  "binary CastleLastMoveTime" should {
    "write" in {
      val clmt = CastleLastMoveTime.init
      write(clmt) must_== {
        "11110000" :: _0_ :: List.fill(3)(_0_)
      }
      write(clmt.copy(castles = clmt.castles without White)) must_== {
        "00110000" :: _0_ :: List.fill(3)(_0_)
      }
      write(clmt.copy(castles = clmt.castles.without(Black, QueenSide))) must_== {
        "11100000" :: _0_ :: List.fill(3)(_0_)
      }
      write(clmt.copy(lastMove = Some(A1 -> A2))) must_== {
        "11110000" :: "00000001" :: List.fill(3)(_0_)
      }
      write(clmt.copy(lastMove = Some(B1 -> H8))) must_== {
        "11110010" :: "00111111" :: List.fill(3)(_0_)
      }
      write(clmt.copy(lastMoveTime = Some(1))) must_== {
        "11110000" :: _0_ :: _0_ :: _0_ :: "00000001" :: Nil
      }
      write(clmt.copy(lastMoveTime = Some(2))) must_== {
        "11110000" :: _0_ :: _0_ :: _0_ :: "00000010" :: Nil
      }
      write(clmt.copy(lastMoveTime = Some(99999))) must_== {
        "11110000" :: _0_ :: "00000001" :: "10000110" :: "10011111" :: Nil
      }
      write(clmt.copy(check = Some(Pos.A1))) must_== {
        "11110000" :: _0_ :: List.fill(3)(_0_) ::: List("00000000")
      }
      write(clmt.copy(check = Some(Pos.A3))) must_== {
        "11110000" :: _0_ :: List.fill(3)(_0_) ::: List("00000010")
      }
      write(clmt.copy(check = Some(Pos.H8))) must_== {
        "11110000" :: _0_ :: List.fill(3)(_0_) ::: List("00111111")
      }
      write(clmt.copy(lastMoveTime = Some(99999), check = Some(Pos.H8))) must_== {
        "11110000" :: _0_ :: "00000001" :: "10000110" :: "10011111" :: "00111111" :: Nil
      }
    }
    "read" in {
      val clmt = CastleLastMoveTime.init
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
      read("11110000" :: _0_ :: _0_ :: _0_ :: "00000001" :: Nil) must_== {
        clmt.copy(lastMoveTime = Some(1))
      }
      read("11110000" :: _0_ :: _0_ :: _0_ :: "00000010" :: Nil) must_== {
        clmt.copy(lastMoveTime = Some(2))
      }
      read("11110000" :: _0_ :: "00000001" :: "10000110" :: "10011111" :: Nil) must_== {
        clmt.copy(lastMoveTime = Some(99999))
      }
      read("11110000" :: _0_ :: List.fill(3)(_0_) ::: List("00000010")) must_== {
        clmt.copy(check = A3.some)
      }
      read("11110000" :: _0_ :: "00000001" :: "10000110" :: "10011111" :: "00111111" :: Nil) must_== {
        clmt.copy(lastMoveTime = Some(99999), check = Some(H8))
      }
    }
  }
}
