package lila.game

import org.specs2.mutable._

import lila.db.ByteArray
import shogi.Centis

class BinaryMoveTimeTest extends Specification {

  val _0_ = "00000000"
  def write(c: Vector[Int]): List[String] =
    (BinaryFormat.moveTime write c.map(Centis(10) * _)).showBytes.split(',').toList
  def read(bytes: List[String], plies: Int): Vector[Int] =
    BinaryFormat.moveTime.read(ByteArray.parseBytes(bytes), plies) map (_.roundTenths)

  "binary move times" should {
    "write" in {
      write(Vector(1, 10, 100, 5)) must_== {
        "00000010" :: "10100001" :: Nil
      }
      write(Vector(1, 10, 100, 5, 600)) must_== {
        "00000010" :: "10100001" :: "11110000" :: Nil
      }
    }
    "read" in {
      read("00000010" :: "10100001" :: Nil, 4) must_== {
        Vector(1, 10, 100, 5)
      }
      read("00000010" :: "10100001" :: "11110000" :: Nil, 6) must_== {
        Vector(1, 10, 100, 5, 600, 1)
      }
    }
    "buckets - long game" in {
      val times = Vector(
        0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45, 48, 51, 54, 57, 60, 63, 66, 69, 72, 75,
        78, 81, 84, 87, 90, 93, 96, 99, 102, 105, 108, 199, 333, 567, 666, 2000
      ).map(Centis(10) * _)
      val rounded = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(times), times.size)
      val expected = Vector(
        1, 1, 5, 10, 10, 15, 20, 20, 20, 30, 30, 30, 40, 40, 40, 40, 50, 50, 50, 60, 60, 60, 60, 60, 80, 80,
        80, 80, 80, 80, 80, 100, 100, 100, 100, 100, 100, 200, 300, 600, 600, 600
      ).map(Centis(10) * _)
      rounded must_== expected
      val again = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(rounded), times.size)
      again must_== rounded
    }
    "buckets - short game" in {
      val times    = Vector(0, 30, 60, 90).map(Centis.apply)
      val rounded  = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(times), 4)
      val expected = Vector(10, 10, 50, 100).map(Centis.apply)
      rounded must_== expected
      val again = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(rounded), 4)
      again must_== rounded
    }
    "buckets - short game - odd number of moves" in {
      val times    = Vector(0, 30, 60).map(Centis.apply)
      val rounded  = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(times), 3)
      val expected = Vector(10, 10, 50).map(Centis.apply)
      rounded must_== expected
      val again = BinaryFormat.moveTime.read(BinaryFormat.moveTime.write(rounded), 3)
      again must_== rounded
    }
  }
}
