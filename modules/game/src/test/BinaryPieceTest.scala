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
      "A1 white doom" in {
        val v = write(Map(A1 -> White.doom))
        v must_== {
          "00001000" :: List.fill(63)(noop)
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
          "00000000" :: "00010110" :: List.fill(62)(noop)
        }
      }
      "A1 black knight, B1 white bishop" in {
        write(Map(A1 -> Black.knight, B1 -> White.bishop)) must_== {
          "00010100" :: "00000101" :: List.fill(62)(noop)
        }
      }
      "A1 black knight, B1 white bishop, C1 white queen" in {
        write(Map(A1 -> Black.knight, B1 -> White.bishop, C1 -> White.queen)) must_== {
          "00010100" :: "00000101" :: "00000010" :: List.fill(61)(noop)
        }
      }
      "H8 black knight" in {
        write(Map(H8 -> Black.knight)) must_== {
          List.fill(63)(noop) :+ "00010100"
        }
      }
      "G8 black knight, H8 white bishop" in {
        write(Map(G8 -> Black.knight, H8 -> White.bishop)) must_== {
          List.fill(62)(noop) :+ "00010100" :+ "00000101"
        }
      }
    }
    "read" should {
      "empty board" in {
        read(List.fill(64)(noop)) must_== Map.empty
        "A1 white king" in {
          read("00000001" :: List.fill(63)(noop)) must_== Map(A1 -> White.king)
        }
        "A1 white doom" in {
          read("00001000" :: List.fill(63)(noop)) must_== Map(A1 -> White.doom)
        }
        "H8 black doom" in {
          read(List.fill(63)(noop) :+ "00011000") must_== Map(H8 -> Black.doom)
        }
        "B1 black pawn" in {
          read("00000000" :: "00010110" :: List.fill(62)(noop)) must_== Map(B1 -> Black.pawn)
        }
      }
    }
  }
}
