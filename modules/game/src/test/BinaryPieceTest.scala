package lila.game

import chess._
import chess.Pos._
import org.specs2.mutable._

import lila.db.ByteArray
import chess.variant.Standard

class BinaryPieceTest extends Specification {

  val noop = "00000000"
  def write(all: PieceMap): List[String] =
    (BinaryFormat.piece write all).showBytes.split(',').toList
  def read(bytes: List[String]): PieceMap =
    BinaryFormat.piece.read(ByteArray.parseBytes(bytes), Standard)

  "binary pieces" should {
    "write" should {
      "empty board" in {
        write(Map.empty) must_== List.fill(64)(noop)
      }
      "A1 white king" in {
        write(Map(A1 -> White.king)) must_== {
          "00000001" :: List.fill(63)(noop)
        }
      }
      "H8 black doom" in {
        val v = write(Map(H8 -> Black.doom))
        v must_== {
          List.fill(63)(noop) :+ "00011000"
        }
      }
      "B1 black pawn" in {
        write(Map(B1 -> Black.pawn)) must_== {
          "00001110" :: List.fill(63)(noop)
        }
      }
      "A1 black knight, B1 white bishop" in {
        write(Map(A1 -> Black.knight, B1 -> White.bishop)) must_== {
          "11000101" :: List.fill(63)(noop)
        }
      }
      "A1 black knight, B1 white bishop, C1 white queen" in {
        write(Map(A1 -> Black.knight, B1 -> White.bishop, C1 -> White.queen)) must_== {
          "11000101" :: "00100000" :: List.fill(62)(noop)
        }
      }
      "H8 black knight" in {
        write(Map(H8 -> Black.knight)) must_== {
          List.fill(63)(noop) :+ "00001100"
        }
      }
      "G8 black knight, H8 white bishop" in {
        write(Map(G8 -> Black.knight, H8 -> White.bishop)) must_== {
          List.fill(63)(noop) :+ "11000101"
        }
      }
    }
    "read" should {
      "empty board" in {
        read(List.fill(64)(noop)) must_== Map.empty
        "A1 white king" in {
          read("00010000" :: List.fill(63)(noop)) must_== Map(A1 -> White.king)
        }
        "A1 white doom" in {
          read("10000000" :: List.fill(63)(noop)) must_== Map(A1 -> White.doom)
        }
        "H8 black doom" in {
          read(List.fill(63)(noop) :+ "00011000") must_== Map(H8 -> Black.doom)
        }
        "B1 black pawn" in {
          read("00001110" :: List.fill(63)(noop)) must_== Map(B1 -> Black.pawn)
        }
      }
    }
  }
}
