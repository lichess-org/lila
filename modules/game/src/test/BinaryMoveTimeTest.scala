package lila.game

import scala.concurrent.duration._

import org.specs2.mutable._
import org.specs2.specification._

import lila.db.ByteArray

class BinaryMoveTimeTest extends Specification {

  val _0_ = "00000000"
  def write(c: List[Float]): List[String] =
    (BinaryFormat.moveTime write c).toString.split(',').toList
  def read(bytes: List[String]): List[Float] =
    BinaryFormat.moveTime read ByteArray.parseBytes(bytes)
  def isomorphism(c: List[Float]): List[Float] =
    BinaryFormat.moveTime read (BinaryFormat.moveTime write c)

  "binary move times" should {
    "write" in {
      write(List(0f, 1f, 10f, 0.5f)) must_== {
        "00000010" :: "10100001" :: Nil
      }
      write(List(0f, 1f, 10f, 0.5f, 60f)) must_== {
        "00000010" :: "10100001" :: "11110000" :: Nil
      }
    }
    "read" in {
      read("00000010" :: "10100001" :: Nil) must_== {
        List(0f, 1f, 10f, 0.5f)
      }
      read("00000010" :: "10100001" :: "11110000" :: Nil) must_== {
        List(0f, 1f, 10f, 0.5f, 60f, 0f)
      }
    }
  }
}
