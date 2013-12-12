package lila.game

import scala.concurrent.duration._

import chess.Clock
import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryClockTest extends Specification {

  val _0_ = "00000000"
  def write(c: Clock): List[String] =
    (BinaryFormat.clock write c).showBytes.split(',').toList
  def read(bytes: List[String]): Clock =
    (BinaryFormat.clock read ByteArray.parseBytes(bytes))(chess.White)
  def isomorphism(c: Clock): Clock =
    (BinaryFormat.clock read (BinaryFormat.clock write c))(chess.White)

  "binary Clock" should {
    val clock = Clock(120, 2)
    val bits22 = List("00000010", "00000010")
    "write" in {
      write(clock) must_== {
        bits22 ::: List.fill(11)(_0_)
      }
      write(clock.giveTime(chess.White, 0.03f)) must_== {
        bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(8)(_0_)
      }
      write(clock.giveTime(chess.White, -0.03f)) must_== {
        bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(8)(_0_)
      }
      write(Clock(0, 2).pp) must_== {
        List("00000000", "00000010", "10000000", "00000000", "11001000", "10000000", "00000000", "11001000") ::: List.fill(5)(_0_)
      }
    }
    "read" in {
      read(bits22 ::: List.fill(11)(_0_)) must_== {
        clock
      }
      read(bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(8)(_0_)) must_== {
        clock.giveTime(chess.White, 0.03f)
      }
      read(bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(8)(_0_)) must_== {
        clock.giveTime(chess.White, -0.03f)
      }
    }
    "isomorphism" in {

      isomorphism(clock) must_== clock

      val c2 = clock.giveTime(chess.White, 15)
      isomorphism(c2) must_== c2

      val c3 = clock.giveTime(chess.Black, 5)
      isomorphism(c3) must_== c3

      val c4 = clock.run
      isomorphism(c4).timerOption.get must beCloseTo(c4.timerOption.get, 1)

      Clock(2, 60) |> { c =>
        isomorphism(c) must_== c
      }
    }
  }
}
