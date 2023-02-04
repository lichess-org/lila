package lila.game

import shogi.{ Centis, Clock, Sente }
import org.specs2.mutable._
import scala.util.chaining._

import lila.db.ByteArray

class BinaryClockTest extends Specification {

  val _0_                  = "00000000"
  val since                = org.joda.time.DateTime.now.minusHours(1)
  def writeBytes(c: Clock) = BinaryFormat.clock(since) write c
  def readBytes(
      bytes: ByteArray,
      periodEntries: PeriodEntries = PeriodEntries.default,
      berserk: Boolean = false
  ): Clock =
    (BinaryFormat.clock(since).read(bytes, periodEntries, berserk, false))(Sente)
  def isomorphism(c: Clock): Clock = readBytes(writeBytes(c))

  def write(c: Clock): List[String] = writeBytes(c).showBytes.split(',').toList
  def read(bytes: List[String])     = readBytes(ByteArray.parseBytes(bytes))

  "binary Clock" should {
    val clock  = Clock(120, 2, 10, 1)
    val bits22 = List("00000010", "00000010")
    val bitsA1 = List("00001010", "00000001")
    "write" in {
      "basic clock" in {
        write(clock) must_== {
          bits22 ::: List.fill(6)(_0_) ::: bitsA1
        }
      }
      "giving 3 time" in {
        write(clock.giveTime(Sente, Centis(3))) must_== {
          bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(3)(_0_) ::: bitsA1
        }
      }
      "giving -3 time" in {
        write(clock.giveTime(Sente, Centis(-3))) must_== {
          bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(3)(_0_) ::: bitsA1
        }
      }
      "inc only" in {
        write(Clock(0, 5, 0, 0)) must_== {
          List(
            "00000000",
            "00000101",
            "10000000",
            "00000001",
            "11110100",
            "10000000",
            "00000001",
            "11110100",
            _0_,
            _0_
          )
        }
      }
    }
    "read" in {
      "with timer" in {
        read(bits22 ::: List.fill(10)(_0_) ::: bitsA1) must_== {
          clock
        }
        read(bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(7)(_0_) ::: bitsA1) must_== {
          clock.giveTime(Sente, Centis(3))
        }
        read(bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(7)(_0_) ::: bitsA1) must_== {
          clock.giveTime(Sente, Centis(-3))
        }
      }
      "without timer bytes" in {
        read(bits22 ::: List.fill(6)(_0_) ::: bitsA1) must_== {
          clock
        }
        read(bits22 ::: List("10000000", "00000000", "00000011") ::: List.fill(3)(_0_) ::: bitsA1) must_== {
          clock.giveTime(Sente, Centis(3))
        }
        read(bits22 ::: List("00000000", "00000000", "00000011") ::: List.fill(3)(_0_) ::: bitsA1) must_== {
          clock.giveTime(Sente, Centis(-3))
        }
      }
    }
    "isomorphism" in {

      "without berserk" in {
        isomorphism(clock) must_== clock

        val c2 = clock.giveTime(Sente, Centis.ofSeconds(15))
        isomorphism(c2) must_== c2

        val c3 = clock.giveTime(shogi.Gote, Centis.ofSeconds(5))
        isomorphism(c3) must_== c3

        val c4 = clock.start
        isomorphism(c4).timer.get.value must beCloseTo(c4.timer.get.value, 10L)

        Clock(120, 60, 0, 0) pipe { c =>
          isomorphism(c) must_== c
        }

        val c5 = Clock(15, 0, 10, 1).giveTime(Sente, Centis.ofSeconds(-20)).start
        isomorphism(c5).timer.get.value must beCloseTo(c5.timer.get.value, 10L)
        isomorphism(c5).currentClockFor(Sente) pipe { cc =>
          cc.periods must_== 1
          cc.time.centis must beCloseTo(500, 10)
        }
      }

      "with berserk" in {
        val b1 = clock.goBerserk(Sente)
        readBytes(writeBytes(b1), berserk = true) must_== b1

        val b2 = clock.giveTime(Sente, Centis(15)).goBerserk(Sente)
        readBytes(writeBytes(b2), berserk = true) must_== b2

        val b3 = Clock(60, 2, 0, 0).goBerserk(Sente)
        readBytes(writeBytes(b3), berserk = true) must_== b3
      }

      "periods" in {
        val b1 = Clock(60, 0, 10, 2).updatePlayer(Sente)(_.spendPeriods(1))
        val pe = PeriodEntries.default.update(Sente, _ => Vector(5))
        readBytes(writeBytes(b1), periodEntries = pe) must_== b1
      }
    }
  }
}
