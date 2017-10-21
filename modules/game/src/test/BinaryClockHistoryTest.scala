package lila.game

import org.specs2.mutable._

import chess.Centis

class BinaryClockHistoryTest extends Specification {

  val hour = Centis(60 * 60 * 100)
  val day = hour * 24

  def beLike(comp: Vector[Centis]) = (acts: Vector[Centis]) => {
    acts.size must_== comp.size
    (comp zip acts) forall {
      case (c, a) => a.centis must beCloseTo(c.centis +/- 4)
    }
  }

  "binary clock history" should {

    "handle empty vectors" in {
      BinaryFormat.clockHistory.writeSide(Centis(720000), Vector.empty, false).isEmpty must beTrue
    }

    "handle singleton vectors" in {
      val times = Vector(Centis(1234))
      val bytes = BinaryFormat.clockHistory.writeSide(Centis(12345), times, false)
      val restored = BinaryFormat.clockHistory.readSide(Centis(12345), bytes, false)

      restored must beLike(times)
    }

    "restorable" in {
      val times = Vector(
        0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45, 48, 51, 54, 57, 60, 63,
        66, 69, 72, 75, 78, 81, 84, 87, 90, 93, 96, 99, 102, 105, 108, 199, 333, 567, 666, 2000, 30
      ).map(t => Centis(21000 - 10 * t))
      val bytes = BinaryFormat.clockHistory.writeSide(hour * 2, times, false)
      val restored = BinaryFormat.clockHistory.readSide(hour * 2, bytes, false)
      restored must beLike(times)
    }

    "restore correspondence" in {
      val times = Vector(118, 204, 80, 191, 75, 230, 48, 258).map(t => day * 2 - Centis(t))
      val bytes = BinaryFormat.clockHistory.writeSide(day * 2, times, false)
      val restored = BinaryFormat.clockHistory.readSide(day * 2, bytes, false)
      restored must beLike(times)
    }

    "not drift" in {
      val times = Vector(5009, 4321, 2999, 321, 3044, 21, 2055, 77).map(Centis.apply)
      var restored = Vector.empty[Centis]
      val start = Centis(6000)
      for (end <- times) {
        val binary = BinaryFormat.clockHistory.writeSide(start, restored :+ end, false)
        restored = BinaryFormat.clockHistory.readSide(start, binary, false)
      }
      restored must beLike(times)
    }
  }
}
