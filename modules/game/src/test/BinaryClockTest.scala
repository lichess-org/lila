package lila.game

import scala.concurrent.duration._

import chess.{ Centis, Clock }
import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryClockTest extends Specification {

  val _0_ = "00000000"
  val since = org.joda.time.DateTime.now.minusHours(1)
  def write(c: Clock): List[String] =
    (BinaryFormat.clock(since) write c).showBytes.split(',').toList
  def read(bytes: List[String]): Clock =
    (BinaryFormat.clock(since).read(ByteArray.parseBytes(bytes), false, false))(chess.White)
  def isomorphism(c: Clock): Clock =
    (BinaryFormat.clock(since).read(BinaryFormat.clock(since) write c, false, false))(chess.White)

  "binary Clock" should {
    val clock = Clock(120, 2)
    val bits22 = List("00000010", "00000010")
    "write" in {
      write(clock) must_== {
        bits22 ::: List.fill(10)(_0_)
      }
      write(clock.giveTime(chess.White, Centis(3))) must_== {
        bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(7)(_0_)
      }
      write(clock.giveTime(chess.White, Centis(-3))) must_== {
        bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(7)(_0_)
      }
      write(Clock(0, 3)) must_== {
        List("00000000", "00000011", "10000000", "00000001", "00101100", "10000000", "00000001", "00101100") ::: List.fill(4)(_0_)
      }
    }
    "read" in {
      read(bits22 ::: List.fill(11)(_0_)) must_== {
        clock
      }
      read(bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(8)(_0_)) must_== {
        clock.giveTime(chess.White, Centis(3))
      }
      read(bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(8)(_0_)) must_== {
        clock.giveTime(chess.White, Centis(-3))
      }
    }
    "isomorphism" in {

      isomorphism(clock) must_== clock

      val c2 = clock.giveTime(chess.White, Centis.ofSeconds(15))
      isomorphism(c2) must_== c2

      val c3 = clock.giveTime(chess.Black, Centis.ofSeconds(5))
      isomorphism(c3) must_== c3

      val c4 = clock.start
      isomorphism(c4).timerOption.get.value must beCloseTo(c4.timerOption.get.value, 10)

      Clock(120, 60) |> { c =>
        isomorphism(c) must_== c
      }
    }
  }
}
