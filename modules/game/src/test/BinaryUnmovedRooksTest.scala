package lila.game

import scala.concurrent.duration._

import chess._
import chess.Pos._
import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryUnmovedRooksTest extends Specification {

  val _0_ = "00000000"
  val _1_ = "11111111"
  def write(all: UnmovedRooks): List[String] =
    (BinaryFormat.unmovedRooks write all).showBytes.split(',').toList
  def read(bytes: List[String]): UnmovedRooks =
    BinaryFormat.unmovedRooks read ByteArray.parseBytes(bytes)

  "binary unmovedRooks" should {
    "write" in {
      write(UnmovedRooks(Set(A1, H1, A8, H8))) must_== {
        List("10000001", "10000001")
      }
      write(UnmovedRooks(Set.empty)) must_== {
        List(_0_, _0_)
      }
      write(UnmovedRooks.default) must_== {
        List(_1_, _1_)
      }
      write(UnmovedRooks(Set(A1, B1, C1))) must_== {
        List("11100000", _0_)
      }
      write(UnmovedRooks(Set(A8, B8, C8))) must_== {
        List(_0_, "11100000")
      }
    }
    "read" in {
      read(List("10000001", "10000001")) must_== {
        UnmovedRooks(Set(A1, H1, A8, H8))
      }
      read(List(_0_, _0_)) must_== {
        UnmovedRooks(Set.empty)
      }
      read(List(_1_, _1_)) must_== {
        UnmovedRooks.default
      }
      read(List("11100000", _0_)) must_== {
        UnmovedRooks(Set(A1, B1, C1))
      }
      read(List(_0_, "11100000")) must_== {
        UnmovedRooks(Set(A8, B8, C8))
      }
    }
  }
}
