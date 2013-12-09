package lila.game

import scala.concurrent.duration._

import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryMoveTimeTest extends Specification {

  val _0_ = "00000000"
  type MT = Int
  def write(c: Vector[MT]): List[String] =
    (BinaryFormat.moveTime write c).showBytes.split(',').toList
  def read(bytes: List[String]): Vector[MT] =
    BinaryFormat.moveTime read ByteArray.parseBytes(bytes)
  def isomorphism(c: Vector[MT]): Vector[MT] =
    BinaryFormat.moveTime read (BinaryFormat.moveTime write c)

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
      read("00000010" :: "10100001" :: Nil) must_== {
        Vector(1, 10, 100, 5)
      }
      read("00000010" :: "10100001" :: "11110000" :: Nil) must_== {
        Vector(1, 10, 100, 5, 600, 1)
      }
    }
  }
}
